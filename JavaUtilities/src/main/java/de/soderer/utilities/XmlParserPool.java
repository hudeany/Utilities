package de.soderer.utilities;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public class XmlParserPool extends GenericObjectPool {

	private static final XmlParserPool SINGLETON_INSTANCE = new XmlParserPool();

	private XmlParserPool() {
		super(new PoolableSAXParserFactory(), new Config());
	}

	public static XmlParserPool getInstance() {
		return XmlParserPool.SINGLETON_INSTANCE;
	}

	@Override
	public SAXParser borrowObject() throws Exception {
		return (SAXParser) super.borrowObject();
	}

	public void returnObject(SAXParser parser) throws Exception {
		super.returnObject(parser);
	}

	@Override
	public void returnObject(Object obj) throws Exception {
		throw new UnsupportedOperationException(XmlParserPool.class.getSimpleName() + " unterstützt nur Objekte vom Typ SAXParser!");
	}

	private static final class Config extends GenericObjectPool.Config {
		public Config() {
			this.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
			this.minIdle = 2;
		}
	}

	private static final class PoolableSAXParserFactory extends BasePoolableObjectFactory {
		
		private SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		
		@Override
		public Object makeObject() throws Exception {
			return parserFactory.newSAXParser();
		}
		
	}
}
