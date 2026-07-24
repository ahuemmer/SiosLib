package com.github.ahuemmer.sioslib;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests can be run when a SIOSLab is attached to the PC and some inputs can be triggered. They make use of all
 * the individual functions offered by the SiosLib, giving you the opportunity to make sure that they are all working
 * with your device.
 * <p>
 * These tests are not meant to be run automatically or assert anything but a SIOSLab connection.
 */
@Disabled // Run these tests manually with a SIOSLab attached to the PC
class SiosLibIntegrationTest {

    /**
     * Operating mode for the tests
     */
    public static final SiosLib.SiosLabMode TEST_MODE = SiosLib.SiosLabMode.COMPULAB_MODE;

    /**
     * The analog input to use with the tests.
     */
    public static final SiosLib.AnalogInput ANALOG_INPUT_TO_USE = SiosLib.AnalogInput.ANALOG_INPUT_1;

    /**
     * The analog output to use with the tests.
     */
    public static final SiosLib.AnalogOutput ANALOG_OUTPUT_TO_USE = SiosLib.AnalogOutput.ANALOG_OUTPUT_1;

    /**
     * The analog output bitwidth to use with the tests.
     */
    public static final SiosLib.BitWidth ANALOG_OUTPUT_BITWIDTH_TO_USE = SiosLib.BitWidth.BIT_WIDTH_8_BITS;

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
                int newData = siosLib.getDigitalInputValue();
                siosLib.setDigitalOutputValue(newData);
            }
        }
    }

    /**
     * Data is read from the digital input and mirrored on the digital output. A ChangeListener is used here, so
     * no polling is necessary.
     * <p>
     * E.g. if digital input 2 is connected to the +5V source, the digital output nr. 2 will be set to "ON", also
     * make the LED nr. 2 light.
     *
     * @throws Exception if the SIOSLab cannot be found or any other error occurs.
     */
    @Test
    void mirrorDataUsingChangeListener() throws Exception {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            assertTrue(siosLib.isConnected());

            long startTime = System.currentTimeMillis();

            BiConsumer<Integer, Integer> changeListener = (oldValue, newValue) -> {
                siosLib.setDigitalOutputValue(newValue);
            };

            siosLib.addDigitalInputChangeListener(changeListener);

            while (System.currentTimeMillis() - startTime < 60000) {
                // Just wait...
            }
        }
    }

    /**
     * Data is read from the analog input sets the digital output accordingly.
     * <p>
     * The higher the voltage (0..5V) on the analog input, the more LEDs on the digital output ports will be lit up.
     * <p>
     * For example, you can attach a trimmer potentiometer's "left" and "right" connectors to the ground and 5V outlets
     * of the device and the central connector to the analog input. Then, turning the trimmer potentiometers adjustment
     * wheel will cause more or less of the output leds to be lit, reflecting the voltage distribution.
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
                int valueIn = siosLib.getAnalogValue(
                        ANALOG_INPUT_TO_USE,
                        TEST_MODE == SiosLib.SiosLabMode.SIOS_MODE
                                ? SiosLib.BitWidth.BIT_WIDTH_10_BITS
                                : SiosLib.BitWidth.BIT_WIDTH_8_BITS);

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
     * Data is read from the analog input sets the digital output accordingly. A ChangeListener is used here, so
     * no polling is necessary.
     * <p>
     * The higher the voltage (0..5V) on the analog input, the more LEDs on the digital output ports will be lit up.
     * <p>
     * For example, you can attach a trimmer potentiometer's "left" and "right" connectors to the ground and 5V outlets
     * of the device and the central connector to the analog input. Then, turning the trimmer potentiometers adjustment
     * wheel will cause more or less of the output leds to be lit, reflecting the voltage distribution.
     *
     * @throws Exception if the SIOSLab cannot be found or any other error occurs.
     */
    @Test
    void reflectVoltageUsingChangeListener() throws Exception {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            assertTrue(siosLib.isConnected());

            long startTime = System.currentTimeMillis();

            int maxValuePlusOne = TEST_MODE == SiosLib.SiosLabMode.SIOS_MODE ? 1024 : 256;

            BiConsumer<Integer, Integer> changeListener = (oldValue, newValue) -> {
                int valueOut = 0;
                int factor = 1;
                for (int i = 0; i <= 7; i++) {
                    if (newValue > (maxValuePlusOne * i * 0.125)) {
                        valueOut += factor;
                    }
                    factor *= 2;
                }

                siosLib.setDigitalOutputValue(valueOut);
            };

            siosLib.addAnalogInputChangeListener(ANALOG_INPUT_TO_USE, ANALOG_OUTPUT_BITWIDTH_TO_USE, changeListener);

            while (System.currentTimeMillis() - startTime < 60000) {
                // Just wait...
            }
        }
    }

    /**
     * Sends an "up and down" pattern to the digital output, turning the first LED on, turning it off again and
     * turning the second one on and so on - and backwards once the eight LED was lit.
     */
    @Test
    void sendUpAndDownPattern() {
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
     */
    @Test
    void sendAlternatingPattern() {
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
     * LEDs. This is repeated eight times.
     */
    @Test
    void sendReverseNarrowingPattern() {
        try (SiosLib siosLib = new SiosLib(TEST_MODE)) {
            for (int i = 0; i < 8; i++) {
                assertTrue(siosLib.isConnected());
                siosLib.setDigitalOutputState(new Boolean[]{true, true, true, true, true, true, true, true});
                pause();
                siosLib.setDigitalOutputState(new Boolean[]{true, true, true, false, false, true, true, true});
                pause();
                siosLib.setDigitalOutputState(new Boolean[]{true, true, false, false, false, false, true, true});
                pause();
                siosLib.setDigitalOutputState(new Boolean[]{true, false, false, false, false, false, false, true});
                pause();
                siosLib.setDigitalOutputState(new Boolean[]{false, false, false, false, false, false, false, false});
                pause();
                siosLib.setDigitalOutputState(new Boolean[]{true, false, false, false, false, false, false, true});
                pause();
                siosLib.setDigitalOutputState(new Boolean[]{true, true, false, false, false, false, true, true});
                pause();
                siosLib.setDigitalOutputState(new Boolean[]{true, true, true, false, false, true, true, true});
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

    /**
     * Steadily increases the value (--> voltage) on the analog output and also increasingly lights more of the digital
     * output LEDs respectively. When the output value reaches its maximum, the output voltage should be coarsely at
     * ~5V. All the digital output LEDs will be lit then.
     * <p>
     * This is possible in SIOS mode only.
     * <p>
     * For a more exact display of the output voltage, connect a multimeter to pin 13 (-) and 25 (+; analog output 1) or
     * 26 (+; analog output 2).
     * <p>
     * Please note, that unless in some of the other tests, the digital output does not show anything <i>measured</i>
     * here, but should give a rough estimation of what the output voltage should be in steps between 0V and ~5V.
     */
    @Test
    void setAnalogOutputValue() {
        try (SiosLib siosLib = new SiosLib(SiosLib.SiosLabMode.SIOS_MODE)) { // not supported in CompuLAB mode!
            assertTrue(siosLib.isConnected());

            int maxValue = ANALOG_OUTPUT_BITWIDTH_TO_USE.equals(SiosLib.BitWidth.BIT_WIDTH_10_BITS) ? 1024 : 256;
            int mutiplicator = ANALOG_OUTPUT_BITWIDTH_TO_USE.equals(SiosLib.BitWidth.BIT_WIDTH_10_BITS) ? 16 : 4;

            int factor = 1;
            int value = 0;
            while (value >= 0) {
                if (value >= maxValue) {
                    value = maxValue - 1;
                    factor = -1;
                }
                siosLib.setAnalogOutputValue(ANALOG_OUTPUT_TO_USE, ANALOG_OUTPUT_BITWIDTH_TO_USE, value);
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
            siosLib.setAnalogOutputValue(ANALOG_OUTPUT_TO_USE, ANALOG_OUTPUT_BITWIDTH_TO_USE, 0);
        }
    }

    /**
     * Wait 250ms before going on.
     */
    private void pause() {
        await().pollDelay(250, TimeUnit.MILLISECONDS).untilAsserted(() -> assertTrue(true));
    }

    /**
     * Sends data to the device and waits for 250ms then.
     *
     * @param siosLib The SiosLib instance with the device connected.
     * @param data    The data to send.
     */
    private void sendAndWait(SiosLib siosLib, int data) {
        siosLib.setDigitalOutputValue(data);
        pause();
    }
}
