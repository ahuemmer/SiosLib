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
    public static final SiosLib.SiosLabMode TEST_MODE = SiosLib.SiosLabMode.SIOS_MODE;

    /**
     * The analog input to use with the tests.
     */
    public static final SiosLib.AnalogInput PREFERRED_ANALOG_INPUT = SiosLib.AnalogInput.DIGITAL_INPUT_4;

    /**
     * The analog output to use with the tests.
     */
    public static final SiosLib.AnalogOutput PREFERRED_ANALOG_OUTPUT = SiosLib.AnalogOutput.ANALOG_OUTPUT_1;

    /**
     * The analog output bitwidth to use with the tests.
     */
    public static final SiosLib.BitWidth PREFERRED_ANALOG_OUTPUT_BITWIDTH = SiosLib.BitWidth.BIT_WIDTH_10_BITS;

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
                siosLib.setDigitalOutputValue(newData);
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

            int maxValuePlusOne = TEST_MODE == SiosLib.SiosLabMode.SIOS_MODE ? 1024 : 256;

            while (System.currentTimeMillis() - startTime < 60000) {
                int valueIn = siosLib.getAnalogValue(PREFERRED_ANALOG_INPUT, TEST_MODE == SiosLib.SiosLabMode.SIOS_MODE ? SiosLib.BitWidth.BIT_WIDTH_10_BITS : SiosLib.BitWidth.BIT_WIDTH_8_BITS);

                int valueOut = 0;
                int factor = 1;
                for (int i = 0; i <= 7; i++) {
                    if (valueIn > (maxValuePlusOne * i * 0.125)) {
                        valueOut += factor;
                    }
                    factor *= 2;
                }

                siosLib.setDigitalOutputValue(valueOut);
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
     * Sends a pattern to the digital output that makes every second LED (0,2,4,6) light and then switch over to the
     * other ones (1,3,5,7). This is repeated eight times.
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

    /**
     * Sends a pattern to the digital output that makes the outer LEDs start lighting and then continues to the center
     * LEDs. This is repeated eight times.
     */
    @Test
    void sendNarrowingPattern() {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            for (int i = 0; i < 8; i++) {
                assertTrue(siosLib.isConnected());
                siosLib.setDigitalOutputState(new boolean[]{true, false, false, false, false, false, false, true});
                pause();
                siosLib.setDigitalOutputState(new boolean[]{true, true, false, false, false, false, true, true});
                pause();
                siosLib.setDigitalOutputState(new boolean[]{true, true, true, false, false, true, true, true});
                pause();
                siosLib.setDigitalOutputState(new boolean[]{true, true, true, true, true, true, true, true});
                pause();
                siosLib.setDigitalOutputState(new boolean[]{true, true, true, false, false, true, true, true});
                pause();
                siosLib.setDigitalOutputState(new boolean[]{true, true, false, false, false, false, true, true});
                pause();
                siosLib.setDigitalOutputState(new boolean[]{true, false, false, false, false, false, false, true});
                pause();
                siosLib.setDigitalOutputState(new boolean[]{false, false, false, false, false, false, false, false});
                pause();
            }
        }
    }

    /**
     * Sends a pattern to the digital output that makes the outer LEDs start lighting and then continues to the center
     * LEDs. Then the LEDs are switched off again, beginning with the outer LEDs. This is repeated eight times.
     */
    @Test
    void sendNarrowingPattern2() {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            assertTrue(siosLib.isConnected());
            for (int i = 0; i < 8; i++) {
                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_0_ON, SiosLib.DigitalOutputState.OUTPUT_7_ON);
                pause();
                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_1_ON, SiosLib.DigitalOutputState.OUTPUT_6_ON);
                pause();
                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_2_ON, SiosLib.DigitalOutputState.OUTPUT_5_ON);
                pause();
                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_3_ON, SiosLib.DigitalOutputState.OUTPUT_4_ON);
                pause();
                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_0_OFF, SiosLib.DigitalOutputState.OUTPUT_7_OFF);
                pause();
                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_1_OFF, SiosLib.DigitalOutputState.OUTPUT_6_OFF);
                pause();
                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_2_OFF, SiosLib.DigitalOutputState.OUTPUT_5_OFF);
                pause();
                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_3_OFF, SiosLib.DigitalOutputState.OUTPUT_4_OFF);
                pause();
            }
        }
    }

    @Test
    void setAnalogOutputValue() {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            assertTrue(siosLib.isConnected());

            int maxValue = PREFERRED_ANALOG_OUTPUT_BITWIDTH.equals(SiosLib.BitWidth.BIT_WIDTH_10_BITS) ? 1024 : 256;
            int mutiplicator = PREFERRED_ANALOG_OUTPUT_BITWIDTH.equals(SiosLib.BitWidth.BIT_WIDTH_10_BITS) ? 16 : 4;

            int factor = 1;
            int value = 0;
            while (value >= 0) {
                if (value >= maxValue) {
                    value = maxValue - 1;
                    factor = -1;
                }
                siosLib.setAnalogOutputValue(PREFERRED_ANALOG_OUTPUT, PREFERRED_ANALOG_OUTPUT_BITWIDTH, value);
                int factorDigital = 1;
                int valueDigital = 0;
                for (int i = 0; i <= 7; i++) {
                    if (value > (maxValue * i * 0.125)) {
                        valueDigital += factorDigital;
                    }
                    factorDigital *= 2;
                }

                siosLib.setDigitalOutputValue(valueDigital);
                pause();
                value += (factor * mutiplicator);
            }
            siosLib.setAnalogOutputValue(PREFERRED_ANALOG_OUTPUT, PREFERRED_ANALOG_OUTPUT_BITWIDTH, 0);
        }
    }

    private void pause() {
        await().pollDelay(250, TimeUnit.MILLISECONDS).untilAsserted(() -> assertTrue(true));
    }

    private void sendAndWait(SiosLib siosLib, int data) {
        siosLib.setDigitalOutputValue(data);
        pause();
    }
}
