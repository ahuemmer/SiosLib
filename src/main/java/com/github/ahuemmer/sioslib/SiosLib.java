package com.github.ahuemmer.sioslib;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Library for accessing the standard input and output functions of the
 * <a href="https://www.ak-modul-bus.de/stat/sioslab_interface_usb_com.html">SIOSLAB interface.</a>
 * <p>
 * Use {@link #SiosLib(com.github.ahuemmer.sioslib.SiosLib.SiosLabMode)} with your favourite {@link #siosLabMode} and
 * enjoy. Not sure which {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode} to choose? If your interface really is
 * a SIOSLAB, the best way to got four you should be the
 * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE} (though it's backward compatible with
 * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE}, with some functional limitations). If you have
 * a COMPULAB interface instead, this library could work with it in
 * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE}, but this has never been tested.
 * <p>
 * This SiosLib is probably not suitable for any other kind of interface/hardware.
 */
public class SiosLib implements AutoCloseable {

    /**
     * The sequence of bytes to send in order to test whether a SIOSLAB (or possibly COMPULAB) is connected.
     */
    protected static final byte[] TEST_DATA_SEQUENCE = new byte[] {0x00, 0x00, 0x00, 0x01};

    /**
     * The first part of the sequence of bytes needed to set the operating mode
     * ({@link #SiosLib(com.github.ahuemmer.sioslib.SiosLib.SiosLabMode)}).
     */
    protected static final byte[] SWITCH_MODE_SEQUENCE_PART_1 =
            new byte[] {0x64, 0x1B, 0x03, -0x01}; // -1 means 255 as unsigned byte

    /**
     * The second part of the sequence of bytes needed to set the operating mode
     * ({@link #SiosLib(com.github.ahuemmer.sioslib.SiosLib.SiosLabMode)}).
     */
    protected static final byte[] SWITCH_MODE_SEQUENCE_PART_2 = new byte[] {0x66, 0x1B};

    /**
     * The "command" byte to trigger a digital output in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE = 0x10;

    /**
     * The "command" byte to trigger a digital output in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE}.
     */
    protected static final byte CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE = 0x51;

    /**
     * The "command" byte to trigger a 10-bit output on the first analog port in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_ANALOG_OUTPUT_1_TEN_BITS_SIOS_MODE = 0x48;

    /**
     * The "command" byte to trigger a 10-bit output on the second analog port in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_ANALOG_OUTPUT_2_TEN_BITS_SIOS_MODE = 0x49;

    /**
     * The "command" byte to trigger an 8-bit output on the first analog port in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_ANALOG_OUTPUT_1_EIGHT_BITS_SIOS_MODE = 0x40;

    /**
     * The "command" byte to trigger an8-bit output on the second analog port in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_ANALOG_OUTPUT_2_EIGHT_BITS_SIOS_MODE = 0x41;

    /**
     * The "command" byte to retrieve a value from the digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_SIOS_MODE = 0x20;

    /**
     * The "command" byte to retrieve a value from the digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_COMPULAB_MODE = -0x2D;

    /**
     * The "command" byte to retrieve a value from the first analog input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE = 0x38;

    /**
     * The "command" byte to retrieve a value from the second analog input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE = 0x39;

    /**
     * The "command" byte to retrieve an 8-byte analog value from the first digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_0_SIOS_MODE_EIGHT_BITS = 0x32;

    /**
     * The "command" byte to retrieve an 8-byte analog value from the second digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_1_SIOS_MODE_EIGHT_BITS = 0x33;

    /**
     * The "command" byte to retrieve an 8-byte analog value from the third digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_2_SIOS_MODE_EIGHT_BITS = 0x34;

    /**
     * The "command" byte to retrieve an 8-byte analog value from the fourth digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_3_SIOS_MODE_EIGHT_BITS = 0x35;

    /**
     * The "command" byte to retrieve an 8-byte analog value from the fifth digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_4_SIOS_MODE_EIGHT_BITS = 0x36;

    /**
     * The "command" byte to retrieve an 8-byte analog value from the sixth digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_5_SIOS_MODE_EIGHT_BITS = 0x37;

    /**
     * The "command" byte to retrieve a 10-byte analog value from the first digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_0_SIOS_MODE_TEN_BITS = 0x3A;

    /**
     * The "command" byte to retrieve a 10-byte analog value from the second digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_1_SIOS_MODE_TEN_BITS = 0x3B;

    /**
     * The "command" byte to retrieve a 10-byte analog value from the third digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_2_SIOS_MODE_TEN_BITS = 0x3C;

    /**
     * The "command" byte to retrieve a 10-byte analog value from the fourth digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_3_SIOS_MODE_TEN_BITS = 0x3D;

    /**
     * The "command" byte to retrieve a 10-byte analog value from the fifth digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_4_SIOS_MODE_TEN_BITS = 0x3E;

    /**
     * The "command" byte to retrieve a 10-byte analog value from the sixth digital input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_DIGITAL_5_SIOS_MODE_TEN_BITS = 0x3F;

    /**
     * The "command" byte to retrieve an 8-byte analog value from the first analog input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_ANALOG_1_COMPULAB_MODE_EIGHT_BITS = 0x3C;

    /**
     * The "command" byte to retrieve an 8-byte analog value from the second analog input in
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE}.
     */
    protected static final byte CONTROL_SET_INPUT_ANALOG_2_COMPULAB_MODE_EIGHT_BITS = 0x3A;

