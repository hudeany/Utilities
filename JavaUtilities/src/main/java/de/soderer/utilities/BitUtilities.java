package de.soderer.utilities;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class BitUtilities {
	private static Pattern BITSTRINGPATTERN_BYTE = Pattern.compile("^[10]{1,8}$"); 
	private static Pattern BITSTRINGPATTERN_INTEGER = Pattern.compile("^[10]{1,32}$"); 
	
	/**
	 * Returns values from 0 to 255 (value range of byte)
	 * 
	 * @param value
	 * @return
	 */
	public static int getUnsignedValue(byte value) {
		if (value < 0) {
			return ((int)value) + 256;
		} else {
			return value;
		}
	}

	/**
	 * Returns values from 0 to 255 (value range of byte)
	 * 
	 * @param value
	 * @return
	 */
	public static int[] getUnsignedValue(byte[] value) {
		int[] returnValue = new int[value.length];
		for (int i = 0; i < value.length; i++) {
			returnValue[i] = getUnsignedValue(value[i]);
		}
		return returnValue;
	}
	
	/**
	 * Returns the value as a Byte which contains only signed values in Java
	 * @param value
	 * @return
	 * @throws Exception
	 */
	public static byte getUnsignedAsByte(int value) throws Exception {
		if (value < 0 || value > 255) {
			throw new Exception("Unsigned value out of range for byte");
		} else if (value > 127) {
			return (byte) (value - 256);
		} else {
			return (byte) value;
		}
	}
	
	public static String getBitString(byte byteItem) {
		StringBuilder result = new StringBuilder();
		for (int i = 7; i >= 0; i--) {
			result.append((byte) ((byteItem >>> i) & 0x01));
		}
		return result.toString();
	}
	
	public static String getBitString(int intItem) {
		StringBuilder result = new StringBuilder();
		for (int i = 31; i >= 0; i--) {
			result.append((byte) ((intItem >>> i) & 0x01));
		}
		return result.toString();
	}
	
	public static String getBitString(byte[] byteArray) {
		return getBitString(byteArray, " ");
	}
	
	public static String getBitString(byte[] byteArray, String byteSeparator) {
		StringBuilder result = new StringBuilder();
		for (byte byteItem : byteArray) {
			if (result.length() > 0) {
				result.append(byteSeparator);
			}
			
			for (int i = 7; i >= 0; i--) {
				result.append((byte) ((byteItem >>> i) & 0x01));
			}
		}
		return result.toString();
	}
	
	public static byte getByteFromBitString(String bitString) throws Exception {
		if (StringUtils.isEmpty(bitString)) {
			throw new Exception("Invalid bitstring for byte");
		}
		
		bitString = bitString.trim();
		
		if (!BITSTRINGPATTERN_BYTE.matcher(bitString).matches()) {
			throw new Exception("Invalid bitstring for byte");
		}
				
		byte result = 0;
		int posValue = 1;
		char[] characters = StringUtils.reverse(bitString).toCharArray();
		for (int i = 0; i < characters.length; i++) {
			if (characters[i] == '1') {
				result += posValue;
			}
			posValue *= 2;
		}
		return result;
	}
	
	public static int getIntegerFromBitString(String bitString) throws Exception {
		if (StringUtils.isEmpty(bitString)) {
			throw new Exception("Invalid bitstring for integer");
		}
		
		bitString = bitString.trim();
		
		if (!BITSTRINGPATTERN_INTEGER.matcher(bitString).matches()) {
			throw new Exception("Invalid bitstring for integer");
		}
				
		int result = 0;
		int posValue = 1;
		char[] characters = StringUtils.reverse(bitString).toCharArray();
		for (int i = 0; i < characters.length; i++) {
			if (characters[i] == '1') {
				result += posValue;
			}
			posValue *= 2;
		}
		return result;
	}
}
