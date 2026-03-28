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
    public final static byte[] CONTROL_SET_INPUT_DIGITAL =new byte[]{32};
    public final static byte[] CONTROL_SET_INPUT_ANALOG_1 =new byte[]{48};
    public final static byte[] CONTROL_SET_INPUT_ANALOG_2 =new byte[]{49};

    public final static boolean ANALOG_INPUT_1=true;
    public final static boolean ANALOG_INPUT_2=false;

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
                    //siosLabPort.writeBytes(CONTROL_SET_INPUT_DIGITAL, CONTROL_SET_INPUT_DIGITAL.length);
                    siosLabPort.writeBytes(CONTROL_SET_INPUT_ANALOG_1, CONTROL_SET_INPUT_ANALOG_1.length);
                }
            }
        });

        //siosLabPort.writeBytes(CONTROL_SET_INPUT_DIGITAL, CONTROL_SET_INPUT_DIGITAL.length);
        siosLabPort.writeBytes(CONTROL_SET_INPUT_ANALOG_1, CONTROL_SET_INPUT_ANALOG_1.length);
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

    public byte getDigitalValue() {
        siosLabPort.writeBytes(CONTROL_SET_INPUT_DIGITAL, CONTROL_SET_INPUT_DIGITAL.length);
        waitForData();
        return receiveData();
    }

    public void waitForNextSlot() {
        try {
            Thread.sleep(1000/BAUD_RATE);
        } catch (InterruptedException e) {

        }
    }

    private byte receiveData() {
        byte[] newData = new byte[1];
        siosLabPort.readBytes(newData, newData.length);
        System.out.print("Received: ");
        for (int i=0; i<newData.length; i++) {
            System.out.print(String.format("%8s", Integer.toBinaryString(newData[i] & 0xFF)).replace(' ', '0'));
        }
        System.out.println();
        return newData[0];
    }

    private void waitForData() {
        while (siosLabPort.bytesAvailable() <= 0) {
            waitForNextSlot();
        }
    }

    public int getAnalogValue(boolean analogInput) {
        if (analogInput == ANALOG_INPUT_1) {
            siosLabPort.writeBytes(CONTROL_SET_INPUT_ANALOG_1, CONTROL_SET_INPUT_ANALOG_1.length);
        }
        else {
            siosLabPort.writeBytes(CONTROL_SET_INPUT_ANALOG_2, CONTROL_SET_INPUT_ANALOG_2.length);
        }

        waitForData();

        byte newData = receiveData();

        int valueIn = newData & 0xFF;
        int valueOut = 0;
        if (valueIn > 0) {
            valueOut +=1;
        }
        if (valueIn > 32) {
            valueOut +=2;
        }
        if (valueIn > 64) {
            valueOut +=4;
        }
        if (valueIn > 96) {
            valueOut +=8;
        }
        if (valueIn > 128) {
            valueOut +=16;
        }
        if (valueIn > 160) {
            valueOut +=32;
        }
        if (valueIn > 192) {
            valueOut +=64;
        }
        if (valueIn > 224) {
            valueOut += 128;
        }
        return valueOut;
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
