package de.soderer.utilities;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.crypto.Asn1Codec;

@SuppressWarnings("static-method")
public class AsnCodecTest {
	@Test
	public void test1() {
		try {
			for (int i = 0; i < 2049; i++) {
				final String resultInt = BitUtilities.getBitString(Asn1Codec.getAsnEncodedInteger(i));
				final String resultLong = BitUtilities.getBitString(Asn1Codec.getAsnEncodedInteger(i));
				assertEquals(resultInt, resultLong, i + ": ");
			}
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
