package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;

import de.soderer.utilities.WrongPasswordException;

public class OpenSshKeyReader implements Closeable {
	private final BufferedReader dataReader;

	public OpenSshKeyReader(final InputStream inputStream) throws IOException {
		dataReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
	}

	public KeyPair readKey(final char[] password) throws WrongPasswordException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws IOException {
		try {
			dataReader.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
