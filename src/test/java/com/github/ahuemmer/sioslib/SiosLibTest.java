package com.github.ahuemmer.sioslib;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiosLibTest {

    @Test
    void mirrorData() throws Exception {
        try (SiosLib siosLib = new SiosLib()) {
            assertTrue(siosLib.isConnected());

            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 60000) {
                byte newData = siosLib.getDigitalValue();
                siosLib.setOutputValue(newData);
                siosLib.waitForNextSlot();
            }
        }
    }

    @Test
    void reflectVoltage() throws Exception {
        try (SiosLib siosLib = new SiosLib()) {
            assertTrue(siosLib.isConnected());

            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 60000) {
                int valueIn = siosLib.getAnalogValue(SiosLib.ANALOG_INPUT_1);

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

                siosLib.setOutputValue(valueOut);
                await().pollDelay(250, TimeUnit.MILLISECONDS).untilAsserted(() -> assertTrue(true));
            }
        }
    }

    @Test
    void sendPattern() throws Exception {
        try (SiosLib siosLib = new SiosLib()) {
            assertTrue(siosLib.isConnected());
            int data = 1;
            while (data < 256) {
                sendAndWait(siosLib, data);
                data *= 2;
            }
            data = 64;
            while (data > 0) {
                sendAndWait(siosLib, data);
                data /= 2;
            }
        }
    }

    @Test
    void sendOtherPattern() throws Exception {
        try (SiosLib siosLib = new SiosLib()) {
            assertTrue(siosLib.isConnected());
            sendAndWait(siosLib, 1 + 4 + 16 + 64);
            sendAndWait(siosLib, 2 + 8 + 32 + 128);
            sendAndWait(siosLib, 1 + 4 + 16 + 64);
            sendAndWait(siosLib, 2 + 8 + 32 + 128);
            sendAndWait(siosLib, 1 + 4 + 16 + 64);
            sendAndWait(siosLib, 2 + 8 + 32 + 128);
            sendAndWait(siosLib, 1 + 4 + 16 + 64);
            sendAndWait(siosLib, 2 + 8 + 32 + 128);
            sendAndWait(siosLib, 1 + 4 + 16 + 64);
            sendAndWait(siosLib, 2 + 8 + 32 + 128);
        }
    }

    private void sendAndWait(SiosLib siosLib, int data) {
        siosLib.setOutputValue(data);
        await().pollDelay(250, TimeUnit.MILLISECONDS).untilAsserted(() -> assertTrue(true));
    }
}
