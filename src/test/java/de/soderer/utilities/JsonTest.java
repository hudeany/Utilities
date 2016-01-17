package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.JsonReader;
import de.soderer.utilities.json.JsonWriter;

public class JsonTest {
	@Test
	public void testEmptyArray() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.openJsonArray();
			writer.closeJsonArray();
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("[]\n", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testArray() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			Date testDate = new Date();
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.openJsonArray();
			writer.addSimpleJsonArrayValue(null);
			writer.addSimpleJsonArrayValue(true);
			writer.addSimpleJsonArrayValue(1);
			writer.addSimpleJsonArrayValue(2);
			writer.addSimpleJsonArrayValue(1.2f);
			writer.addSimpleJsonArrayValue(0.000002d);
			writer.addSimpleJsonArrayValue(1.3d);
			writer.addSimpleJsonArrayValue(0.000003d);
			writer.addSimpleJsonArrayValue(TextUtilities.GERMAN_TEST_STRING);
			writer.addSimpleJsonArrayValue(testDate);
			writer.closeJsonArray();
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals(
				"[\n"
				+ "null,\n"
				+ "true,\n"
				+ "1,\n"
				+ "2,\n"
				+ "1.2,\n"
				+ "2.0E-6,\n"
				+ "1.3,\n"
				+ "3.0E-6,\n"
				+ "\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\\\"´`^°¹²³*#.,;:=+-~_|½¼¬\",\n"
				+ "\"" + DateUtilities.ISO_8601_DATETIME_FORMAT.format(testDate) + "\"\n"
				+ "]\n",
				result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testArray2() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			Date testDate = new Date();
			output = new ByteArrayOutputStream();
			
			JsonArray jsonArray = new JsonArray();
			jsonArray.add(null);
			jsonArray.add(true);
			jsonArray.add(1);
			jsonArray.add(2);
			jsonArray.add(1.2f);
			jsonArray.add(0.000002d);
			jsonArray.add(1.3d);
			jsonArray.add(0.000003d);
			jsonArray.add(TextUtilities.GERMAN_TEST_STRING);
			jsonArray.add(testDate);

			writer = new JsonWriter(output, "UTF-8");
			writer.add(jsonArray);
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals(jsonArray.toString(), result);
			Assert.assertEquals(
				"[\n"
				+ "null,\n"
				+ "true,\n"
				+ "1,\n"
				+ "2,\n"
				+ "1.2,\n"
				+ "2.0E-6,\n"
				+ "1.3,\n"
				+ "3.0E-6,\n"
				+ "\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\\\"´`^°¹²³*#.,;:=+-~_|½¼¬\",\n"
				+ "\"" + DateUtilities.ISO_8601_DATETIME_FORMAT.format(testDate) + "\"\n"
				+ "]\n",
				result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testEmptyObject() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.openJsonObject();
			writer.closeJsonObject();
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("{}\n", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testObject() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			Date testDate = new Date();
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.openJsonObject();
			writer.openJsonObjectProperty("test_null");
			writer.addSimpleJsonObjectPropertyValue(null);
			writer.openJsonObjectProperty("test_true");
			writer.addSimpleJsonObjectPropertyValue(true);
			writer.openJsonObjectProperty("1");
			writer.addSimpleJsonObjectPropertyValue(1);
			writer.openJsonObjectProperty("2");
			writer.addSimpleJsonObjectPropertyValue(2);
			writer.openJsonObjectProperty("test_float");
			writer.addSimpleJsonObjectPropertyValue(1.2f);
			writer.openJsonObjectProperty("test_floatE");
			writer.addSimpleJsonObjectPropertyValue(0.000002d);
			writer.openJsonObjectProperty("test_double");
			writer.addSimpleJsonObjectPropertyValue(1.3d);
			writer.openJsonObjectProperty("test_doubleE");
			writer.addSimpleJsonObjectPropertyValue(0.000003d);
			writer.openJsonObjectProperty("test_äÄ");
			writer.addSimpleJsonObjectPropertyValue(TextUtilities.GERMAN_TEST_STRING);
			writer.openJsonObjectProperty("test_date");
			writer.addSimpleJsonObjectPropertyValue(testDate);
			writer.closeJsonObject();
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals(
				"{\n"
				+ "\t\"test_null\": null,\n"
				+ "\t\"test_true\": true,\n"
				+ "\t\"1\": 1,\n"
				+ "\t\"2\": 2,\n"
				+ "\t\"test_float\": 1.2,\n"
				+ "\t\"test_floatE\": 2.0E-6,\n"
				+ "\t\"test_double\": 1.3,\n"
				+ "\t\"test_doubleE\": 3.0E-6,\n"
				+ "\t\"test_äÄ\": \"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\\\"´`^°¹²³*#.,;:=+-~_|½¼¬\",\n"
				+ "\t\"test_date\": \"" + DateUtilities.ISO_8601_DATETIME_FORMAT.format(testDate) + "\"\n"
				+ "}\n",
				result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testObject2() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			Date testDate = new Date();
			output = new ByteArrayOutputStream();
			
			JsonObject jsonObject = new JsonObject();	
			jsonObject.add("test_null", null);
			jsonObject.add("test_true", true);
			jsonObject.add("1", 1);
			jsonObject.add("2", 2);
			jsonObject.add("test_float", 1.2f);
			jsonObject.add("test_floatE", 0.000002d);
			jsonObject.add("test_double", 1.3d);
			jsonObject.add("test_doubleE", 0.000003d);
			jsonObject.add("test_äÄ", TextUtilities.GERMAN_TEST_STRING);
			jsonObject.add("test_date", testDate);

			writer = new JsonWriter(output, "UTF-8");
			writer.add(jsonObject);
			writer.close();
			output.close();

			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals(jsonObject.toString(), result);
			Assert.assertEquals(
				"{\n"
				+ "\t\"test_null\": null,\n"
				+ "\t\"test_true\": true,\n"
				+ "\t\"1\": 1,\n"
				+ "\t\"2\": 2,\n"
				+ "\t\"test_float\": 1.2,\n"
				+ "\t\"test_floatE\": 2.0E-6,\n"
				+ "\t\"test_double\": 1.3,\n"
				+ "\t\"test_doubleE\": 3.0E-6,\n"
				+ "\t\"test_äÄ\": \"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\\\"´`^°¹²³*#.,;:=+-~_|½¼¬\",\n"
				+ "\t\"test_date\": \"" + DateUtilities.ISO_8601_DATETIME_FORMAT.format(testDate) + "\"\n"
				+ "}\n",
				result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testBigDataReader() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			
			writer = new JsonWriter(output, "UTF-8");
			
			writer.openJsonArray();
			for (int i = 0; i < 5; i++) {
				JsonObject jsonObject = new JsonObject();	
				jsonObject.add("number", i);
				jsonObject.add("text", TextUtilities.GERMAN_TEST_STRING);
				writer.add(jsonObject);
			}
			writer.closeJsonArray();
			
			writer.close();
			output.close();

			String result = new String(output.toByteArray(), "UTF-8");
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			jsonReader.openRootArray();
			
			Object nextRootArrayObject;
			int count = 0;
			while((nextRootArrayObject = jsonReader.readNextRootArrayJsonItem()) != null) {
				if (nextRootArrayObject instanceof JsonObject) {
					Assert.assertEquals(new Long(count), ((JsonObject) nextRootArrayObject).get("number"));
				} else {
					Assert.fail();
				}
				count++;
			}
			Assert.assertEquals(5, count);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
}
