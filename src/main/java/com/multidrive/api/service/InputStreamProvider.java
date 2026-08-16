package com.multidrive.api.service;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamProvider {

	InputStream openStream() throws IOException;

}
