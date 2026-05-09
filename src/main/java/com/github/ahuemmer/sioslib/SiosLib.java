package com.github.ahuemmer.sioslib;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

public class SiosLib implements AutoCloseable {

    private SerialPort siosLabPort;

    private static final Logger LOG = LogManager.getLogger(SiosLib.class);

    protected static final byte[] TEST_DATA_SEQUENCE = new byte[] {0x00, 0x00, 0x00, 0x01};
    protected static final byte[] SWITCH_MODE_SEQUENCE_PART_1 =
            new byte[] {0x64, 0x1B, 0x03, -0x01}; // -1 means 255 as unsigned byte
    protected static final byte[] SWITCH_MODE_SEQUENCE_PART_2 = new byte[] {0x66, 0x1B};
    protected static final byte CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE = 0x10;
    protected static final byte CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE = 0x51;
    protected static final byte CONTROL_SET_ANALOG_OUTPUT_1_TEN_BITS_SIOS_MODE = 0x48;
    protected static final byte CONTROL_SET_ANALOG_OUTPUT_2_TEN_BITS_SIOS_MODE = 0x49;
    protected static final byte CONTROL_SET_ANALOG_OUTPUT_1_EIGHT_BITS_SIOS_MODE = 0x40;
    protected static final byte CONTROL_SET_ANALOG_OUTPUT_2_EIGHT_BITS_SIOS_MODE = 0x41;
    protected static final byte CONTROL_SET_INPUT_DIGITAL_SIOS_MODE = 0x20;
    protected static final byte CONTROL_SET_INPUT_DIGITAL_COMPULAB_MODE = -0x2D;
    protected static final byte CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE = 0x38;
    protected static final byte CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE = 0x39;
    protected static final byte CONTROL_SET_INPUT_ANALOG_1_COMPULAB_MODE = 0x3C;
    protected static final byte CONTROL_SET_INPUT_ANALOG_2_COMPULAB_MODE = 0x3A;
    protected static final byte CONTROL_SET_MODE_SIOS = 0x00;
    protected static final byte CONTROL_SET_MODE_COMPULAB = 0x01;
    protected static final byte CONTROL_GET_NEXT_BYTE = 0x01;

    protected static final byte MODE_INDICATOR_SIOSLAB = 0x0A;
    protected static final byte MODE_INDICATOR_COMPULAB = -0x37;

    protected static final int POLLING_INTERVAL = 50; // ms
    protected static final int RECEIVE_TIMEOUT = 250; // ms

    protected static final int BAUD_RATE = 19200;
    protected static final int NUM_DATABITS = 8;

    private SiosLabMode siosLabMode = SiosLabMode.SIOS_MODE;

    private int digitalOutputValue;

    public SiosLib(SiosLabMode mode) throws NoSerialPortFoundException {

        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0) {
            throw new NoSerialPortFoundException();
        }

        for (SerialPort port : ports) {
            LOG.debug("Trying to open serial port {} / {}", port.getSystemPortName(), port.getDescriptivePortName());
            SerialPort portToTry = SerialPort.getCommPort(port.getSystemPortName());
            portToTry.setBaudRate(BAUD_RATE);
            portToTry.setNumDataBits(NUM_DATABITS);
            portToTry.setNumStopBits(SerialPort.ONE_STOP_BIT);
            portToTry.setParity(SerialPort.NO_PARITY);

            if (portToTry.openPort()) {
                LOG.debug("Port opened: {}", portToTry.getSystemPortName());
                SiosLabMode modeDetected = testSiosLab(portToTry);
                if (modeDetected != null) {
                    siosLabPort = portToTry;
                    siosLabMode = modeDetected;
                    break;
                }
                port.closePort();
            }
        }

        if (siosLabPort == null) {
            throw new NoSiosLabFoundException();
        }

