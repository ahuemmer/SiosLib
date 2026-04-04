package com.github.ahuemmer.sioslib;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SiosLib implements AutoCloseable {

    private SerialPort siosLabPort;

    private static final Logger LOG = LogManager.getLogger(SiosLib.class);

    protected static final byte[] TEST_DATA_SEQUENCE = new byte[]{0, 0, 0, 1};
    protected static final byte[] SWITCH_MODE_SEQUENCE_PART_1 = new byte[]{100, 27, 3, -1}; // -1 means 255 as unsigned byte
    protected static final byte[] SWITCH_MODE_SEQUENCE_PART_2 = new byte[]{102, 27};
    protected static final byte CONTROL_SET_OUTPUT_SIOS_MODE = (byte) 16;
    protected static final byte CONTROL_SET_OUTPUT_COMPULAB_MODE = (byte) 81;
    protected static final byte CONTROL_SET_INPUT_DIGITAL_SIOS_MODE = (byte) 32;
    protected static final byte CONTROL_SET_INPUT_DIGITAL_COMPULAB_MODE = (byte) -45;
    protected static final byte CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE = (byte) 56;
    protected static final byte CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE = (byte) 57;
    protected static final byte CONTROL_SET_INPUT_ANALOG_1_COMPULAB_MODE = (byte) 60;
    protected static final byte CONTROL_SET_INPUT_ANALOG_2_COMPULAB_MODE = (byte) 58;
    protected static final byte CONTROL_SET_MODE_SIOS = (byte) 0;
    protected static final byte CONTROL_SET_MODE_COMPULAB = (byte) 1;
    protected static final int MODE_INDICATOR_SIOSLAB = 10;
    protected static final int MODE_INDICATOR_COMPULAB = 201;

    public static final boolean ANALOG_INPUT_1 = true;
    public static final boolean ANALOG_INPUT_2 = false;

    public static final boolean SIOS_MODE = false;
    public static final boolean COMPULAB_MODE = true;

    public static final int POLLING_INTERVAL = 50; // ms

    public static final int BAUD_RATE = 19200;
    public static final int NUM_DATABITS = 8;

    private boolean siosLabMode = SIOS_MODE;

    public SiosLib(boolean mode) throws NoSerialPortFoundException {

        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0) {
            throw new NoSerialPortFoundException();
        }

        for (SerialPort port : ports) {
            LOG.debug(
                    "Trying to open serial port {} / {}",
                    port.getSystemPortName(),
                    port.getDescriptivePortName());
            SerialPort portToTry = SerialPort.getCommPort(port.getSystemPortName());
            portToTry.setBaudRate(BAUD_RATE);
            portToTry.setNumDataBits(NUM_DATABITS);
            portToTry.setNumStopBits(SerialPort.ONE_STOP_BIT);
            portToTry.setParity(SerialPort.NO_PARITY);

            if (portToTry.openPort()) {
                LOG.debug("Port opened: {}", portToTry.getSystemPortName());
                Boolean modeDetected = testSiosLab(portToTry);
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

    public void switchMode(boolean newMode) {
        LOG.info("Setting mode to {}", newMode == SIOS_MODE ? "SIOSLAB" : "COMPULAB");

        for (byte b : SWITCH_MODE_SEQUENCE_PART_1) {
            sendData(b);
        }

        sendData(newMode == SIOS_MODE ? CONTROL_SET_MODE_SIOS : CONTROL_SET_MODE_COMPULAB);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while setting mode!", e);
            Thread.currentThread().interrupt();
        }

        for (byte b : SWITCH_MODE_SEQUENCE_PART_2) {
            sendData(b);
        }

        sendData(newMode == SIOS_MODE ? CONTROL_SET_MODE_SIOS : CONTROL_SET_MODE_COMPULAB);

        LOG.trace("Finished setting mode.");
    }

    @Nullable
    private Boolean testSiosLab(SerialPort port) {

        for (byte b : TEST_DATA_SEQUENCE) {
            port.writeBytes(new byte[]{b}, 1);
        }

        byte[] buffer = new byte[1];

        boolean readSomething = false;

        while (!readSomething) {

            if (port.bytesAvailable() > 0 && port.bytesAvailable() == 1) {
                int numBytesRead = port.readBytes(buffer, buffer.length);

                if (numBytesRead == 1) {

                    LOG.trace("{} bytes read", numBytesRead);
                    LOG.trace("Received: {}", buffer[0]);

                    if (buffer[0] == MODE_INDICATOR_SIOSLAB) {
                        LOG.info("Found SiosLab running in SIOS mode on port {}.", port.getSystemPortName());
                        return SIOS_MODE;
                    } else if (Byte.toUnsignedInt(buffer[0]) == MODE_INDICATOR_COMPULAB) {
                        LOG.info(
                                "Found SiosLab running in CompuLAB mode on port {}.", port.getSystemPortName());
                        return COMPULAB_MODE;
                    }

                    readSomething = true;
                }
            }
        }
        return null;
    }

    public boolean isConnected() {
        return siosLabPort != null && siosLabPort.isOpen();
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
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

                byte[] buffer = new byte[siosLabPort.bytesAvailable()];
                siosLabPort.readBytes(buffer, buffer.length);
                responseFuture.complete(buffer);
            }
        };

        siosLabPort.addDataListener(listener);

        try {
            sendData(data);
            byte[] result = responseFuture.completeOnTimeout(new byte[]{0}, 250, TimeUnit.MILLISECONDS).get();
            LOG.trace("Received: {}", () -> formatByteToString(result));
            return result;
        } finally {
            siosLabPort.removeDataListener();
        }
    }

    private void sendData(byte data) {
        LOG.trace("Sending: {}", () -> formatByteToString(new byte[]{data}));
        siosLabPort.writeBytes(new byte[]{data}, 1);
        waitForNextSlot();
    }

    public int getDigitalValue() throws ExecutionException, InterruptedException {
        byte[] result = sendDataAndWaitForAnswer(siosLabMode == SIOS_MODE ? CONTROL_SET_INPUT_DIGITAL_SIOS_MODE : CONTROL_SET_INPUT_DIGITAL_COMPULAB_MODE);
        return byteArrayToInt(result);
    }

    public void setOutputValue(int value) {
        byte[] bytes = ByteBuffer.allocate(4).putInt(value).array();
        byte byteToSend = bytes[3];
        LOG.trace("Setting output value: {}", byteToSend);
        sendData(siosLabMode == SIOS_MODE ? CONTROL_SET_OUTPUT_SIOS_MODE : CONTROL_SET_OUTPUT_COMPULAB_MODE);
        sendData(byteToSend);
        LOG.trace("Finished setting output value.");
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
            dataString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            if (i > 0) {
                dataString.append("|");
            }
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

    public int getAnalogValue(boolean analogInput) throws ExecutionException, InterruptedException {

        LOG.trace("Getting analog value {}", () -> analogInput == ANALOG_INPUT_1 ? "1" : "2");

        byte controlByte;

        if (analogInput == ANALOG_INPUT_1) {
            controlByte = (siosLabMode == SIOS_MODE ? CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE : CONTROL_SET_INPUT_ANALOG_1_COMPULAB_MODE);
        } else {
            controlByte = (siosLabMode == SIOS_MODE ? CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE : CONTROL_SET_INPUT_ANALOG_2_COMPULAB_MODE);
        }

        byte[] newData = sendDataAndWaitForAnswer(controlByte);

        byte[] result;

        if (siosLabMode == SIOS_MODE) {
            byte[] additionalData = sendDataAndWaitForAnswer((byte) 1);
            result = new byte[newData.length + additionalData.length];
            System.arraycopy(additionalData, 0, result, 0, additionalData.length);
            System.arraycopy(newData, 0, result, additionalData.length, newData.length);
        } else {
            result = newData;
        }

        LOG.trace("Received analog value: {}", () -> formatByteToString(newData));

        return byteArrayToInt(result);

    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        LOG.info("Closing SiosLab port {}", siosLabPort.getSystemPortName());
        sendData(siosLabMode == SIOS_MODE ? CONTROL_SET_OUTPUT_SIOS_MODE : CONTROL_SET_OUTPUT_COMPULAB_MODE);
        sendData((byte) 0);
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted when shutting down!", e);
            Thread.currentThread().interrupt();
        }
        siosLabPort.closePort();
    }
}
