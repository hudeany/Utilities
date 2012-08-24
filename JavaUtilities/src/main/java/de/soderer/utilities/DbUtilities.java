package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class DbUtilities {
    public static int getResultRowValueAsInt(String valueName, Map<String, Object> resultRow) {
    	return ((Number)resultRow.get(valueName)).intValue();
    }
    
    public static Integer getResultRowValueAsInteger(String valueName, Map<String, Object> resultRow) {
    	return new Integer(((Number)resultRow.get(valueName)).intValue());
    }
    
    public static String getResultRowValueAsString(String valueName, Map<String, Object> resultRow) {
    	return (String)resultRow.get(valueName);
    }

    public static Timestamp getResultRowValueAsTimestamp(String valueName, Map<String, Object> resultRow) {
    	return (Timestamp)resultRow.get(valueName);
    }
    
    public static int readoutTableInCsvFile(DataSource dataSource, String statementString, File outputFile, char separator) throws Exception {
		if (outputFile.exists()) throw new Exception("Outputfile already exists");
		
		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		CsvWriter csvWriter = null;
		try {
			connection = dataSource.getConnection();
			statement = connection.createStatement();
			resultSet = statement.executeQuery(statementString);
			csvWriter = new CsvWriter(new FileOutputStream(outputFile));
			ResultSetMetaData metaData = resultSet.getMetaData();
			// write headers
			List<String> headers = new ArrayList<String>();
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				headers.add(metaData.getColumnName(i));
			}
			csvWriter.writeValues(headers);
			
			// write values
			while (resultSet.next()) {
				List<String> values = new ArrayList<String>();
				for (int i = 1; i <= metaData.getColumnCount(); i++) {					
					values.add(resultSet.getString(i));
				}
				csvWriter.writeValues(values);
			}
			
			return csvWriter.getWrittenLines();
		} finally {	
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connection != null) {
				connection.close();
			}
			
			IOUtils.closeQuietly(csvWriter);
		}
	}
	
	public static Map<Integer, Object[]> importDataInTable(DataSource dataSource, String tableName, String[] tableColumns, List<Object[]> dataSets, boolean commitOnFullSuccessOnly) throws Exception {
		if (StringUtils.isBlank(tableName)) {
			throw new Exception("Missing parameter tableName for dataimport");
		}
		
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);
			statement = connection.createStatement();
		
			checkTableAndColumnsExist(connection, tableName, tableColumns);
			
			// Insert data
			Map<Integer, Object[]> notInsertedData = new HashMap<Integer, Object[]>();
			String insertStatementString = "INSERT INTO " + tableName + " (" + StringUtils.join(tableColumns, ", ") + ") VALUES (" + Utilities.repeatString("?", tableColumns.length, ", ") + ")";
			preparedStatement = connection.prepareStatement(insertStatementString);
			boolean hasOpenData = false;
			List<Object[]> currentUncommitedLines = new ArrayList<Object[]>();
			int datasetIndex;
			for (datasetIndex = 0; datasetIndex < dataSets.size(); datasetIndex++) {
				Object[] dataSet = dataSets.get(datasetIndex);
				currentUncommitedLines.add(dataSet);
				hasOpenData = true;
				
				if (dataSet.length != tableColumns.length) {
					if (!commitOnFullSuccessOnly) {
						notInsertedData.put(datasetIndex, dataSet);
					} else {
						connection.rollback();
						throw new Exception("Error on insert of dataset at index " + datasetIndex + ": invalid number of dataitems");
					}
				} else {
					preparedStatement.clearParameters();
					for (int parameterIndex = 0; parameterIndex < dataSet.length; parameterIndex++) {
						preparedStatement.setObject(parameterIndex + 1, dataSet[parameterIndex]);
					}
					preparedStatement.addBatch();
					
					if ((datasetIndex + 1) % 100 == 0) {
						hasOpenData = false;
						try {
							preparedStatement.executeBatch();
							if (!commitOnFullSuccessOnly) {
								connection.commit();
							}
							currentUncommitedLines.clear();
						} catch (BatchUpdateException bue) {
							if (commitOnFullSuccessOnly) {
								connection.rollback();
								throw new Exception("Error on insert of dataset between index " + (datasetIndex - currentUncommitedLines.size()) + " and index " + datasetIndex + ": " + bue.getMessage());
							} else {
								connection.rollback();
								importDataInTable(datasetIndex - currentUncommitedLines.size(), connection, preparedStatement, tableColumns, currentUncommitedLines, notInsertedData);
							}
						} catch (Exception e) {
							connection.rollback();
							throw new Exception("Error on insert of dataset between index " + (datasetIndex - currentUncommitedLines.size()) + " and index " + datasetIndex + ": " + e.getMessage());
						}
					}
				}
			}
			
			if (hasOpenData) {
				hasOpenData = false;
				try {
					preparedStatement.executeBatch();
					if (!commitOnFullSuccessOnly) {
						connection.commit();
					}
					currentUncommitedLines.clear();
				} catch (BatchUpdateException bue) {
					if (commitOnFullSuccessOnly) {
						connection.rollback();
						throw new Exception("Error on insert of dataset between index " + (datasetIndex - currentUncommitedLines.size()) + " and index " + datasetIndex + ": " + bue.getMessage());
					} else {
						connection.rollback();
						importDataInTable(datasetIndex - currentUncommitedLines.size(), connection, preparedStatement, tableColumns, currentUncommitedLines, notInsertedData);
					}
				} catch (Exception e) {
					connection.rollback();
					throw new Exception("Error on insert of dataset between index " + (datasetIndex - currentUncommitedLines.size()) + " and index " + datasetIndex + ": " + e.getMessage());
				}
			}
			
			if (commitOnFullSuccessOnly) {
				connection.commit();
			}
			
			return notInsertedData;
		} finally {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}
			
			if (preparedStatement != null) {
				preparedStatement.close();
			}

			if (connection != null) {
				connection.rollback();
				connection.close();
			}
		}
	}
	
	private static void importDataInTable(int offsetIndex, Connection connection, PreparedStatement preparedStatement, String[] columnMapping, List<Object[]> data, Map<Integer, Object[]> notInsertedData) throws Exception {
		int dataLineIndex = offsetIndex;
		for (Object[] dataLine : data) {
			dataLineIndex++;
			if (dataLine.length != columnMapping.length) {
				notInsertedData.put(dataLineIndex, dataLine);
			} else {
				int parameterIndex = 1;
				for (int csvValueIndex = 0; csvValueIndex < dataLine.length; csvValueIndex++) {
					if (columnMapping[csvValueIndex] != null) {
						preparedStatement.setObject(parameterIndex++, dataLine[csvValueIndex]);
					}
				}
				
				try {
					preparedStatement.execute();
					connection.commit();
				} catch (Exception e) {
					notInsertedData.put(dataLineIndex, dataLine);
					connection.rollback();
				}
			}
		}
	}
	
	public static Map<Integer, List<String>> importCsvFileInTable(DataSource dataSource, String tableName, String[] columnMapping, File csvFile, String encoding, boolean commitOnFullSuccessOnly) throws Exception {
		return importCsvFileInTable(dataSource, tableName, columnMapping, csvFile, encoding, false, commitOnFullSuccessOnly);
	}
	
	public static Map<Integer, List<String>> importCsvFileInTable(DataSource dataSource, String tableName, String[] columnMapping, File csvFile, String encoding, boolean fillMissingTrailingColumnsWithNull, boolean commitOnFullSuccessOnly) throws Exception {
		if (StringUtils.isBlank(tableName)) {
			throw new Exception("Missing parameter tableName for dataimport");
		}
		
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		Statement statement = null;
		ResultSet resultSet = null;
		
		CsvReader csvReader = null;
		
		try {
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);
			statement = connection.createStatement();
			
			checkTableAndColumnsExist(connection, tableName, columnMapping);
			
			csvReader = new CsvReader(new FileInputStream(csvFile), encoding, ';');
			csvReader.setFillMissingTrailingColumnsWithNull(fillMissingTrailingColumnsWithNull);

			// First line contains headers
			List<String> csvLine = csvReader.readNextCsvLine();
			if (columnMapping == null) {
				columnMapping = csvLine.toArray(new String[0]);
			}
		
			List<String> dbColumns = new ArrayList<String>();
			for (String column : columnMapping) {
				if (column != null) {
					dbColumns.add(column);
				}
			}

			Map<Integer, List<String>> notInsertedData = new HashMap<Integer, List<String>>();
			String insertStatementString = "INSERT INTO " + tableName + " (" + StringUtils.join(dbColumns, ", ") + ") VALUES (" + Utilities.repeatString("?", dbColumns.size(), ", ") + ")";
			preparedStatement = connection.prepareStatement(insertStatementString);

			// Read and insert data
			int csvLineIndex = 1; // index obeys headerline => real lineindex in csv-file
			boolean hasOpenData = false;
			List<List<String>> currentUncommitedLines = new ArrayList<List<String>>();
			while ((csvLine = csvReader.readNextCsvLine()) != null) {
				csvLineIndex++;
				currentUncommitedLines.add(csvLine);
				hasOpenData = true;
				
				if (csvLine.size() != columnMapping.length) {
					if (!commitOnFullSuccessOnly) {
						notInsertedData.put(csvLineIndex, csvLine);
					} else {
						connection.rollback();
						throw new Exception("Error on insert of dataset at line " + csvLineIndex + ": invalid number of dataitems");
					}
				} else {
					int parameterIndex = 1;
					for (int csvValueIndex = 0; csvValueIndex < csvLine.size(); csvValueIndex++) {
						if (columnMapping[csvValueIndex] != null) {
							preparedStatement.setString(parameterIndex++, csvLine.get(csvValueIndex));
						}
					}
					preparedStatement.addBatch();
					
					if (csvLineIndex % 100 == 0) {
						hasOpenData = false;
						try {
							preparedStatement.executeBatch();
							if (!commitOnFullSuccessOnly) {
								connection.commit();
							}
							currentUncommitedLines.clear();
						} catch (BatchUpdateException bue) {
							if (commitOnFullSuccessOnly) {
								connection.rollback();
								throw new Exception("Error on insert of dataset between line " + (csvLineIndex - currentUncommitedLines.size()) + " and line " + csvLineIndex + ": " + bue.getMessage());
							} else {
								connection.rollback();
								importCsvDataInTable(csvLineIndex - currentUncommitedLines.size(), connection, preparedStatement, columnMapping, currentUncommitedLines, notInsertedData);
							}
						} catch (Exception e) {
							if (!commitOnFullSuccessOnly) {
								notInsertedData.put(csvLineIndex, csvLine);
								connection.rollback();
							} else {
								connection.rollback();
								throw new Exception("Error on insert of dataset at line " + csvLineIndex + ": " + e.getMessage());
							}
						}
					}
				}
			}
			
			if (hasOpenData) {
				hasOpenData = false;
				try {
					preparedStatement.executeBatch();
					if (!commitOnFullSuccessOnly) {
						connection.commit();
					}
					currentUncommitedLines.clear();
				} catch (BatchUpdateException bue) {
					if (commitOnFullSuccessOnly) {
						connection.rollback();
						throw new Exception("Error on insert of dataset between line " + (csvLineIndex - currentUncommitedLines.size()) + " and line " + csvLineIndex + ": " + bue.getMessage());
					} else {
						connection.rollback();
						importCsvDataInTable(csvLineIndex - currentUncommitedLines.size(), connection, preparedStatement, columnMapping, currentUncommitedLines, notInsertedData);
					}
				} catch (Exception e) {
					connection.rollback();
					throw new Exception("Error on insert of dataset between line " + (csvLineIndex - currentUncommitedLines.size()) + " and line " + csvLineIndex + ": " + e.getMessage());
				}
			}
			
			if (commitOnFullSuccessOnly) {
				connection.commit();
			}
			
			return notInsertedData;
		} finally {
			if (csvReader != null) {
				csvReader.close();
			}
			
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}
			
			if (preparedStatement != null) {
				preparedStatement.close();
			}

			if (connection != null) {
				connection.rollback();
				connection.close();
			}
		}
	}
	
	private static void importCsvDataInTable(int offsetIndex, Connection connection, PreparedStatement preparedStatement, String[] columnMapping, List<List<String>> data, Map<Integer, List<String>> notInsertedData) throws Exception {
		int csvLineIndex = offsetIndex;
		for (List<String> csvLine : data) {
			csvLineIndex++;
			if (csvLine.size() != columnMapping.length) {
				notInsertedData.put(csvLineIndex, csvLine);
			} else {
				int parameterIndex = 1;
				for (int csvValueIndex = 0; csvValueIndex < csvLine.size(); csvValueIndex++) {
					if (columnMapping[csvValueIndex] != null) {
						preparedStatement.setString(parameterIndex++, csvLine.get(csvValueIndex));
					}
				}
				
				try {
					preparedStatement.execute();
					connection.commit();
				} catch (Exception e) {
					notInsertedData.put(csvLineIndex, csvLine);
					connection.rollback();
				}
			}
		}
	}
	
	public static boolean checkDbVendorIsOracle(DataSource dataSource) {
		// Alternative check method:
		//		SimpleJdbcTemplate jdbc = new SimpleJdbcTemplate(dataSource);
		//		try {
		//			int result = jdbc.queryForInt("SELECT 1 FROM user_tab_cols");
		//			return result == 1;
		//		} catch (DataAccessException e) {
		//			return false;
		//		}
		
		Connection connection = null;
		try {
			return checkDbVendorIsOracle(connection);
		} finally {
			closeQuietly(connection);
		}
	}
	
	public static boolean checkDbVendorIsOracle(Connection connection) {
		// Alternative check method:
		//		Statement statement = null;
		//		try {
		//			statement = connection.createStatement();
		//			statement.execute("SELECT 1 FROM user_tab_cols");
		//			return true;
		//		} catch (SQLException e) {
		//			return false;
		//		} finally {
		//			closeQuietly(statement);
		//		}
		
		try {
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			if (databaseMetaData != null) {
				String productName = databaseMetaData.getDatabaseProductName();
				if ("oracle".equalsIgnoreCase(productName)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch (SQLException e) {
			return false;
		}
	}
	
	public static void checkTableAndColumnsExist(Connection connection, String tableName, String[] columns) throws Exception {
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			statement = connection.createStatement();
			
			// Check if table exists
			try {
				resultSet = statement.executeQuery("SELECT * FROM " + tableName + " WHERE 1 = 0");
			} catch (Exception e) {
				throw new Exception("Table '" + tableName + "' does not exist");
			}
			
			// Check if all needed columns exist
			Set<String> dbTableColumns = new HashSet<String>();
			ResultSetMetaData metaData = resultSet.getMetaData();
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				dbTableColumns.add(metaData.getColumnName(i).toUpperCase());
			}
			for (String column : columns) {
				if (column != null && !dbTableColumns.contains(column.toUpperCase())) {
					throw new Exception("Column '" + column + "' does not exist in table '" + tableName + "'");
				}
			}
		} finally {
			closeQuietly(resultSet);
			resultSet = null;
			closeQuietly(statement);
			statement = null;
		}
	}
	
	public static String callStoredProcedureWithDbmsOutput(Connection connection, String procedureName, Object... parameters) throws SQLException {
		CallableStatement callableStatement = null;
		try {
			callableStatement = connection.prepareCall("begin dbms_output.enable(:1); end;");
			callableStatement.setLong(1, 10000);
			callableStatement.executeUpdate();
			callableStatement.close();
			callableStatement = null;
			
			if (parameters != null) {
				callableStatement = connection.prepareCall("{call " + procedureName + "(" + Utilities.repeatString("?", parameters.length, ", ") + ")}");
				for (int i = 0; i < parameters.length; i++) {
					callableStatement.setObject(i + 1, parameters[i]);
				}
			} else {
				callableStatement = connection.prepareCall("{call " + procedureName + "()}");
			}
			callableStatement.execute();
			callableStatement.close();
			callableStatement = null;

			callableStatement = connection
				.prepareCall(
					"declare "
					+ "    l_line varchar2(255); "
					+ "    l_done number; "
					+ "    l_buffer long; "
					+ "begin "
					+ "  loop "
					+ "    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; "
					+ "    dbms_output.get_line( l_line, l_done ); "
					+ "    l_buffer := l_buffer || l_line || chr(10); "
					+ "  end loop; " + " :done := l_done; "
					+ " :buffer := l_buffer; "
					+ "end;");

			callableStatement.registerOutParameter(2, Types.INTEGER);
			callableStatement.registerOutParameter(3, Types.VARCHAR);
			StringBuffer dbmsOutput = new StringBuffer(1024);
			while (true) {
				callableStatement.setInt(1, 32000);
				callableStatement.executeUpdate();
				dbmsOutput.append(callableStatement.getString(3).trim());
				if (callableStatement.getInt(2) == 1) {
					break;
				}
			}
			callableStatement.close();
			callableStatement = null;
			
			callableStatement = connection.prepareCall("begin dbms_output.disable; end;");
			callableStatement.executeUpdate();
			callableStatement.close();
			callableStatement = null;
			
			return dbmsOutput.toString();
		} finally {
			closeQuietly(callableStatement);
		}
	}
	
	public static void closeQuietly(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
			}
		}
	}
	
	public static void closeQuietly(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
			}
		}
	}
	
	public static void closeQuietly(ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
			}
		}
	}
}