    /**
     * Command byte for choosing {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE} during the
     * "switch mode sequence" (see {@link #SWITCH_MODE_SEQUENCE_PART_1} and {@link #SWITCH_MODE_SEQUENCE_PART_2}, where
     * this byte is inserted between the two parts).
     */
    protected static final byte CONTROL_SET_MODE_SIOS = 0x00;

    /**
     * Command byte for choosing {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE} during the
     * "switch mode sequence" (see {@link #SWITCH_MODE_SEQUENCE_PART_1} and {@link #SWITCH_MODE_SEQUENCE_PART_2}, where
     * this byte is inserted between the two parts).
     */
    protected static final byte CONTROL_SET_MODE_COMPULAB = 0x01;

    /**
     * The "command" byte for retrieving the next byte (of a 10-bit value).
     */
    protected static final byte CONTROL_GET_NEXT_BYTE = 0x01;

    /**
     * Control byte indicating {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     */
    protected static final byte MODE_INDICATOR_SIOSLAB = 0x0A;

    /**
     * Control byte indicating {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE}.
     */
    protected static final byte MODE_INDICATOR_COMPULAB = -0x37;

    /**
     * The interval (in ms) to wait after the connection shutdown has been started.
     */
    protected static final int SHUTDOWN_WAITING_INTERVAL = 50; // ms

    /**
     * The maximum amount of time (in ms) to wait for data requested.
     */
    protected static final int RECEIVE_TIMEOUT = 250; // ms

    /**
     * The baud rate used for communicating with the device's serial port.
     */
    protected static final int BAUD_RATE = 19200;

    /**
     * The number of data bits used when communicating with the device's serial port.
     */
    protected static final int NUM_DATABITS = 8;

    /**
     * The logger.
     */
    private static final Logger LOG = LogManager.getLogger(SiosLib.class);

    /**
     * The serial port the device is connected to.
     */
    private SerialPort siosLabPort;

    /**
     * The {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode} the device is operating in.
     */
    private SiosLabMode siosLabMode = null;

    /**
     * The current value of the digital output (will be set to 0 during initialization).
     */
    private int digitalOutputValue;

    /**
     * The 16 possible states (each: on or off) of the 8 digital outputs.
     */
    public enum DigitalOutputState {
        OUTPUT_0_ON,
        OUTPUT_0_OFF,
        OUTPUT_1_ON,
        OUTPUT_1_OFF,
        OUTPUT_2_ON,
        OUTPUT_2_OFF,
        OUTPUT_3_ON,
        OUTPUT_3_OFF,
        OUTPUT_4_ON,
        OUTPUT_4_OFF,
        OUTPUT_5_ON,
        OUTPUT_5_OFF,
        OUTPUT_6_ON,
        OUTPUT_6_OFF,
        OUTPUT_7_ON,
        OUTPUT_7_OFF,
    }

