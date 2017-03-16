package de.soderer.utilities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class TestObject {
	public static String STATICFIELD = "textField1 value";
	
	private transient String transientField = "textField1 value";

	String packageTextField = "packageTextField value";
	private String privateTextField = "privateTextField value";
	protected String protectedTextField = "protectedTextField value";
	public String publicTextField = "publicTextField value";

	private int id = 0;
	
	private int intField = 1;
	private double doubleField = Math.PI;
	private boolean boolField = true;
	private Object nullField = null;
	private Object[] arrayField;
	private List<Object> listField;
	private Set<Object> setField;
	private TestObject otherObjectField = null;
	
	public TestObject() {
		arrayField = new Object[] { 1, 2, 3, "Text Value", true, null, new TestObject(1) };
		listField = Arrays.asList(new Object[] { 1, 2, 3, "Text Value", true, null, new TestObject(2) });
		setField = new HashSet<Object>(Arrays.asList(new Object[] { 1, 2, 3, "Text Value", true, null, new TestObject(3) }));
		otherObjectField = new TestObject(4);
	}
	
	private TestObject(int id) {
		this.id = id;
	}
}
