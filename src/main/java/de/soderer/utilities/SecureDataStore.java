package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonReader.JsonToken;
import de.soderer.json.JsonSerializer;
import de.soderer.json.JsonWriter;

public class SecureDataStore extends UnsecureDataStore {
	private static final String SYMMETRIC_ENCRYPTION_METHOD = "AES/GCM/NoPadding";
	private static final int RANDOMSALTSIZE = 16;
	private static final int GCM_IV_SIZE = 12;
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int DERIVED_KEY_LENGTH_BITS = 256;
	private static final int PBKDF2_ITERATIONS = 210000;

	@Override
	public void save(final File storeFile) throws Exception {
		save(storeFile, null);
	}

	public void save(final File storeFile, char[] password) throws Exception {
		try {
			if (storeFile == null) {
				throw new Exception("SecureDataStore file is undefined");
			} else {
				if (password == null) {
					password = "".toCharArray();
				}
				byte[] salt = null;
				byte[] iv = null;
				try (FileOutputStream outputStream = new FileOutputStream(storeFile)) {
					salt = new byte[RANDOMSALTSIZE];
					new SecureRandom().nextBytes(salt);
					outputStream.write(salt);

					iv = new byte[GCM_IV_SIZE];
					new SecureRandom().nextBytes(iv);
					outputStream.write(iv);

					final byte[] keyBytes = stretchPassword(password, salt);
					final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
					final Cipher encryptCipher = Cipher.getInstance(SYMMETRIC_ENCRYPTION_METHOD);
					encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
					try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, encryptCipher)) {
						try (JsonWriter jsonWriter = new JsonWriter(cipherOutputStream)) {
							jsonWriter.openJsonArray();
							for (final Entry<String, Map<String, Object>> entryMap : dataEntries.entrySet()) {
								for (final Entry<String, Object> entry : entryMap.getValue().entrySet()) {
									final JsonObject entryObject = new JsonObject();
									entryObject.add("class", entryMap.getKey());
									entryObject.add("name", entry.getKey());
									entryObject.add("value", JsonSerializer.serialize(entry.getValue(), false, false, false, true));
									jsonWriter.add(entryObject);
								}
							}
							jsonWriter.closeJsonArray();
						}
					}
				} finally {
					Utilities.clear(salt);
					Utilities.clear(iv);
				}
			}
		} finally {
			Arrays.fill(password, '\u0000');
		}
	}

	@Override
	public void load(final File storeFile) throws Exception {
		load(storeFile, null);
	}

	public void load(final File storeFile, char[] password) throws Exception {
		try {
			if (storeFile == null) {
				throw new Exception("SecureDataStore file is undefined");
			} else if (!storeFile.exists()) {
				throw new Exception("SecureDataStore file does not exist: " + storeFile.getAbsolutePath());
			} else {
				if (password == null) {
					password = "".toCharArray();
				}
				dataEntries = new HashMap<>();
				byte[] salt = null;
				byte[] iv = null;
				try (FileInputStream inputStream = new FileInputStream(storeFile)) {
					salt = new byte[RANDOMSALTSIZE];
					final int readSaltBytes = inputStream.read(salt);
					if (readSaltBytes != salt.length) {
						throw new Exception("Cannot read password salt prefix: Data is too short");
					}

					iv = new byte[GCM_IV_SIZE];
					final int readIvBytes = inputStream.read(iv);
					if (readIvBytes != iv.length) {
						throw new Exception("Cannot read initialization vector prefix: Data is too short");
					}

					final byte[] keyBytes = stretchPassword(password, salt);
					final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
					final Cipher decryptCipher = Cipher.getInstance(SYMMETRIC_ENCRYPTION_METHOD);
					decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
					try (CipherInputStream cipherInputStream = new CipherInputStream(inputStream, decryptCipher)) {
						try (JsonReader jsonReader = new JsonReader(cipherInputStream)) {
							final JsonToken jsonToken = jsonReader.readNextToken();
							if (jsonToken != JsonToken.JsonArray_Open) {
								throw new Exception("SecureDataStore is corrupt");
							} else {
								JsonNode nextJsonNode;
								while ((nextJsonNode = jsonReader.readNextJsonNode()) != null) {
									if (nextJsonNode.isJsonObject()) {
										final JsonObject entryObject = (JsonObject) nextJsonNode;
										final String className = (String) entryObject.getSimpleValue("class");
										if (!dataEntries.containsKey(className)) {
											dataEntries.put(className, new HashMap<>());
										}
										final String entryName = (String) entryObject.getSimpleValue("name");
										dataEntries.get(className).put(entryName, JsonSerializer.deserialize((JsonObject) entryObject.get("value")));
									} else {
										throw new Exception("Unexpected JSON data in SecureDataStore: " + nextJsonNode.getJsonDataType().getName());
									}
								}
							}
						}
					} catch (final IOException e) {
						if (e.getCause() != null && (e.getCause() instanceof AEADBadTagException || e.getCause() instanceof BadPaddingException)) {
							throw new WrongPasswordException();
						} else {
							throw e;
						}
					}
				} finally {
					Utilities.clear(salt);
					Utilities.clear(iv);
				}
			}
		} finally {
			Arrays.fill(password, '\u0000');
		}
	}

	private static byte[] stretchPassword(final char[] password, final byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
		final String algorithm = "PBKDF2WithHmacSHA512";
		final KeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, DERIVED_KEY_LENGTH_BITS);
		final SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
		return f.generateSecret(spec).getEncoded();
	}
}
