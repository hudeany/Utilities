package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

public class TextFilePropertiesReader extends TextPropertiesReader {
	private File file;
	private Tuple<String, Boolean> encodingData = null;
	private long filesize = -1;

	public TextFilePropertiesReader(String filePath, Tuple<String, Boolean> encodingData) throws Exception {
		this(new File(filePath), encodingData);
	}
	
	public TextFilePropertiesReader(String filePath) throws Exception {
		this(new File(filePath));
	}
	
	public TextFilePropertiesReader(File file) throws Exception {
		this(file, null);
	}
	
	public TextFilePropertiesReader(File file, Tuple<String, Boolean> encodingData) throws Exception {
		this.file = file;
		
		if (!file.exists()) {
			throw new Exception("File does not exist");
		} else if (!file.isFile()) {
			throw new Exception("Path isn't a file");
		}
		
		this.encodingData = encodingData;
	}
	
	public String readFileToString() throws Exception {
		filesize = file.length();
		byte[] data = FileUtils.readFileToByteArray(file);
		if (encodingData == null) {
			encodingData = detectEncoding(data);
		}
		if (encodingData == null) {
			// File encoding cannot be detected so use UTF-8
			encodingData = new Tuple<String, Boolean>("UTF-8", false);
		}
		dataString = decodeByteArray(data, encodingData);
		super.readProperties();
		return dataString;
	}
	
	@Override
	public void readProperties() throws Exception {
		readFileToString();
	}

	public long getFilesize() {
		return filesize;
	}

	public Tuple<String, Boolean> getEncoding() {
		return encodingData;
	}
	
	public static Tuple<String, Boolean> getEncodingData(String encodingString) {
		if ("UnicodeBig".equalsIgnoreCase(encodingString)
			|| "UTF-16BE-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<String, Boolean>("UTF-16BE", true);
		} else if ("UnicodeLittle".equalsIgnoreCase(encodingString)
			|| "UTF-16LE-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<String, Boolean>("UTF-16LE", true);
		} else if ("UTF-8-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<String, Boolean>("UTF-8", true);
		} else {
			return new Tuple<String, Boolean>(encodingString, false);
		}
	}
	
	public static String getEncodingDisplayString(Tuple<String, Boolean> encodingData) {
		if ("UTF-16BE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
			return "UTF-16BE-BOM";
		} else if ("UTF-16BE".equalsIgnoreCase(encodingData.getFirst()) && !encodingData.getSecond()) {
			return "UTF-16BE";
		} else if ("UnicodeBig".equalsIgnoreCase(encodingData.getFirst())) {
			return "UTF-16BE-BOM";
		} else if ("UTF-16".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
			return "UTF-16BE-BOM";
		} else if ("UTF-16".equalsIgnoreCase(encodingData.getFirst()) && !encodingData.getSecond()) {
			return "UTF-16BE";
		} else if ("UnicodeLittle".equalsIgnoreCase(encodingData.getFirst())) {
			return "UTF-16LE-BOM";
		} else if ("UTF-16LE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
			return "UTF-16LE-BOM";
		} else if ("UTF-16LE".equalsIgnoreCase(encodingData.getFirst()) && !encodingData.getSecond()) {
			return "UTF-16LE";
		} else if ("UTF-8".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
			return "UTF-8-BOM";
		} else {
			return encodingData.getFirst();
		}
	}
	
