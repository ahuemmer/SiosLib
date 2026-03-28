package com.github.ahuemmer.sioslib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SiosLabInterfaceTest {

    @Test
    void mirrorData() throws Exception {
        try (SiosLabInterface siosLabInterface = new SiosLabInterface()) {
            assertTrue(siosLabInterface.isConnected());

            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 60000) {
                byte newData = siosLabInterface.getDigitalValue();
                send(siosLabInterface, newData);
                siosLabInterface.waitForNextSlot();
            }

        }
    }

    @Test
    void reflectVoltage() throws Exception {
        try (SiosLabInterface siosLabInterface = new SiosLabInterface()) {
            assertTrue(siosLabInterface.isConnected());

            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 60000) {
                int valueIn = siosLabInterface.getAnalogValue(SiosLabInterface.ANALOG_INPUT_1);

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

                send(siosLabInterface, valueOut);
                siosLabInterface.waitForNextSlot();
            }
        }
    }

    @Test
    void sendPattern() throws Exception {
        try (SiosLabInterface siosLabInterface = new SiosLabInterface()) {
            assertTrue(siosLabInterface.isConnected());
            int data = 1;
            while (data < 256) {
                sendAndWait(siosLabInterface, data);
                data *= 2;
            }
            data = 64;
            while (data > 0) {
                sendAndWait(siosLabInterface, data);
                data /= 2;
            }
        }
    }

    @Test
    void sendOtherPattern() throws Exception {
        try (SiosLabInterface siosLabInterface = new SiosLabInterface()) {
            assertTrue(siosLabInterface.isConnected());
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
        }
    }

    private void send(SiosLabInterface siosLabInterface, int data) {
        siosLabInterface.sendData(data);
    }

    private void sendAndWait(SiosLabInterface siosLabInterface, int data) {
        send(siosLabInterface, data);
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
        }
    }
}