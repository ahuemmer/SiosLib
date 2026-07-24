package com.github.ahuemmer.sioslib;

/**
 * Thrown if one or more serial port(s) were found on your PC, but a SIOSLab does not seem to be attached on one
 * of them.
 */
public class NoSiosLabFoundException extends RuntimeException {

    /**
     * Constructor (self-explaining).
     */
    public NoSiosLabFoundException() {
        super();
    }
}
