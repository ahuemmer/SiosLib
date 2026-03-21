package com.github.ahuemmer.sioslib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SiosLabInterfaceTest {

    @Test
    void mirrorData() throws Exception {
        try(SiosLabInterface siosLabInterface = new SiosLabInterface()) {
            assertTrue(siosLabInterface.isConnected());

            siosLabInterface.attachDataListener(newData -> {
                send(siosLabInterface, newData[0]);
            });

            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 60000) {
                // now check that it's working...
            }

        }
    }

    @Test
    void sendPattern() throws Exception {
        try(SiosLabInterface siosLabInterface = new SiosLabInterface()) {
            assertTrue(siosLabInterface.isConnected());
            int data = 1;
            while (data < 256) {
                sendAndWait(siosLabInterface, data);
                data *= 2;
            }
            data = 64;
            while (data > 0) {
                sendAndWait(siosLabInterface, data);
                data /= 2;
            }
        }
    }

    @Test
    void sendOtherPattern() throws Exception {
        try(SiosLabInterface siosLabInterface = new SiosLabInterface()) {
            assertTrue(siosLabInterface.isConnected());
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
            sendAndWait(siosLabInterface, 1 + 4 + 16 + 64);
            sendAndWait(siosLabInterface, 2 + 8 + 32 + 128);
        }
    }

    private void send(SiosLabInterface siosLabInterface, int data) {
        siosLabInterface.sendData(data);
    }

    private void sendAndWait(SiosLabInterface siosLabInterface, int data) {
        send(siosLabInterface, data);
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {}
    }
}