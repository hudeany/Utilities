package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.Asn1Codec;

public class AsnCodecTest {
	@Test
	public void test1() {
		try {
			for (int i = 0; i < 2049; i++) {
				final String resultInt = BitUtilities.getBitString(Asn1Codec.getAsnEncodedInteger(i));
				final String resultLong = BitUtilities.getBitString(Asn1Codec.getAsnEncodedInteger(i));
				Assert.assertEquals(i + ": ", resultInt, resultLong);
			}
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
