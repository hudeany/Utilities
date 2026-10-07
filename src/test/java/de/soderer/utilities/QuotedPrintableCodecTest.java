package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class QuotedPrintableCodecTest {
	@Test
	public void testEncode() {
		try {
			assertEquals(
					"=61=62=63=64=65=66=67=68=69=6A=6B=6C=6D=6E=6F=70=71=72=73=74=75=76=77=\n"
							+ "=78=79=7A=41=42=43=44=45=46=47=48=49=4A=4B=4C=4D=4E=4F=50=51=52=53=54=\n"
							+ "=55=56=57=58=59=5A=30=31=32=33=34=35=36=37=38=39=20=C3=A4=C3=B6=C3=BC=\n"
							+ "=C3=9F=C3=84=C3=96=C3=9C=C2=B5=21=3F=C2=A7=40=E2=82=AC=24=25=26=2F=5C=\n"
							+ "=3C=3E=28=29=7B=7D=5B=5D=27=22=C2=B4=60=5E=C2=B0=C2=B9=C2=B2=C2=B3=2A=\n"
							+ "=23=2E=2C=3B=3A=3D=2B=2D=7E=5F=7C=C2=BD=C2=BC=C2=AC=4C=69=6E=65=62=72=\n"
							+ "=65=61=6B=0D=0A=54=61=62=09=78",
							QuotedPrintableCodec.encode(TextUtilities.GERMAN_TEST_STRING + "Linebreak\r\nTab\tx", StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDecodeEncoded() {
		try {
			assertEquals(
					TextUtilities.GERMAN_TEST_STRING + "Linebreak\r\nTab\tx",
					QuotedPrintableCodec.decode(QuotedPrintableCodec.encode(TextUtilities.GERMAN_TEST_STRING + "Linebreak\r\nTab\tx", StandardCharsets.UTF_8), StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
