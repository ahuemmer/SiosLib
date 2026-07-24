package com.github.ahuemmer.sioslib;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class for regularly polling the SIOSLab / CompuLab when listening for changes.
 */
class DevicePoller {

    /**
     * Remember the last digital input received for comparison.
     */
    private int lastDigitalInput = 0;

    /**
     * Remember the last analog inputs received for comparison.
     */
    private final Map<SiosLib.AnalogInput, Map<SiosLib.BitWidth, Integer>> lastAnalogInputValues =
            new EnumMap<>(SiosLib.AnalogInput.class);

    /**
     * The interval the device is polled at.
     */
    private final int pollingInterval;

    /**
     * The logger.
     */
    private static final Logger LOG = LogManager.getLogger(DevicePoller.class);

    /**
     * The {@link com.github.ahuemmer.sioslib.SiosLib} instance this poller belongs to.
     */
    private final SiosLib siosLib;

    /**
     * The scheduler scheduling the polling task.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * The actual polling task.
     */
    private ScheduledFuture<?> pollingTask;

    /**
     * Poller constructor.
     * <p>
     * A poller will always be created when the SiosLib is initialized, but it will be started if needed only.
     *
     * @param siosLib         the {@link com.github.ahuemmer.sioslib.SiosLib} instance this poller belongs to.
     * @param pollingInterval the interval the device is polled at.
     */
    public DevicePoller(SiosLib siosLib, Integer pollingInterval) {

        if (pollingInterval < 0) {
            throw new IllegalArgumentException("pollingInterval must be a positive integer");
        }
        if (pollingInterval < 50) {
            LOG.warn("A polling interval less than 50ms is possible, but not recommended!");
        }

        this.siosLib = siosLib;
        this.pollingInterval = pollingInterval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts polling the device in the interval defined.
     */
    public void startPolling() {
        pollingTask = scheduler.scheduleWithFixedDelay(this::pollDevice, 0, pollingInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops polling the device (e.g. when no more ChangeListeners are available or the SiosLib is shut down).
     */
    public void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(true);
        }
    }

    /**
     * Shuts down the poller.
     */
    public void shutdown() {
        stopPolling();
        scheduler.shutdownNow();
    }

    /**
     * The actual "content" of the poller task.
     */
    private void pollDevice() {
        pollDigitalInput();
        pollAnalogInput();
    }

    /**
     * Poll the digital input, if any ChangeListeners are defined respectively, and call the ChangeListener function(s)
     * given, if the value has changed.
     */
    private void pollDigitalInput() {
        if (siosLib.getDigitalInputChangeListeners().isEmpty()) {
            return;
        }

        LOG.debug("Polling digital input value");
        try {
            int digitalInput = siosLib.getDigitalInputValue(false);
            if (digitalInput != lastDigitalInput) {
                siosLib.getDigitalInputChangeListeners().forEach(c -> c.accept(lastDigitalInput, digitalInput));
                lastDigitalInput = digitalInput;
            }
        } catch (ExecutionException e) {
            LOG.warn("Error when polling: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted when polling: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Poll the analog input, if any ChangeListeners are defined respectively, and call the ChangeListener function(s)
     * given, if the value has changed.
     */
    private void pollAnalogInput() {
        if (siosLib.getAnalogInputChangeListeners().isEmpty()) {
            return;
        }

        LOG.debug("Polling analog input value");
        for (SiosLib.AnalogInput analogInput :
                siosLib.getAnalogInputChangeListeners().keySet()) {
            try {

                for (SiosLib.BitWidth bitWidth :
                        siosLib.getAnalogInputChangeListeners().get(analogInput).keySet()) {

                    int analogInputValue = siosLib.getAnalogValue(analogInput, bitWidth, false);

                    int lastAnalogInputValue = lastAnalogInputValues
                            .getOrDefault(analogInput, Collections.emptyMap())
                            .getOrDefault(bitWidth, 0);

                    if (analogInputValue != lastAnalogInputValue) {
                        siosLib.getAnalogInputChangeListeners()
                                .get(analogInput)
                                .get(bitWidth)
                                .forEach(c -> c.accept(lastAnalogInputValue, analogInputValue));
                        lastAnalogInputValues.computeIfAbsent(analogInput, k -> new EnumMap<>(SiosLib.BitWidth.class));
                        lastAnalogInputValues.get(analogInput).put(bitWidth, analogInputValue);
                    }
                }
            } catch (ExecutionException e) {
                LOG.warn("Error when polling: {}", e.getMessage());
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted when polling: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }
}
