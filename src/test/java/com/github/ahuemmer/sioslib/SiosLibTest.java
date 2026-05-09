package com.github.ahuemmer.sioslib;

import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_ANALOG_OUTPUT_1_EIGHT_BITS_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_ANALOG_OUTPUT_1_TEN_BITS_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_ANALOG_OUTPUT_2_EIGHT_BITS_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_ANALOG_OUTPUT_2_TEN_BITS_SIOS_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_DIGITAL_OUTPUT_COMPULAB_MODE;
import static com.github.ahuemmer.sioslib.SiosLib.CONTROL_SET_DIGITAL_OUTPUT_SIOS_MODE;
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
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
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
public class SiosLibTest {

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
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsedMs >= 50L);

            verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
            verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
            verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
            verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
            verify(serialPort, times(1)).openPort();
            verify(serialPort, times(mode == SiosLib.SiosLabMode.COMPULAB_MODE ? 4 : 6))
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
    @DisplayName("getDigitalValue")
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
                            Thread.sleep(10);
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_AVAILABLE));
                            return null;
                        })
                        .when(serialPort)
                        .addDataListener(any(SerialPortDataListener.class));

                Awaitility.await()
                        .atMost(Duration.ofMillis(SiosLib.RECEIVE_TIMEOUT))
                        .untilAsserted(() -> assertEquals(33, siosLib.getDigitalValue()));

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

                assertEquals(0, siosLib.getDigitalValue());
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
    }

    @Nested
    @DisplayName("changeDigitalOutputState with boolean array")
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
        @EnumSource(SiosLib.AnalogInput.class)
        void returns_a_valid_analog_1_value_if_data_could_be_fetched_in_time_in_sios_mode(SiosLib.AnalogInput input)
                throws ExecutionException, InterruptedException {
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
                            Thread.sleep(10);
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_AVAILABLE));
                            return null;
                        })
                        .when(serialPort)
                        .addDataListener(any(SerialPortDataListener.class));

                Awaitility.await()
                        .atMost(Duration.ofMillis(SiosLib.RECEIVE_TIMEOUT))
                        .untilAsserted(() -> assertEquals(272, siosLib.getAnalogValue(input)));

                byte[] data = new byte[] {
                    input.equals(SiosLib.AnalogInput.ANALOG_INPUT_1)
                            ? SiosLib.CONTROL_SET_INPUT_ANALOG_1_SIOS_MODE
                            : SiosLib.CONTROL_SET_INPUT_ANALOG_2_SIOS_MODE
                };
                byte[] nextByteData = new byte[] {SiosLib.CONTROL_GET_NEXT_BYTE};
                verify(serialPort, times(1)).writeBytes(data, data.length);
                verify(serialPort, times(1)).writeBytes(nextByteData, nextByteData.length);
            }
        }

        @ParameterizedTest
        @DisplayName("returns a valid analog value if data could be fetched in time for CompuLab mode")
        @EnumSource(SiosLib.AnalogInput.class)
        void returns_a_valid_analog_value_if_data_could_be_fetched_in_time_for_CompuLab_mode(SiosLib.AnalogInput input)
                throws ExecutionException, InterruptedException {
            try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

                prepareSerialPortMock();

                serialPortStaticMock
                        .when(() -> SerialPort.getCommPort("Serial Port 123"))
                        .thenReturn(serialPort);
                serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[] {serialPort});

                SiosLib siosLib = new SiosLib(SiosLib.SiosLabMode.COMPULAB_MODE);

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
                            Thread.sleep(10);
                            serialPortDataListener.serialEvent(
                                    new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_AVAILABLE));
                            return null;
                        })
                        .when(serialPort)
                        .addDataListener(any(SerialPortDataListener.class));

                Awaitility.await()
                        .atMost(Duration.ofMillis(SiosLib.RECEIVE_TIMEOUT))
                        .untilAsserted(() -> assertEquals(16, siosLib.getAnalogValue(input)));

                byte[] data = new byte[] {
                    input.equals(SiosLib.AnalogInput.ANALOG_INPUT_1)
                            ? SiosLib.CONTROL_SET_INPUT_ANALOG_1_COMPULAB_MODE
                            : SiosLib.CONTROL_SET_INPUT_ANALOG_2_COMPULAB_MODE
                };
                byte[] nextByteData = new byte[] {SiosLib.CONTROL_GET_NEXT_BYTE};
                verify(serialPort, times(1)).writeBytes(data, data.length);
                verify(serialPort, times(0)).writeBytes(nextByteData, nextByteData.length);
            }
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

                verify(serialPort, timeout(2 * SiosLib.POLLING_INTERVAL).times(1))
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

    // TODO:
    // 1. GetAnalogInput auch für die Output-Kanäle ermöglichen (??)
    // 2. Code prüfen und ggf. verbessern, auch mit SonarQube etc.
    // 3. Doku etc. für GitHub ergänzen.
}