    /**
     * The operation mode of the device.
     */
    public enum SiosLabMode {
        SIOS_MODE,
        COMPULAB_MODE,
    }

    /**
     * Analog input to read values from.
     * (Yes, it's a specialty of the SIOSLab, that one can read <i>analog</i> values from all but two <i>digital</i>
     * ports.)
     */
    public enum AnalogInput {
        ANALOG_INPUT_1,
        ANALOG_INPUT_2,
        DIGITAL_INPUT_0,
        DIGITAL_INPUT_1,
        DIGITAL_INPUT_2,
        DIGITAL_INPUT_3,
        DIGITAL_INPUT_4,
        DIGITAL_INPUT_5
    }

    /**
     * Analog output to write a value to.
     */
    public enum AnalogOutput {
        ANALOG_OUTPUT_1,
        ANALOG_OUTPUT_2
    }

    /**
     * Bit widths for analog value reading and writing.
     */
    public enum BitWidth {
        BIT_WIDTH_8_BITS,
        BIT_WIDTH_10_BITS
    }

    /**
     * Initializes the library by scanning all serial ports for the presence of a SIOSLAB / COMPULAB device and setting
     * it to the mode supplied.
     *
     * @param mode The operation mode to use. {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE} should
     *             be the best option for most cases, unless you have connected a COMPULAB device instead of a SIOSLAB.
     * @throws NoSerialPortFoundException if no serial port was found at all.
     * @throws NoSiosLabFoundException    if serial ports were found, but there does not seem to be a SIOLAB or COMPULAB
     *                                    connected to one of them.
     */
    public SiosLib(SiosLabMode mode) throws NoSerialPortFoundException {

        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0) {
            throw new NoSerialPortFoundException();
        }

        SiosLabMode modeDetected = null;

        for (SerialPort port : ports) {
            LOG.debug("Trying to open serial port {} / {}", port.getSystemPortName(), port.getDescriptivePortName());
            SerialPort portToTry = SerialPort.getCommPort(port.getSystemPortName());
            portToTry.setBaudRate(BAUD_RATE);
            portToTry.setNumDataBits(NUM_DATABITS);
            portToTry.setNumStopBits(SerialPort.ONE_STOP_BIT);
            portToTry.setParity(SerialPort.NO_PARITY);

            if (portToTry.openPort()) {
                LOG.debug("Port opened: {}", portToTry.getSystemPortName());
                modeDetected = testSiosLab(portToTry);
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

        if (modeDetected != mode) {
            switchMode(mode);
        }
    }

    /**
     * Switch between {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE} and
     * {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#COMPULAB_MODE}. If necessary, this is done automatically
     * when initializing the connection using the {@link #SiosLib(com.github.ahuemmer.sioslib.SiosLib.SiosLabMode)
     * constructor} and probably not needed afterward anymore.
     *
     * @param newMode The mode to switch to. If NULL, {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}
     *                will be used.
     */
    public void switchMode(SiosLabMode newMode) {

        if (siosLabMode == newMode) {
            LOG.info(
                    "Switching to {} mode was requested, but this is the currect operating mode already. "
                            + "Not doing anything therefore.",
                    (newMode == SiosLabMode.COMPULAB_MODE ? "COMPULAB" : "SIOSLAB"));
            return;
        }

        LOG.info("Setting mode to {}", newMode == SiosLabMode.COMPULAB_MODE ? "COMPULAB" : "SIOSLAB");

        for (byte b : SWITCH_MODE_SEQUENCE_PART_1) {
            sendData(b);
        }

        sendData(newMode == SiosLabMode.COMPULAB_MODE ? CONTROL_SET_MODE_COMPULAB : CONTROL_SET_MODE_SIOS);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while setting mode!", e);
            Thread.currentThread().interrupt();
        }

        for (byte b : SWITCH_MODE_SEQUENCE_PART_2) {
            sendData(b);
        }

        sendData(newMode == SiosLabMode.COMPULAB_MODE ? CONTROL_SET_MODE_COMPULAB : CONTROL_SET_MODE_SIOS);

        siosLabMode = newMode;

        setDigitalOutputValue(0);

        LOG.trace("Finished setting mode.");
    }

