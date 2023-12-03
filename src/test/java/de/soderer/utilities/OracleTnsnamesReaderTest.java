package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.db.OracleTnsnamesReader;

@SuppressWarnings("static-method")
public class OracleTnsnamesReaderTest {
	@Test
	public void testSimple() throws Exception {
		final String tnsNamesContent =
				"TNSNAME=" + "(1=1)";

		try (OracleTnsnamesReader reader = new OracleTnsnamesReader(new ByteArrayInputStream(tnsNamesContent.getBytes(StandardCharsets.UTF_8)))) {
			final Map<String, Map<String, Object>> tnsnamesEntries = reader.read();
			Assert.assertNotNull(tnsnamesEntries);

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME"));
		}
	}

	@Test
	public void testSimple2() throws Exception {
		final String tnsNamesContent =
				"TNSNAME=" + "(B=(A=1))";

		try (OracleTnsnamesReader reader = new OracleTnsnamesReader(new ByteArrayInputStream(tnsNamesContent.getBytes(StandardCharsets.UTF_8)))) {
			final Map<String, Map<String, Object>> tnsnamesEntries = reader.read();
			Assert.assertNotNull(tnsnamesEntries);

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME"));
		}
	}

	@Test
	public void testSimple3() throws Exception {
		final String tnsNamesContent =
				"TNSNAME=" + "(C=(A=1)(B=1))";

		try (OracleTnsnamesReader reader = new OracleTnsnamesReader(new ByteArrayInputStream(tnsNamesContent.getBytes(StandardCharsets.UTF_8)))) {
			final Map<String, Map<String, Object>> tnsnamesEntries = reader.read();
			Assert.assertNotNull(tnsnamesEntries);

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME"));
		}
	}

	@Test
	public void testSimple4() throws Exception {
		final String tnsNamesContent =
				"TNSNAME=" + "(D="
						+ "(A=1)"
						+ "(B=2)"
						+ "(C=3)"
						+ ")";

		try (OracleTnsnamesReader reader = new OracleTnsnamesReader(new ByteArrayInputStream(tnsNamesContent.getBytes(StandardCharsets.UTF_8)))) {
			final Map<String, Map<String, Object>> tnsnamesEntries = reader.read();
			Assert.assertNotNull(tnsnamesEntries);

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME"));
		}
	}

	@Test
	public void testSimple5() throws Exception {
		final String tnsNamesContent =
				"TNSNAME1=" + "(A=1)\n"
						+ "TNSNAME2=" + "(B=2)";

		try (OracleTnsnamesReader reader = new OracleTnsnamesReader(new ByteArrayInputStream(tnsNamesContent.getBytes(StandardCharsets.UTF_8)))) {
			final Map<String, Map<String, Object>> tnsnamesEntries = reader.read();
			Assert.assertNotNull(tnsnamesEntries);

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME1"));

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME2"));
		}
	}

	@Test
	public void test() throws Exception {
		final String description1 =
				"TNSNAME1=" + "(DESCRIPTION="
						+ "(ADDRESS=(PROTOCOL=TCPS)(HOST=serverHost1)(PORT=1521))"
						+ "(CONNECT_DATA=(SERVICE_NAME=serviceName1))"
						+ "(SECURITY=(ssl_server_cert_dn=\"trusted CN1\"))"
						+ ")";

		final String description2 =
				"TNSNAME2 = " + "(DESCRIPTION = " + "\n"
						+ "(ADDRESS = (PROTOCOL = TCP)(HOST = serverHost2)(PORT = 1522))" + "\n"
						+ "(CONNECT_DATA = (SERVICE_NAME = serviceName2))" + "\n"
						+ ")" + "\n";

		final String description3 =
				"TNSNAME3 = " + "(DESCRIPTION = " + "\n"
						+ "(ADDRESSLIST = " + "\n"
						+ "(ADDRESS = (PROTOCOL = TCP)(HOST = serverHost31)(PORT = 15231))" + "\n"
						+ "(ADDRESS = (PROTOCOL = TCP)(HOST = serverHost32)(PORT = 15232))" + "\n"
						+ ")" + "\n"
						+ "(CONNECT_DATA = (SERVICE_NAME = serviceName3))" + "\n"
						+ ")" + "\n";

		final String description4 =
				"TNSNAME4 = " + "(DESCRIPTION = " + "\n"
						+ "(ADDRESSLIST = " + "\n"
						+ "(ADDRESS = (PROTOCOL = TCP)(HOST = serverHost4)(PORT = 1524))" + "\n"
						+ ")" + "\n"
						+ "(CONNECT_DATA = (SID = sidName4))" + "\n"
						+ ")" + "\n";

		final String tnsNamesContent =
				description1
				+ description2 + "\n"
				+ description3 + "\n"
				+ description4;

		try (OracleTnsnamesReader reader = new OracleTnsnamesReader(new ByteArrayInputStream(tnsNamesContent.getBytes(StandardCharsets.UTF_8)))) {
			final Map<String, Map<String, Object>> tnsnamesEntries = reader.read();
			Assert.assertNotNull(tnsnamesEntries);

			Assert.assertNotNull(tnsnamesEntries.get("tnsname1"));
			@SuppressWarnings("unchecked")
			final Map<String, Object> description_1 = (Map<String, Object>) tnsnamesEntries.get("tnsname1").get("description");
			@SuppressWarnings("unchecked")
			final Map<String, Object> address1 = (Map<String, Object>) description_1.get("address");
			Assert.assertEquals("TCPS", address1.get("protocol"));
			Assert.assertEquals("serverHost1", address1.get("host"));
			Assert.assertEquals("1521", address1.get("port"));
			@SuppressWarnings("unchecked")
			final Map<String, Object> security1 = (Map<String, Object>) description_1.get("security");
			Assert.assertEquals("trusted CN1", security1.get("ssl_server_cert_dn"));

			Assert.assertEquals("(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=serverHost1)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=serviceName1))(SECURITY=(SSL_SERVER_CERT_DN=\"trusted CN1\")))",
					OracleTnsnamesReader.getSingleLineFormatedTnsEntryData(tnsnamesEntries.get("tnsname1")));

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME2"));

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME3"));

			Assert.assertNotNull(tnsnamesEntries.get("TNSNAME4"));
			@SuppressWarnings("unchecked")
			final Map<String, Object> description_4 = (Map<String, Object>) tnsnamesEntries.get("tnsname4").get("description");
			@SuppressWarnings("unchecked")
			final Map<String, Object> connectdata4 = (Map<String, Object>) description_4.get("connect_data");
			Assert.assertEquals("sidName4", connectdata4.get("SID"));
		}
	}
}
