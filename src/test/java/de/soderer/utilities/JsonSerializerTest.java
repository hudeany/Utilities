package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonSerializer;
import de.soderer.utilities.json.JsonWriter;

public class JsonSerializerTest {
	@Test
	public void test1() throws Exception {
		JsonNode dataNode = JsonSerializer.serialize(new TestObject(), true, true, true);
		JsonWriter.getJsonItemString(dataNode);
	}
	
	@Test
	public void test2() throws Exception {
		JsonNode dataNode = JsonSerializer.serialize(new TestObject(), true, true, true);
		JsonWriter.getJsonItemString(dataNode);
	}
	
	@Test
	public void testCylic1() throws Exception {
		try {
			List<Object> list = new ArrayList<Object>();
			list.add(1);
			list.add(2);
			list.add(3);
			list.add(list);
			list.add(4);
			JsonSerializer.serialize(list);
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		}
	}
	
	@Test
	public void testCylic2() throws Exception {
		try {
			Object someObject = new Object();
			
			List<Object> list1 = new ArrayList<Object>();
			List<Object> list2 = new ArrayList<Object>();
			
			list1.add(1);
			list1.add(2);
			list1.add(someObject);
			list1.add(list1);
			list1.add(someObject);

			list2.add(1);
			list2.add(2);
			list2.add(someObject);
			list2.add(list1);
			list2.add(someObject);
			JsonSerializer.serialize(list1);
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		}
	}
	
	@Test
	public void testNonCylic() throws Exception {
		Object someObject = new Object();
		
		List<Object> list = new ArrayList<Object>();
		list.add(1);
		list.add(2);
		list.add(someObject);
		list.add(someObject);
		list.add(4);
		JsonSerializer.serialize(list);
	}
}