    /**
     * Tests whether a SIOSLAB / COMPULAB device is connected to the given port.
     *
     * @param port The serial port to check for a SIOSLAB / COMPULAB connection.
     * @return The mode of the device found (if any).
     */
    @Nullable
    private SiosLabMode testSiosLab(SerialPort port) {

        for (byte b : TEST_DATA_SEQUENCE) {
            port.writeBytes(new byte[] {b}, 1);
        }

        byte[] buffer = new byte[1];

        boolean readSomething = false;

        long timeBefore = System.currentTimeMillis();

        while (!readSomething && (System.currentTimeMillis() - timeBefore < RECEIVE_TIMEOUT)) {

            if (port.bytesAvailable() == 1) {
                int numBytesRead = port.readBytes(buffer, buffer.length);

                if (numBytesRead == 1) {

                    LOG.trace("{} bytes read", numBytesRead);
                    LOG.trace("Received: {}", buffer[0]);

                    if (buffer[0] == MODE_INDICATOR_SIOSLAB) {
                        LOG.info("Found SiosLab running in SIOS mode on port {}.", port.getSystemPortName());
                        return SiosLabMode.SIOS_MODE;
                    } else if (buffer[0] == MODE_INDICATOR_COMPULAB) {
                        LOG.info("Found SiosLab running in CompuLAB mode on port {}.", port.getSystemPortName());
                        return SiosLabMode.COMPULAB_MODE;
                    }

                    readSomething = true;
                }
            }
        }
        return null;
    }

    /**
     * Returns whether a SIOSLAB / COMPULAB is connected.
     *
     * @return whether a SIOSLAB / COMPULAB is connected.
     */
    public boolean isConnected() {
        return siosLabPort.isOpen();
    }

    /**
     * Sends a single byte of data and waits for an answer from the device respectively.
     *
     * @param data The byte to send (usually a "command" byte to initiate value reading)
     * @return The data received. The value {@code 0} will be returned if {@code 0} was really received <i>or</i> if
     * a timeout occurred while waiting for the answer (see {@link #RECEIVE_TIMEOUT}).
     * @throws ExecutionException   if an exception occurred while waiting for the answer.
     * @throws InterruptedException if waiting for the answer was interrupted.
     */
    private byte[] sendDataAndWaitForAnswer(byte data) throws ExecutionException, InterruptedException {
        CompletableFuture<byte[]> responseFuture = new CompletableFuture<>();

        siosLabPort.addDataListener(new SiosLibResponseListener(responseFuture));

        byte[] result;

        try {
            sendData(data);
            result = responseFuture
                    .completeOnTimeout(new byte[] {0}, RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS)
                    .get();
            LOG.trace("Received: {}", () -> formatByteToString(result));
        } finally {
            siosLabPort.removeDataListener();
        }
        return result;
    }

    /**
     * Sends one byte of data to the device.
     *
     * @param data The byte of data to send.
     */
    private void sendData(byte data) {
        LOG.trace("Sending: {}", () -> formatByteToString(new byte[] {data}));
        siosLabPort.writeBytes(new byte[] {data}, 1);
    }

    /**
     * Gets the combined value of all digital inputs. As there are eight of those, each having {@code 0} or {@code 1},
     * the resulting value will be between {@code 0} and {@code 255}.
     * The highest (eighth) digital port will carry the most significant bit ({@code 127} or {@code 0}).
     *
     * @return The digital inputs' value.
     * @throws ExecutionException   if an exception occurred while waiting for the data.
     * @throws InterruptedException if waiting for the data was interrupted.
     */
    public int getDigitalInputValue() throws ExecutionException, InterruptedException {
        byte[] result = sendDataAndWaitForAnswer(
                siosLabMode == SiosLabMode.SIOS_MODE
                        ? CONTROL_SET_INPUT_DIGITAL_SIOS_MODE
                        : CONTROL_SET_INPUT_DIGITAL_COMPULAB_MODE);
        return byteArrayToInt(result);
    }

