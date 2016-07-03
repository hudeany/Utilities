package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import de.soderer.utilities.DbColumnType.SimpleDataType;
import de.soderer.utilities.collection.CaseInsensitiveMap;
import de.soderer.utilities.collection.CaseInsensitiveSet;

public class DbUtilities {
	public static final String DOWNLOAD_LOCATION_MYSQL = "https://dev.mysql.com/downloads/connector/j/";
	public static final String DOWNLOAD_LOCATION_ORACLE = "http://www.oracle.com/technetwork/apps-tech/jdbc-112010-090769.html";
	public static final String DOWNLOAD_LOCATION_POSTGRESQL = "https://jdbc.postgresql.org/download.html";
	public static final String DOWNLOAD_LOCATION_FIREBIRD = "http://www.firebirdsql.org/en/jdbc-driver";
	public static final String DOWNLOAD_LOCATION_DERBY = "https://db.apache.org/derby/derby_downloads.html";
	public static final String DOWNLOAD_LOCATION_SQLITE = "https://bitbucket.org/xerial/sqlite-jdbc/downloads";
	public static final String DOWNLOAD_LOCATION_HSQL = "http://hsqldb.org/download";
	
	/**
	 * In an Oracle DB the statement "SELECT CURRENT_TIMESTAMP FROM DUAL" return this special Oracle type "oracle.sql.TIMESTAMPTZ",
	 * which is not listed in java.sql.Types, but can be read via ResultSet.getTimestamp(i) into a normal java.sql.Timestamp object
	 */
	public static final int ORACLE_TIMESTAMPTZ_TYPECODE = -101;
	
	public enum DbVendor {
		Oracle("oracle.jdbc.OracleDriver", 1521),
		MySQL("com.mysql.jdbc.Driver", 3306),
		PostgreSQL("org.postgresql.Driver", 5432),
		Firebird("org.firebirdsql.jdbc.FBDriver", 3050),
		SQLite("org.sqlite.JDBC", 0),
		Derby("org.apache.derby.jdbc.EmbeddedDriver", 0),
		HSQL("org.hsqldb.jdbc.JDBCDriver", 0);
		
		public static DbVendor getDbVendorByName(String dbVendorName) throws Exception {
			if ("oracle".equalsIgnoreCase(dbVendorName)) {
				return DbVendor.Oracle;
			} else if ("mysql".equalsIgnoreCase(dbVendorName)) {
				return DbVendor.MySQL;
			} else if ("postgres".equalsIgnoreCase(dbVendorName) || "postgresql".equalsIgnoreCase(dbVendorName)) {
				return DbVendor.PostgreSQL;
			} else if ("sqlite".equalsIgnoreCase(dbVendorName)) {
				return DbVendor.SQLite;
			} else if ("derby".equalsIgnoreCase(dbVendorName)) {
				return DbVendor.Derby;
			} else if ("hsql".equalsIgnoreCase(dbVendorName) || "hypersql".equalsIgnoreCase(dbVendorName)) {
				return DbVendor.HSQL;
			} else if ("firebird".equalsIgnoreCase(dbVendorName)) {
				return DbVendor.Firebird;
			} else {
				throw new Exception("Unknown db vendor: " + dbVendorName);
			}
		}
		
		private String driverClassName;
		private int defaultPort;
		
		private DbVendor(String driverClassName, int defaultPort) {
			this.driverClassName = driverClassName;
			this.defaultPort = defaultPort;
		}
		
		public String getDriverClassName() {
			return driverClassName;
		}
		
		public int getDefaultPort() {
			return defaultPort;
		}
	}

	public static String generateUrlConnectionString(DbVendor dbVendor, String dbServerHostname, int dbServerPort, String dbName) throws Exception {
		if (DbVendor.Oracle == dbVendor) {
			if (dbName.startsWith("/")) {
				// Some Oracle databases only accept the SID separated by a "/"
				return "jdbc:oracle:thin:@" + dbServerHostname + ":" + (dbServerPort <= 0 ? dbVendor.defaultPort : dbServerPort) + dbName;
			} else {
				return "jdbc:oracle:thin:@" + dbServerHostname + ":" + (dbServerPort <= 0 ? dbVendor.defaultPort : dbServerPort) + ":" + dbName;
			}
		} else if (DbVendor.MySQL == dbVendor) {
			return "jdbc:mysql://" + dbServerHostname + ":" + (dbServerPort <= 0 ? dbVendor.defaultPort : dbServerPort) + "/" + dbName + "?useEncoding=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull";
		} else if (DbVendor.PostgreSQL == dbVendor) {
			return "jdbc:postgresql://" + dbServerHostname + ":" + (dbServerPort <= 0 ? dbVendor.defaultPort : dbServerPort) + "/" + dbName;
		} else if (DbVendor.SQLite == dbVendor) {
			return "jdbc:sqlite:" + dbName.replace("~", System.getProperty("user.home"));
		} else if (DbVendor.Derby == dbVendor) {
			return "jdbc:derby:" + dbName.replace("~", System.getProperty("user.home"));
		} else if (DbVendor.Firebird == dbVendor) {
			return "jdbc:firebirdsql:" + dbServerHostname + "/" + (dbServerPort <= 0 ? dbVendor.defaultPort : dbServerPort) + ":" + dbName.replace("~", System.getProperty("user.home"));
		} else if (DbVendor.HSQL == dbVendor) {
			dbName = dbName.replace("~", System.getProperty("user.home"));
			if (dbName.startsWith("/")) {
				return "jdbc:hsqldb:file:" + dbName + ";shutdown=true";
			} else if (Utilities.isNotBlank(dbServerHostname)) {
				if (!dbServerHostname.toLowerCase().startsWith("http")) {
					dbServerHostname = "http://" + dbServerHostname;
				}
				if (dbServerHostname.toLowerCase().startsWith("https://") && dbServerPort == 443) {
					dbServerPort = -1;
				} else if (dbServerHostname.toLowerCase().startsWith("http://") && dbServerPort == 80) {
					dbServerPort = -1;
				}
				return "jdbc:hsqldb:" + dbServerHostname + (dbServerPort <= 0 ? "" : ":" + dbServerPort) + "/" + dbName;
			} else {
				return "jdbc:hsqldb:mem:" + dbName;
			}
		} else {
			throw new Exception("Unknown db vendor");
		}
	}
	
	public static Connection createNewDatabase(DbVendor dbVendor, String dbPath) throws Exception {
		if (dbVendor == null) {
			throw new Exception("Unknown db vendor");
		}
		
		Class.forName(dbVendor.getDriverClassName());
	
		if (dbVendor == DbVendor.SQLite) {
			dbPath = dbPath.replace("~", System.getProperty("user.home"));
			if (new File(dbPath).exists()) {
				throw new Exception("SQLite db file '" + dbPath + "' already exists");
			}
			return DriverManager.getConnection(generateUrlConnectionString(dbVendor, "", 0, dbPath));
		} else if (dbVendor == DbVendor.Derby) {
			dbPath = dbPath.replace("~", System.getProperty("user.home"));
			if (new File(dbPath).exists()) {
				throw new Exception("Derby db directory '" + dbPath + "' already exists");
			}
			return DriverManager.getConnection(generateUrlConnectionString(dbVendor, "", 0, dbPath) + ";create=true");
		} else if (dbVendor == DbVendor.HSQL) {
			dbPath = dbPath.replace("~", System.getProperty("user.home"));
			if (dbPath.startsWith("/")) {
				if (getFilesByPattern(new File(dbPath.substring(0, dbPath.lastIndexOf("/"))), dbPath.substring(dbPath.lastIndexOf("/") + 1).replace(".", "\\.") + "\\..*", false).size() > 0) {
					throw new Exception("HSQL db '" + dbPath + "' already exists");
				}
			}
			
			// Logger must be kept in a local variable for making it work
			Logger dbLogger = Logger.getLogger("hsqldb.db");
			dbLogger.setLevel(Level.WARNING);
			
			return DriverManager.getConnection(generateUrlConnectionString(dbVendor, "", 0, dbPath));
		} else {
			throw new Exception("Invalid db vendor '" + dbVendor.toString() + "'. Only SQLite, HSQL or Derby db can be created this way.");
		}
	}
	
	public static void deleteDatabase(DbVendor dbVendor, String dbPath) throws Exception {
		if (dbVendor == null) {
			throw new Exception("Unknown db vendor");
		}
	
		if (dbVendor == DbVendor.SQLite) {
			dbPath = dbPath.replace("~", System.getProperty("user.home"));
			if (new File(dbPath).exists()) {
				new File(dbPath).delete();
			}
		} else if (dbVendor == DbVendor.Derby) {
			dbPath = dbPath.replace("~", System.getProperty("user.home"));
			if (new File(dbPath).exists()) {
				Utilities.delete(new File(dbPath));
			}
		} else if (dbVendor == DbVendor.HSQL) {
			dbPath = dbPath.replace("~", System.getProperty("user.home"));
			if (dbPath.startsWith("/")) {
				File baseDirectory = new File(dbPath.substring(0, dbPath.lastIndexOf("/")));
				String basename = dbPath.substring(dbPath.lastIndexOf("/") + 1);
				for (File fileToDelete : baseDirectory.listFiles()) {
					if (fileToDelete.getName().startsWith(basename)) {
						Utilities.delete(fileToDelete);
					}
				}
			}
		} else {
			throw new Exception("Invalid db vendor '" + dbVendor.toString() + "'. Only SQLite, HSQL or Derby db can be deleted this way.");
		}
	}
	
	public static Connection createConnection(DbVendor dbVendor, String hostname, String dbName, String userName, char[] password) throws Exception {
		if (dbVendor == null) {
			throw new Exception("Unknown db vendor");
		} else if (Utilities.isEmpty(hostname) && dbVendor != DbVendor.HSQL && dbVendor != DbVendor.SQLite && dbVendor != DbVendor.Derby){
			throw new Exception("Cannot create db connection: Missing hostname");
		} else if (Utilities.isEmpty(dbName)){
			throw new Exception("Cannot create db connection: Missing dbName");
		}
		
		try {
			Class.forName(dbVendor.getDriverClassName());
		} catch (Exception e) {
			throw new Exception("Cannot create db connection, caused by unknown DriverClassName: " + e.getMessage());
		}
	
		try {
			if (dbVendor == DbVendor.SQLite) {
				dbName = dbName.replace("~", System.getProperty("user.home"));
				if (!new File(dbName).exists()) {
					throw new DbNotExistsException("SQLite db file '" + dbName + "' is not available");
				} else if (!new File(dbName).isFile()) {
					throw new Exception("SQLite db file '" + dbName + "' is not a file");
				}
				return DriverManager.getConnection(generateUrlConnectionString(dbVendor, "", 0, dbName));
			} else if (dbVendor == DbVendor.Derby) {
				dbName = dbName.replace("~", System.getProperty("user.home"));
				if (!new File(dbName).exists()) {
					throw new DbNotExistsException("Derby db directory '" + dbName + "' is not available");
				} else if (!new File(dbName).isDirectory()) {
					throw new Exception("Derby db directory '" + dbName + "' is not a directory");
				}
				return DriverManager.getConnection(generateUrlConnectionString(dbVendor, "", 0, dbName));
			} else if (dbVendor == DbVendor.HSQL) {
				dbName = dbName.replace("~", System.getProperty("user.home"));
				if (dbName.startsWith("/")) {
					if (getFilesByPattern(new File(dbName.substring(0, dbName.lastIndexOf("/"))), dbName.substring(dbName.lastIndexOf("/") + 1).replace(".", "\\.") + "\\..*", false).size() <= 0) {
						throw new DbNotExistsException("HSQL db '" + dbName + "' is not available");
					}
				}
				int port;
				String[] hostParts = hostname.split(":");
				if (hostParts.length == 2) {
					try {
						port = Integer.parseInt(hostParts[1]);
					} catch (Exception e) {
						throw new Exception("Invalid port: " + hostParts[1]);
					}
				} else {
					port = dbVendor.getDefaultPort();
				}
				
				// Logger must be kept in a local variable for making it work
				Logger dbLogger = Logger.getLogger("hsqldb.db");
				dbLogger.setLevel(Level.WARNING);
				
				return DriverManager.getConnection(generateUrlConnectionString(dbVendor, hostParts[0], port, dbName), (Utilities.isNotEmpty(userName) ? userName : "SA"), (password != null ? new String(password) : ""));
			} else {
				int port;
				String[] hostParts = hostname.split(":");
				if (hostParts.length == 2) {
					try {
						port = Integer.parseInt(hostParts[1]);
					} catch (Exception e) {
						throw new Exception("Invalid port: " + hostParts[1]);
					}
				} else {
					port = dbVendor.getDefaultPort();
				}
			
				return DriverManager.getConnection(generateUrlConnectionString(dbVendor, hostParts[0], port, dbName), userName, new String(password));
			}
		} catch (DbNotExistsException e) {
			throw e;
		} catch (Exception e) {
			try {
				int port;
				if (hostname.contains(":")) {
					try {
						port = Integer.parseInt(hostname.substring(hostname.indexOf(":") + 1));
					} catch (Exception e1) {
						throw new Exception("Invalid port: " + hostname.substring(hostname.indexOf(":") + 1));
					}
					NetworkUtilities.testConnection(hostname.substring(0, hostname.indexOf(":")), port);
				} else {
					port = dbVendor.getDefaultPort();
					NetworkUtilities.testConnection(hostname, port);
				}
				
				// No Exception from testConnection, so it must be some other problem
				throw new Exception("Cannot create db connection: " + e.getMessage());
			} catch (Exception e1) {
				throw new Exception("Cannot create db connection, caused by Url (" + hostname + "): " + e1.getMessage(), e1);
			}
		}
	}
	
