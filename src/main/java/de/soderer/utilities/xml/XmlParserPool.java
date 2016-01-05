package de.soderer.utilities.xml;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public class XmlParserPool extends GenericObjectPool<SAXParser> {

	private static final XmlParserPool SINGLETON_INSTANCE = new XmlParserPool();

	private XmlParserPool() {
		super(new PoolableSAXParserFactory(), new Config());
	}

	public static XmlParserPool getInstance() {
		return XmlParserPool.SINGLETON_INSTANCE;
	}

	@Override
	public SAXParser borrowObject() throws Exception {
		return super.borrowObject();
	}

	@Override
	public void returnObject(SAXParser parser) throws Exception {
		super.returnObject(parser);
	}

	private static final class Config extends GenericObjectPool.Config {
		public Config() {
			whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
			minIdle = 2;
		}
	}

	private static final class PoolableSAXParserFactory extends BasePoolableObjectFactory<SAXParser> {
		private SAXParserFactory parserFactory = SAXParserFactory.newInstance();

		@Override
		public SAXParser makeObject() throws Exception {
			return parserFactory.newSAXParser();
		}
	}
}