    /**
     * Sets the combined value of all digital outputs.
     *
     * @param value The value to set as digital output, where the highest (eighth) digital port carries the most
     *              significant bit ({@code 127}).
     */
    public void setDigitalOutputValue(int value) {

        if ((value < 0) || (value > 255)) {
            throw new IllegalArgumentException("Cannot set a digital output value less than 0 or more than 255");
        }

        byte byteToSend = (byte) value;
        LOG.trace("Setting digital output value: {}", byteToSend);
        sendData(
                siosLabMode == SiosLabMode.SIOS_MODE
                        ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE
                        : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE);
        sendData(byteToSend);
        LOG.trace("Finished setting digital output value.");
        this.digitalOutputValue = value;
    }

    /**
     * Returns the value currently set as digital output.
     *
     * @return the value currently set as digital output.
     * @see #setDigitalOutputValue(int)
     */
    public int getDigitalOutputValue() {
        return this.digitalOutputValue;
    }

    /**
     * Sets the value of an analog output. The voltage measurable on this output will then (roughly!) resemble these
     * values:
     * <ul>
     *     <li>5V for value {@code 1023} with 10 bits bitwidth</li>
     *     <li>2.5V for value {@code 512} with 10 bits bitwidth</li>
     *     <li>5V for value {@code 255} with 8 bits bitwidth</li>
     *     <li>2.5V for value {@code 128} with 8 bits bitwidth</li>
     *     <li>0V for value {@code 0} with either bitwidth</li>
     * </ul>
     * <p>
     * (And, of course "everything in between" for other values.)
     *
     * @param analogOutput which analog output to use
     * @param bitWidth     the bitwidth of the value
     * @param value        the value to use
     */
    public void setAnalogOutputValue(AnalogOutput analogOutput, BitWidth bitWidth, int value) {

        if (this.siosLabMode != SiosLabMode.SIOS_MODE) {
            throw new IllegalStateException("Analog output can only be used in SIOS mode.");
        }

        if ((bitWidth.equals(BitWidth.BIT_WIDTH_10_BITS)) && ((value < 0) || (value > 1023))) {
            throw new IllegalArgumentException("Analog output value must be between 0 and 1023 in 10-bit mode");
        }
        if ((bitWidth.equals(BitWidth.BIT_WIDTH_8_BITS)) && ((value < 0) || (value > 255))) {
            throw new IllegalArgumentException("Analog output value must be between 0 and 255 in 8-bit mode");
        }
        byte[] bytes = ByteBuffer.allocate(4).putInt(value).array();
        byte[] bytesToSend = new byte[] {bytes[2], bytes[3]};

        LOG.trace(
                "Setting analog output value {}: {}|{} ({})",
                analogOutput.equals(AnalogOutput.ANALOG_OUTPUT_1) ? "1" : "2",
                bytesToSend[1],
                bytesToSend[0],
                value);
        if (bitWidth.equals(BitWidth.BIT_WIDTH_10_BITS)) {
            sendData(
                    analogOutput.equals(AnalogOutput.ANALOG_OUTPUT_1)
                            ? CONTROL_SET_ANALOG_OUTPUT_1_TEN_BITS_SIOS_MODE
                            : CONTROL_SET_ANALOG_OUTPUT_2_TEN_BITS_SIOS_MODE);
            sendData(bytesToSend[0]);
            sendData(bytesToSend[1]);
        } else {
            sendData(
                    analogOutput.equals(AnalogOutput.ANALOG_OUTPUT_1)
                            ? CONTROL_SET_ANALOG_OUTPUT_1_EIGHT_BITS_SIOS_MODE
                            : CONTROL_SET_ANALOG_OUTPUT_2_EIGHT_BITS_SIOS_MODE);
            sendData(bytesToSend[1]);
        }
    }

