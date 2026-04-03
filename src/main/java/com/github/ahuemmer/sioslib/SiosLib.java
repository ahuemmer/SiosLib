package com.github.ahuemmer.sioslib;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SiosLib implements AutoCloseable {

    private SerialPort siosLabPort;

    private static final Logger LOG = LogManager.getLogger(SiosLib.class);

    protected static final byte[] TEST_DATA_SEQUENCE = new byte[]{0, 0, 0, 1};
    protected static final byte CONTROL_SET_OUTPUT = (byte) 16;
    protected static final byte CONTROL_SET_INPUT_DIGITAL = (byte) 32;
    protected static final byte CONTROL_SET_INPUT_ANALOG_1 = (byte) 48;
    protected static final byte CONTROL_SET_INPUT_ANALOG_2 = (byte) 49;
    protected static final int MODE_INDICATOR_SIOSLAB = 10;
    protected static final int MODE_INDICATOR_COMPULAB = 201;

    public static final boolean ANALOG_INPUT_1 = true;
    public static final boolean ANALOG_INPUT_2 = false;

    public static final int POLLING_INTERVAL = 50; // ms

    public static final int BAUD_RATE = 19200;
    public static final int NUM_DATABITS = 8;

    public SiosLib() throws NoSerialPortFoundException {

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
                if (testSiosLab(portToTry)) {
                    siosLabPort = portToTry;
                    break;
                }
                port.closePort();
            }
        }

        if (siosLabPort == null) {
            throw new NoSiosLabFoundException();
        }

        sendData(CONTROL_SET_INPUT_ANALOG_1);
    }

    private boolean testSiosLab(SerialPort port) {

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
                        LOG.info("Found SiosLab running in SIOS mode on port {} .", port.getSystemPortName());
                        return true;
                    } else if (Byte.toUnsignedInt(buffer[0]) == MODE_INDICATOR_COMPULAB) {
                        LOG.info(
                                "Found SiosLab running in CompuLAB mode on port {} .", port.getSystemPortName());
                        return true;
                    }

                    readSomething = true;
                }
            }
        }
        return false;
    }

    public boolean isConnected() {
        return siosLabPort != null && siosLabPort.isOpen();
    }

    public byte sendDataAndWaitForAnswer(byte data) throws ExecutionException, InterruptedException {
        CompletableFuture<Byte> responseFuture = new CompletableFuture<>();

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
                responseFuture.complete(buffer[0]);
            }
        };

        siosLabPort.addDataListener(listener);

        try {
            sendData(data);
            return responseFuture.completeOnTimeout((byte) 0, 250, TimeUnit.MILLISECONDS).get();
        } finally {
            siosLabPort.removeDataListener();
        }
    }

    private void sendData(byte data) {
        LOG.trace("Sending: {}", () -> formatByteToString(data));
        siosLabPort.writeBytes(new byte[]{data}, 1);
    }

    public byte getDigitalValue() throws ExecutionException, InterruptedException {
        return sendDataAndWaitForAnswer(CONTROL_SET_INPUT_DIGITAL);
    }

    public void setOutputValue(int value) {
        byte[] bytes = ByteBuffer.allocate(4).putInt(value).array();
        byte byteToSend = bytes[3];
        sendData(CONTROL_SET_OUTPUT);
        sendData(byteToSend);
    }

    public void waitForNextSlot() {
        try {
            Thread.sleep(1000 / BAUD_RATE);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted when waiting for next data slot!", e);
            Thread.currentThread().interrupt();
        }
    }

    private String formatByteToString(byte data) {
        StringBuilder dataString = new StringBuilder();
        dataString.append(String.format("%03d", Byte.toUnsignedInt(data)));
        dataString.append(" / ");
        dataString.append(String.format("%8s", Integer.toBinaryString(data & 0xFF)).replace(' ', '0'));
        return dataString.toString();
    }

    public int getAnalogValue(boolean analogInput) throws ExecutionException, InterruptedException {

        byte newData = sendDataAndWaitForAnswer(analogInput == ANALOG_INPUT_1 ? CONTROL_SET_INPUT_ANALOG_1 : CONTROL_SET_INPUT_ANALOG_2);
        LOG.trace("Received: {}", () -> formatByteToString(newData));

        int valueIn = newData & 0xFF;
        int valueOut = 0;
        if (valueIn > 0) {
            valueOut += 1;
        }
        if (valueIn > 32) {
            valueOut += 2;
        }
        if (valueIn > 64) {
            valueOut += 4;
        }
        if (valueIn > 96) {
            valueOut += 8;
        }
        if (valueIn > 128) {
            valueOut += 16;
        }
        if (valueIn > 160) {
            valueOut += 32;
        }
        if (valueIn > 192) {
            valueOut += 64;
        }
        if (valueIn > 224) {
            valueOut += 128;
        }
        return valueOut;
    }

    @Override
    public void close() {
        LOG.info("Closing SiosLab port {}", siosLabPort.getSystemPortName());
        sendData(CONTROL_SET_OUTPUT);
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
