package com.github.ahuemmer.sioslib;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * These tests can be run when a SIOSLab is attached to the PC and some inputs can be triggered.
 * <p>
 * These tests are not meant to be run automatically or assert anything but a SIOSLab connection.
 */
@Disabled // Run these tests manually with a SIOSLab attached to the PC
class SiosLibIntegrationTest {

    /**
     * Operating mode for the tests
     */
    public static final boolean TEST_MODE = SiosLib.SIOS_MODE;

    /**
     * The analog input to use with the tests.
     */
    public static final boolean PREFERRED_ANALOG_INPUT = SiosLib.ANALOG_INPUT_1;

    /**
     * Data is read from the digital input and mirrored on the digital output.
     * <p>
     * E.g. if digital input 2 is connected to the +5V source, the digital output nr. 2 will be set to "ON", also
     * make the LED nr. 2 light.
     *
     * @throws Exception if the SIOSLab cannot be found or any other error occurs.
     */
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

    /**
     * Data is read from the analog input sets the digital output accordingly.
     * <p>
     * The higher the voltage (0..5V) on the analog input, the more LEDs on the digital output ports will be lit up.
     *
     * @throws Exception if the SIOSLab cannot be found or any other error occurs.
     */
    @Test
    void reflectVoltage() throws Exception {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            assertTrue(siosLib.isConnected());

            long startTime = System.currentTimeMillis();

            int maxValuePlusOne = TEST_MODE == SiosLib.SIOS_MODE ? 1024 : 256;

            while (System.currentTimeMillis() - startTime < 60000) {
                int valueIn = siosLib.getAnalogValue(PREFERRED_ANALOG_INPUT);

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

    /**
     * Sends an "up and down" pattern to the digital output, turning the first LED on, turning it off again and
     * turning the second one on and so on - and backwards once the eight LED was lit.
     *
     * @throws Exception if the SIOSLab cannot be found or any other error occurs.
     */
    @Test
    void sendUpAndDownPattern() throws Exception {
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

    /**
     * Sends a pattern to the digital output that makes second LED (0,2,4,6) light and then switch over to the other
     * ones (1,3,5,7). This is repeated
     *
     * @throws Exception if the SIOSLab cannot be found or any other error occurs.
     */
    @Test
    void sendAlternatingPattern() throws ExecutionException, InterruptedException {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            assertTrue(siosLib.isConnected());
            for (int i = 0; i < 8; i++) {
                sendAndWait(siosLib, 1 + 4 + 16 + 64);
                sendAndWait(siosLib, 2 + 8 + 32 + 128);
            }
        }
    }

    private void sendAndWait(SiosLib siosLib, int data) throws ExecutionException, InterruptedException {
        siosLib.setOutputValue(data);
        await().pollDelay(250, TimeUnit.MILLISECONDS).untilAsserted(() -> assertTrue(true));
    }
}
