package com.github.ahuemmer.sioslib;

import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_ANALOG_OUTPUT_1_EIGHT_BITS_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_ANALOG_OUTPUT_1_TEN_BITS_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_ANALOG_OUTPUT_2_EIGHT_BITS_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_ANALOG_OUTPUT_2_TEN_BITS_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_0_SIOS_MODE_EIGHT_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_0_SIOS_MODE_TEN_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_1_SIOS_MODE_EIGHT_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_1_SIOS_MODE_TEN_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_2_SIOS_MODE_EIGHT_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_2_SIOS_MODE_TEN_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_3_SIOS_MODE_EIGHT_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_3_SIOS_MODE_TEN_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_4_SIOS_MODE_EIGHT_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_4_SIOS_MODE_TEN_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_5_SIOS_MODE_EIGHT_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_INPUT_DIGITAL_5_SIOS_MODE_TEN_BITS;
import static com.github.ahuemmer.sioslib.SiosLib.SiosLabMode.SIOS_MODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class SiosLibTest {

    @Mock
    SerialPort serialPort;

    @Nested
    @DisplayName("connection and initialization")
    class ConnectionAndInitialization {

        @Test
        @DisplayName("throws NoSerialPortFoundException, if no serial ports were found")
        void throws_NoSerialPortFoundException_if_no_COM_ports_were_found() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {});
                assertThrows(NoSerialPortFoundException.class, () -> new SiosLib(SiosLib.SiosLabMode.SIOS_MODE));
            }
        }

        @Test
        @DisplayName("throws NoSiosLabFoundException, if no serial ports could be opened")
        void throws_NoSiosLabFoundException_if_no_COM_ports_could_be_opened() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                when(serialPort.openPort()).thenReturn(false);
                when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
                when(serialPort.getDescriptivePortName()).thenReturn("COM123");
                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});
                assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SiosLabMode.SIOS_MODE));
            }
        }

        @Test
        @DisplayName(
                "tries to open every serial port and throws NoSiosLabFoundException, if no SIOSLab was found in time")
        void tries_to_open_every_serial_port_and_throws_NoSiosLabFoundException_if_no_SIOSLab_was_found_in_time() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                SerialPort serialPort2 = mock(SerialPort.class);

                when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
                when(serialPort.getDescriptivePortName()).thenReturn("COM123");
                when(serialPort.openPort()).thenReturn(true);

                when(serialPort2.getSystemPortName()).thenReturn("Serial Port 124");
                when(serialPort2.getDescriptivePortName()).thenReturn("COM124");
                when(serialPort2.openPort()).thenReturn(true);

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 124"))
                        .thenReturn(serialPort2);

                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort, serialPort2
                });

                assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SiosLabMode.SIOS_MODE));
                verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
                verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
                verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
                verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
                verify(serialPort, times(1)).openPort();
            }
        }

        @Test
        @DisplayName(
                "tries to find a SIOSLab on an opened port and throws NoSiosLabFoundException, if it fails because of wrong answer from the interface")
        void
                tries_to_find_a_SIOSLab_on_an_opened_port_and_throws_NoSiosLabFoundException_if_it_fails_because_of_wrong_answer_from_the_interface() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
                when(serialPort.getDescriptivePortName()).thenReturn("COM123");
                when(serialPort.openPort()).thenReturn(true);
                when(serialPort.bytesAvailable()).thenReturn(1);
                doAnswer(invocation -> {
                            byte[] buffer = invocation.getArgument(0);
                            byte[] testData = {0x00};
                            System.arraycopy(testData, 0, buffer, 0, testData.length);
                            return testData.length;
                        })
                        .when(serialPort)
                        .readBytes(any(byte[].class), eq(1));

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);

                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SiosLabMode.SIOS_MODE));
                verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
                verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
                verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
                verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
                verify(serialPort, times(1)).openPort();
            }
        }

        @Test
        @DisplayName(
                "tries to find a SIOSLab on an opened port and throws NoSiosLabFoundException, if it fails because of wrong number of bytes available")
        void
                tries_to_find_a_SIOSLab_on_an_opened_port_and_throws_NoSiosLabFoundException_if_it_fails_because_of_wrong_number_of_bytes_available() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
                when(serialPort.getDescriptivePortName()).thenReturn("COM123");
                when(serialPort.openPort()).thenReturn(true);
                when(serialPort.bytesAvailable()).thenReturn(5);

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);

                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SiosLabMode.SIOS_MODE));
                verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
                verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
                verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
                verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
                verify(serialPort, times(1)).openPort();
            }
        }

        @Test
        @DisplayName(
                "tries to find a SIOSLab on an opened port and throws NoSiosLabFoundException, if it fails because of wrong number of bytes read")
        void
                tries_to_find_a_SIOSLab_on_an_opened_port_and_throws_NoSiosLabFoundException_if_it_fails_because_of_wrong_number_of_bytes_read() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
                when(serialPort.getDescriptivePortName()).thenReturn("COM123");
                when(serialPort.openPort()).thenReturn(true);
                when(serialPort.bytesAvailable()).thenReturn(1);
                doAnswer(invocation -> {
                            byte[] buffer = invocation.getArgument(0);
                            byte[] testData = new byte[] {0x00};
                            System.arraycopy(testData, 0, buffer, 0, testData.length);
                            return 99;
                        })
                        .when(serialPort)
                        .readBytes(any(byte[].class), eq(1));

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);

                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SiosLabMode.SIOS_MODE));
                verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
                verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
                verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
                verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
                verify(serialPort, times(1)).openPort();
            }
        }
    }

    @ParameterizedTest
    @DisplayName("connects to SIOSLab")
    @EnumSource(SiosLib.SiosLabMode.class)
    void connects_to_SIOSLab(SiosLib.SiosLabMode mode) {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

            when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
            when(serialPort.getDescriptivePortName()).thenReturn("COM123");
            when(serialPort.openPort()).thenReturn(true);
            when(serialPort.bytesAvailable()).thenReturn(1);
            doAnswer(invocation -> {
                        byte[] buffer = invocation.getArgument(0);
                        byte[] testData = new byte[] {SiosLib.MODE_INDICATOR_SIOSLAB};
                        System.arraycopy(testData, 0, buffer, 0, testData.length);
                        return testData.length;
                    })
                    .when(serialPort)
                    .readBytes(any(byte[].class), eq(1));

            serialPortStaticMock
                    .when(() -> SerialPort.getCommPort("Serial Port 123"))
                    .thenReturn(serialPort);

            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

            long start = System.nanoTime();
            SiosLib siosLib = new SiosLib(mode);

            if (mode == SiosLib.SiosLabMode.COMPULAB_MODE) {
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                assertTrue(elapsedMs >= 50L);
            }

            verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
            verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
            verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
            verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
            verify(serialPort, times(1)).openPort();
            verify(serialPort, times(mode == SiosLib.SiosLabMode.COMPULAB_MODE ? 4 : 3))
                    .writeBytes(new byte[] {0x00}, 1);
            verify(serialPort, times(mode == SiosLib.SiosLabMode.COMPULAB_MODE ? 3 : 1))
                    .writeBytes(new byte[] {0x01}, 1);
            assertEquals(0, siosLib.getDigitalOutputValue());
        }
    }

    @Test
    @DisplayName("handles InterruptedException during mode switching")
    void handles_InterruptedException_during_mode_switching() throws InterruptedException {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

            prepareSerialPortMock();

            serialPortStaticMock
                    .when(() -> SerialPort.getCommPort("Serial Port 123"))
                    .thenReturn(serialPort);
            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

            AtomicBoolean wasInterrupted = new AtomicBoolean(false);

            SiosLib siosLib = new SiosLib(SiosLib.SiosLabMode.SIOS_MODE);

            Thread testThread = new Thread(() -> {
                Thread.currentThread().interrupt();
                siosLib.switchMode(SiosLib.SiosLabMode.COMPULAB_MODE);
                wasInterrupted.set(Thread.currentThread().isInterrupted());
            });

            testThread.start();
            testThread.join();

            assertTrue(wasInterrupted.get());
        }
    }

    @ParameterizedTest
    @DisplayName("does not switch to a mode already set")
    @EnumSource(SiosLib.SiosLabMode.class)
    void does_not_switch_to_a_mode_already_set(SiosLib.SiosLabMode mode) {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

            prepareSerialPortMock();

            serialPortStaticMock
                    .when(() -> SerialPort.getCommPort("Serial Port 123"))
                    .thenReturn(serialPort);
            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

            SiosLib siosLib = new SiosLib(mode);

            clearInvocations(serialPort);

            siosLib.switchMode(mode);

            verify(serialPort, times(0)).writeBytes(new byte[] {0x00}, 1);
            verify(serialPort, times(0)).writeBytes(new byte[] {0x01}, 1);
        }
    }

    @Nested
    @DisplayName("isConnected")
    class IsConnected {

        @Test
        @DisplayName("indicates whether SiosLib is connected")
        void indicates_whether_SiosLib_is_connected() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SiosLib.SiosLabMode.SIOS_MODE);

                when(serialPort.isOpen()).thenReturn(true);
                assertTrue(siosLib.isConnected());
                when(serialPort.isOpen()).thenReturn(false);
                assertFalse(siosLib.isConnected());
            }
        }
    }

    @Nested
    @DisplayName("getDigitalInputValue")
    class GetDigitalValue {

        @ParameterizedTest
        @DisplayName("returns a valid digital value if data could be fetched in time")
        @EnumSource(SiosLib.SiosLabMode.class)
        void returns_a_valid_digital_value_if_data_could_be_fetched_in_time(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(mode);

                when(serialPort.bytesAvailable()).thenReturn(1);
                doAnswer(invocation -> {
                            byte[] buffer = invocation.getArgument(0);
                            byte[] testData = new byte[] {33};
                            System.arraycopy(testData, 0, buffer, 0, testData.length);
                            return testData.length;
                        })
                        .when(serialPort)
                        .readBytes(any(byte[].class), eq(1));

                doAnswer(invocation -> {
                            SerialPortDataListener serialPortDataListener = invocation.getArgument(0);
                            assertEquals(
                                    SerialPort.LISTENING_EVENT_DATA_AVAILABLE,
                                    serialPortDataListener.getListeningEvents());

                            // First fire another event that is to be ignored
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.TIMEOUT_NONBLOCKING));
                            Awaitility.await().atLeast(Duration.ofMillis(10));
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_AVAILABLE));
                            return null;
                        })
                        .when(serialPort)
                        .addDataListener(any(SerialPortDataListener.class));

                Awaitility.await()
                        .atMost(Duration.ofMillis(SiosLib.RECEIVE_TIMEOUT))
                        .untilAsserted(() -> assertEquals(33, siosLib.getDigitalInputValue()));

                byte[] data = new byte[] {
                    mode == SIOS_MODE
                            ? SiosLib.CONTROL_SET_INPUT_DIGITAL_SIOS_MODE
                            : SiosLib.CONTROL_SET_INPUT_DIGITAL_COMPULAB_MODE
                };
                verify(serialPort, times(1)).writeBytes(data, data.length);
            }
        }

        @ParameterizedTest
        @DisplayName("returns 0 for digital value if no data could be fetched in time")
        @EnumSource(SiosLib.SiosLabMode.class)
        void returns_0_for_digital_value_if_no_data_could_be_fetched_in_time(SiosLib.SiosLabMode mode)
                throws ExecutionException, InterruptedException {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(mode);

                assertEquals(0, siosLib.getDigitalInputValue());
            }
        }
    }

    @Nested
    @DisplayName("setDigitalOutputValue")
    class SetDigitalOutputValue {

        @ParameterizedTest
        @DisplayName("sets output value")
        @EnumSource(SiosLib.SiosLabMode.class)
        void sets_output_value_in_SIOS_mode(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(mode);

                clearInvocations(serialPort);

                siosLib.setDigitalOutputValue(5);

                byte[] controlData = new byte[] {
                    mode == SIOS_MODE ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE
                };

                verify(serialPort, times(1)).writeBytes(controlData, controlData.length);
                verify(serialPort, times(1)).writeBytes(new byte[] {5}, 1);
                assertEquals(5, siosLib.getDigitalOutputValue());
            }
        }

        @Test
        @DisplayName("does not allow values below 0")
        void does_not_allow_values_below_0() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                assertThrows(IllegalArgumentException.class, () -> siosLib.setDigitalOutputValue(-10));
            }
        }

        @Test
        @DisplayName("does not allow values higher than 255")
        void does_not_allow_values_higher_than_255() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                assertThrows(IllegalArgumentException.class, () -> siosLib.setDigitalOutputValue(17849715));
            }
        }
    }

    @Nested
    @DisplayName("changeDigitalOutputState with primitive boolean array")
    class SetDigitalOutputStateWithPrimitiveBooleanArray {

        @ParameterizedTest
        @DisplayName("sets output state")
        @EnumSource(SiosLib.SiosLabMode.class)
        void sets_output_state(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(mode);

                clearInvocations(serialPort);

                siosLib.setDigitalOutputState(new boolean[] {true, false, true, false, true, false, true, false});

                byte[] controlData = new byte[] {
                    mode == SIOS_MODE ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE
                };

                verify(serialPort, times(1)).writeBytes(controlData, controlData.length);
                verify(serialPort, times(1)).writeBytes(new byte[] {85}, 1);
                assertEquals(85, siosLib.getDigitalOutputValue());
            }
        }

        @ParameterizedTest
        @DisplayName("throws IllegalArgumentException if outputstate is null")
        @EnumSource(SiosLib.SiosLabMode.class)
        void throws_IllegalArgumentException_if_outputstate_is_null(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                clearInvocations(serialPort);

                assertThrows(IllegalArgumentException.class, () -> siosLib.setDigitalOutputState((boolean[]) null));

                byte[] controlData = new byte[] {
                    mode == SIOS_MODE ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE
                };

                verify(serialPort, times(0)).writeBytes(controlData, controlData.length);
            }
        }

        @ParameterizedTest
        @DisplayName("throws IllegalArgumentException if outputstate does not have exactly eight bits")
        @EnumSource(SiosLib.SiosLabMode.class)
        void throws_IllegalArgumentException_if_outputstate_does_not_have_exactly_eight_bits(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                clearInvocations(serialPort);

                assertThrows(IllegalArgumentException.class, () -> siosLib.setDigitalOutputState(new boolean[] {true}));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.setDigitalOutputState(
                                new boolean[] {true, true, true, true, true, true, true, true, true}));

                byte[] controlData = new byte[] {
                    mode == SIOS_MODE ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE
                };

                verify(serialPort, times(0)).writeBytes(controlData, controlData.length);
            }
        }
    }

    @Nested
    @DisplayName("changeDigitalOutputState with Boolean array")
    class SetDigitalOutputStateWithBooleanArray {

        @ParameterizedTest
        @DisplayName("sets output state")
        @EnumSource(SiosLib.SiosLabMode.class)
        void sets_output_state(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(mode);

                clearInvocations(serialPort);

                siosLib.setDigitalOutputState(new Boolean[] {true, false, true, false, true, false, true, false});

                byte[] controlData = new byte[] {
                    mode == SIOS_MODE ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE
                };

                verify(serialPort, times(1)).writeBytes(controlData, controlData.length);
                verify(serialPort, times(1)).writeBytes(new byte[] {85}, 1);
                assertEquals(85, siosLib.getDigitalOutputValue());
            }
        }

        @ParameterizedTest
        @DisplayName("throws IllegalArgumentException if outputstate is null")
        @EnumSource(SiosLib.SiosLabMode.class)
        void throws_IllegalArgumentException_if_outputstate_is_null(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                clearInvocations(serialPort);

                assertThrows(IllegalArgumentException.class, () -> siosLib.setDigitalOutputState((Boolean[]) null));

                byte[] controlData = new byte[] {
                    mode == SIOS_MODE ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE
                };

                verify(serialPort, times(0)).writeBytes(controlData, controlData.length);
            }
        }

        @ParameterizedTest
        @DisplayName("throws IllegalArgumentException if outputstate does not have exactly eight bits")
        @EnumSource(SiosLib.SiosLabMode.class)
        void throws_IllegalArgumentException_if_outputstate_does_not_have_exactly_eight_bits(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                clearInvocations(serialPort);

                assertThrows(IllegalArgumentException.class, () -> siosLib.setDigitalOutputState(new Boolean[] {true}));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.setDigitalOutputState(
                                new boolean[] {true, true, true, true, true, true, true, true, true}));

                byte[] controlData = new byte[] {
                    mode == SIOS_MODE ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE
                };

                verify(serialPort, times(0)).writeBytes(controlData, controlData.length);
            }
        }
    }

    @Nested
    @DisplayName("changeDigitalOutputState with state flags")
    class SetDigitalOutputStateStateFlags {

        @Test
        @DisplayName("throws IllegalArgumentException if desired output states are null")
        void throws_IllegalArgumentException_if_desired_output_states_are_null() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState((SiosLib.DigitalOutputState) null));
            }
        }

        @Test
        @DisplayName("throws IllegalArgumentException on contradictory state flags")
        void throws_IllegalArgumentException_on_contradictory_state_flags() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_0_ON, SiosLib.DigitalOutputState.OUTPUT_0_OFF));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_1_ON,
                                SiosLib.DigitalOutputState.OUTPUT_1_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_4_OFF));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_1_ON,
                                SiosLib.DigitalOutputState.OUTPUT_2_ON,
                                SiosLib.DigitalOutputState.OUTPUT_2_OFF));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_7_ON,
                                SiosLib.DigitalOutputState.OUTPUT_3_ON,
                                SiosLib.DigitalOutputState.OUTPUT_3_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_4_OFF));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_4_ON,
                                SiosLib.DigitalOutputState.OUTPUT_2_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_4_OFF));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_1_ON,
                                SiosLib.DigitalOutputState.OUTPUT_5_ON,
                                SiosLib.DigitalOutputState.OUTPUT_5_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_4_OFF));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_4_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_6_ON,
                                SiosLib.DigitalOutputState.OUTPUT_7_ON,
                                SiosLib.DigitalOutputState.OUTPUT_6_OFF));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_1_ON,
                                SiosLib.DigitalOutputState.OUTPUT_7_ON,
                                SiosLib.DigitalOutputState.OUTPUT_5_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_7_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_4_OFF));
            }
        }

        @Test
        @DisplayName("throws IllegalArgumentException on more than eight state flags")
        void throws_IllegalArgumentException_on_more_than_eight_state_flags() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.changeDigitalOutputState(
                                SiosLib.DigitalOutputState.OUTPUT_0_ON,
                                SiosLib.DigitalOutputState.OUTPUT_0_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_0_ON,
                                SiosLib.DigitalOutputState.OUTPUT_0_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_0_ON,
                                SiosLib.DigitalOutputState.OUTPUT_0_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_0_ON,
                                SiosLib.DigitalOutputState.OUTPUT_0_OFF,
                                SiosLib.DigitalOutputState.OUTPUT_2_OFF));
            }
        }

        @Test
        @DisplayName("correctly sets the new output state based on the previous one")
        void correctly_sets_the_new_output_state_based_on_the_previous_one() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                siosLib.setDigitalOutputValue(33); // 00100001

                siosLib.changeDigitalOutputState(SiosLib.DigitalOutputState.OUTPUT_0_OFF); // 00100000

                assertEquals(32, siosLib.getDigitalOutputValue());

                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_5_OFF, SiosLib.DigitalOutputState.OUTPUT_2_ON); // 00000100

                assertEquals(4, siosLib.getDigitalOutputValue());

                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_7_ON,
                        SiosLib.DigitalOutputState.OUTPUT_6_ON,
                        SiosLib.DigitalOutputState.OUTPUT_4_OFF,
                        SiosLib.DigitalOutputState.OUTPUT_3_ON,
                        SiosLib.DigitalOutputState.OUTPUT_2_OFF,
                        SiosLib.DigitalOutputState.OUTPUT_0_ON); // 11001001

                assertEquals(201, siosLib.getDigitalOutputValue());

                siosLib.changeDigitalOutputState(
                        SiosLib.DigitalOutputState.OUTPUT_3_ON, SiosLib.DigitalOutputState.OUTPUT_1_ON);

                assertEquals(203, siosLib.getDigitalOutputValue());
            }
        }
    }

    @Nested
    @DisplayName("analog value retrieval")
    class AnalogValueRetrieval {

        @ParameterizedTest
        @DisplayName("returns a valid analog value if data could be fetched in time in SIOS mode")
        @MethodSource("provideArguments")
        void returns_a_valid_analog_value_if_data_could_be_fetched_in_time_in_sios_mode(
                SiosLib.AnalogInput input, SiosLib.BitWidth bitWidth) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                clearInvocations(serialPort); // important, as 0x01 (both CONTROL_GET_NEXT_BYTE and part of the
                // TEST_DATA_SEQUENCE) is also sent during initialization

                when(serialPort.bytesAvailable()).thenReturn(1);

                doAnswer(new Answer() {
                            int answerCount = 0;

                            @Override
                            public Object answer(InvocationOnMock invocation) {
                                byte[] buffer = invocation.getArgument(0);
                                byte[] testData = answerCount == 0 ? new byte[] {0x01} : new byte[] {0x10};
                                System.arraycopy(testData, 0, buffer, 0, testData.length);
                                answerCount++;
                                return testData.length;
                            }
                        })
                        .when(serialPort)
                        .readBytes(any(byte[].class), eq(1));

                doAnswer(invocation -> {
                            SerialPortDataListener serialPortDataListener = invocation.getArgument(0);
                            assertEquals(
                                    SerialPort.LISTENING_EVENT_DATA_AVAILABLE,
                                    serialPortDataListener.getListeningEvents());

                            // First fire another event that is to be ignored
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.TIMEOUT_NONBLOCKING));
                            Awaitility.await().atLeast(Duration.ofMillis(10));
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_AVAILABLE));
                            return null;
                        })
                        .when(serialPort)
                        .addDataListener(any(SerialPortDataListener.class));

                Awaitility.await()
                        .atMost(Duration.ofMillis(SiosLib.RECEIVE_TIMEOUT))
                        .untilAsserted(() -> assertEquals(272, siosLib.getAnalogValue(input, bitWidth)));

                byte controlByte =
                        switch (input) {
                            case ANALOG_INPUT_1 -> CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE;
                            case ANALOG_INPUT_2 -> CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE;
                            case DIGITAL_INPUT_0 ->
                                bitWidth == SiosLib.BitWidth.BIT_WIDTH_8_BITS
                                        ? CONTROL_SET_INPUT_DIGITAL_0_SIOS_MODE_EIGHT_BITS
                                        : CONTROL_SET_INPUT_DIGITAL_0_SIOS_MODE_TEN_BITS;
                            case DIGITAL_INPUT_1 ->
                                bitWidth == SiosLib.BitWidth.BIT_WIDTH_8_BITS
                                        ? CONTROL_SET_INPUT_DIGITAL_1_SIOS_MODE_EIGHT_BITS
                                        : CONTROL_SET_INPUT_DIGITAL_1_SIOS_MODE_TEN_BITS;
                            case DIGITAL_INPUT_2 ->
                                bitWidth == SiosLib.BitWidth.BIT_WIDTH_8_BITS
                                        ? CONTROL_SET_INPUT_DIGITAL_2_SIOS_MODE_EIGHT_BITS
                                        : CONTROL_SET_INPUT_DIGITAL_2_SIOS_MODE_TEN_BITS;
                            case DIGITAL_INPUT_3 ->
                                bitWidth == SiosLib.BitWidth.BIT_WIDTH_8_BITS
                                        ? CONTROL_SET_INPUT_DIGITAL_3_SIOS_MODE_EIGHT_BITS
                                        : CONTROL_SET_INPUT_DIGITAL_3_SIOS_MODE_TEN_BITS;
                            case DIGITAL_INPUT_4 ->
                                bitWidth == SiosLib.BitWidth.BIT_WIDTH_8_BITS
                                        ? CONTROL_SET_INPUT_DIGITAL_4_SIOS_MODE_EIGHT_BITS
                                        : CONTROL_SET_INPUT_DIGITAL_4_SIOS_MODE_TEN_BITS;
                            default ->
                                bitWidth == SiosLib.BitWidth.BIT_WIDTH_8_BITS
                                        ? CONTROL_SET_INPUT_DIGITAL_5_SIOS_MODE_EIGHT_BITS
                                        : CONTROL_SET_INPUT_DIGITAL_5_SIOS_MODE_TEN_BITS;
                        };

                byte[] data = new byte[] {controlByte};

                byte[] nextByteData = new byte[] {SiosLib.CONTROL_GET_NEXT_BYTE};
                verify(serialPort, times(1)).writeBytes(data, data.length);
                verify(serialPort, times(1)).writeBytes(nextByteData, nextByteData.length);
            }
        }

        @ParameterizedTest
        @DisplayName("returns a valid analog value if data could be fetched in time for CompuLab mode")
        @MethodSource("provideArguments")
        void returns_a_valid_analog_value_if_data_could_be_fetched_in_time_for_CompuLab_mode(
                SiosLib.AnalogInput input, SiosLib.BitWidth bitWidth) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SiosLib.SiosLabMode.COMPULAB_MODE);

                if (((input != SiosLib.AnalogInput.ANALOG_INPUT_1) && (input != SiosLib.AnalogInput.ANALOG_INPUT_2))
                        || (bitWidth == SiosLib.BitWidth.BIT_WIDTH_10_BITS)) {
                    assertThrows(IllegalArgumentException.class, () -> siosLib.getAnalogValue(input, bitWidth));
                    return;
                }

                clearInvocations(serialPort); // important, as 0x01 (both CONTROL_GET_NEXT_BYTE and part of the
                // TEST_DATA_SEQUENCE) is also sent during initialization

                when(serialPort.bytesAvailable()).thenReturn(1);

                doAnswer(invocation -> {
                            byte[] buffer = invocation.getArgument(0);
                            byte[] testData = new byte[] {0x10};
                            System.arraycopy(testData, 0, buffer, 0, testData.length);
                            return testData.length;
                        })
                        .when(serialPort)
                        .readBytes(any(byte[].class), eq(1));

                doAnswer(invocation -> {
                            SerialPortDataListener serialPortDataListener = invocation.getArgument(0);
                            assertEquals(
                                    SerialPort.LISTENING_EVENT_DATA_AVAILABLE,
                                    serialPortDataListener.getListeningEvents());

                            // First fire another event that is to be ignored
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.TIMEOUT_NONBLOCKING));
                            Awaitility.await().atLeast(Duration.ofMillis(10));
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_AVAILABLE));
                            return null;
                        })
                        .when(serialPort)
                        .addDataListener(any(SerialPortDataListener.class));

                Awaitility.await()
                        .atMost(Duration.ofMillis(SiosLib.RECEIVE_TIMEOUT))
                        .untilAsserted(() -> assertEquals(16, siosLib.getAnalogValue(input, bitWidth)));

                byte[] data = new byte[] {
                    input.equals(SiosLib.AnalogInput.ANALOG_INPUT_1)
                            ? SiosLib.CONTROL_SET_INPUT_ANALOG_1_COMPULAB_MODE_EIGHT_BITS
                            : SiosLib.CONTROL_SET_INPUT_ANALOG_2_COMPULAB_MODE_EIGHT_BITS
                };
                byte[] nextByteData = new byte[] {SiosLib.CONTROL_GET_NEXT_BYTE};
                verify(serialPort, times(1)).writeBytes(data, data.length);
                verify(serialPort, times(0)).writeBytes(nextByteData, nextByteData.length);
            }
        }

        private static Stream<Arguments> provideArguments() {
            return Stream.of(
                    Arguments.of(SiosLib.AnalogInput.ANALOG_INPUT_1, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogInput.ANALOG_INPUT_2, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_0, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_1, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_2, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_3, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_4, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_5, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogInput.ANALOG_INPUT_1, SiosLib.BitWidth.BIT_WIDTH_10_BITS),
                    Arguments.of(SiosLib.AnalogInput.ANALOG_INPUT_2, SiosLib.BitWidth.BIT_WIDTH_10_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_0, SiosLib.BitWidth.BIT_WIDTH_10_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_1, SiosLib.BitWidth.BIT_WIDTH_10_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_2, SiosLib.BitWidth.BIT_WIDTH_10_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_3, SiosLib.BitWidth.BIT_WIDTH_10_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_4, SiosLib.BitWidth.BIT_WIDTH_10_BITS),
                    Arguments.of(SiosLib.AnalogInput.DIGITAL_INPUT_5, SiosLib.BitWidth.BIT_WIDTH_10_BITS));
        }
    }

    @Nested
    @DisplayName("setAnalogOutputValue")
    class SetAnalogOutputValue {

        @ParameterizedTest
        @DisplayName("sets the analog output value")
        @MethodSource("provideArguments")
        void sets_the_analog_output_value(SiosLib.AnalogOutput analogOutput, SiosLib.BitWidth bitWidth) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                int value = bitWidth.equals(SiosLib.BitWidth.BIT_WIDTH_10_BITS) ? 987 : 123;

                clearInvocations(serialPort);

                siosLib.setAnalogOutputValue(analogOutput, bitWidth, value);

                byte[] data = new byte[] {
                    analogOutput.equals(SiosLib.AnalogOutput.ANALOG_OUTPUT_1)
                            ? (bitWidth.equals(SiosLib.BitWidth.BIT_WIDTH_10_BITS)
                                    ? CONTROL_SET_ANALOG_OUTPUT_1_TEN_BITS_SIOS_MODE
                                    : CONTROL_SET_ANALOG_OUTPUT_1_EIGHT_BITS_SIOS_MODE)
                            : (bitWidth.equals(SiosLib.BitWidth.BIT_WIDTH_10_BITS)
                                    ? CONTROL_SET_ANALOG_OUTPUT_2_TEN_BITS_SIOS_MODE
                                    : CONTROL_SET_ANALOG_OUTPUT_2_EIGHT_BITS_SIOS_MODE)
                };
                verify(serialPort, times(1)).writeBytes(data, data.length);

                data = (value == 987) ? new byte[] {3, -37} : new byte[] {123};

                for (int i = 0; i < data.length; i++) {
                    verify(serialPort, times(1)).writeBytes(new byte[] {data[i]}, 1);
                }
            }
        }

        @ParameterizedTest
        @DisplayName("does not set analog output in CompuLab mode")
        @MethodSource("provideArguments")
        void does_not_set_analog_output_in_compulab_mode(SiosLib.AnalogOutput analogOutput, SiosLib.BitWidth bitWidth) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SiosLib.SiosLabMode.COMPULAB_MODE);

                assertThrows(
                        IllegalStateException.class, () -> siosLib.setAnalogOutputValue(analogOutput, bitWidth, 120));
            }
        }

        @ParameterizedTest
        @DisplayName("does not allow invalid values")
        @MethodSource("provideArguments")
        void does_not_allow_invalid_values(SiosLib.AnalogOutput analogOutput, SiosLib.BitWidth bitWidth) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.setAnalogOutputValue(analogOutput, bitWidth, -42));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> siosLib.setAnalogOutputValue(analogOutput, bitWidth, 20000));
            }
        }

        private static Stream<Arguments> provideArguments() {
            return Stream.of(
                    Arguments.of(SiosLib.AnalogOutput.ANALOG_OUTPUT_1, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogOutput.ANALOG_OUTPUT_1, SiosLib.BitWidth.BIT_WIDTH_10_BITS),
                    Arguments.of(SiosLib.AnalogOutput.ANALOG_OUTPUT_2, SiosLib.BitWidth.BIT_WIDTH_8_BITS),
                    Arguments.of(SiosLib.AnalogOutput.ANALOG_OUTPUT_2, SiosLib.BitWidth.BIT_WIDTH_10_BITS));
        }
    }

    @Nested
    @DisplayName("Change listener handling")
    class ChangeListenerHandling {

        @Test
        @DisplayName("initially has no change listeners defined")
        void initially_has_no_change_listeners_defined() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                try (SiosLib siosLib = new SiosLib(SIOS_MODE); ) {
                    assertTrue(siosLib.getAnalogInputChangeListeners().isEmpty());
                    assertTrue(siosLib.getDigitalInputChangeListeners().isEmpty());
                }
            }
        }

        @Test
        @DisplayName("change listeners are added and removed correctly")
        void change_listeners_are_added_and_removed_correctly() {

            BiConsumer<Integer, Integer> digitalChangeListener1 = (oldValue, newValue) -> {
                System.out.println(oldValue + " -> " + newValue);
            };
            BiConsumer<Integer, Integer> digitalChangeListener2 = (oldValue, newValue) -> {
                System.out.println(oldValue + " became " + newValue);
            };

            BiConsumer<Integer, Integer> analogInputChangeListener1 = (oldValue, newValue) -> {
                System.out.println("Analog: " + oldValue + " -> " + newValue);
            };
            BiConsumer<Integer, Integer> analogInputChangeListener2 = (oldValue, newValue) -> {
                System.out.println("Analog: " + oldValue + " became " + newValue);
            };
            BiConsumer<Integer, Integer> analogInputChangeListener3 = (oldValue, newValue) -> {
                System.out.println("Analog: " + oldValue + " got " + newValue);
            };

            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                try (SiosLib siosLib = new SiosLib(SIOS_MODE); ) {
                    siosLib.addDigitalInputChangeListener(digitalChangeListener1);
                    siosLib.addDigitalInputChangeListener(digitalChangeListener2);
                    siosLib.addAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_10_BITS,
                            analogInputChangeListener1);
                    siosLib.addAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_8_BITS,
                            analogInputChangeListener2);
                    siosLib.addAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_8_BITS,
                            analogInputChangeListener3);

                    assertEquals(2, siosLib.getDigitalInputChangeListeners().size());

                    assertEquals(
                            digitalChangeListener1,
                            siosLib.getDigitalInputChangeListeners().get(0));
                    assertEquals(
                            digitalChangeListener2,
                            siosLib.getDigitalInputChangeListeners().get(1));

                    assertEquals(1, siosLib.getAnalogInputChangeListeners().size());
                    assertEquals(
                            2,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .size());
                    assertEquals(
                            1,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_10_BITS)
                                    .size());
                    assertEquals(
                            2,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_8_BITS)
                                    .size());
                    assertEquals(
                            analogInputChangeListener1,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_10_BITS)
                                    .get(0));
                    assertEquals(
                            analogInputChangeListener2,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_8_BITS)
                                    .get(0));
                    assertEquals(
                            analogInputChangeListener3,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_8_BITS)
                                    .get(1));

                    siosLib.removeDigitalInputChangeListener(digitalChangeListener1);
                    siosLib.removeAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_8_BITS,
                            analogInputChangeListener2);

                    // has no effect, just increasing coverage:
                    siosLib.removeAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_1,
                            SiosLib.BitWidth.BIT_WIDTH_10_BITS,
                            analogInputChangeListener1);

                    assertEquals(
                            1,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_10_BITS)
                                    .size());
                    assertEquals(
                            analogInputChangeListener1,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_10_BITS)
                                    .get(0));

                    assertEquals(
                            1,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_8_BITS)
                                    .size());

                    assertEquals(1, siosLib.getDigitalInputChangeListeners().size());
                    assertEquals(
                            digitalChangeListener2,
                            siosLib.getDigitalInputChangeListeners().get(0));

                    siosLib.removeAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_10_BITS,
                            analogInputChangeListener1);
                    assertEquals(
                            1,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .size());
                    assertFalse(siosLib.getAnalogInputChangeListeners()
                            .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                            .containsKey(SiosLib.BitWidth.BIT_WIDTH_10_BITS));

                    // has no effect, just increasing coverage:
                    siosLib.removeAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_10_BITS,
                            analogInputChangeListener1);

                    siosLib.removeAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_8_BITS,
                            analogInputChangeListener2);
                    assertEquals(
                            1,
                            siosLib.getAnalogInputChangeListeners()
                                    .get(SiosLib.AnalogInput.ANALOG_INPUT_2)
                                    .get(SiosLib.BitWidth.BIT_WIDTH_8_BITS)
                                    .size());

                    siosLib.removeAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_8_BITS,
                            analogInputChangeListener3);
                    assertEquals(0, siosLib.getAnalogInputChangeListeners().size());
                }
            }
        }

        @Test
        @DisplayName("checks polling after digital change listener change")
        void checks_polling_after_digital_change_listener_change() throws NoSuchFieldException, IllegalAccessException {
            BiConsumer<Integer, Integer> digitalChangeListener = (oldValue, newValue) -> {
                System.out.println(oldValue + " -> " + newValue);
            };
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                try (SiosLib siosLib = new SiosLib(SIOS_MODE)) {

                    DevicePoller devicePoller = mock(DevicePoller.class);

                    Field devicePollerField = SiosLib.class.getDeclaredField("devicePoller");
                    devicePollerField.setAccessible(true);
                    devicePollerField.set(siosLib, devicePoller);

                    siosLib.addDigitalInputChangeListener(digitalChangeListener);
                    verify(devicePoller, times(1)).startPolling();
                    verify(devicePoller, times(0)).stopPolling();

                    siosLib.removeDigitalInputChangeListener(digitalChangeListener);
                    verify(devicePoller, times(1)).stopPolling();
                }
            }
        }

        @Test
        @DisplayName("checks polling after analog change listener change")
        void checks_polling_after_analog_change_listener_change() throws NoSuchFieldException, IllegalAccessException {
            BiConsumer<Integer, Integer> analogInputChangeListener = (oldValue, newValue) -> {
                System.out.println("Analog: " + oldValue + " -> " + newValue);
            };
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                try (SiosLib siosLib = new SiosLib(SIOS_MODE)) {

                    DevicePoller devicePoller = mock(DevicePoller.class);

                    Field devicePollerField = SiosLib.class.getDeclaredField("devicePoller");
                    devicePollerField.setAccessible(true);
                    devicePollerField.set(siosLib, devicePoller);

                    siosLib.addAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_10_BITS,
                            analogInputChangeListener);
                    verify(devicePoller, times(1)).startPolling();
                    verify(devicePoller, times(0)).stopPolling();

                    siosLib.removeAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_2,
                            SiosLib.BitWidth.BIT_WIDTH_10_BITS,
                            analogInputChangeListener);
                    verify(devicePoller, times(1)).stopPolling();
                }
            }
        }

        @Test
        @DisplayName(
                "logs a warning when \"manually\" querying the digital input after a respective ChangeListener was added")
        void logs_a_warning_when_manually_querying_the_digital_input_after_a_respective_ChangeListener_was_added()
                throws ExecutionException, InterruptedException, IOException {
            BiConsumer<Integer, Integer> digitalChangeListener = (oldValue, newValue) -> {
                System.out.println(oldValue + " -> " + newValue);
            };
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                try (SiosLib siosLib = new SiosLib(SIOS_MODE)) {

                    siosLib.addDigitalInputChangeListener(digitalChangeListener);

                    Logger logger = (Logger) LogManager.getLogger(SiosLib.class);
                    Configuration configuration = logger.getContext().getConfiguration();
                    OutputStream outputStream = new ByteArrayOutputStream();
                    Appender appender =
                            OutputStreamAppender.createAppender(null, null, outputStream, "testAppender1", false, true);
                    configuration.addLoggerAppender(logger, appender);
                    appender.start();

                    siosLib.getDigitalInputValue();

                    appender.stop();
                    outputStream.close();
                    configuration.getRootLogger().removeAppender("testAppender1");

                    assertTrue(
                            outputStream
                                    .toString()
                                    .contains(
                                            "Calling getDigitalInputValue while having a change listener for digital input changes in-place is not recommended as the change listener will automatically be informed on digital input changes."));
                }
            }
        }

        @Test
        @DisplayName(
                "logs a warning when \"manually\" querying the analog input after a respective ChangeListener was added")
        void logs_a_warning_when_manually_querying_the_analog_input_after_a_respective_ChangeListener_was_added()
                throws ExecutionException, InterruptedException, IOException {
            BiConsumer<Integer, Integer> analogChangeListener = (oldValue, newValue) -> {
                System.out.println(oldValue + " -> " + newValue);
            };
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                try (SiosLib siosLib = new SiosLib(SIOS_MODE)) {

                    siosLib.addAnalogInputChangeListener(
                            SiosLib.AnalogInput.ANALOG_INPUT_1,
                            SiosLib.BitWidth.BIT_WIDTH_8_BITS,
                            analogChangeListener);

                    Logger logger = (Logger) LogManager.getLogger(SiosLib.class);
                    Configuration configuration = logger.getContext().getConfiguration();
                    OutputStream outputStream = new ByteArrayOutputStream();
                    Appender appender =
                            OutputStreamAppender.createAppender(null, null, outputStream, "testAppender2", false, true);
                    configuration.addLoggerAppender(logger, appender);
                    appender.start();

                    siosLib.getAnalogValue(SiosLib.AnalogInput.ANALOG_INPUT_1, SiosLib.BitWidth.BIT_WIDTH_8_BITS);
                    siosLib.getAnalogValue(SiosLib.AnalogInput.ANALOG_INPUT_1, SiosLib.BitWidth.BIT_WIDTH_10_BITS);
                    siosLib.getAnalogValue(SiosLib.AnalogInput.ANALOG_INPUT_2, SiosLib.BitWidth.BIT_WIDTH_8_BITS);

                    appender.stop();
                    outputStream.close();
                    configuration.getRootLogger().removeAppender("testAppender2");

                    // The second and third getAnalogValue call should NOT have produced a warning as no ChangeListener
                    // was registered for this combination of values.
                    int occurrences = outputStream
                                    .toString()
                                    .split(
                                            "Calling getAnalogValue for an AnalogInput while having a change listener for this input in-place is not recommended as the change listener will automatically be informed on input changes.",
                                            -1)
                                    .length
                            - 1;

                    assertEquals(1, occurrences);
                }
            }
        }
    }

    @Nested
    @DisplayName("Closing")
    class Closing {

        @ParameterizedTest
        @DisplayName("closes the port after usage")
        @EnumSource(SiosLib.SiosLabMode.class)
        void closes_the_port_after_usage(SiosLib.SiosLabMode mode) {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
                prepareSerialPortMock();
                when(serialPort.isOpen()).thenReturn(true);

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                try (SiosLib siosLib = new SiosLib(mode); ) {
                    assertTrue(siosLib.isConnected()); // This is not really necessary here, just to do "something" with
                    // the SiosLib...
                    clearInvocations(serialPort);
                }

                byte[] data = new byte[] {
                    mode == SiosLib.SiosLabMode.SIOS_MODE
                            ? CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE
                            : CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE
                };

                verify(serialPort, times(1)).writeBytes(data, data.length);
                verify(serialPort, times(1)).writeBytes(new byte[] {0}, 1);

                verify(
                                serialPort,
                                timeout(2 * SiosLib.SHUTDOWN_WAITING_INTERVAL).times(1))
                        .closePort();
            }
        }

        @Test
        @DisplayName("handles InterruptedException during closing")
        void handles_InterruptedException_during_closing() throws InterruptedException {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                AtomicBoolean wasInterrupted = new AtomicBoolean(false);

                SiosLib siosLib = new SiosLib(SiosLib.SiosLabMode.SIOS_MODE);

                Thread testThread = new Thread(() -> {
                    Thread.currentThread().interrupt();
                    siosLib.close();
                    wasInterrupted.set(Thread.currentThread().isInterrupted());
                });

                testThread.start();
                testThread.join();

                assertTrue(wasInterrupted.get());
            }
        }
    }

    @Nested
    @DisplayName("getSerialPortName")
    class GetSerialPortName {

        @Test
        @DisplayName("returns the serial port name")
        void returns_the_serial_port_name() {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SIOS_MODE);

                assertEquals("COM123", siosLib.getSerialPortName());
            }
        }
    }

    private void prepareSerialPortMock() {
        when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
        when(serialPort.getDescriptivePortName()).thenReturn("COM123");
        when(serialPort.openPort()).thenReturn(true);
        when(serialPort.bytesAvailable()).thenReturn(1);
        doAnswer(invocation -> {
                    byte[] buffer = invocation.getArgument(0);
                    byte[] testData = new byte[] {SiosLib.MODE_INDICATOR_COMPULAB};
                    System.arraycopy(testData, 0, buffer, 0, testData.length);
                    return testData.length;
                })
                .when(serialPort)
                .readBytes(any(byte[].class), eq(1));
    }
}
