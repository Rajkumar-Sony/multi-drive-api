package com.multidrive.api.service;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface DriveContentWriter {

    void writeTo(
            OutputStream outputStream
    ) throws IOException;
}
