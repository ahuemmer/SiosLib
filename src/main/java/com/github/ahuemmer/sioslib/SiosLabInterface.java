package com.github.ahuemmer.sioslib;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.ByteBuffer;
import java.util.Enumeration;

public class SiosLabInterface implements AutoCloseable {

    private Enumeration portList;

    private SerialPort siosLabPort;

    public final static byte[] TEST_DATA_SEQUENCE = new byte[]{0,0,0,1};
    public final static byte[] CONTROL_SET_OUTPUT=new byte[]{16};
    public final static byte[] CONTROL_SET_INPUT=new byte[]{32};

    public final static int POLLING_INTERVAL = 50; //ms

    public final static int BAUD_RATE = 19200;
    public final static int NUM_DATABITS = 8;

    private DataListener dataListener;

    public SiosLabInterface() throws NoSerialPortFoundException {

        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0) {
            throw new NoSerialPortFoundException();
        }

        for (SerialPort port : ports) {
            System.out.println(port.getSystemPortName() + " - " + port.getDescriptivePortName());
            SerialPort portToTry = SerialPort.getCommPort(port.getSystemPortName());
            portToTry.setBaudRate(BAUD_RATE);
            portToTry.setNumDataBits(NUM_DATABITS);
            portToTry.setNumStopBits(SerialPort.ONE_STOP_BIT);
            portToTry.setParity(SerialPort.NO_PARITY);

            if (portToTry.openPort()) {
                System.out.println("Port opened: "+portToTry.getSystemPortName());
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

        siosLabPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            @Override
            public void serialEvent(SerialPortEvent event)
            {
                if (((event.getEventType() & SerialPort.LISTENING_EVENT_DATA_AVAILABLE) > 0) && (dataListener != null))
                {
                    byte[] newData = new byte[siosLabPort.bytesAvailable()];
                    siosLabPort.readBytes(newData, newData.length);
                    dataListener.dataReceived(newData);
                    try {
                        Thread.sleep(POLLING_INTERVAL);
                    }
                    catch (InterruptedException e) {}
                    siosLabPort.writeBytes(CONTROL_SET_INPUT, CONTROL_SET_INPUT.length);
                }
            }
        });

        siosLabPort.writeBytes(CONTROL_SET_INPUT, CONTROL_SET_INPUT.length);
    }

    public void attachDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
    }

    private boolean testSiosLab(SerialPort port) {

        for(byte b: TEST_DATA_SEQUENCE) {
            port.writeBytes(new byte[]{b}, 1);
        }

        byte[] buffer = new byte[1];

        boolean readSomething = false;

        while (!readSomething) {

            if (port.bytesAvailable() > 0) {

                if (port.bytesAvailable() == 1) {
                    int numBytesRead = port.readBytes(buffer, buffer.length);

                    if (numBytesRead == 1) {

                        System.out.println(numBytesRead + " bytes read");

                        System.out.println("Received: "+buffer[0]);

                        if (buffer[0] == 10) {
                            System.out.println("Found SiosLab running in SIOS mode on port "+port.getSystemPortName()+".");
                            return true;
                        }
                        else if (buffer[0] == 201) {
                            System.out.println("Found SiosLab running in CompuLab mode on port "+port.getSystemPortName()+".");
                            return true;
                        }

                        readSomething = true;

                    }
                }
            }
        }

        return false;

    }

    public boolean isConnected() {
        return siosLabPort != null && siosLabPort.isOpen();
    }

    public void sendData(int data) {

        siosLabPort.writeBytes(CONTROL_SET_OUTPUT, CONTROL_SET_OUTPUT.length);

        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(data);

        byte[] bytes = new byte[]{b.array()[3]};

        System.out.print("Sending: "+bytes[0]);
        System.out.println();

        siosLabPort.writeBytes(bytes, bytes.length);
    }

    @Override
    public void close() throws Exception {
        System.out.println("Closing SiosLab port "+siosLabPort.getSystemPortName());
        sendData(0);
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        siosLabPort.closePort();
    }
}
