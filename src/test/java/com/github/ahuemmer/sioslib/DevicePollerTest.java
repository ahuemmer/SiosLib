package com.github.ahuemmer.sioslib;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DevicePollerTest {

    @Mock
    SiosLib siosLib;

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private ScheduledFuture pollingTask;

    @Mock
    private DevicePoller cut;

    private void setUpExecutor() {
        try (MockedStatic<Executors> executorsStaticMock = mockStatic(Executors.class)) {
            executorsStaticMock
                    .when(Executors::newSingleThreadScheduledExecutor)
                    .thenReturn(scheduler);
            cut = new DevicePoller(siosLib, 123);
        }
        when(scheduler.scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(123L), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(pollingTask);
    }

    @Test
    @DisplayName("starts polling")
    void startsPolling() {
        setUpExecutor();
        cut.startPolling();
        verify(scheduler, times(1))
                .scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(123L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("stops polling")
    void stopsPolling() {
        setUpExecutor();

        // Does nothing if there is no polling task yet:
        cut.stopPolling();
        verify(pollingTask, times(0)).cancel(anyBoolean());

        cut.startPolling();
        cut.stopPolling();
        verify(pollingTask, times(1)).cancel(true);
    }

    @Test
    void shutdown() {
        setUpExecutor();
        cut.startPolling();
        cut.shutdown();
        verify(pollingTask, times(1)).cancel(true);
        verify(scheduler, times(1)).shutdownNow();
    }

    @Test
    @DisplayName("does not do anything if no listeners are present")
    void does_not_do_anything_if_no_listeners_are_present() throws ExecutionException, InterruptedException {
        when(siosLib.getDigitalInputChangeListeners()).thenReturn(Collections.emptyList());
        when(siosLib.getAnalogInputChangeListeners()).thenReturn(Collections.emptyMap());
        cut = new DevicePoller(siosLib, 10);
        cut.startPolling();
        Awaitility.waitAtMost(150, TimeUnit.MILLISECONDS);
        cut.stopPolling();
        verify(siosLib, times(0)).getDigitalInputValue();
        verify(siosLib, times(0))
                .getAnalogValue(any(SiosLib.AnalogInput.class), any(SiosLib.BitWidth.class), eq(false));
    }

    @Test
    @DisplayName("notifies listeners on digital input change")
    void notifies_listeners_on_digital_input_change() throws ExecutionException, InterruptedException {
        List<AbstractMap.SimpleEntry<Integer, Integer>> digitalChangesReceived = new ArrayList<>();
        BiConsumer<Integer, Integer> testChangeListener = (oldValue, newValue) -> {
            digitalChangesReceived.add(new AbstractMap.SimpleEntry<>(oldValue, newValue));
        };
        when(siosLib.getDigitalInputChangeListeners()).thenReturn(List.of(testChangeListener));
        when(siosLib.getAnalogInputChangeListeners()).thenReturn(Collections.emptyMap());
        when(siosLib.getDigitalInputValue(false)).thenReturn(33, 44, 44, 55);
        cut = new DevicePoller(siosLib, 10);
        cut.startPolling();
        Awaitility.waitAtMost(360, TimeUnit.MILLISECONDS)
                .pollDelay(20, TimeUnit.MILLISECONDS)
                .until(() -> digitalChangesReceived.size() == 3);
        cut.stopPolling();
        assertEquals(3, digitalChangesReceived.size());
        assertEquals(new AbstractMap.SimpleEntry<>(0, 33), digitalChangesReceived.get(0));
        assertEquals(new AbstractMap.SimpleEntry<>(33, 44), digitalChangesReceived.get(1));
        assertEquals(new AbstractMap.SimpleEntry<>(44, 55), digitalChangesReceived.get(2));
    }

    @Test
    @DisplayName("notifies listeners on analog input change")
    void notifies_listeners_on_analog_input_change() throws ExecutionException, InterruptedException {
        List<AbstractMap.SimpleEntry<Integer, Integer>> analogChangesReceived = new ArrayList<>();
        BiConsumer<Integer, Integer> testChangeListener = (oldValue, newValue) -> {
            analogChangesReceived.add(new AbstractMap.SimpleEntry<>(oldValue, newValue));
        };
        Map<SiosLib.BitWidth, List<BiConsumer<Integer, Integer>>> bitwidthMap =
                Map.of(SiosLib.BitWidth.BIT_WIDTH_10_BITS, List.of(testChangeListener));
        Map<SiosLib.AnalogInput, Map<SiosLib.BitWidth, List<BiConsumer<Integer, Integer>>>> analogInputMap =
                Map.of(SiosLib.AnalogInput.ANALOG_INPUT_2, bitwidthMap);

        when(siosLib.getDigitalInputChangeListeners()).thenReturn(Collections.emptyList());
        when(siosLib.getAnalogInputChangeListeners()).thenReturn(analogInputMap);
        when(siosLib.getAnalogValue(SiosLib.AnalogInput.ANALOG_INPUT_2, SiosLib.BitWidth.BIT_WIDTH_10_BITS, false))
                .thenReturn(999, 888, 888, 777);
        cut = new DevicePoller(siosLib, 10);
        cut.startPolling();
        Awaitility.waitAtMost(1, TimeUnit.SECONDS)
                .pollDelay(20, TimeUnit.MILLISECONDS)
                .until(() -> analogChangesReceived.size() == 3);
        cut.stopPolling();
        assertEquals(3, analogChangesReceived.size());
        assertEquals(new AbstractMap.SimpleEntry<>(0, 999), analogChangesReceived.get(0));
        assertEquals(new AbstractMap.SimpleEntry<>(999, 888), analogChangesReceived.get(1));
        assertEquals(new AbstractMap.SimpleEntry<>(888, 777), analogChangesReceived.get(2));

        // Make sure only the input with a listener was queried:
        for (SiosLib.AnalogInput analogInput : analogInputMap.keySet()) {
            if (analogInput == SiosLib.AnalogInput.ANALOG_INPUT_2) {
                continue;
            }
            verify(siosLib, times(0)).getAnalogValue(analogInput, any(SiosLib.BitWidth.class), any());
        }
    }

    @Test
    @DisplayName("throws RuntimeException, if ExecutionException occurs on querying digital input changes")
    void throws_RuntimeException_if_ExecutionException_occurs_on_querying_digital_input_changes()
            throws InterruptedException, ExecutionException, NoSuchMethodException {
        BiConsumer<Integer, Integer> testChangeListener = (oldValue, newValue) -> {
            // nothing here...
        };
        when(siosLib.getDigitalInputChangeListeners()).thenReturn(List.of(testChangeListener));
        when(siosLib.getAnalogInputChangeListeners()).thenReturn(Collections.emptyMap());
        cut = new DevicePoller(siosLib, 10);
        Method pollDigitalInputMethod = DevicePoller.class.getDeclaredMethod("pollDigitalInput");
        pollDigitalInputMethod.setAccessible(true);
        ExecutionException cause = new ExecutionException("TEST exception", new RuntimeException("underlying"));
        when(siosLib.getDigitalInputValue(false)).thenThrow(cause);

        InvocationTargetException thrown =
                assertThrows(InvocationTargetException.class, () -> pollDigitalInputMethod.invoke(cut));

        Throwable actual = thrown.getCause();
        assertInstanceOf(RuntimeException.class, actual);
        assertSame(cause, actual.getCause());
    }

    @Test
    @DisplayName("interrupts thread, if InterruptedException occurs on querying digital input changes")
    void interrupts_thread_if_InterruptedException_occurs_on_querying_digital_input_changes()
            throws InterruptedException, ExecutionException, NoSuchMethodException {
        BiConsumer<Integer, Integer> testChangeListener = (oldValue, newValue) -> {
            // nothing here...
        };
        when(siosLib.getDigitalInputChangeListeners()).thenReturn(List.of(testChangeListener));
        when(siosLib.getAnalogInputChangeListeners()).thenReturn(Collections.emptyMap());
        when(siosLib.getDigitalInputValue(false)).thenThrow(new InterruptedException("TEST Exception"));
        Method pollDigitalInputMethod = DevicePoller.class.getDeclaredMethod("pollDigitalInput");
        pollDigitalInputMethod.setAccessible(true);
        cut = new DevicePoller(siosLib, 10);
        assertDoesNotThrow(() -> pollDigitalInputMethod.invoke(cut));

        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    @DisplayName("throws RuntimeException, if ExecutionException occurs on querying analog input changes")
    void throws_RuntimeException_if_ExecutionException_occurs_on_querying_analog_input_changes()
            throws InterruptedException, ExecutionException, NoSuchMethodException {
        BiConsumer<Integer, Integer> testChangeListener = (oldValue, newValue) -> {
            // Nothing here...
        };
        Map<SiosLib.BitWidth, List<BiConsumer<Integer, Integer>>> bitwidthMap =
                Map.of(SiosLib.BitWidth.BIT_WIDTH_10_BITS, List.of(testChangeListener));
        Map<SiosLib.AnalogInput, Map<SiosLib.BitWidth, List<BiConsumer<Integer, Integer>>>> analogInputMap =
                Map.of(SiosLib.AnalogInput.ANALOG_INPUT_1, bitwidthMap);

        when(siosLib.getDigitalInputChangeListeners()).thenReturn(Collections.emptyList());
        when(siosLib.getAnalogInputChangeListeners()).thenReturn(analogInputMap);

        cut = new DevicePoller(siosLib, 10);
        Method pollAnalogInputMethod = DevicePoller.class.getDeclaredMethod("pollAnalogInput");
        pollAnalogInputMethod.setAccessible(true);
        ExecutionException cause = new ExecutionException("TEST exception", new RuntimeException("underlying"));
        when(siosLib.getAnalogValue(SiosLib.AnalogInput.ANALOG_INPUT_1, SiosLib.BitWidth.BIT_WIDTH_10_BITS, false))
                .thenThrow(cause);

        InvocationTargetException thrown =
                assertThrows(InvocationTargetException.class, () -> pollAnalogInputMethod.invoke(cut));

        Throwable actual = thrown.getCause();
        assertInstanceOf(RuntimeException.class, actual);
        assertSame(cause, actual.getCause());
    }

    @Test
    @DisplayName("interrupts thread, if InterruptedException occurs on querying analog input changes")
    void interrupts_thread_if_InterruptedException_occurs_on_querying_analog_input_changes()
            throws InterruptedException, ExecutionException, NoSuchMethodException {
        BiConsumer<Integer, Integer> testChangeListener = (oldValue, newValue) -> {
            // Nothing here...
        };
        Map<SiosLib.BitWidth, List<BiConsumer<Integer, Integer>>> bitwidthMap =
                Map.of(SiosLib.BitWidth.BIT_WIDTH_10_BITS, List.of(testChangeListener));
        Map<SiosLib.AnalogInput, Map<SiosLib.BitWidth, List<BiConsumer<Integer, Integer>>>> analogInputMap =
                Map.of(SiosLib.AnalogInput.ANALOG_INPUT_1, bitwidthMap);

        when(siosLib.getDigitalInputChangeListeners()).thenReturn(Collections.emptyList());
        when(siosLib.getAnalogInputChangeListeners()).thenReturn(analogInputMap);
        when(siosLib.getAnalogValue(SiosLib.AnalogInput.ANALOG_INPUT_1, SiosLib.BitWidth.BIT_WIDTH_10_BITS, false))
                .thenThrow(new InterruptedException("TEST Exception"));
        Method pollAnalogInputMethod = DevicePoller.class.getDeclaredMethod("pollAnalogInput");
        pollAnalogInputMethod.setAccessible(true);
        cut = new DevicePoller(siosLib, 10);
        assertDoesNotThrow(() -> pollAnalogInputMethod.invoke(cut));

        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {
        @Test
        @DisplayName("throws IllegalArgumentException, if a negative polling interval is given")
        void throws_IllegalArgumentException_if_a_negative_polling_interval_is_given() {
            assertThrows(IllegalArgumentException.class, () -> new DevicePoller(siosLib, -123));
        }

        @Test
        @DisplayName("logs a warning if a polling interval <50ms is used")
        void logs_a_warning_if_a_polling_interval_less_than_50ms_is_used() throws IOException {
            Logger logger = (Logger) LogManager.getLogger(DevicePoller.class);
            Configuration configuration = logger.getContext().getConfiguration();
            OutputStream outputStream = new ByteArrayOutputStream();
            Appender appender =
                    OutputStreamAppender.createAppender(null, null, outputStream, "testAppender", false, true);
            configuration.addLoggerAppender(logger, appender);
            appender.start();

            new DevicePoller(siosLib, 10);

            appender.stop();
            outputStream.close();

            assertTrue(outputStream
                    .toString()
                    .contains("A polling interval less than 50ms is possible, but not recommended!"));
        }
    }

    @AfterEach
    void tearDown() {
        reset(siosLib);
    }
}
