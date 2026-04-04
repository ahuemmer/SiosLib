package com.github.ahuemmer.sioslib;

import com.fazecast.jSerialComm.SerialPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SiosLibTest {

    @Mock
    SerialPort serialPort;

    @Test
    @DisplayName("throws NoSerialPortFoundException, if no serial ports were found")
    void throws_NoSerialPortFoundException_if_no_COM_ports_were_found() {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[]{});
            assertThrows(NoSerialPortFoundException.class, () -> new SiosLib(SiosLib.SIOS_MODE));
        }
    }

    @Test
    @DisplayName("throws NoSiosLabFoundException, if no serial ports could be opened")
    void throws_NoSiosLabFoundException_if_no_COM_ports_could_be_opened() {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {
            when(serialPort.openPort()).thenReturn(false);
            when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
            when(serialPort.getDescriptivePortName()).thenReturn("COM123");
            serialPortStaticMock.when(() -> SerialPort.getCommPort("Serial Port 123")).thenReturn(serialPort);
            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[]{serialPort});
            assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SIOS_MODE));
        }
    }

    @Test
    @DisplayName("tries to open every serial port and throws NoSiosLabFoundException, if no SIOSLab was found in time")
    void tries_to_open_every_serial_port_and_throws_NoSiosLabFoundException_if_no_SIOSLab_was_found_in_time() {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

            SerialPort serialPort2 = mock(SerialPort.class);

            when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
            when(serialPort.getDescriptivePortName()).thenReturn("COM123");
            when(serialPort.openPort()).thenReturn(true);

            when(serialPort2.getSystemPortName()).thenReturn("Serial Port 124");
            when(serialPort2.getDescriptivePortName()).thenReturn("COM124");
            when(serialPort2.openPort()).thenReturn(true);

            serialPortStaticMock.when(() -> SerialPort.getCommPort("Serial Port 123")).thenReturn(serialPort);
            serialPortStaticMock.when(() -> SerialPort.getCommPort("Serial Port 124")).thenReturn(serialPort2);

            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[]{serialPort, serialPort2});

            assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SIOS_MODE));
            verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
            verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
            verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
            verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
            verify(serialPort, times(1)).openPort();
        }
    }

    @Test
    @DisplayName("tries to find a SIOSLab on an opened port and throws NoSiosLabFoundException, if it fails because of wrong answer from the interface")
    void tries_to_find_a_SIOSLab_on_an_opened_port_and_throws_NoSiosLabFoundException_if_it_fails_because_of_wrong_answer_from_the_interface() {
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
            }).when(serialPort).readBytes(any(byte[].class), eq(1));

            serialPortStaticMock.when(() -> SerialPort.getCommPort("Serial Port 123")).thenReturn(serialPort);

            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[]{serialPort});

            assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SIOS_MODE));
            verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
            verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
            verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
            verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
            verify(serialPort, times(1)).openPort();
        }
    }

    @Test
    @DisplayName("tries to find a SIOSLab on an opened port and throws NoSiosLabFoundException, if it fails because of wrong number of bytes available")
    void tries_to_find_a_SIOSLab_on_an_opened_port_and_throws_NoSiosLabFoundException_if_it_fails_because_of_wrong_number_of_bytes_available() {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

            when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
            when(serialPort.getDescriptivePortName()).thenReturn("COM123");
            when(serialPort.openPort()).thenReturn(true);
            when(serialPort.bytesAvailable()).thenReturn(5);

            serialPortStaticMock.when(() -> SerialPort.getCommPort("Serial Port 123")).thenReturn(serialPort);

            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[]{serialPort});

            assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SIOS_MODE));
            verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
            verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
            verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
            verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
            verify(serialPort, times(1)).openPort();
        }
    }

    @Test
    @DisplayName("tries to find a SIOSLab on an opened port and throws NoSiosLabFoundException, if it fails because of wrong number of bytes read")
    void tries_to_find_a_SIOSLab_on_an_opened_port_and_throws_NoSiosLabFoundException_if_it_fails_because_of_wrong_number_of_bytes_read() {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

            when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
            when(serialPort.getDescriptivePortName()).thenReturn("COM123");
            when(serialPort.openPort()).thenReturn(true);
            when(serialPort.bytesAvailable()).thenReturn(1);
            doAnswer(invocation -> {
                byte[] buffer = invocation.getArgument(0);
                byte[] testData = new byte[]{0x00};
                System.arraycopy(testData, 0, buffer, 0, testData.length);
                return 99;
            }).when(serialPort).readBytes(any(byte[].class), eq(1));

            serialPortStaticMock.when(() -> SerialPort.getCommPort("Serial Port 123")).thenReturn(serialPort);

            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[]{serialPort});

            assertThrows(NoSiosLabFoundException.class, () -> new SiosLib(SiosLib.SIOS_MODE));
            verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
            verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
            verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
            verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
            verify(serialPort, times(1)).openPort();
        }
    }

    @Test
    @DisplayName("connects to SIOSLab in SIOS mode")
    void connects_to_SIOSLab_in_SIOS_mode() {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

            when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
            when(serialPort.getDescriptivePortName()).thenReturn("COM123");
            when(serialPort.openPort()).thenReturn(true);
            when(serialPort.bytesAvailable()).thenReturn(1);
            doAnswer(invocation -> {
                byte[] buffer = invocation.getArgument(0);
                byte[] testData = new byte[]{SiosLib.MODE_INDICATOR_SIOSLAB};
                System.arraycopy(testData, 0, buffer, 0, testData.length);
                return testData.length;
            }).when(serialPort).readBytes(any(byte[].class), eq(1));

            serialPortStaticMock.when(() -> SerialPort.getCommPort("Serial Port 123")).thenReturn(serialPort);

            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[]{serialPort});

            new SiosLib(SiosLib.SIOS_MODE);
            verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
            verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
            verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
            verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
            verify(serialPort, times(1)).openPort();
            verify(serialPort, times(5)).writeBytes(new byte[]{0x00}, 1);
            verify(serialPort, times(1)).writeBytes(new byte[]{0x01}, 1);
        }
    }

    @Test
    @DisplayName("connects to SIOSLab in CompuLAB mode")
    void connects_to_SIOSLab_in_CompueLAB_mode() {
        try (MockedStatic<SerialPort> serialPortStaticMock = mockStatic(SerialPort.class)) {

            when(serialPort.getSystemPortName()).thenReturn("Serial Port 123");
            when(serialPort.getDescriptivePortName()).thenReturn("COM123");
            when(serialPort.openPort()).thenReturn(true);
            when(serialPort.bytesAvailable()).thenReturn(1);
            doAnswer(invocation -> {
                byte[] buffer = invocation.getArgument(0);
                byte[] testData = new byte[]{SiosLib.MODE_INDICATOR_COMPULAB};
                System.arraycopy(testData, 0, buffer, 0, testData.length);
                return testData.length;
            }).when(serialPort).readBytes(any(byte[].class), eq(1));

            serialPortStaticMock.when(() -> SerialPort.getCommPort("Serial Port 123")).thenReturn(serialPort);

            serialPortStaticMock.when(SerialPort::getCommPorts).thenReturn(new SerialPort[]{serialPort});

            new SiosLib(SiosLib.COMPULAB_MODE);
            verify(serialPort, times(1)).setBaudRate(SiosLib.BAUD_RATE);
            verify(serialPort, times(1)).setNumDataBits(SiosLib.NUM_DATABITS);
            verify(serialPort, times(1)).setNumStopBits(SerialPort.ONE_STOP_BIT);
            verify(serialPort, times(1)).setParity(SerialPort.NO_PARITY);
            verify(serialPort, times(1)).openPort();
            verify(serialPort, times(3)).writeBytes(new byte[]{0x00}, 1);
            verify(serialPort, times(3)).writeBytes(new byte[]{0x01}, 1);
        }
    }

}