    /**
     * Checks whether there are conflicting (= contradictory) states in the input set, e. g. of
     * {@link #changeDigitalOutputState(com.github.ahuemmer.sioslib.SiosLib.DigitalOutputState...)}.
     *
     * @param states The set of the states to check for conflicts.
     * @return {@code true}, if there is a contradiction in the states set, {@code false} otherwise.
     */
    private boolean outputStatesAreConflicting(Set<DigitalOutputState> states) {
        int i = 0;

        boolean stateOn = false;
        for (DigitalOutputState digitalOutputState : DigitalOutputState.values()) {
            if (i % 2 == 0) {
                stateOn = states.contains(digitalOutputState);
            } else {
                if (stateOn && states.contains(digitalOutputState)) {
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    /**
     * Change the state of one or more digital outputs at once. Only the states of the outputs given in the
     * {@code state} parameter are changed (if necessary). All other digital output states are left as they were before.
     * This allows a more convenient control of the individual digital outputs as {@link #setDigitalOutputValue(int)}
     * does.
     * <p>
     * If you specify, for example, {@link com.github.ahuemmer.sioslib.SiosLib.DigitalOutputState#OUTPUT_1_OFF} and
     * {@link com.github.ahuemmer.sioslib.SiosLib.DigitalOutputState#OUTPUT_4_ON} here, the second digital output will
     * be set off (if it has not been yet) and the fifth digital output will be set on (same). All other outputs will
     * remain in the states they had before. (Please note the difference between the digital output count and their
     * labels on the device. The <i>first</i> digital output is digital output <i>0</i> and so on.)
     *
     * @param state The new state/states, the digital outputs contained should have.
     */
    public void changeDigitalOutputState(DigitalOutputState... state) {

        if (state[0] == null) {
            throw new IllegalArgumentException("Given digital output states are null");
        }

        int digitalOutputChannels = DigitalOutputState.values().length / 2;

        if (state.length > digitalOutputChannels) {
            throw new IllegalArgumentException(
                    "Digital output state must have at most " + digitalOutputChannels + " parameters");
        }

        Set<DigitalOutputState> states = new HashSet<>(Arrays.asList(state));

        if (this.outputStatesAreConflicting(states)) {
            throw new IllegalArgumentException("Contradictory output states (ON and OFF for the same channel) given");
        }

        int i;

        boolean[] newOutputState = new boolean[digitalOutputChannels];
        for (i = 0; i < digitalOutputChannels; ++i) {
            newOutputState[i] = (this.digitalOutputValue & (1 << i)) != 0;
        }
        Boolean[] desiredOutputState = new Boolean[digitalOutputChannels];

        i = 0;
        boolean onOrOff = true;

        for (DigitalOutputState digitalOutputState : DigitalOutputState.values()) {
            if (states.contains(digitalOutputState)) {
                desiredOutputState[i / 2] = onOrOff;
            }
            onOrOff = !onOrOff;
            i += 1;
        }

        for (i = 0; i < digitalOutputChannels; i++) {
            if (desiredOutputState[i] != null) {
                newOutputState[i] = desiredOutputState[i];
            }
        }
        setDigitalOutputState(newOutputState);
    }

    /**
     * Formats a byte array as a string, just for better readability in the logs.
     *
     * @param data The byte array to turn into a string.
     * @return The string representation of the byte array.
     */
    private static String formatByteToString(byte[] data) {
        StringBuilder dataString = new StringBuilder(String.format("%04d", byteArrayToInt(data)));

        dataString.append(" / ");

        for (int i = data.length - 1; i >= 0; i--) {
            byte b = data[i];
            dataString.append(
                    String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        return dataString.toString();
    }

    /**
     * Returns the integer value of a byte array.
     *
     * @param bytes The byte array the integer value of which is needed.
     * @return the integer value of the byte array.
     */
    public static int byteArrayToInt(byte[] bytes) {
        int result = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            result = (result << 8) | (bytes[i] & 0xFF);
        }
        return result;
    }

    /**
     * Gets the current value of an analog input, reflecting the voltage applied to it.
     *
     * @param analogInput The input to query.
     * @param bitWidth    The desired bit width of the result. If you choose
     *                    {@link com.github.ahuemmer.sioslib.SiosLib.BitWidth#BIT_WIDTH_10_BITS}, the values are more
     *                    fine-grained between {@code 0} and {@code 1023}. Otherwise, they will be between {@code 0} and
     *                    {@code 255}.
     *                    Note: {@link com.github.ahuemmer.sioslib.SiosLib.BitWidth#BIT_WIDTH_10_BITS} is only possible
     *                    in {@link com.github.ahuemmer.sioslib.SiosLib.SiosLabMode#SIOS_MODE}.
     * @return The value read from the analog input.
     * @throws ExecutionException   if an exception occurred while waiting for the data.
     * @throws InterruptedException if waiting for the data was interrupted.
     */
    public int getAnalogValue(AnalogInput analogInput, BitWidth bitWidth)
            throws ExecutionException, InterruptedException {

        LOG.trace("Getting analog value from {}", analogInput);

        if ((siosLabMode == SiosLabMode.COMPULAB_MODE)
                && (analogInput != AnalogInput.ANALOG_INPUT_1)
                && (analogInput != AnalogInput.ANALOG_INPUT_2)) {
            throw new IllegalArgumentException(
                    "The digital inputs may be used for analog value retrieval in SIOSLab mode only.");
        }

        if ((siosLabMode == SiosLabMode.COMPULAB_MODE) && (bitWidth == BitWidth.BIT_WIDTH_10_BITS)) {
            throw new IllegalArgumentException("10-bit values can only be retrieved in SIOSLab mode.");
        }

        byte[] newData = sendDataAndWaitForAnswer(getAnalogInputCommandByte(analogInput, bitWidth));

        byte[] result;

        if (siosLabMode == SiosLabMode.SIOS_MODE) {
            byte[] additionalData = sendDataAndWaitForAnswer(CONTROL_GET_NEXT_BYTE);
            result = new byte[newData.length + additionalData.length];
            System.arraycopy(additionalData, 0, result, 0, additionalData.length);
            System.arraycopy(newData, 0, result, additionalData.length, newData.length);
        } else {
            result = newData;
        }

        LOG.trace("Received analog value: {}", () -> formatByteToString(newData));

        return byteArrayToInt(result);
    }

    /**
     * Determines the "command" byte to send to the device in order to query an analog input, e. g. in
     * {@link #getAnalogValue(com.github.ahuemmer.sioslib.SiosLib.AnalogInput, com.github.ahuemmer.sioslib.SiosLib.BitWidth)}.
     *
     * @param analogInput The analog input to use.
     * @param bitWidth    The bitwidth to use.
     * @return The command byte associated with the given input and bitwidth.
     */
    private byte getAnalogInputCommandByte(AnalogInput analogInput, BitWidth bitWidth) {
        if (siosLabMode == SiosLabMode.COMPULAB_MODE) {
            return analogInput == AnalogInput.ANALOG_INPUT_1
                    ? CONTROL_SET_INPUT_ANALOG_1_COMPULAB_MODE_EIGHT_BITS
                    : CONTROL_SET_INPUT_ANALOG_2_COMPULAB_MODE_EIGHT_BITS;
        }

        return switch (analogInput) {
            case ANALOG_INPUT_1 -> CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE;
            case ANALOG_INPUT_2 -> CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE;
            case DIGITAL_INPUT_0 ->
                bitWidth == BitWidth.BIT_WIDTH_8_BITS
                        ? CONTROL_SET_INPUT_DIGITAL_0_SIOS_MODE_EIGHT_BITS
                        : CONTROL_SET_INPUT_DIGITAL_0_SIOS_MODE_TEN_BITS;
            case DIGITAL_INPUT_1 ->
                bitWidth == BitWidth.BIT_WIDTH_8_BITS
                        ? CONTROL_SET_INPUT_DIGITAL_1_SIOS_MODE_EIGHT_BITS
                        : CONTROL_SET_INPUT_DIGITAL_1_SIOS_MODE_TEN_BITS;
            case DIGITAL_INPUT_2 ->
                bitWidth == BitWidth.BIT_WIDTH_8_BITS
                        ? CONTROL_SET_INPUT_DIGITAL_2_SIOS_MODE_EIGHT_BITS
                        : CONTROL_SET_INPUT_DIGITAL_2_SIOS_MODE_TEN_BITS;
            case DIGITAL_INPUT_3 ->
                bitWidth == BitWidth.BIT_WIDTH_8_BITS
                        ? CONTROL_SET_INPUT_DIGITAL_3_SIOS_MODE_EIGHT_BITS
                        : CONTROL_SET_INPUT_DIGITAL_3_SIOS_MODE_TEN_BITS;
            case DIGITAL_INPUT_4 ->
                bitWidth == BitWidth.BIT_WIDTH_8_BITS
                        ? CONTROL_SET_INPUT_DIGITAL_4_SIOS_MODE_EIGHT_BITS
                        : CONTROL_SET_INPUT_DIGITAL_4_SIOS_MODE_TEN_BITS;
            default ->
                bitWidth == BitWidth.BIT_WIDTH_8_BITS
                        ? CONTROL_SET_INPUT_DIGITAL_5_SIOS_MODE_EIGHT_BITS
                        : CONTROL_SET_INPUT_DIGITAL_5_SIOS_MODE_TEN_BITS;
        };
    }

    /**
     * Convenience function to set the digital output state via an array of eight booleans.
     *
     * @param digitalOutputState The desired new digital output state. The first array entry denotes the first digital
     *                           output and so on.
     */
    public void setDigitalOutputState(boolean[] digitalOutputState) {
        if (digitalOutputState == null) {
            throw new IllegalArgumentException("digitalOutputState must not be null.");
        }
        if (digitalOutputState.length != 8) {
            throw new IllegalArgumentException(
                    "digitalOutputState must have exactly 8 booleans, each representing one output channel.");
        }
        int newDigitalOutputValue = 0;
        int bitValue = 1;
        for (int i = 0; i < 8; i++) {
            if (digitalOutputState[i]) {
                newDigitalOutputValue += bitValue;
            }
            bitValue *= 2;
        }
        setDigitalOutputValue(newDigitalOutputValue);
    }

    /**
     * Close the connection to the SIOSLAB / COMPULAB.
     */
    @Override
    public void close() {
        LOG.info("Closing SiosLab port {}", siosLabPort.getSystemPortName());
        sendData(
                siosLabMode == SiosLabMode.SIOS_MODE
                        ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE
                        : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE);
        sendData((byte) 0);
        try {
            Thread.sleep(SHUTDOWN_WAITING_INTERVAL);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted when shutting down!", e);
            Thread.currentThread().interrupt();
        }
        siosLabPort.closePort();
    }

    /**
     * Specialized SerialPortDataListener to react just on
     * {@link com.fazecast.jSerialComm.SerialPort#LISTENING_EVENT_DATA_AVAILABLE} events.
     */
    class SiosLibResponseListener implements SerialPortDataListener {

        /**
         * The future for the response (data read).
         */
        private CompletableFuture<byte[]> responseFuture;

        /**
         * Constructor, handing over the response future to complete after data receival.
         *
         * @param responseFuture The response future to complete after data receival.
         */
        public SiosLibResponseListener(CompletableFuture<byte[]> responseFuture) {
            this.responseFuture = responseFuture;
        }

        /**
         * Specification of the listening events of this listener - just
         * {@link com.fazecast.jSerialComm.SerialPort#LISTENING_EVENT_DATA_AVAILABLE} here.
         *
         * @return {@link com.fazecast.jSerialComm.SerialPort#LISTENING_EVENT_DATA_AVAILABLE}.
         */
        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
        }

        /**
         * How to react on the listening event. In this case, the available data will be read and returned via the
         * {@link #responseFuture}.
         *
         * @param event A {@link SerialPortEvent} object containing information and/or data about the serial events that
         *              occurred.
         */
        @Override
        public void serialEvent(SerialPortEvent event) {
            LOG.debug("Got serial event {}", event);
            if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

            byte[] buffer = new byte[siosLabPort.bytesAvailable()];
            siosLabPort.readBytes(buffer, buffer.length);
            responseFuture.complete(buffer);
        }
    }
}