        switchMode(mode);
    }

    public void switchMode(SiosLabMode newMode) {
        LOG.info("Setting mode to {}", newMode == SiosLabMode.SIOS_MODE ? "SIOSLAB" : "COMPULAB");

        for (byte b : SWITCH_MODE_SEQUENCE_PART_1) {
            sendData(b);
        }

        sendData(newMode == SiosLabMode.SIOS_MODE ? CONTROL_SET_MODE_SIOS : CONTROL_SET_MODE_COMPULAB);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while setting mode!", e);
            Thread.currentThread().interrupt();
        }

        for (byte b : SWITCH_MODE_SEQUENCE_PART_2) {
            sendData(b);
        }

        sendData(newMode == SiosLabMode.SIOS_MODE ? CONTROL_SET_MODE_SIOS : CONTROL_SET_MODE_COMPULAB);

        siosLabMode = newMode;

        setDigitalOutputValue(0);

        LOG.trace("Finished setting mode.");
    }

    @Nullable
    private SiosLabMode testSiosLab(SerialPort port) {

        for (byte b : TEST_DATA_SEQUENCE) {
            port.writeBytes(new byte[] {b}, 1);
        }

        byte[] buffer = new byte[1];

        boolean readSomething = false;

        long timeBefore = System.currentTimeMillis();

        while (!readSomething && (System.currentTimeMillis() - timeBefore < RECEIVE_TIMEOUT)) {

            if (port.bytesAvailable() == 1) {
                int numBytesRead = port.readBytes(buffer, buffer.length);

                if (numBytesRead == 1) {

                    LOG.trace("{} bytes read", numBytesRead);
                    LOG.trace("Received: {}", buffer[0]);

                    if (buffer[0] == MODE_INDICATOR_SIOSLAB) {
                        LOG.info("Found SiosLab running in SIOS mode on port {}.", port.getSystemPortName());
                        return SiosLabMode.SIOS_MODE;
                    } else if (buffer[0] == MODE_INDICATOR_COMPULAB) {
                        LOG.info("Found SiosLab running in CompuLAB mode on port {}.", port.getSystemPortName());
                        return SiosLabMode.COMPULAB_MODE;
                    }

                    readSomething = true;
                }
            }
        }
        return null;
    }

    public boolean isConnected() {
        return siosLabPort.isOpen();
    }

    private byte[] sendDataAndWaitForAnswer(byte data) throws ExecutionException, InterruptedException {
        CompletableFuture<byte[]> responseFuture = new CompletableFuture<>();

        SerialPortDataListener listener = new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                LOG.debug("Got serial event {}", event);
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

                byte[] buffer = new byte[siosLabPort.bytesAvailable()];
                siosLabPort.readBytes(buffer, buffer.length);
                responseFuture.complete(buffer);
            }
        };

        siosLabPort.addDataListener(listener);

        byte[] result;

        try {
            sendData(data);
            result = responseFuture
                    .completeOnTimeout(new byte[] {0}, RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS)
                    .get();
            LOG.trace("Received: {}", () -> formatByteToString(result));
        } finally {
            siosLabPort.removeDataListener();
        }
        return result;
    }

    private void sendData(byte data) {
        LOG.trace("Sending: {}", () -> formatByteToString(new byte[] {data}));
        siosLabPort.writeBytes(new byte[] {data}, 1);
        waitForNextSlot();
    }

    public int getDigitalValue() throws ExecutionException, InterruptedException {
        byte[] result = sendDataAndWaitForAnswer(
                siosLabMode == SiosLabMode.SIOS_MODE
                        ? CONTROL_SET_INPUT_DIGITAL_SIOS_MODE
                        : CONTROL_SET_INPUT_DIGITAL_COMPULAB_MODE);
        return byteArrayToInt(result);
    }

    public void setDigitalOutputValue(int value) {
        byte[] bytes = ByteBuffer.allocate(4).putInt(value).array();
        byte byteToSend = bytes[3];
        LOG.trace("Setting digital output value: {}", byteToSend);
        sendData(
                siosLabMode == SiosLabMode.SIOS_MODE
                        ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE
                        : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE);
        sendData(byteToSend);
        LOG.trace("Finished setting digital output value.");
        this.digitalOutputValue = value;
    }

    public int getDigitalOutputValue() {
        return this.digitalOutputValue;
    }

    public void setAnalogOutputValue(AnalogOutput analogOutput, BitWidth bitWidth, int value) {

        if (this.siosLabMode != SiosLabMode.SIOS_MODE) {
            throw new IllegalStateException("Analog output can only be used in SIOS mode.");
        }

        if ((bitWidth.equals(BitWidth.BIT_WIDTH_10_BITS)) && ((value < 0) || (value > 1023))) {
            throw new IllegalArgumentException("Analog output value must be between 0 and 1023 in 10-bit mode");
        }
        if ((bitWidth.equals(BitWidth.BIT_WIDTH_8_BITS)) && ((value < 0) || (value > 255))) {
            throw new IllegalArgumentException("Analog output value must be between 0 and 255 in 8-bit mode");
        }
        byte[] bytes = ByteBuffer.allocate(4).putInt(value).array();
        byte[] bytesToSend = new byte[] {bytes[2], bytes[3]};

        LOG.trace(
                "Setting analog output value {}: {}|{} ({})",
                analogOutput.equals(AnalogOutput.ANALOG_OUTPUT_1) ? "1" : "2",
                bytesToSend[1],
                bytesToSend[0],
                value);
        if (bitWidth.equals(BitWidth.BIT_WIDTH_10_BITS)) {
            sendData(
                    analogOutput.equals(AnalogOutput.ANALOG_OUTPUT_1)
                            ? CONTROL_SET_ANALOG_OUTPUT_1_TEN_BITS_SIOS_MODE
                            : CONTROL_SET_ANALOG_OUTPUT_2_TEN_BITS_SIOS_MODE);
            sendData(bytesToSend[0]);
            sendData(bytesToSend[1]);
        } else {
            sendData(
                    analogOutput.equals(AnalogOutput.ANALOG_OUTPUT_1)
                            ? CONTROL_SET_ANALOG_OUTPUT_1_EIGHT_BITS_SIOS_MODE
                            : CONTROL_SET_ANALOG_OUTPUT_2_EIGHT_BITS_SIOS_MODE);
            sendData(bytesToSend[1]);
        }
    }

    public void changeDigitalOutputState(DigitalOutputState... state) {

        if (state[0] == null) {
            throw new IllegalArgumentException("Given digital output states are null");
        }

        int digitalOutputChannels = DigitalOutputState.values().length / 2;

        if (state.length > digitalOutputChannels) {
            throw new IllegalArgumentException(
                    "Digital output state must have at most " + digitalOutputChannels + " parameters");
        }

        Set<DigitalOutputState> states = new HashSet<>(Arrays.asList(state));

        int i = 0;

        boolean stateOn = false;

        for (DigitalOutputState digitalOutputState : DigitalOutputState.values()) {
            if (i % 2 == 0) {
                stateOn = states.contains(digitalOutputState);
            } else {
                if (stateOn && states.contains(digitalOutputState)) {
                    throw new IllegalArgumentException(
                            "Contradictory output states (ON and OFF for the same channel) given");
                }
            }
            i++;
        }

        boolean[] newOutputState = new boolean[digitalOutputChannels];
        for (i = 0; i < digitalOutputChannels; ++i) {
            newOutputState[i] = (this.digitalOutputValue & (1 << i)) != 0;
        }
        Boolean[] desiredOutputState = new Boolean[digitalOutputChannels];

        i = 0;
        boolean onOrOff = true;

        for (DigitalOutputState digitalOutputState : DigitalOutputState.values()) {
            if (states.contains(digitalOutputState)) {
                desiredOutputState[i / 2] = onOrOff;
            }
            onOrOff = !onOrOff;
            i += 1;
        }

        for (i = 0; i < digitalOutputChannels; i++) {
            if (desiredOutputState[i] != null) {
                newOutputState[i] = desiredOutputState[i];
            }
        }
        setDigitalOutputState(newOutputState);
    }

    public void waitForNextSlot() {
        try {
            Thread.sleep(1000 / BAUD_RATE);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted when waiting for next data slot!", e);
            Thread.currentThread().interrupt();
        }
    }

    private static String formatByteToString(byte[] data) {
        StringBuilder dataString = new StringBuilder(String.format("%04d", byteArrayToInt(data)));

        dataString.append(" / ");

        for (int i = data.length - 1; i >= 0; i--) {
            byte b = data[i];
            dataString.append(
                    String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        return dataString.toString();
    }

    public static int byteArrayToInt(byte[] bytes) {
        int result = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            result = (result << 8) | (bytes[i] & 0xFF);
        }
        return result;
    }

    public int getAnalogValue(AnalogInput analogInput) throws ExecutionException, InterruptedException {

        LOG.trace("Getting analog value {}", () -> analogInput.equals(AnalogInput.ANALOG_INPUT_1) ? "1" : "2");

        byte controlByte;

        if (analogInput.equals(AnalogInput.ANALOG_INPUT_1)) {
            controlByte = (siosLabMode == SiosLabMode.SIOS_MODE
                    ? CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE
                    : CONTROL_SET_INPUT_ANALOG_1_COMPULAB_MODE);
        } else {
            controlByte = (siosLabMode == SiosLabMode.SIOS_MODE
                    ? CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE
                    : CONTROL_SET_INPUT_ANALOG_2_COMPULAB_MODE);
        }

        byte[] newData = sendDataAndWaitForAnswer(controlByte);

        byte[] result;

        if (siosLabMode == SiosLabMode.SIOS_MODE) {
            byte[] additionalData = sendDataAndWaitForAnswer(CONTROL_GET_NEXT_BYTE);
            result = new byte[newData.length + additionalData.length];
            System.arraycopy(additionalData, 0, result, 0, additionalData.length);
            System.arraycopy(newData, 0, result, additionalData.length, newData.length);
        } else {
            result = newData;
        }

        LOG.trace("Received analog value: {}", () -> formatByteToString(newData));

        return byteArrayToInt(result);
    }

    public void setDigitalOutputState(boolean[] digitalOutputState) {
        if (digitalOutputState == null) {
            throw new IllegalArgumentException("digitalOutputState must not be null.");
        }
        if (digitalOutputState.length != 8) {
            throw new IllegalArgumentException(
                    "digitalOutputState must have exactly 8 booleans, each representing one output channel.");
        }
        int digitalOutputValue = 0;
        int bitValue = 1;
        for (int i = 0; i < 8; i++) {
            if (digitalOutputState[i]) {
                digitalOutputValue += bitValue;
            }
            bitValue *= 2;
        }
        setDigitalOutputValue(digitalOutputValue);
    }

    @Override
    public void close() {
        LOG.info("Closing SiosLab port {}", siosLabPort.getSystemPortName());
        sendData(
                siosLabMode == SiosLabMode.SIOS_MODE
                        ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE
                        : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE);
        sendData((byte) 0);
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted when shutting down!", e);
            Thread.currentThread().interrupt();
        }
        siosLabPort.closePort();
    }

    public enum DigitalOutputState {
        OUTPUT_0_ON,
        OUTPUT_0_OFF,
        OUTPUT_1_ON,
        OUTPUT_1_OFF,
        OUTPUT_2_ON,
        OUTPUT_2_OFF,
        OUTPUT_3_ON,
        OUTPUT_3_OFF,
        OUTPUT_4_ON,
        OUTPUT_4_OFF,
        OUTPUT_5_ON,
        OUTPUT_5_OFF,
        OUTPUT_6_ON,
        OUTPUT_6_OFF,
        OUTPUT_7_ON,
        OUTPUT_7_OFF,
    }

    public enum SiosLabMode {
        SIOS_MODE,
        COMPULAB_MODE,
    }

    public enum AnalogInput {
        ANALOG_INPUT_1,
        ANALOG_INPUT_2
    }

    public enum AnalogOutput {
        ANALOG_OUTPUT_1,
        ANALOG_OUTPUT_2
    }

    public enum BitWidth {
        BIT_WIDTH_8_BITS,
        BIT_WIDTH_10_BITS
    }
}
