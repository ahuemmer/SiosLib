package com.github.ahuemmer.sioslib;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiosLibTest {

    public static final boolean TEST_MODE = SiosLib.SIOS_MODE;

    @Test
    void mirrorData() throws Exception {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            assertTrue(siosLib.isConnected());

            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 60000) {
                int newData = siosLib.getDigitalValue();
                siosLib.setOutputValue(newData);
                siosLib.waitForNextSlot();
            }
        }
    }

    @Test
    void reflectVoltage() throws Exception {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            assertTrue(siosLib.isConnected());

            long startTime = System.currentTimeMillis();

            int maxValuePlusOne = TEST_MODE == SiosLib.SIOS_MODE ? 1024 : 256;

            while (System.currentTimeMillis() - startTime < 60000) {
                int valueIn = siosLib.getAnalogValue(SiosLib.ANALOG_INPUT_1);

                int valueOut = 0;
                int factor = 1;
                for (int i = 0; i <= 7; i++) {
                    if (valueIn > (maxValuePlusOne * i * 0.125)) {
                        valueOut += factor;
                    }
                    factor *= 2;
                }

                siosLib.setOutputValue(valueOut);
                await().pollDelay(250, TimeUnit.MILLISECONDS).untilAsserted(() -> assertTrue(true));
            }
        }
    }

    @Test
    void sendPattern() throws ExecutionException, InterruptedException {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
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
    void sendOtherPattern() throws ExecutionException, InterruptedException {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
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

    private void sendAndWait(SiosLib siosLib, int data) throws ExecutionException, InterruptedException {
        siosLib.setOutputValue(data);
        await().pollDelay(250, TimeUnit.MILLISECONDS).untilAsserted(() -> assertTrue(true));
    }
}
