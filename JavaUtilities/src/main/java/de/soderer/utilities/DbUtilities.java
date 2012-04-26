package de.soderer.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import de.soderer.utilities.CsvWriter;

public class DbUtilities {
	public static int readoutTableInCsvFile(DataSource dataSource, String statementString, String filePathAndName, char separator) throws Exception {
		return readoutTableInCsvFile(dataSource, statementString, new File(filePathAndName), separator);
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
			
			return csvWriter.getDataLinesWritten();
		} finally {			
			if (resultSet != null)
				resultSet.close();

			if (statement != null)
				statement.close();

			if (connection != null)
				connection.close();
			
			if (csvWriter != null)
				csvWriter.close();
		}
	}
}