	public static byte[] encodeString(String text, Tuple<String, Boolean> encodingData) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		String encoding = encodingData.getFirst();
		if ("UnicodeBig".equalsIgnoreCase(encodingData.getFirst())
			|| ("UTF-16BE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond())
			|| ("UTF-16".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond())) {
			output.write(Utilities.UTF_16_BE_BOM);
			encoding = "UTF-16BE";
		} else if ("UnicodeLittle".equalsIgnoreCase(encodingData.getFirst())
			|| ("UTF-16LE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond())) {
			output.write(Utilities.UTF_16_LE_BOM);
			encoding = "UTF-16LE";
		} else if ("UTF-8".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
			output.write(Utilities.UTF_8_BOM);
		}
		output.write(text.getBytes(encoding));
		return output.toByteArray();
	}
	
	public static String decodeByteArray(byte[] data, Tuple<String, Boolean> encodingData) throws IOException {
		if (data.length > 2 && data[0] == Utilities.UTF_16_BE_BOM[0] && data[1] == Utilities.UTF_16_BE_BOM[1]) {
			return new String(data, 2, data.length - 2, encodingData.getFirst());
		} else if (data.length > 2 && data[0] == Utilities.UTF_16_LE_BOM[0] && data[1] == Utilities.UTF_16_LE_BOM[1]) {
			return new String(data, 2, data.length - 2, encodingData.getFirst());
		} else if (data.length > 3 && data[0] == Utilities.UTF_8_BOM[0] && data[1] == Utilities.UTF_8_BOM[1] && data[2] == Utilities.UTF_8_BOM[2]) {
			return new String(data, 3, data.length - 3, encodingData.getFirst());
		} else {
			return new String(data, encodingData.getFirst());
		}
	}
	
	public static Tuple<String, Boolean> detectEncoding(byte[] data) throws UnsupportedEncodingException {
		if (data.length > 2 && data[0] == Utilities.UTF_16_BE_BOM[0] && data[1] == Utilities.UTF_16_BE_BOM[1]) {
			return new Tuple<String, Boolean>("UTF-16BE", true);
		} else if (data.length > 2 && data[0] == Utilities.UTF_16_LE_BOM[0] && data[1] == Utilities.UTF_16_LE_BOM[1]) {
			return new Tuple<String, Boolean>("UTF-16LE", true);
		} else if (data.length > 3 && data[0] == Utilities.UTF_8_BOM[0] && data[1] == Utilities.UTF_8_BOM[1] && data[2] == Utilities.UTF_8_BOM[2]) {
			return new Tuple<String, Boolean>("UTF-8", true);
		} else {
			String interimString = new String(data, 0, 100, "UTF-8").toLowerCase();
			
			// Detect Xml Encoding
			try {
				String reducedInterimString = interimString.replace("\u0000", "");
				int encodingStart = reducedInterimString.indexOf("encoding");
				if (encodingStart >= 0) {
					encodingStart = reducedInterimString.indexOf("=", encodingStart);
					if (encodingStart >= 0) {
						encodingStart++;
						while (reducedInterimString.charAt(encodingStart) == ' ') {
							encodingStart++;
						}
						if (reducedInterimString.charAt(encodingStart) == '"' || reducedInterimString.charAt(encodingStart) == '\'') {
							encodingStart++;
						}
						StringBuilder encodingString = new StringBuilder();
						while (Character.isLetter(reducedInterimString.charAt(encodingStart)) || Character.isDigit(reducedInterimString.charAt(encodingStart)) || reducedInterimString.charAt(encodingStart) == '-') {
							encodingString.append(reducedInterimString.charAt(encodingStart));
							encodingStart++;
						}
						Charset.forName(encodingString.toString());
						if (encodingString.toString().startsWith("utf-16") && data[0] == 0) {
							return new Tuple<String, Boolean>("UTF-16BE", false);
						} else if (encodingString.toString().startsWith("utf-16") && data[1] == 0) {
							return new Tuple<String, Boolean>("UTF-16LE", false);
						} else {
							return new Tuple<String, Boolean>(encodingString.toString().toUpperCase(), false);
						}
					}
				}
			} catch (Exception e) {
			}
			
			int zeroIndex = interimString.indexOf('\u0000');
			if (zeroIndex >= 0) {
				if (zeroIndex % 2 == 0) {
					return new Tuple<String, Boolean>("UTF-16BE", false);
				} else {
					return new Tuple<String, Boolean>("UTF-16LE", false);
				}
			}
			
			return null;
		}
	}
}
