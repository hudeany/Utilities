package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class IoUtilities {
	public static long copy(final InputStream input, final OutputStream output) throws IOException {
		long count = 0;
		if (input != null && output != null) {
			final byte[] buffer = new byte[4096];
			int n;
			while (-1 != (n = input.read(buffer))) {
				output.write(buffer, 0, n);
				count += n;
			}
		}
		return count;
	}

	public static String toString(final InputStream inputStream, final Charset encoding) throws IOException {
		return new String(toByteArray(inputStream), encoding);
	}

	public static byte[] toByteArray(final InputStream inputStream) throws IOException {
		try (final ByteArrayOutputStream bufferStream = new ByteArrayOutputStream()) {
			copy(inputStream, bufferStream);
			return bufferStream.toByteArray();
		}
	}
}