	public static int readoutInOutputStream(DataSource dataSource, String statementString, OutputStream outputStream, String encoding, char separator, Character stringQuote) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			return readoutInOutputStream(connection, statementString, outputStream, encoding, separator, stringQuote);
		}
	}
	
	public static int readoutInOutputStream(Connection connection, String statementString, OutputStream outputStream, String encoding, char separator, Character stringQuote) throws Exception {
		DbVendor dbVendor = getDbVendor(connection);
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(statementString);
				CsvWriter csvWriter = new CsvWriter(outputStream, encoding, new CsvFormat().setSeparator(separator).setStringQuote(stringQuote))) {
			ResultSetMetaData metaData = resultSet.getMetaData();

			List<String> headers = new ArrayList<>();
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				headers.add(metaData.getColumnLabel(i));
			}
			csvWriter.writeValues(headers);

			while (resultSet.next()) {
				List<String> values = new ArrayList<String>();
				for (int i = 1; i <= metaData.getColumnCount(); i++) {
					if (metaData.getColumnType(i) == Types.BLOB
							|| metaData.getColumnType(i) == Types.BINARY
							|| metaData.getColumnType(i) == Types.VARBINARY
							|| metaData.getColumnType(i) == Types.LONGVARBINARY) {
						if (dbVendor == DbVendor.SQLite || dbVendor == DbVendor.PostgreSQL) {
							// SQLite does not allow "resultSet.getBlob(i)"
							InputStream input = null;
							try {
								input = resultSet.getBinaryStream(metaData.getColumnName(i));
								if (input != null) {
									byte[] data = Utilities.toByteArray(input);
									values.add(Base64.getEncoder().encodeToString(data));
								} else {
									values.add("");
								}
							} catch (Exception e) {
								// NULL blobs throw a NullpointerException in SQLite
								values.add("");
							} finally {
								Utilities.closeQuietly(input);
							}
						} else {
							Blob blob = resultSet.getBlob(i);
							if (resultSet.wasNull()) {
								values.add("");
							} else {
								try (InputStream input = blob.getBinaryStream()) {
									byte[] data = Utilities.toByteArray(input);
									values.add(Base64.getEncoder().encodeToString(data));
								}
							}
						}
					} else {
						values.add(resultSet.getString(i));
					}
				}
				csvWriter.writeValues(values);
			}

			return csvWriter.getWrittenLines() - 1;
		}
	}

	public static String readout(DataSource dataSource, String statementString, char separator, Character stringQuote) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			return readout(connection, statementString, separator, stringQuote);
		}
	}
    
    public static String readout(Connection connection, String statementString, char separator, Character stringQuote) throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		readoutInOutputStream(connection, statementString, outputStream, "UTF-8", separator, stringQuote);
		return new String(outputStream.toByteArray(), "UTF-8");
	}
    
    public static String readoutTable(DataSource dataSource, String tableName, char separator, Character stringQuote) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			return readoutTable(connection, tableName, separator, stringQuote);
		}
    }
    
    public static String readoutTable(Connection connection, String tableName) throws Exception {
		return readoutTable(connection, tableName, ';', '"');
    }

	public static String readoutTable(Connection connection, String tableName, char separator, Character stringQuote) throws Exception {
		if (connection == null) {
			throw new Exception("Invalid empty connection for getColumnNames");
		} else if (Utilities.isBlank(tableName)) {
			throw new Exception("Invalid empty tableName for getColumnNames");
		} else {
			List<String> columnNames = new ArrayList<String>(getColumnNames(connection, tableName));
			Collections.sort(columnNames);
			List<String> keyColumnNames = new ArrayList<String>(getPrimaryKeyColumns(connection, tableName));
			Collections.sort(keyColumnNames);
			List<String> readoutColumns = new ArrayList<String>();
			readoutColumns.addAll(keyColumnNames);
			for (String columnName : columnNames) {
				if (!Utilities.containsIgnoreCase(readoutColumns, columnName)) {
					readoutColumns.add(columnName);
				}
			}
			String orderPart = "";
			if (!keyColumnNames.isEmpty()) {
				orderPart = " ORDER BY " + Utilities.join(keyColumnNames, ", ");
			}
			return readout(connection, "SELECT " + Utilities.join(readoutColumns, ", ") + " FROM " + tableName + orderPart, separator, stringQuote);
		}
	}

	public static DbVendor getDbVendor(DataSource dataSource) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			return getDbVendor(connection);
		} catch (SQLException e) {
			throw new RuntimeException("Cannot check db vendor: " + e.getMessage(), e);
		}
	}

	public static DbVendor getDbVendor(Connection connection) throws Exception {
		try {
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			if (databaseMetaData != null) {
				String productName = databaseMetaData.getDatabaseProductName();
				if (productName != null && productName.toLowerCase().contains("oracle")) {
					return DbVendor.Oracle;
				} else if (productName != null && productName.toLowerCase().contains("mysql")) {
					return DbVendor.MySQL;
				} else if (productName != null && productName.toLowerCase().contains("postgres")) {
					return DbVendor.PostgreSQL;
				} else if (productName != null && productName.toLowerCase().contains("sqlite")) {
					return DbVendor.SQLite;
				} else if (productName != null && productName.toLowerCase().contains("derby")) {
					return DbVendor.Derby;
				} else if (productName != null && productName.toLowerCase().contains("hsql")) {
					return DbVendor.HSQL;
				} else if (productName != null && productName.toLowerCase().contains("firebird")) {
					return DbVendor.Firebird;
				} else {
					throw new Exception("Unknown db vendor: " + productName);
				}
			} else {
				throw new Exception("Undetectable db vendor");
			}
		} catch (SQLException e) {
			throw new Exception("Error while detecting db vendor: " + e.getMessage(), e);
		}
	}

	public static String getDbUrl(DataSource dataSource) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			return getDbUrl(connection);
		}
	}

	public static String getDbUrl(Connection connection) {
		try {
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			if (databaseMetaData != null) {
				return databaseMetaData.getURL();
			} else {
				return null;
			}
		} catch (SQLException e) {
			return null;
		}
	}

	public static boolean checkTableAndColumnsExist(Connection connection, String tableName, String... columns) throws Exception {
		return checkTableAndColumnsExist(connection, tableName, false, columns);
	}

	public static boolean checkTableAndColumnsExist(DataSource dataSource, String tableName, boolean throwExceptionOnError, String... columns) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			return checkTableAndColumnsExist(connection, tableName, throwExceptionOnError, columns);
		}
	}

	public static boolean checkTableAndColumnsExist(Connection connection, String tableName, boolean throwExceptionOnError, String... columns) throws Exception {
		CaseInsensitiveSet dbTableColumns = getColumnNames(connection, tableName);
		if (columns != null) {
			for (String column : columns) {
				if (column != null && !dbTableColumns.contains(column)) {
					if (throwExceptionOnError) {
						throw new Exception("Column '" + column + "' does not exist in table '" + tableName + "'");
					} else {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public static boolean checkTableExist(Connection connection, String tableName) throws Exception {
		return checkTableExist(connection, tableName, false);
	}

	public static boolean checkTableExist(Connection connection, String tableName, boolean throwExceptionOnError) throws Exception {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE 1 = 0")) {
			return true;
		} catch (Exception e) {
			if (throwExceptionOnError) {
				throw new Exception("Table '" + tableName + "' does not exist");
			} else {
				return false;
			}
		}
	}

	public static String callStoredProcedureWithDbmsOutput(Connection connection, String procedureName, Object... parameters) throws SQLException {
		try (CallableStatement callableStatement = connection.prepareCall("begin dbms_output.enable(:1); end;")) {
			callableStatement.setLong(1, 10000);
			callableStatement.executeUpdate();
		}

		if (parameters != null) {
			try (CallableStatement callableStatement = connection.prepareCall("{call " + procedureName + "(" + TextUtilities.repeatString("?", parameters.length, ", ") + ")}")) {
				for (int i = 0; i < parameters.length; i++) {
					if (parameters[i].getClass() == Date.class) {
						parameters[i] = new java.sql.Date(((Date) parameters[i]).getTime());
					}
				}
				for (int i = 0; i < parameters.length; i++) {
					callableStatement.setObject(i + 1, parameters[i]);
				}
				callableStatement.execute();
			}
		} else {
			try (CallableStatement callableStatement = connection.prepareCall("{call " + procedureName + "()}")) {
				callableStatement.execute();
			}
		}

		StringBuffer dbmsOutput;
		try (CallableStatement callableStatement = connection.prepareCall(
				"declare "
				+ "    l_line varchar2(255); "
				+ "    l_done number; "
				+ "    l_buffer long; "
				+ "begin "
				+ "  loop "
				+ "    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; "
				+ "    dbms_output.get_line( l_line, l_done ); "
				+ "    l_buffer := l_buffer || l_line || chr(10); "
				+ "  end loop; "
				+ " :done := l_done; "
				+ " :buffer := l_buffer; "
				+ "end;")) {
			callableStatement.registerOutParameter(2, Types.INTEGER);
			callableStatement.registerOutParameter(3, Types.VARCHAR);
			dbmsOutput = new StringBuffer(1024);
			while (true) {
				callableStatement.setInt(1, 32000);
				callableStatement.executeUpdate();
				dbmsOutput.append(callableStatement.getString(3).trim());
				if (callableStatement.getInt(2) == 1) {
					break;
				}
			}
		}
	
		try (CallableStatement callableStatement = connection.prepareCall("begin dbms_output.disable; end;")) {
			callableStatement.executeUpdate();
		}
		
		return dbmsOutput == null ? null : dbmsOutput.toString();
	}

	public static String getResultAsTextTable(DataSource datasource, String selectString) throws Exception {
		try (Connection connection = datasource.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(selectString);
				ResultSet resultSet = preparedStatement.executeQuery()) {
			TextTable textTable = new TextTable();
			for (int columnIndex = 1; columnIndex <= resultSet.getMetaData().getColumnCount(); columnIndex++) {
				textTable.addColumn(resultSet.getMetaData().getColumnLabel(columnIndex));
			}
			while (resultSet.next()) {
				textTable.startNewLine();
				for (int columnIndex = 1; columnIndex <= resultSet.getMetaData().getColumnCount(); columnIndex++) {
					if (resultSet.getString(columnIndex) != null) {
						textTable.addValueToCurrentLine(resultSet.getString(columnIndex));
					} else {
						textTable.addValueToCurrentLine("<null>");
					}
				}
			}
			return textTable.toString();
		}
	}
	
	public static CaseInsensitiveSet getColumnNames(DataSource dataSource, String tableName) throws Exception {
		if (dataSource == null) {
			throw new Exception("Invalid empty dataSource for getColumnNames");
		}
		
		try (Connection connection = dataSource.getConnection()) {
			return getColumnNames(connection, tableName);
		} catch (SQLException e) {
			throw new RuntimeException("Cannot read columns for table " + tableName + ": " + e.getMessage(), e);
		}
	}

	public static CaseInsensitiveSet getColumnNames(Connection connection, String tableName) throws Exception {
		if (connection == null) {
			throw new Exception("Invalid empty connection for getColumnNames");
		} else if (Utilities.isBlank(tableName)) {
			throw new Exception("Invalid empty tableName for getColumnNames");
		} else {
			try (Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery("SELECT * FROM " + getSQLSafeString(tableName) + " WHERE 1 = 0")) {
				CaseInsensitiveSet columnNamesList = new CaseInsensitiveSet();
				for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
					columnNamesList.add(resultSet.getMetaData().getColumnName(i));
				}
				return columnNamesList;
			}
		}
	}
	
	public static CaseInsensitiveMap<DbColumnType> getColumnDataTypes(DataSource dataSource, String tableName) throws Exception {
		if (dataSource == null) {
			throw new Exception("Invalid empty dataSource for getColumnDataTypes");
		} else if (Utilities.isBlank(tableName)) {
			throw new Exception("Invalid empty tableName for getColumnDataTypes");
		} else {
			try (Connection connection = dataSource.getConnection()) {
				return getColumnDataTypes(connection, tableName);
			}
		}
	}

	public static CaseInsensitiveMap<DbColumnType> getColumnDataTypes(Connection connection, String tableName) throws Exception {
		if (connection == null) {
			throw new Exception("Invalid empty connection for getColumnDataTypes");
		} else if (Utilities.isBlank(tableName)) {
			throw new Exception("Invalid empty tableName for getColumnDataTypes");
		} else {
			CaseInsensitiveMap<DbColumnType> returnMap = new CaseInsensitiveMap<DbColumnType>();
			DbVendor dbVendor = getDbVendor(connection);
			if (DbVendor.Oracle == dbVendor) {
				// Watchout: Oracle's timestamp datatype is "TIMESTAMP(6)", so remove the bracket value
				String sql = "SELECT column_name, NVL(substr(data_type, 1, instr(data_type, '(') - 1), data_type) as data_type, data_length, data_precision, data_scale, nullable FROM user_tab_columns WHERE LOWER(table_name) = LOWER(?)";
				try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
					preparedStatement.setString(1, tableName);
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						while (resultSet.next()) {
							int characterLength = resultSet.getInt("data_length");
							if (resultSet.wasNull()) {
								characterLength = -1;
							}
							int numericPrecision = resultSet.getInt("data_precision");
							if (resultSet.wasNull()) {
								numericPrecision = -1;
							}
							int numericScale = resultSet.getInt("data_scale");
							if (resultSet.wasNull()) {
								numericScale = -1;
							}
							boolean isNullable = resultSet.getString("nullable").equalsIgnoreCase("y");
	
							// TODO AutoIncrements will be introduced with Oracle 12
							returnMap.put(resultSet.getString("column_name"), new DbColumnType(resultSet.getString("data_type"), characterLength, numericPrecision, numericScale, isNullable, false));
						}
					}
				}
			} else if (DbVendor.MySQL == dbVendor) {
				String sql = "SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale, is_nullable, extra FROM information_schema.columns WHERE table_schema = schema() AND LOWER(table_name) = LOWER(?)";
				try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
					preparedStatement.setString(1, tableName);
					try (ResultSet resultSet = preparedStatement.executeQuery() ) {
						while (resultSet.next()) {
							long characterLength = resultSet.getLong("character_maximum_length");
							if (resultSet.wasNull()) {
								characterLength = -1;
							}
							int numericPrecision = resultSet.getInt("numeric_precision");
							if (resultSet.wasNull()) {
								numericPrecision = -1;
							}
							int numericScale = resultSet.getInt("numeric_scale");
							if (resultSet.wasNull()) {
								numericScale = -1;
							}
							boolean isNullable = resultSet.getString("is_nullable").equalsIgnoreCase("yes");
							boolean isAutoIncrement = resultSet.getString("extra").equalsIgnoreCase("auto_increment");
	
							returnMap.put(resultSet.getString("column_name"), new DbColumnType(resultSet.getString("data_type"), characterLength, numericPrecision, numericScale, isNullable, isAutoIncrement));
						}
					}
				}
			} else if (DbVendor.HSQL == dbVendor) {
				String sql = "SELECT column_name, type_name, column_size, decimal_digits, is_nullable, is_autoincrement FROM information_schema.system_columns WHERE LOWER(table_name) = LOWER(?)";
				try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
					preparedStatement.setString(1, tableName);
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						while (resultSet.next()) {
							long characterLength = resultSet.getLong("column_size");
							if (resultSet.wasNull()) {
								characterLength = -1;
							}
							int numericPrecision = resultSet.getInt("column_size");
							if (resultSet.wasNull()) {
								numericPrecision = -1;
							}
							int numericScale = resultSet.getInt("decimal_digits");
							if (resultSet.wasNull()) {
								numericScale = -1;
							}
							boolean isNullable = resultSet.getString("is_nullable").equalsIgnoreCase("yes");
							boolean isAutoIncrement = resultSet.getString("is_autoincrement").equalsIgnoreCase("yes");
	
							returnMap.put(resultSet.getString("column_name"), new DbColumnType(resultSet.getString("type_name"), characterLength, numericPrecision, numericScale, isNullable, isAutoIncrement));
						}
					}
				}
			} else if (DbVendor.Derby == dbVendor) {
				String sql = "SELECT columnname, columndatatype, autoincrementvalue FROM sys.systables, sys.syscolumns WHERE tableid = referenceid AND LOWER(tablename) = LOWER(?)";
				try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
					preparedStatement.setString(1, tableName);
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						while (resultSet.next()) {
							String type = resultSet.getString("columndatatype");
							
							long characterLength = -1;
							int numericPrecision = -1;
							int numericScale = -1;
							
							boolean isNullable;
							if (type.toLowerCase().endsWith("not null")) {
								isNullable = false;
								type = type.substring(0, type.length() - 8).trim();
							} else {
								isNullable = true;
							}
	
							boolean autoincrement = !Utilities.isBlank(resultSet.getString("autoincrementvalue"));
							
							if (type.contains("(")) {
								characterLength = Long.parseLong(type.substring(type.indexOf("(") + 1, type.indexOf(")")));
								type = type.substring(0, type.indexOf("("));
							}
	
							returnMap.put(resultSet.getString("columnname"), new DbColumnType(type, characterLength, numericPrecision, numericScale, isNullable, autoincrement));
						}
					}
				}
			} else if (DbVendor.Firebird == dbVendor) {
				String sql = "SELECT rf.rdb$field_name, f.rdb$field_type, f.rdb$field_sub_type, f.rdb$field_length, f.rdb$field_scale, f.rdb$null_flag"
					+ " FROM rdb$fields f JOIN rdb$relation_fields rf ON rf.rdb$field_source = f.rdb$field_name WHERE rf.rdb$relation_name = ?";
				try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
					preparedStatement.setString(1, tableName.toUpperCase());
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						while (resultSet.next()) {
							long characterLength = resultSet.getLong("rdb$field_length");
							if (resultSet.wasNull()) {
								characterLength = -1;
							}
							int numericPrecision = resultSet.getInt("rdb$field_length");
							if (resultSet.wasNull()) {
								numericPrecision = -1;
							}
							int numericScale = resultSet.getInt("rdb$field_scale");
							if (resultSet.wasNull()) {
								numericScale = -1;
							}
							boolean isNullable = resultSet.getObject("rdb$null_flag") == null;
							
							String dataType;
							switch (resultSet.getInt("rdb$field_type")) {
								case 7: dataType = "SMALLINT";
									break;
								case 8: dataType = "INTEGER";
									break;
								case 10: dataType = "FLOAT";
									break;
								case 12: dataType = "DATE";
									break;
								case 13: dataType = "TIME";
									break;
								case 14: dataType = "CHAR";
									break;
								case 16: dataType = "BIGINT";
									break;
								case 27: dataType = "DOUBLE PRECISION";
									break;
								case 35: dataType = "TIMESTAMP";
									break;
								case 37: dataType = "VARCHAR";
									break;
								case 261:
									if (resultSet.getInt("rdb$field_sub_type") == 1) {
										dataType = "CLOB";
									} else {
										dataType = "BLOB";
									}
									break;
								default: dataType = getTypeNameById(resultSet.getInt("rdb$field_type"));
							}
	
							//TODO check autoincrement
							returnMap.put(resultSet.getString("rdb$field_name").trim(), new DbColumnType(dataType, characterLength, numericPrecision, numericScale, isNullable, false));
						}
					}
				}
			} else if (DbVendor.SQLite == dbVendor) {
				boolean hasAutoIncrement = false;
				if (checkTableExist(connection, "sqlite_sequence")) {
					// sqlite_sequence only exists if there is any table with auto_increment
					try (PreparedStatement preparedStatement2 = connection.prepareStatement("SELECT COUNT(*) FROM sqlite_sequence WHERE LOWER(name) = LOWER(?)")) {
						preparedStatement2.setString(1, tableName);
						try (ResultSet resultSet2 = preparedStatement2.executeQuery()) {
							if (resultSet2.next()) {
								hasAutoIncrement = resultSet2.getInt(1) > 0;
							}
						}
					}
				}
				
				String sql = "PRAGMA table_info(" + tableName + ")";
				try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
						ResultSet resultSet = preparedStatement.executeQuery()) {
					while (resultSet.next()) {
						long characterLength = -1;
						int numericPrecision = -1;
						int numericScale = -1;
						boolean isNullable = resultSet.getInt("notnull") == 0;
						// Only the primary key can be auto incremented in SQLite
						boolean isAutoIncrement = hasAutoIncrement && resultSet.getInt("pk") > 0;
						
						String type = resultSet.getString("type");
						
						if (type.contains("(")) {
							characterLength = Long.parseLong(type.substring(type.indexOf("(") + 1, type.indexOf(")")));
							type = type.substring(0, type.indexOf("("));
						}

						returnMap.put(resultSet.getString("name"), new DbColumnType(type, characterLength, numericPrecision, numericScale, isNullable, isAutoIncrement));
					}
				}
			} else if (DbVendor.PostgreSQL == dbVendor) {
				String sql = "SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale, is_nullable, column_default FROM information_schema.columns WHERE table_schema = CURRENT_SCHEMA() AND LOWER(table_name) = LOWER(?)";
				try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
					preparedStatement.setString(1, tableName);
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						while (resultSet.next()) {
							long characterLength = resultSet.getLong("character_maximum_length");
							if (resultSet.wasNull()) {
								characterLength = -1;
							}
							int numericPrecision = resultSet.getInt("numeric_precision");
							if (resultSet.wasNull()) {
								numericPrecision = -1;
							}
							int numericScale = resultSet.getInt("numeric_scale");
							if (resultSet.wasNull()) {
								numericScale = -1;
							}
							boolean isNullable = resultSet.getString("is_nullable").equalsIgnoreCase("yes");
							
							String defaultValue = resultSet.getString("column_default");
							boolean isAutoIncrement = false;
							if (defaultValue!= null && defaultValue.toLowerCase().startsWith("nextval(")) {
								isAutoIncrement = true;
							}
	
							returnMap.put(resultSet.getString("column_name"), new DbColumnType(resultSet.getString("data_type"), characterLength, numericPrecision, numericScale, isNullable, isAutoIncrement));
						}
					}
				}
			} else {
				throw new Exception("Unsupported db vendor");
			}
			return returnMap;
		}
	}

	public static int getTableEntriesCount(Connection connection, String tableName) throws Exception {
		if (Utilities.isBlank(tableName)) {
			throw new Exception("Invalid empty tableName for getTableEntriesNumber");
		} else {
			try (Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + getSQLSafeString(tableName))) {
				if (resultSet.next()) {
					return resultSet.getInt(1);
				} else {
					return 0;
				}
			}
		}
	}

	public static boolean containsColumnName(DataSource dataSource, String tableName, String columnName) throws Exception {
		if (dataSource == null) {
			throw new Exception("Invalid empty dataSource for containsColumnName");
		} else if (Utilities.isBlank(tableName)) {
			throw new Exception("Invalid empty tableName for containsColumnName");
		} else if (Utilities.isBlank(columnName)) {
			throw new Exception("Invalid empty columnName for containsColumnName");
		} else {
			try (Connection connection = dataSource.getConnection();
					Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery("SELECT * FROM " + getSQLSafeString(tableName) + " WHERE 1 = 0")) {
				for (int columnIndex = 1; columnIndex <= resultSet.getMetaData().getColumnCount(); columnIndex++) {
					if (resultSet.getMetaData().getColumnName(columnIndex).equalsIgnoreCase(columnName.trim())) {
						return true;
					}
				}
				return false;
			}
		}
	}

	public static String getColumnDefaultValue(DataSource dataSource, String tableName, String columnName) throws Exception {
		if (dataSource == null) {
			throw new Exception("Invalid empty dataSource for getDefaultValueOf");
		} else if (Utilities.isBlank(tableName)) {
			throw new Exception("Invalid empty tableName for getDefaultValueOf");
		} else if (Utilities.isBlank(columnName)) {
			throw new Exception("Invalid empty columnName for getDefaultValueOf");
		} else {
			try (Connection connection = dataSource.getConnection()) {
				DbVendor dbVendor = getDbVendor(connection);
				if (DbVendor.Oracle == dbVendor) {
					String sql = "SELECT data_default FROM user_tab_cols WHERE table_name = ? AND column_name = ?";
					try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
						preparedStatement.setString(1, tableName.toUpperCase());
						preparedStatement.setString(2, columnName.toUpperCase());
						try (ResultSet resultSet = preparedStatement.executeQuery()) {
							if (resultSet.next()) {
								String defaultvalue = resultSet.getString(1);
								String returnValue;
								if (defaultvalue == null || "null".equalsIgnoreCase(defaultvalue)) {
									returnValue = null;
								} else if (defaultvalue.startsWith("'") && defaultvalue.endsWith("'")) {
									returnValue = defaultvalue.substring(1, defaultvalue.length() - 1);
								} else {
									returnValue = defaultvalue;
								}
								if (resultSet.next()) {
									throw new Exception("Cannot retrieve column datatype");
								} else {
									return returnValue;
								}
							} else {
								throw new Exception("Cannot retrieve column datatype");
							}
						}
					}
				} else {
					String sql = "SELECT column_default FROM information_schema.columns WHERE table_schema = (SELECT schema()) AND table_name = ? AND column_name = ?";
					try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
						preparedStatement.setString(1, tableName);
						preparedStatement.setString(2, columnName);
						try (ResultSet resultSet = preparedStatement.executeQuery()) {
							if (resultSet.next()) {
								String returnValue = resultSet.getString(1);
								if (resultSet.next()) {
									throw new Exception("Cannot retrieve column datatype");
								} else {
									return returnValue;
								}
							} else {
								throw new Exception("Cannot retrieve column datatype");
							}
						}
					}
				}
			}
		}
	}

	public static String getDateDefaultValue(DataSource dataSource, String fieldDefault) throws Exception {
		DbVendor dbVendor = getDbVendor(dataSource);
		if (fieldDefault.equalsIgnoreCase("sysdate") || fieldDefault.equalsIgnoreCase("sysdate()") || fieldDefault.equalsIgnoreCase("current_timestamp")) {
			if (dbVendor == DbVendor.Oracle) {
				return "current_timestamp";
			} else if (dbVendor == DbVendor.MySQL) {
				return "sysdate()";
			} else {
				throw new Exception("Unsupported db vendor");
			}
		} else {
			if (dbVendor == DbVendor.Oracle) {
				return "to_date('" + fieldDefault + "', 'DD.MM.YYYY')";
			} else if (dbVendor == DbVendor.MySQL) {
				return "'" + fieldDefault + "'";
			} else {
				throw new Exception("Unsupported db vendor");
			}
		}
	}

	public static boolean addColumnToDbTable(DataSource dataSource, String tablename, String fieldname, String fieldType, int length, String fieldDefault, boolean notNull) throws Exception {
		if (Utilities.isBlank(fieldname)) {
			return false;
		} else if (!tablename.equalsIgnoreCase(getSQLSafeString(tablename))) {
			return false;
		} else if (Utilities.isBlank(fieldname)) {
			return false;
		} else if (!fieldname.equalsIgnoreCase(getSQLSafeString(fieldname))) {
			return false;
		} else if (Utilities.isBlank(fieldType)) {
			return false;
		} else if (containsColumnName(dataSource, tablename, fieldname)) {
			return false;
		} else {
			fieldType = fieldType.toUpperCase().trim();

			String addColumnStatement = "ALTER TABLE " + tablename + " ADD (" + fieldname.toLowerCase() + " " + fieldType;
			if (fieldType.startsWith("VARCHAR")) {
				if (length <= 0) {
					length = 100;
				}
				addColumnStatement += "(" + length + ")";
			}

			// Default Value
			if (Utilities.isNotEmpty(fieldDefault)) {
				if (fieldType.startsWith("VARCHAR")) {
					addColumnStatement += " DEFAULT '" + fieldDefault + "'";
				} else if (fieldType.equalsIgnoreCase("DATE")) {
					addColumnStatement += " DEFAULT " + getDateDefaultValue(dataSource, fieldDefault);
				} else {
					addColumnStatement += " DEFAULT " + fieldDefault;
				}
			}

			// Maybe null
			if (notNull) {
				addColumnStatement += " NOT NULL";
			}

			addColumnStatement += ")";

			try (Connection connection = dataSource.getConnection();
					Statement statement = connection.createStatement()) {
				statement.executeUpdate(addColumnStatement);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	public static boolean alterColumnTypeInDbTable(DataSource dataSource, String tablename, String fieldname, String fieldType, int length, String fieldDefault, boolean notNull) throws Exception {
		if (Utilities.isBlank(fieldname)) {
			return false;
		} else if (!tablename.equalsIgnoreCase(getSQLSafeString(tablename))) {
			return false;
		} else if (Utilities.isBlank(fieldname)) {
			return false;
		} else if (!fieldname.equalsIgnoreCase(getSQLSafeString(fieldname))) {
			return false;
		} else if (Utilities.isBlank(fieldType)) {
			return false;
		} else if (!containsColumnName(dataSource, tablename, fieldname)) {
			return false;
		} else {
			fieldType = fieldType.toUpperCase().trim();

			String changeColumnStatementPart = fieldname.toLowerCase() + " " + fieldType;
			if (fieldType.startsWith("VARCHAR")) {
				if (length <= 0) {
					length = 100;
				}
				changeColumnStatementPart += "(" + length + ")";
			}

			// Default Value
			if (Utilities.isNotEmpty(fieldDefault)) {
				if (fieldType.equalsIgnoreCase("VARCHAR")) {
					changeColumnStatementPart += " DEFAULT '" + fieldDefault + "'";
				} else if (fieldType.equalsIgnoreCase("DATE")) {
					changeColumnStatementPart += " DEFAULT " + getDateDefaultValue(dataSource, fieldDefault);
				} else {
					changeColumnStatementPart += " DEFAULT " + fieldDefault;
				}
			}

			// Maybe null
			if (notNull) {
				changeColumnStatementPart += " NOT NULL";
			}

			String changeColumnStatement;
			DbVendor dbVendor = getDbVendor(dataSource);
			if (DbVendor.Oracle == dbVendor) {
				changeColumnStatement = "ALTER TABLE " + tablename + " MODIFY (" + changeColumnStatementPart + ")";
			} else if (DbVendor.MySQL == dbVendor) {
				changeColumnStatement = "ALTER TABLE " + tablename + " MODIFY " + changeColumnStatementPart;
			} else {
				throw new Exception("Unsupported db vendor");
			}

			try (Connection connection = dataSource.getConnection();
					Statement statement = connection.createStatement()) {
				statement.executeUpdate(changeColumnStatement);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	private static String getSQLSafeString(String value) {
		if (value == null) {
			return null;
		} else {
			return value.replace("'", "''");
		}
	}

	public static boolean checkOracleTablespaceExists(DataSource dataSource, String tablespaceName) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			return checkOracleTablespaceExists(connection, tablespaceName);
		} catch (SQLException e) {
			throw new Exception("Cannot check db tablespace " + tablespaceName + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Check if oracle tablespace exists
	 *
	 * @param connection
	 * @param tablespaceName
	 * @return
	 * @throws Exception 
	 */
	public static boolean checkOracleTablespaceExists(Connection connection, String tablespaceName) throws Exception {
		DbVendor dbVendor = getDbVendor(connection);
		if (dbVendor == DbVendor.Oracle && tablespaceName != null) {
			try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM dba_tablespaces WHERE LOWER(tablespace_name) = ?")) {
				statement.setString(1, tablespaceName.toLowerCase());
				try (ResultSet resultSet = statement.executeQuery()) {
					return resultSet.getInt(1) > 0;
				}
			} catch (Exception e) {
				throw new RuntimeException("Cannot check db tablespace " + tablespaceName + ": " + e.getMessage(), e);
			}
		} else {
			return false;
		}
	}

	public static CaseInsensitiveSet getPrimaryKeyColumns(DataSource dataSource, String tableName) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			return getPrimaryKeyColumns(connection, tableName);
		} catch (SQLException e) {
			throw new Exception("Cannot read primarykey columns for table " + tableName + ": " + e.getMessage(), e);
		}
	}

	public static CaseInsensitiveSet getPrimaryKeyColumns(Connection connection, String tableName) {
		if (Utilities.isBlank(tableName)) {
			return null;
		} else {
			try {
				if (getDbVendor(connection) == DbVendor.Oracle || getDbVendor(connection) == DbVendor.HSQL || getDbVendor(connection) == DbVendor.Derby) {
					tableName = tableName.toUpperCase();
				}
				
				DatabaseMetaData metaData = connection.getMetaData();
				try (ResultSet resultSet = metaData.getPrimaryKeys(connection.getCatalog(), null, tableName)) {
					CaseInsensitiveSet returnList = new CaseInsensitiveSet();
					while (resultSet.next()) {
						returnList.add(resultSet.getString("COLUMN_NAME"));
					}
					return returnList;
				}
			} catch (Exception e) {
				throw new RuntimeException("Cannot read primarykey columns for table " + tableName + ": " + e.getMessage(), e);
			}
		}
	}

	public static List<List<String>> getForeignKeys(Connection connection, String tableName) {
		if (Utilities.isBlank(tableName)) {
			return null;
		} else {
			try {
				if (getDbVendor(connection) == DbVendor.Oracle || getDbVendor(connection) == DbVendor.HSQL || getDbVendor(connection) == DbVendor.Derby) {
					tableName = tableName.toUpperCase();
				}
				
				DatabaseMetaData metaData = connection.getMetaData();
				try (ResultSet resultSet = metaData.getImportedKeys(connection.getCatalog(), null, tableName)) {
					List<List<String>> returnList = new ArrayList<List<String>>();
					while (resultSet.next()) {
						List<String> nextForeignKey = new ArrayList<String>();
						nextForeignKey.add(resultSet.getString("FKTABLE_NAME"));
						nextForeignKey.add(resultSet.getString("FKCOLUMN_NAME"));
						nextForeignKey.add(resultSet.getString("PKTABLE_NAME"));
						nextForeignKey.add(resultSet.getString("PKCOLUMN_NAME"));
						returnList.add(nextForeignKey);
					}
					return returnList;
				}
			} catch (Exception e) {
				throw new RuntimeException("Cannot read primarykey columns for table " + tableName + ": " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * tablePatternExpression contains a comma-separated list of tablenames with wildcards *? and !(not, before tablename)
	 * 
	 * @param connection
	 * @param tablePatternExpression
	 * @return
	 * @throws Exception
	 */
	public static List<String> getAvailableTables(Connection connection, String tablePatternExpression) throws Exception {
		try (Statement statement = connection.createStatement()) {
			DbVendor dbVendor = getDbVendor(connection);

			String tableQuery;
			if (DbVendor.Oracle == dbVendor) {
				tableQuery = "SELECT DISTINCT table_name FROM all_tables WHERE owner NOT IN ('CTXSYS', 'DBSNMP', 'MDDATA', 'MDSYS', 'DMSYS', 'OLAPSYS', 'ORDPLUGINS', 'OUTLN', 'SI_INFORMATN_SCHEMA', 'SYS', 'SYSMAN', 'SYSTEM')";
				for (String tablePattern : tablePatternExpression.split(",| |;|\\||\n")) {
					if (Utilities.isNotBlank(tablePattern)) {
						tablePattern = tablePattern.trim().toUpperCase().replace("%", "\\%").replace("_", "\\_").replace("*", "%").replace("?", "_");
						if (tablePattern.startsWith("!")) {
							tableQuery += " AND table_name NOT LIKE '" + tablePattern.substring(1) + "' ESCAPE '\\'";
						} else {
							tableQuery += " AND table_name LIKE '" + tablePattern + "' ESCAPE '\\'";
						}
					}
				}
				tableQuery += " ORDER BY table_name";

				try (ResultSet resultSet = statement.executeQuery(tableQuery)) {
					List<String> tableNamesToExport = new ArrayList<String>();
					while (resultSet.next()) {
						tableNamesToExport.add(resultSet.getString("table_name"));
					}
					return tableNamesToExport;
				}
			} else if (DbVendor.MySQL == dbVendor) {
				tableQuery = "SELECT DISTINCT table_name FROM information_schema.tables WHERE table_schema NOT IN ('information_schema')";
				for (String tablePattern : tablePatternExpression.split(",| |;|\\||\n")) {
					if (Utilities.isNotBlank(tablePattern)) {
						tablePattern = tablePattern.trim().replace("%", "\\%").replace("_", "\\_").replace("*", "%").replace("?", "_");
						if (tablePattern.startsWith("!")) {
							tableQuery += " AND table_name NOT LIKE '" + tablePattern.substring(1) + "'";
						} else {
							tableQuery += " AND table_name LIKE '" + tablePattern + "'";
						}
					}
				}
				tableQuery += " ORDER BY table_name";

				try (ResultSet resultSet = statement.executeQuery(tableQuery)) {
					List<String> tableNamesToExport = new ArrayList<String>();
					while (resultSet.next()) {
						tableNamesToExport.add(resultSet.getString("table_name"));
					}
					return tableNamesToExport;
				}
			} else if (DbVendor.PostgreSQL == dbVendor) {
				tableQuery = "SELECT DISTINCT table_name FROM information_schema.tables WHERE table_schema NOT IN ('information_schema', 'pg_catalog')";
				for (String tablePattern : tablePatternExpression.split(",| |;|\\||\n")) {
					if (Utilities.isNotBlank(tablePattern)) {
						tablePattern = tablePattern.trim().replace("%", "\\%").replace("_", "\\_").replace("*", "%").replace("?", "_");
						if (tablePattern.startsWith("!")) {
							tableQuery += " AND table_name NOT LIKE '" + tablePattern.substring(1) + "' ESCAPE '\\'";
						} else {
							tableQuery += " AND table_name LIKE '" + tablePattern + "' ESCAPE '\\'";
						}
					}
				}
				tableQuery += " ORDER BY table_name";

				try (ResultSet resultSet = statement.executeQuery(tableQuery)) {
					List<String> tableNamesToExport = new ArrayList<String>();
					while (resultSet.next()) {
						tableNamesToExport.add(resultSet.getString("table_name"));
					}
					return tableNamesToExport;
				}
			} else if (DbVendor.SQLite == dbVendor) {
				tableQuery = "SELECT name FROM sqlite_master WHERE type = 'table'";
				for (String tablePattern : tablePatternExpression.split(",| |;|\\||\n")) {
					if (Utilities.isNotBlank(tablePattern)) {
						tablePattern = tablePattern.trim().toUpperCase().replace("%", "\\%").replace("_", "\\_").replace("*", "%").replace("?", "_");
						if (tablePattern.startsWith("!")) {
							tableQuery += " AND name NOT LIKE '" + tablePattern.substring(1) + "' ESCAPE '\\'";
						} else {
							tableQuery += " AND name LIKE '" + tablePattern + "' ESCAPE '\\'";
						}
					}
				}
				tableQuery += " ORDER BY name";
				
				try (ResultSet resultSet = statement.executeQuery(tableQuery)) {
					List<String> tableNamesToExport = new ArrayList<String>();
					while (resultSet.next()) {
						tableNamesToExport.add(resultSet.getString("name"));
					}
					return tableNamesToExport;
				}
			} else if (DbVendor.Derby == dbVendor) {
				tableQuery = "SELECT tablename FROM sys.systables WHERE tabletype = 'T'";
				for (String tablePattern : tablePatternExpression.split(",| |;|\\||\n")) {
					if (Utilities.isNotBlank(tablePattern)) {
						tablePattern = tablePattern.trim().toUpperCase().replace("%", "\\%").replace("_", "\\_").replace("*", "%").replace("?", "_");
						if (tablePattern.startsWith("!")) {
							tableQuery += " AND tablename NOT LIKE '" + tablePattern.substring(1) + "' {ESCAPE '\\'}";
						} else {
							tableQuery += " AND tablename LIKE '" + tablePattern + "' {ESCAPE '\\'}";
						}
					}
				}
				tableQuery += " ORDER BY tablename";
				
				try (ResultSet resultSet = statement.executeQuery(tableQuery)) {
					List<String> tableNamesToExport = new ArrayList<String>();
					while (resultSet.next()) {
						tableNamesToExport.add(resultSet.getString("tablename"));
					}
					return tableNamesToExport;
				}
			} else if (DbVendor.Firebird == dbVendor) {
				tableQuery = "SELECT TRIM(rdb$relation_name) AS table_name FROM rdb$relations WHERE rdb$view_blr IS NULL AND (rdb$system_flag IS NULL OR rdb$system_flag = 0)";
				for (String tablePattern : tablePatternExpression.split(",| |;|\\||\n")) {
					if (Utilities.isNotBlank(tablePattern)) {
						tablePattern = tablePattern.trim().toUpperCase().replace("%", "\\%").replace("_", "\\_").replace("*", "%").replace("?", "_");
						if (tablePattern.startsWith("!")) {
							tableQuery += " AND TRIM(rdb$relation_name) NOT LIKE '" + tablePattern.substring(1) + "' ESCAPE '\\'";
						} else {
							tableQuery += " AND TRIM(rdb$relation_name) LIKE '" + tablePattern + "' ESCAPE '\\'";
						}
					}
				}
				tableQuery += " ORDER BY rdb$relation_name";
				
				try (ResultSet resultSet = statement.executeQuery(tableQuery)) {
					List<String> tableNamesToExport = new ArrayList<String>();
					while (resultSet.next()) {
						tableNamesToExport.add(resultSet.getString("table_name"));
					}
					return tableNamesToExport;
				}
			} else if (DbVendor.HSQL == dbVendor) {
				tableQuery = "SELECT table_name FROM information_schema.system_tables WHERE table_type = 'TABLE' AND table_schem = 'PUBLIC'";
				for (String tablePattern : tablePatternExpression.split(",| |;|\\||\n")) {
					if (Utilities.isNotBlank(tablePattern)) {
						tablePattern = tablePattern.trim().toUpperCase().replace("%", "\\%").replace("_", "\\_").replace("*", "%").replace("?", "_");
						if (tablePattern.startsWith("!")) {
							tableQuery += " AND table_name NOT LIKE '" + tablePattern.substring(1) + "' ESCAPE '\\'";
						} else {
							tableQuery += " AND table_name LIKE '" + tablePattern + "' ESCAPE '\\'";
						}
					}
				}
				tableQuery += " ORDER BY table_name";
				
				try (ResultSet resultSet = statement.executeQuery(tableQuery)) {
					List<String> tableNamesToExport = new ArrayList<String>();
					while (resultSet.next()) {
						tableNamesToExport.add(resultSet.getString("table_name"));
					}
					return tableNamesToExport;
				}
			} else {
				throw new Exception("Unknown db vendor");
			}
		}
	}
	
	private static List<File> getFilesByPattern(File startDirectory, String patternString, boolean traverseCompletely) {
		return getFilesByPattern(startDirectory, Pattern.compile(patternString), traverseCompletely);
	}

	private static List<File> getFilesByPattern(File startDirectory, Pattern pattern, boolean traverseCompletely) {
		List<File> files = new ArrayList<File>();
		if (startDirectory.isDirectory()) {
			for (File file : startDirectory.listFiles()) {
				if (file.isDirectory()) {
					if (traverseCompletely) {
						files.addAll(getFilesByPattern(file, pattern, traverseCompletely));
					}
				} else if (file.isFile() && pattern.matcher(file.getName()).matches()) {
					files.add(file);
				}
			}
		}
		return files;
	}

	public static void createTable(Connection connection, String tablename, Map<String, DbColumnType> columnsAndTypes, Collection<String> keyColumns) throws Exception {
		if  (keyColumns != null) {
			for (String keyColumn : keyColumns) {
				if (!columnsAndTypes.containsKey(keyColumn)) {
					throw new Exception("Cannot create table. Keycolumn '" + keyColumn + "' is not included in column types");
				}
			}
		}
		
		try (Statement statement = connection.createStatement()) {
			DbVendor dbVendor = getDbVendor(connection);
			
			String columnPart = "";
			for (Entry<String, DbColumnType> columnAndType : columnsAndTypes.entrySet()) {
				if (columnPart.length() > 0) {
					columnPart += ", ";
				}
				DbColumnType columnType = columnAndType.getValue();
				if (columnType != null) {
					String dataType = getDataType(dbVendor, columnType.getSimpleDataType());
					int dataLength = dataType.toLowerCase().contains("varchar") ? (int) columnType.getCharacterLength() : 0;
					columnPart += columnAndType.getKey() + " " + dataType + (dataLength > 0 ? "(" + dataLength + ")" : "");
				} else {
					String dataType = getDataType(dbVendor, SimpleDataType.String);
					columnPart += columnAndType.getKey() + " " + dataType + "(1)";
				}
			}
			
			String primaryKeyPart = "";
			if (Utilities.isNotEmpty(keyColumns)) {
				primaryKeyPart = ", PRIMARY KEY (" + Utilities.join(keyColumns, ", ") + ")";
			}
			statement.execute("CREATE TABLE " + tablename + " (" + columnPart + primaryKeyPart + ")");
			if (getDbVendor(connection) == DbVendor.Derby) {
				connection.commit();
			}
		}
	}

	private static String getDataType(DbVendor dbVendor, SimpleDataType simpleDataType) throws Exception {
		if (dbVendor == DbVendor.Oracle) {
			switch (simpleDataType) {
				case Blob: return "BLOB";
				case Clob: return "CLOB";
				case Date: return "TIMESTAMP";
				case Integer: return "NUMBER";
				case Double: return "NUMBER";
				case String: return "VARCHAR2";
				default: return "VARCHAR2";
			}
		} else if (dbVendor == DbVendor.MySQL) {
			switch (simpleDataType) {
				case Blob: return "LONGBLOB";
				case Clob: return "LONGTEXT";
				case Date: return "TIMESTAMP NULL";
				case Integer: return "INT";
				case Double: return "DOUBLE";
				case String: return "VARCHAR";
				default: return "VARCHAR";
			}
		} else if (dbVendor == DbVendor.HSQL) {
			switch (simpleDataType) {
				case Blob: return "BLOB";
				case Clob: return "CLOB";
				case Date: return "TIMESTAMP";
				case Integer: return "INTEGER";
				case Double: return "DOUBLE";
				case String: return "VARCHAR";
				default: return "VARCHAR";
			}
		} else if (dbVendor == DbVendor.PostgreSQL) {
			switch (simpleDataType) {
				case Blob: return "BYTEA";
				case Clob: return "TEXT";
				case Date: return "TIMESTAMP";
				case Integer: return "INTEGER";
				case Double: return "REAL";
				case String: return "VARCHAR";
				default: return "VARCHAR";
			}
		} else if (dbVendor == DbVendor.SQLite) {
			switch (simpleDataType) {
				case Blob: return "BLOB";
				case Clob: return "CLOB";
				case Date: return "TIMESTAMP";
				case Integer: return "INTEGER";
				case Double: return "DOUBLE";
				case String: return "VARCHAR";
				default: return "VARCHAR";
			}
		} else if (dbVendor == DbVendor.Derby) {
			switch (simpleDataType) {
				case Blob: return "BLOB";
				case Clob: return "CLOB";
				case Date: return "TIMESTAMP";
				case Integer: return "INTEGER";
				case Double: return "DOUBLE";
				case String: return "VARCHAR";
				default: return "VARCHAR";
			}
		} else if (dbVendor == DbVendor.Firebird) {
			switch (simpleDataType) {
				case Blob: return "BLOB SUB_TYPE BINARY";
				case Clob: return "BLOB SUB_TYPE TEXT";
				case Date: return "TIMESTAMP";
				case Integer: return "INTEGER";
				case Double: return "DOUBLE PRECISION";
				case String: return "VARCHAR";
				default: return "VARCHAR";
			}
		} else {
			throw new Exception("Cannot get datatype: " + dbVendor + "/" + simpleDataType);
		}
	}
	
	public static String getTypeNameById(int typeId) throws Exception {
		switch (typeId) {
			case Types.BIT: return "BIT";
			case Types.TINYINT: return "TINYINT";
			case Types.SMALLINT: return "SMALLINT";
			case Types.INTEGER: return "INTEGER";
			case Types.BIGINT: return "BIGINT";
			case Types.FLOAT: return "FLOAT";
			case Types.REAL: return "REAL";
			case Types.DOUBLE: return "DOUBLE";
			case Types.NUMERIC: return "NUMERIC";
			case Types.DECIMAL: return "DECIMAL";
			case Types.CHAR: return "CHAR";
			case Types.VARCHAR: return "VARCHAR";
			case Types.LONGVARCHAR: return "LONGVARCHAR";
			case Types.DATE: return "DATE";
			case Types.TIME: return "TIME";
			case Types.TIMESTAMP: return "TIMESTAMP";
			case Types.BINARY: return "BINARY";
			case Types.VARBINARY: return "VARBINARY";
			case Types.LONGVARBINARY: return "LONGVARBINARY";
			case Types.NULL: return "NULL";
			case Types.OTHER: return "OTHER";
			case Types.JAVA_OBJECT: return "JAVA_OBJECT";
			case Types.DISTINCT: return "DISTINCT";
			case Types.STRUCT: return "STRUCT";
			case Types.ARRAY: return "ARRAY";
			case Types.BLOB: return "BLOB";
			case Types.CLOB: return "CLOB";
			case Types.REF: return "REF";
			case Types.DATALINK: return "DATALINK";
			case Types.BOOLEAN: return "BOOLEAN";
			case Types.ROWID: return "ROWID";
			case Types.NCHAR: return "NCHAR";
			case Types.NVARCHAR: return "NVARCHAR";
			case Types.LONGNVARCHAR: return "LONGNVARCHAR";
			case Types.NCLOB: return "NCLOB";
			case Types.SQLXML: return "SQLXML";
			case Types.REF_CURSOR: return "REF_CURSOR";
			case Types.TIME_WITH_TIMEZONE: return "TIME_WITH_TIMEZONE";
			case Types.TIMESTAMP_WITH_TIMEZONE: return "TIMESTAMP_WITH_TIMEZONE";
			default: throw new Exception("Unknown type id: " + typeId);
		}
	}

	public static String getDownloadUrl(DbVendor dbVendor) throws Exception {
		if (dbVendor == DbVendor.MySQL) {
			return DOWNLOAD_LOCATION_MYSQL;
		} else if (dbVendor == DbVendor.Oracle) {
			return DOWNLOAD_LOCATION_ORACLE;
		} else if (dbVendor == DbVendor.PostgreSQL) {
			return DOWNLOAD_LOCATION_POSTGRESQL;
		} else if (dbVendor == DbVendor.Firebird) {
			return DOWNLOAD_LOCATION_FIREBIRD;
		} else if (dbVendor == DbVendor.Derby) {
			return DOWNLOAD_LOCATION_DERBY;
		} else if (dbVendor == DbVendor.SQLite) {
			return DOWNLOAD_LOCATION_SQLITE;
		} else if (dbVendor == DbVendor.HSQL) {
			return DOWNLOAD_LOCATION_HSQL;
		} else {
			throw new Exception("Invalid db vendor");
		}
	}

	/**
	 * Update the duplicateIndexColumn column of all entries of a table with the minimum index value from itemIndexColumn of other duplicates.
	 * 
	 * @param connection
	 * @param tableName
	 * @param keyColumns
	 * @param keyColumnsWithFunctions
	 * @param itemIndexColumn
	 * @param duplicateIndexColumn
	 * @throws Exception
	 */
	public static void markDuplicates(Connection connection, String tableName, Collection<String> keyColumnsWithFunctions, String itemIndexColumn, String duplicateIndexColumn) throws Exception {
		if (Utilities.isNotEmpty(keyColumnsWithFunctions)) {
			StringBuilder selectPart = new StringBuilder();
			StringBuilder wherePart = new StringBuilder();
			int columnIndex = 0;
			for (String columnWithFunction : keyColumnsWithFunctions) {
				columnWithFunction = columnWithFunction.trim();
				
				if (selectPart.length() > 0) {
					selectPart.append(", ");
					wherePart.append(", ");
				}
				
				selectPart.append(columnWithFunction + " AS col" + columnIndex);
				
				wherePart.append("subselect.col" + columnIndex);
				
				wherePart.append(" = ");

				if (columnWithFunction.contains("(")) {
					wherePart.append(columnWithFunction.replace("(", "(" + tableName + "."));
				} else {
					wherePart.append(tableName + "." + columnWithFunction);
				}
				
				columnIndex++;
			}
			
			try (Statement statement = connection.createStatement()) {
				// The indirection with a subselect is needed for MySQL (You can't specify target table for update in FROM clause)
				String setDuplicateReferences = "UPDATE " + tableName + " SET " + duplicateIndexColumn + " = (SELECT subselect." + itemIndexColumn + " FROM"
					+ " (SELECT " + selectPart.toString() + ", MIN(" + itemIndexColumn + ") AS " + itemIndexColumn + " FROM " + tableName + " GROUP BY " + Utilities.join(keyColumnsWithFunctions, ", ") + ") subselect"
					+ " WHERE " + wherePart.toString() + ")";
				statement.executeUpdate(setDuplicateReferences);
				connection.commit();
			} catch (Exception e) {
				connection.rollback();
				throw new Exception("Cannot markTrailingDuplicates: " + e.getMessage(), e);
			}
		} else {
			throw new Exception("Cannot markDuplicates: Missing keycolumns");
		}
	}
	
	public static String getKeyColumnEquationList(Collection<String> columnNamesWithFunctions, String tableAlias1, String tableAlias2) {
		StringBuilder returnValue = new StringBuilder();
		for (String columnName : columnNamesWithFunctions) {
			columnName = columnName.trim();
			
			if (returnValue.length() > 0) {
				returnValue.append(", ");
			}
			
			if (Utilities.isNotBlank(tableAlias1)) {
				if (columnName.contains("(")) {
					returnValue.append(columnName.replace("(", "(" + tableAlias1 + "."));
				} else {
					returnValue.append(tableAlias1 + "." + columnName);
				}
			} else {
				returnValue.append(columnName);
			}
			
			returnValue.append(" = ");

			if (Utilities.isNotBlank(tableAlias2)) {
				if (columnName.contains("(")) {
					returnValue.append(columnName.replace("(", "(" + tableAlias2 + "."));
				} else {
					returnValue.append(tableAlias2 + "." + columnName);
				}
			} else {
				returnValue.append(columnName);
			}
		}
		return returnValue.toString();
	}

	public static int detectDuplicates(Connection connection, String tableName, Collection<String> keyColumnsWithFunctions) throws Exception {
		try (Statement statement = connection.createStatement()) {
			String countDuplicatesStatement = "SELECT COUNT(*) FROM (SELECT COUNT(*) FROM " + tableName + " GROUP BY " + Utilities.join(keyColumnsWithFunctions, ", ") + " HAVING COUNT(*) > 1) subsel";
			try (ResultSet resultSet = statement.executeQuery(countDuplicatesStatement)) {
				if (resultSet.next()) {
					return resultSet.getInt(1);
				} else {
					return 0;
				}
			}
		} catch (Exception e) {
			throw new Exception("Cannot detectDuplicates: " + e.getMessage(), e);
		}
	}

	public static int detectDuplicatesCrossTables(Connection connection, String detectTableName, String fromTableName, List<String> keyColumnsWithFunctions) throws Exception {
		try (Statement statement = connection.createStatement()) {
			String selectDuplicatesNumber = "SELECT COUNT(*) FROM " + detectTableName + " a WHERE EXISTS (SELECT 1 FROM " + fromTableName + " b WHERE " + getKeyColumnEquationList(keyColumnsWithFunctions, "a", "b") + ")";
			try (ResultSet resultSet = statement.executeQuery(selectDuplicatesNumber)) {
				resultSet.next();
				return resultSet.getInt(1);
			}
		} catch (Exception e) {
			throw new Exception("Cannot detectDuplicatesCrossTables: " + e.getMessage(), e);
		}
	}

	public static String addIndexedIntegerColumn(Connection connection, String tableName, String columnBaseName) throws Exception {
		String columnName = columnBaseName;
		int i = 0;
		while (checkTableAndColumnsExist(connection, tableName, columnName) && i < 10) {
			i++;
			columnName = columnBaseName + "_" + i;
		}
		if (i >= 10) {
			throw new Exception("Cannot create columnBaseName " + columnBaseName + " in table " + tableName);
		}
		
		try (Statement statement = connection.createStatement()) {
			statement.execute("ALTER TABLE " + tableName + " ADD " + columnName + " INTEGER");
			String dateSuffix = DateUtilities.YYYYMMDDHHMMSSSSS.format(new Date());
			try {
				statement.execute("CREATE INDEX tmp" + dateSuffix + "_idx ON " + tableName + " (" + columnName + ")");
			} catch (Exception e) {
				e.printStackTrace();
				// Work without index. Maybe it already exists or it is a not allowed function based index
			}
		}
		
		return columnName;
	}

	public static int dropDuplicatesCrossTable(Connection connection, String keepInTableName, String deleteInTableName, List<String> keyColumnsWithFunctions) throws Exception {
		if (Utilities.isEmpty(keyColumnsWithFunctions)) {
			try (Statement statement = connection.createStatement()) {
				String deleteDuplicates = "DELETE FROM " + deleteInTableName + " WHERE " + Utilities.join(keyColumnsWithFunctions,  ", ") + " IN (SELECT " + Utilities.join(keyColumnsWithFunctions,  ", ") + " FROM " + keepInTableName + ")";
				int numberOfDeletedDuplicates = statement.executeUpdate(deleteDuplicates);
				connection.commit();
				return numberOfDeletedDuplicates;
			} catch (Exception e) {
				connection.rollback();
				throw new Exception("Cannot deleteTableCrossDuplicates: " + e.getMessage(), e);
			}
		} else {
			return 0;
		}
	}

	public static int dropDuplicates(Connection connection, String tableName, Collection<String> keyColumns) throws Exception {
		if (detectDuplicates(connection, tableName, keyColumns) > 0) {
		String originalItemIndexColumn = null;
			String originalDuplicateIndexColumn = null;
			try (Statement statement = connection.createStatement()) {
				originalItemIndexColumn = createLineNumberIndexColumn(connection, tableName, "drop_idx");
				originalDuplicateIndexColumn = addIndexedIntegerColumn(connection, tableName, "drop_dpl");
				
				// Try to create an additional index columns
				if (Utilities.isNotEmpty(keyColumns)) {
					try {
						String dateSuffix = DateUtilities.YYYYMMDDHHMMSS.format(new Date());
						statement.execute("CREATE INDEX tmp" + dateSuffix + "_idx ON " + tableName + " (" + Utilities.join(keyColumns, ", ") + ")");
					} catch (Exception e) {
						// Work without index. Maybe it already exists or it is a not allowed function based index
					}
				}
				
				markDuplicates(connection, tableName, keyColumns, originalItemIndexColumn, originalDuplicateIndexColumn);
				
				int numberOfDeletedDuplicates = statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + originalItemIndexColumn + " != " + originalDuplicateIndexColumn);
				connection.commit();
				return numberOfDeletedDuplicates;
			} catch (Exception e) {
				connection.rollback();
				throw new Exception("Cannot dropDuplicates: " + e.getMessage(), e);
			} finally {
				dropColumnIfExists(connection, tableName, originalItemIndexColumn);
				dropColumnIfExists(connection, tableName, originalDuplicateIndexColumn);
			}
		} else {
			return 0;
		}
	}
	
	public static int joinDuplicates(Connection connection, String tableName, Collection<String> keyColumnsWithFunctions, boolean updateWithNullValues) throws Exception {
		if (detectDuplicates(connection, tableName, keyColumnsWithFunctions) > 0) {
			DbVendor dbVendor = getDbVendor(connection);
			String dateSuffix = DateUtilities.YYYYMMDDHHMMSS.format(new Date());
			String interimTableName = "tmp_join_" + dateSuffix;
			String originalItemIndexColumn = null;
			String originalDuplicateIndexColumn = null;
			
			// Join all duplicates in destination table
			try (Statement statement = connection.createStatement()) {
				// Create additional index columns
				if (Utilities.isNotEmpty(keyColumnsWithFunctions)) {
					try {
						statement.execute("CREATE INDEX tmp" + dateSuffix + "_idx ON " + tableName + " (" + Utilities.join(keyColumnsWithFunctions, ", ") + ")");
						connection.commit();
					} catch (Exception e) {
						// Work without index. Maybe it already exists or it is a not allowed function based index
					}
				}
				
				originalItemIndexColumn = createLineNumberIndexColumn(connection, tableName, "join_idx");
				originalDuplicateIndexColumn = addIndexedIntegerColumn(connection, tableName, "join_dpl");
				
				markDuplicates(connection, tableName, keyColumnsWithFunctions, originalItemIndexColumn, originalDuplicateIndexColumn);
				connection.commit();
				
				// Create temp table
				if (dbVendor == DbVendor.HSQL || dbVendor == DbVendor.Derby) {
					statement.execute("CREATE TABLE " + interimTableName + " AS (SELECT * FROM " + tableName + ") WITH NO DATA");
					statement.executeUpdate("INSERT INTO " + interimTableName + " (SELECT * FROM " + tableName + " WHERE " + originalItemIndexColumn + " != " + originalDuplicateIndexColumn + ")");
				} else if (dbVendor == DbVendor.PostgreSQL) {
					// Close a maybe open transaction to allow DDL-statement
					connection.rollback();
					statement.execute("CREATE TABLE " + interimTableName + " AS SELECT * FROM " + tableName + " WHERE " + originalItemIndexColumn + " != " + originalDuplicateIndexColumn);
				} else if (dbVendor == DbVendor.Firebird) {
					// There is no "create table as select"-statmenet in firebird
					createTable(connection, interimTableName, getColumnDataTypes(connection, tableName), null);
				} else {
					statement.execute("CREATE TABLE " + interimTableName + " AS SELECT * FROM " + tableName + " WHERE " + originalItemIndexColumn + " != " + originalDuplicateIndexColumn);
				}
				connection.commit();
				
				int deletedDuplicatesInDB = dropDuplicates(connection, tableName, keyColumnsWithFunctions);

				List<String> columnsWithoutAutoIncrement = new ArrayList<String>();
				for (Entry<String, DbColumnType> column : getColumnDataTypes(connection, tableName).entrySet()) {
					if (!column.getValue().isAutoIncrement()) {
						columnsWithoutAutoIncrement.add(column.getKey());
					}
				}
				
				updateAllExistingItems(connection, interimTableName, tableName, columnsWithoutAutoIncrement, keyColumnsWithFunctions, originalItemIndexColumn, updateWithNullValues, null);
				connection.commit();
				return deletedDuplicatesInDB;
			} catch (Exception e) {
				throw new Exception("Cannot joinDuplicates: " + e.getMessage(), e);
			} finally {
				dropTableIfExists(connection, interimTableName);
				dropColumnIfExists(connection, tableName, originalItemIndexColumn);
				dropColumnIfExists(connection, tableName, originalDuplicateIndexColumn);
			}
		} else {
			return 0;
		}
	}

	private static String createLineNumberIndexColumn(Connection connection, String tableName, String indexColumnNameBaseName) throws Exception {
		DbVendor dbVendor = getDbVendor(connection);
		String indexColumnName = addIndexedIntegerColumn(connection, tableName, indexColumnNameBaseName);

		try (Statement statement = connection.createStatement()) {
			if (dbVendor == DbVendor.MySQL) {
				statement.execute("SELECT @n := 0");
				statement.execute("UPDATE " + tableName + " SET " + indexColumnName + " = @n := @n + 1");
			} else if (dbVendor == DbVendor.Oracle) {
				statement.executeUpdate("UPDATE " + tableName + " SET " + indexColumnName + " = ROWNUM");
			} else if (dbVendor == DbVendor.SQLite) {
				statement.executeUpdate("UPDATE " + tableName + " SET " + indexColumnName + " = ROWID");
			} else if (dbVendor == DbVendor.HSQL) {
				statement.executeUpdate("UPDATE " + tableName + " SET " + indexColumnName + " = ROWNUM()");
			} else if (dbVendor == DbVendor.Derby) {
				String autoIncrementColumn = null;
				List<String> columnsWithoutAutoIncrement = new ArrayList<String>();
				for (Entry<String, DbColumnType> column : getColumnDataTypes(connection, tableName).entrySet()) {
					if (!column.getValue().isAutoIncrement()) {
						columnsWithoutAutoIncrement.add(column.getKey());
					} else {
						autoIncrementColumn = column.getKey();
					}
				}
				columnsWithoutAutoIncrement.remove(indexColumnName);
				if (autoIncrementColumn != null) {
					statement.executeUpdate("UPDATE " + tableName + " SET " + indexColumnName + " = " + autoIncrementColumn);
				} else {
					statement.executeUpdate("INSERT INTO " + tableName + " (" + Utilities.join(columnsWithoutAutoIncrement, ", ") + ", " + indexColumnName + ") (SELECT " + Utilities.join(columnsWithoutAutoIncrement, ", ") + ", ROW_NUMBER() OVER() FROM " + tableName + ")");
					statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + indexColumnName + " IS NULL");
				}
			} else if (dbVendor == DbVendor.PostgreSQL) {
				String autoIncrementColumn = null;
				List<String> columnsWithoutAutoIncrement = new ArrayList<String>();
				for (Entry<String, DbColumnType> column : getColumnDataTypes(connection, tableName).entrySet()) {
					if (!column.getValue().isAutoIncrement()) {
						columnsWithoutAutoIncrement.add(column.getKey());
					} else {
						autoIncrementColumn = column.getKey();
					}
				}
				columnsWithoutAutoIncrement.remove(indexColumnName);
				if (autoIncrementColumn != null) {
					statement.executeUpdate("UPDATE " + tableName + " SET " + indexColumnName + " = " + autoIncrementColumn);
				} else {
					statement.executeUpdate("INSERT INTO " + tableName + " (" + Utilities.join(columnsWithoutAutoIncrement, ", ") + ", " + indexColumnName + ") (SELECT " + Utilities.join(columnsWithoutAutoIncrement, ", ") + ", ROW_NUMBER() OVER() FROM " + tableName + ")");
					statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + indexColumnName + " IS NULL");
				}
			} else {
				throw new Exception("Unsupported db vendor");
			}
			connection.commit();
		} catch (Exception e) {
			throw new Exception("Cannot create lineNumberIndexColumn: " + e.getMessage(), e);
		}
		return indexColumnName;
	}

	public static boolean dropColumnIfExists(Connection connection, String tableName, String columnName) throws Exception {
		if (connection != null && tableName != null && columnName != null && checkTableAndColumnsExist(connection, tableName, columnName)) {
			DbVendor dbVendor = getDbVendor(connection);
			if (dbVendor == DbVendor.SQLite) {
				// SQLite cannot drop colmns
				try (Statement statement = connection.createStatement()) {
					String dateSuffix = DateUtilities.YYYYMMDDHHMMSS.format(new Date());
					
					String createTableStatement = null;
					try (ResultSet resultSet = statement.executeQuery("SELECT sql FROM sqlite_master WHERE type = 'table' AND LOWER(name) = LOWER('" + tableName + "')")) {
						if (resultSet.next()) {
							createTableStatement = resultSet.getString("sql");
						}
					}
					
					createTableStatement = createTableStatement.replaceAll(", " + columnName + " [a-zA-Z0-9()]+\\)", ")").replaceAll(", " + columnName + " [a-zA-Z0-9()]+,", ",");
					
					statement.execute("ALTER TABLE " + tableName + " RENAME TO tmp" + dateSuffix + "_old");
					statement.execute(createTableStatement);
					Set<String> columns = getColumnNames(connection, tableName);
					columns.remove(columnName);
					statement.execute("INSERT INTO " + tableName + " (" + Utilities.join(columns, ", ") + ") SELECT " + Utilities.join(columns, ", ") + " FROM tmp" + dateSuffix + "_old");
					statement.execute("DROP TABLE tmp" + dateSuffix + "_old");
					connection.commit();
					return true;
				} catch (Exception e) {
					connection.rollback();
					throw e;
				}
			} else {
				try (Statement statement = connection.createStatement()) {
					statement.execute("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
					connection.commit();
					return true;
				} catch (Exception e) {
					connection.rollback();
					throw e;
				}
			}
		}
		return false;
	}
	
	public static boolean dropTableIfExists(Connection connection, String tableName) throws Exception {
		if (connection != null && tableName != null && checkTableExist(connection, tableName)) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("DROP TABLE " + tableName);
				connection.commit();
				return true;
			}
		}
		return false;
	}
	
	public static void copyTableStructure(Connection connection, String sourceTableName, List<String> columnNames, List<String> keyColmns, String destinationTableName) throws Exception {
		try (Statement statement = connection.createStatement()) {
			DbVendor dbVendor = getDbVendor(connection);
			if (dbVendor == DbVendor.HSQL || dbVendor == DbVendor.Derby) {
				statement.execute("CREATE TABLE " + destinationTableName + " AS (SELECT " + Utilities.join(columnNames, ", ") + " FROM " + sourceTableName + ") WITH NO DATA");
			} else if (dbVendor == DbVendor.PostgreSQL) {
				// Close a maybe open transaction to allow DDL-statement
				connection.rollback();
				statement.execute("CREATE TABLE " + destinationTableName + " AS SELECT " + Utilities.join(columnNames, ", ") + " FROM " + sourceTableName + " WHERE 1 = 0");
			} else if (dbVendor == DbVendor.Firebird) {
				// There is no "create table as select"-statmenet in firebird
				createTable(connection, destinationTableName, getColumnDataTypes(connection, sourceTableName), null);
			} else {
				statement.execute("CREATE TABLE " + destinationTableName + " AS SELECT " + Utilities.join(columnNames, ", ") + " FROM " + sourceTableName + " WHERE 1 = 0");
			}
			
			// Make all columns nullable
			CaseInsensitiveMap<DbColumnType> columnDataTypes = getColumnDataTypes(connection, destinationTableName);
			for (Entry<String, DbColumnType> columnDataType : columnDataTypes.entrySet()) {
				if (!columnDataType.getValue().isNullable() && (keyColmns == null || !keyColmns.contains(columnDataType.getKey()))) {
					String typeString = columnDataType.getValue().getTypeName();
					if (columnDataType.getValue().getSimpleDataType() == SimpleDataType.String) {
						typeString += "(" + (Long.toString(columnDataType.getValue().getCharacterLength())) + ")";
					} else if (columnDataType.getValue().getSimpleDataType() == SimpleDataType.Integer) {
						typeString += "(" + (Integer.toString(columnDataType.getValue().getNumericPrecision())) + ")";
					} else if (columnDataType.getValue().getSimpleDataType() == SimpleDataType.Double) {
						typeString += "(" + (Integer.toString(columnDataType.getValue().getNumericPrecision())) + ")";
					}
					statement.execute("ALTER TABLE " + destinationTableName + " MODIFY " + columnDataType.getKey() + " " + typeString + " NULL");
				}
			}
			
			if (dbVendor == DbVendor.PostgreSQL || dbVendor == DbVendor.Firebird) {
				connection.commit();
			}
		}
	}
	
	public static void createIndex(Connection connection, String tableName, List<String> columns) throws Exception {
		try (Statement statement = connection.createStatement()) {
			String dateSuffix = DateUtilities.YYYYMMDDHHMMSS.format(new Date());
			statement.execute("CREATE INDEX " + tableName + "_" + dateSuffix + "_ix ON " + tableName + " (" + Utilities.join(columns, ", ") + ")");
		} catch (Exception e) {
			throw new Exception("Cannot create index: " + e.getMessage(), e);
		}
	}
	
	public static int clearTable(Connection connection, String tableName) throws Exception {
		try (Statement statement = connection.createStatement()) {
			return statement.executeUpdate("DELETE FROM " + tableName);
		} catch (Exception e) {
			throw new Exception("Cannot clear table: " + e.getMessage(), e);
		}
	}
	
	public static int insertNotExistingItems(Connection connection, String sourceTableName, String destinationTableName, List<String> insertColumns, List<String> keyColumnsWithFunctions, String additionalInsertValues) throws Exception {
		try (Statement statement = connection.createStatement()) {
			String additionalInsertValuesSqlColumns = "";
			String additionalInsertValuesSqlValues = "";
			if (Utilities.isNotBlank(additionalInsertValues)) {
				for (String line : Utilities.splitAndTrimListQuoted(additionalInsertValues, '\n', '\r', ';')) {
					String columnName = line.substring(0, line.indexOf("=")).trim();
					String columnvalue = line.substring(line.indexOf("=") + 1).trim();
					additionalInsertValuesSqlColumns += columnName + ", ";
					additionalInsertValuesSqlValues += columnvalue + ", ";
				}
			}
			
			String insertDataStatement = "INSERT INTO " + destinationTableName + " (" + additionalInsertValuesSqlColumns + Utilities.join(insertColumns, ", ") + ") SELECT " + additionalInsertValuesSqlValues + Utilities.join(insertColumns, ", ") + " FROM " + sourceTableName + " a";
			if (Utilities.isNotEmpty(keyColumnsWithFunctions)) {
				insertDataStatement += " WHERE NOT EXISTS (SELECT 1 FROM " + destinationTableName + " b WHERE " + getKeyColumnEquationList(keyColumnsWithFunctions, "a", "b") + ")";
			}
			int numberOfInserts = statement.executeUpdate(insertDataStatement);
			connection.commit();
			return numberOfInserts;
		} catch (Exception e) {
			connection.rollback();
			throw new Exception("Cannot insert: " + e.getMessage(), e);
		}
	}

	public static int insertAllItems(Connection connection, String sourceTableName, String destinationTableName, List<String> insertColumns, String additionalInsertValues) throws Exception {
		return insertNotExistingItems(connection, sourceTableName, destinationTableName, insertColumns, null, additionalInsertValues);
	}

	public static int updateAllExistingItems(Connection connection, String sourceTableName, String destinationTableName, Collection<String> updateColumns, Collection<String> keyColumns, String itemIndexColumn, boolean updateWithNullValues, String additionalUpdateValues) throws Exception {
		if (keyColumns == null || keyColumns.isEmpty()) {
			throw new Exception("Missing keycolumns");
		}
		
		// Do not update the keycolumns
		updateColumns = new ArrayList<String>(updateColumns);
		updateColumns.removeAll(keyColumns);

		int updatedItems = 0;
		
		if (!updateColumns.isEmpty()) {
			try (Statement statement = connection.createStatement()) {
				String additionalUpdateValuesSql = "";
				if (Utilities.isNotBlank(additionalUpdateValues)) {
					for (String line : Utilities.splitAndTrimListQuoted(additionalUpdateValues, '\n', '\r', ';')) {
						String columnName = line.substring(0, line.indexOf("=")).trim();
						String columnvalue = line.substring(line.indexOf("=") + 1).trim();
						additionalUpdateValuesSql += columnName + " = " + columnvalue + ", ";
					}
				}
				
				updateColumns = new ArrayList<String>(updateColumns);
				updateColumns.removeAll(keyColumns);
				if (updateColumns.size() > 0 || additionalUpdateValuesSql.length() > 0) {
					String updatedIndexColumn = null;
					try {
						if (updateWithNullValues) {
							String updateSetPart = "";
							for (String updateColumn : updateColumns) {
								if (updateSetPart.length() > 0) {
									updateSetPart += ", ";
								}
								updateSetPart += updateColumn + " = (SELECT " + updateColumn + " FROM " + sourceTableName + " WHERE " + itemIndexColumn + " ="
									+ " (SELECT MAX(" + itemIndexColumn + ") FROM " + sourceTableName + " c WHERE " + getKeyColumnEquationList(keyColumns, destinationTableName, "c") + "))";
							}
							String updateAllAtOnce = "UPDATE " + destinationTableName + " SET " + additionalUpdateValuesSql + updateSetPart
								+ " WHERE EXISTS (SELECT 1 FROM " + sourceTableName + " b WHERE " + getKeyColumnEquationList(keyColumns, destinationTableName, "b") + ")";
							updatedItems = statement.executeUpdate(updateAllAtOnce);
						} else {
							updatedIndexColumn = addIndexedIntegerColumn(connection, destinationTableName, "updatedindex");
							for (String updateColumn : updateColumns) {
								String updateSingleColumn = "UPDATE " + destinationTableName
									+ " SET " + additionalUpdateValuesSql + updatedIndexColumn + " = 1, " + updateColumn + " = (SELECT " + updateColumn + " FROM " + sourceTableName + " WHERE " + itemIndexColumn + " ="
										+ " (SELECT MAX(" + itemIndexColumn + ") FROM " + sourceTableName + " c WHERE " + updateColumn + " IS NOT NULL AND " + getKeyColumnEquationList(keyColumns, destinationTableName, "c") + "))"
									+ " WHERE EXISTS (SELECT 1 FROM " + sourceTableName + " b WHERE " + updateColumn + " IS NOT NULL AND " + getKeyColumnEquationList(keyColumns, destinationTableName, "b") + ")";
								statement.executeUpdate(updateSingleColumn);
							}

							try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + destinationTableName + " WHERE " + updatedIndexColumn + " = 1")) {
								resultSet.next();
								updatedItems = resultSet.getInt(1);
							}
						}
						connection.commit();
					} catch (Exception e) {
						connection.rollback();
						throw e;
					} finally {
						dropColumnIfExists(connection, destinationTableName, updatedIndexColumn);
					}
				}
			} catch (Exception e) {
				throw new Exception("Cannot update: " + e.getMessage(), e);
			}
		}
		
		return updatedItems;
	}
	
	public static int updateFirstExistingItems(Connection connection, String sourceTableName, String destinationTableName, Collection<String> updateColumns, Collection<String> keyColumns, String itemIndexColumn, boolean updateWithNullValues, String additionalUpdateValues) throws Exception {
		if (keyColumns == null || keyColumns.isEmpty()) {
			throw new Exception("Missing keycolumns");
		}
		
		// Do not update the keycolumns
		updateColumns = new ArrayList<String>(updateColumns);
		updateColumns.removeAll(keyColumns);
		
		int updatedItems = 0;
		
		if (!updateColumns.isEmpty()) {
			try (Statement statement = connection.createStatement()) {
				String additionalUpdateValuesSql = "";
				if (Utilities.isNotBlank(additionalUpdateValues)) {
					for (String line : Utilities.splitAndTrimListQuoted(additionalUpdateValues, '\n', '\r', ';')) {
						String columnName = line.substring(0, line.indexOf("=")).trim();
						String columnvalue = line.substring(line.indexOf("=") + 1).trim();
						additionalUpdateValuesSql += columnName + " = " + columnvalue + ", ";
					}
				}
				
				updateColumns = new ArrayList<String>(updateColumns);
				updateColumns.removeAll(keyColumns);
				if (updateColumns.size() > 0 || additionalUpdateValuesSql.length() > 0) {
					String originalItemIndexColumn = null;
					String updatedIndexColumn = null;
					try {
						originalItemIndexColumn = createLineNumberIndexColumn(connection, destinationTableName, "itemindex");
						
						// Mark duplicates in temp table
						List<String> keycolumnParts = new ArrayList<String>();
						for (String keyColumn : keyColumns) {
							keycolumnParts.add("src." + keyColumn + " = " + sourceTableName + "." + keyColumn + " AND src." + keyColumn + " IS NOT NULL");
						}
						String updateStatement = "UPDATE " + sourceTableName + " SET " + itemIndexColumn + " = COALESCE((SELECT MIN(src." + originalItemIndexColumn + ") FROM " + destinationTableName + " src WHERE " + Utilities.join(keycolumnParts, " AND ") + "), 0)";
						statement.executeUpdate(updateStatement);
						connection.commit();
						
						// Update with marked items
						if (updateWithNullValues) {
							String updateSetPart = "";
							for (String updateColumn : updateColumns) {
								if (updateSetPart.length() > 0) {
									updateSetPart += ", ";
								}
								updateSetPart += updateColumn + " = (SELECT " + updateColumn + " FROM " + sourceTableName + " WHERE " + itemIndexColumn + " ="
									+ " (SELECT MAX(" + itemIndexColumn + ") FROM " + sourceTableName + " c WHERE " + getKeyColumnEquationList(keyColumns, destinationTableName, "c") + "))";
							}
							String updateAllAtOnce = "UPDATE " + destinationTableName + " SET " + additionalUpdateValuesSql + updateSetPart
								+ " WHERE EXISTS (SELECT 1 FROM " + sourceTableName + " b WHERE " + originalItemIndexColumn + " = b." + itemIndexColumn + ")";
							updatedItems = statement.executeUpdate(updateAllAtOnce);
						} else {
							updatedIndexColumn = addIndexedIntegerColumn(connection, destinationTableName, "updatedindex");
							for (String updateColumn : updateColumns) {
								String updateSingleColumn = "UPDATE " + destinationTableName
									+ " SET " + additionalUpdateValuesSql + updatedIndexColumn + " = 1, " + updateColumn + " = (SELECT " + updateColumn + " FROM " + sourceTableName + " WHERE " + itemIndexColumn + " ="
										+ " (SELECT MAX(" + itemIndexColumn + ") FROM " + sourceTableName + " c WHERE " + updateColumn + " IS NOT NULL AND " + getKeyColumnEquationList(keyColumns, destinationTableName, "c") + "))"
									+ " WHERE EXISTS (SELECT 1 FROM " + sourceTableName + " b WHERE " + originalItemIndexColumn + " = b." + itemIndexColumn + ")";
								statement.executeUpdate(updateSingleColumn);
							}
							
							try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + destinationTableName + " WHERE " + updatedIndexColumn + " = 1")) {
								resultSet.next();
								updatedItems = resultSet.getInt(1);
							}
						}
						connection.commit();
					} catch (Exception e) {
						connection.rollback();
						throw e;
					} finally {
						dropColumnIfExists(connection, destinationTableName, originalItemIndexColumn);
						dropColumnIfExists(connection, destinationTableName, updatedIndexColumn);
					}
				}
			} catch (Exception e) {
				throw new Exception("Cannot update first entries: " + e.getMessage(), e);
			}
		}
		
		return updatedItems;
	}
}
