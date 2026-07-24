package com.github.ahuemmer.sioslib;

/**
 * Is thrown if no serial port was found on your PC.
 */
public class NoSerialPortFoundException extends RuntimeException {

    /**
     * Constructor (self-explaining).
     */
    public NoSerialPortFoundException() {
        super();
    }
}
