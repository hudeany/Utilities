package de.soderer.utilities;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.SystemException;

public class ZipUtils {
	/**
	 * Zippen einer Datei oder rekursiv aller Dateien und Unterdateien eines Ordners
	 * Dabei wird das Zipfile im gleichen Verzeichnis wie die zu zippende Datei abgelegt
	 * 
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	public static File zipFile(File sourceFile) throws IOException {
		File zippedFile = new File(sourceFile.getAbsolutePath() + ".zip");
		zipFile(sourceFile, zippedFile);
		return zippedFile;
	}
	
	/**
	 * Zippen einer Datei oder rekursiv aller Dateien und Unterdateien eines Ordners
	 * 
	 * @param sourceFile
	 * @param destinationZipFile
	 * @throws IOException
	 */
	public static void zipFile(File sourceFile, File destinationZipFile) throws IOException {
		ZipOutputStream zipOutputStream = null;
		
		if (!sourceFile.exists())
			throw new IOException("SourceFile does not exist");
			
		if (destinationZipFile.exists())
			throw new IOException("DestinationFile already exists");
		
		try {
			zipOutputStream = openNewZipOutputStream(destinationZipFile);
			
			addFileToOpenZipFileStream(sourceFile, zipOutputStream);

			closeZipOutputStream(zipOutputStream);
			zipOutputStream = null;
		}
		catch (IOException e) {
			if (destinationZipFile.exists()) {
				if (zipOutputStream != null) {
					try {
						zipOutputStream.close();
					}
					catch (Exception ex) {
					}
					zipOutputStream = null;
				}	
				destinationZipFile.delete();
			}
			throw e;
		}
		finally {
			if (zipOutputStream != null) {
				try {
					zipOutputStream.close();
				}
				catch (Exception e) {
				}
				zipOutputStream = null;
			}
		}
	}
	
	/**
	 * Zippen einer Datei oder rekursiv aller Dateien und Unterdateien eines Ordners
	 * Es wird hierbei ein neuer relativer Pfad begonnen
	 * 
	 * @param sourceFile
	 * @param destinationZipFileSream
	 * @throws IOException
	 */
	public static void addFileToOpenZipFileStream(File sourceFile, ZipOutputStream destinationZipFileSream) throws IOException {
		addFileToOpenZipFileStream(sourceFile, File.separator, destinationZipFileSream);
	}
	
	/**
	 * Zippen einer Datei oder rekursiv aller Dateien und Unterdateien eines Ordners
	 * 
	 * @param sourceFile
	 * @param destinationZipFileSream
	 * @throws IOException
	 */
	public static void addFileToOpenZipFileStream(File sourceFile, String relativeDirPath, ZipOutputStream destinationZipFileSream) throws IOException {
		BufferedInputStream bufferedFileInputStream = null;
		
		if (!sourceFile.exists())
			throw new IOException("SourceFile does not exist");
			
		if (destinationZipFileSream == null)
			throw new IOException("DestinationStream is not ready");
		
		if (relativeDirPath == null
				|| (!relativeDirPath.endsWith("/")
				&& !relativeDirPath.endsWith("\\")))
			throw new IOException("RelativeDirPath is invalid");
		
		try {
			if (!sourceFile.isDirectory()) {
				ZipEntry entry = new ZipEntry(relativeDirPath + sourceFile.getName());
				entry.setTime(sourceFile.lastModified());
				destinationZipFileSream.putNextEntry(entry);
				
				bufferedFileInputStream = new BufferedInputStream(new FileInputStream(sourceFile));
				byte[] bufferArray = new byte[1024];
				int byteBufferFillLength = bufferedFileInputStream.read(bufferArray);
				while (byteBufferFillLength > -1) {
					destinationZipFileSream.write(bufferArray, 0, byteBufferFillLength);
					byteBufferFillLength = bufferedFileInputStream.read(bufferArray);
				}
				bufferedFileInputStream.close();
				bufferedFileInputStream = null;
				
				destinationZipFileSream.flush();
				destinationZipFileSream.closeEntry();
			}
			else {	
				for (File sourceSubFile : sourceFile.listFiles()) {
					addFileToOpenZipFileStream(sourceSubFile, relativeDirPath + sourceFile.getName() + File.separator, destinationZipFileSream);
				}
			}
		}
		catch (IOException e) {
			throw e;
		}
		finally {			
			if (bufferedFileInputStream != null) {
				try {
					bufferedFileInputStream.close();
				}
				catch (Exception e) {
				}
				bufferedFileInputStream = null;
			}
		}
	}
	
	/**
	 * Hinzufügen von Daten zu einem offenen ZipOutputStream als virtuelle Datei
	 * 
	 * @param fileData
	 * @param filename
	 * @param destinationZipFileSream
	 * @throws IOException
	 */
	public static void addFileDataToOpenZipFileStream(byte[] fileData, String filename, ZipOutputStream destinationZipFileSream) throws IOException {
		addFileDataToOpenZipFileStream(fileData, File.separator, filename, destinationZipFileSream);
	}
	
	/**
	 * Öffnen eines neuen ZipOutputStream basierend auf eine Datei in die geschrieben werden soll
	 * 
	 * @param destinationZipFile
	 * @return
	 * @throws IOException
	 */
	public static ZipOutputStream openNewZipOutputStream(File destinationZipFile) throws IOException {
		if (destinationZipFile.exists())
			throw new IOException("DestinationFile already exists");
		else if (!destinationZipFile.getParentFile().exists())
			throw new IOException("DestinationDirectory does not exist");
		
		try {
			return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destinationZipFile)));
		}
		catch (IOException e) {
			if (destinationZipFile.exists()) {	
				destinationZipFile.delete();
			}
			throw e;
		}
	}
	
	/**
	 * Öffnen eines neuen ZipOutputStream basierend auf einem beliebeigen OutputStream in den geschrieben werden soll
	 * 
	 * @param destinationZipStream
	 * @return
	 * @throws IOException
	 */
	public static ZipOutputStream openNewZipOutputStream(OutputStream destinationZipStream) throws IOException {
		if (destinationZipStream == null)
			throw new IOException("DestinationStream is missing");
		
		return new ZipOutputStream(new BufferedOutputStream(destinationZipStream));
	}
	
	/**
	 * Schließen eines offenen ZipOutputStreams
	 * 
	 * @param zipOutputStream
	 * @throws IOException
	 */
	public static void closeZipOutputStream(ZipOutputStream zipOutputStream) throws IOException {
		try {
			zipOutputStream.finish();
			zipOutputStream.flush();
			zipOutputStream.close();
			zipOutputStream = null;
		}
		catch (IOException e) {
			throw e;
		}
		finally {
			if (zipOutputStream != null) {
				try {
					zipOutputStream.close();
				}
				catch (Exception e) {
				}
				zipOutputStream = null;
			}
		}
	}
	
	public static void closeZipOutputStreamQuietly(ZipOutputStream zipOutputStream) {
		try {
			zipOutputStream.finish();
			zipOutputStream.flush();
			zipOutputStream.close();
			zipOutputStream = null;
		}
		catch (IOException e) {
		}
		finally {
			if (zipOutputStream != null) {
				try {
					zipOutputStream.close();
				}
				catch (Exception e) {
				}
				zipOutputStream = null;
			}
		}
	}
	
	/**
	 * Hinzufügen von Daten zu einem offenen ZipOutputStream als virtuelle Datei
	 * 
	 * @param fileData
	 * @param relativeDirPath
	 * @param filename
	 * @param destinationZipFileSream
	 * @throws IOException
	 */
	public static void addFileDataToOpenZipFileStream(byte[] fileData, String relativeDirPath, String filename, ZipOutputStream destinationZipFileSream) throws IOException {
		if (fileData == null)
			throw new IOException("FileData is missing");
		
		if (StringUtils.isEmpty(filename) || filename.trim().length() == 0)
			throw new IOException("Filename is missing");
			
		if (destinationZipFileSream == null)
			throw new IOException("DestinationStream is not ready");
		
		if (relativeDirPath == null
				|| (!relativeDirPath.endsWith("/")
				&& !relativeDirPath.endsWith("\\")))
			throw new IOException("RelativeDirPath is invalid");
		
		ZipEntry entry = new ZipEntry(relativeDirPath + filename);
		entry.setTime(new Date().getTime());
		destinationZipFileSream.putNextEntry(entry);
		
		destinationZipFileSream.write(fileData);
		
		destinationZipFileSream.flush();
		destinationZipFileSream.closeEntry();
	}
	
	/**
	 * Hinzufügen einer Datei oder rekursiv aller Dateien und Unterdateien eines Ordners zu einer bereits existierenden ZipFile
	 * Dabei werden alle Einträge der OriginalZipFile in eine neue ZipFile umkopiert
	 * 
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	public static void addFileToExistingzipFile(File sourceFile, File zipFile) throws IOException {
		ZipOutputStream zipOutputStream = openExistingZipFileForExtension(zipFile);
		try {
			addFileToOpenZipFileStream(sourceFile, zipOutputStream);
		}
		finally {
			closeZipOutputStream(zipOutputStream);
		}
	}
	
	/**
	 * Hinzufügen einer Liste von Datei oder rekursiv aller Dateien und Unterdateien eines Ordners zu einer bereits existierenden ZipFile
	 * Dabei werden alle Einträge der OriginalZipFile in eine neue ZipFile umkopiert
	 * 
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	public static void addFileToExistingzipFile(List<File> sourceFiles, File zipFile) throws IOException {
		ZipOutputStream zipOutputStream = openExistingZipFileForExtension(zipFile);
		try {
			for (File file : sourceFiles) {
				addFileToOpenZipFileStream(file, zipOutputStream);
			}
		}
		finally {
			closeZipOutputStream(zipOutputStream);
		}
	}

	/**
	 * Öffnen einer existierenden Zip-Datei um neue Dateien anzuhängen oder wenn diese Datei noch nicht existiert eine neue Zip-Datei erstellen
	 * @param file
	 * @return
	 * @throws SystemException
	 */
	public static ZipOutputStream openExistingZipFileForExtensionOrCreateNewZipFile(File zipFile) throws IOException {
		if (zipFile.exists())
			return openExistingZipFileForExtension(zipFile);
		else
			return openNewZipOutputStream(zipFile);
	}
	
	/**
	 * Öffnen einer existierenden ZipFile um neue Dateien anzuhängen
	 * Dabei werden alle Einträge der OriginalZipFile in eine neue ZipFile umkopiert
	 * 
	 * @param zipFile
	 * @return
	 * @throws IOException 
	 * @throws ZipException 
	 */
	public static ZipOutputStream openExistingZipFileForExtension(File zipFile) throws IOException {
		//Umbenennen des QuellZipFiles (Achtung: der String Pfad und Name von zipFile bleibt dabei erhalten und wird nicht mit umbenannt)
		File originalFileTemp = new File(zipFile.getParentFile().getAbsolutePath() + "/" + String.valueOf(System.currentTimeMillis()));
		zipFile.renameTo(originalFileTemp);
		
		ZipFile sourceZipFile = new ZipFile(originalFileTemp);
		ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
		
		BufferedInputStream bufferedInputStream = null;
		
		try {
			// Umkopieren der Einträge
			Enumeration<? extends ZipEntry> srcEntries = sourceZipFile.entries();
			while (srcEntries.hasMoreElements()) {
				ZipEntry sourceZipFileEntry = srcEntries.nextElement();
				zipOutputStream.putNextEntry(sourceZipFileEntry);
			
				bufferedInputStream = new BufferedInputStream(sourceZipFile.getInputStream(sourceZipFileEntry));
			
				byte[] bufferArray = new byte[1024];
				int byteBufferFillLength = bufferedInputStream.read(bufferArray);
				while (byteBufferFillLength > -1) {
					zipOutputStream.write(bufferArray, 0, byteBufferFillLength);
					byteBufferFillLength = bufferedInputStream.read(bufferArray);
				}
				
				zipOutputStream.closeEntry();
			
				bufferedInputStream.close();
				bufferedInputStream = null;
			}
			
			zipOutputStream.flush();
			sourceZipFile.close();
			originalFileTemp.delete();
			
			return zipOutputStream;
		}
		catch (IOException e) {
			// Löschen des evtl. vorhandenen neuen ZipFiles
			if (zipFile.exists()) {
				if (zipOutputStream != null) {
					try {
						zipOutputStream.close();
					}
					catch (Exception ex) {
					}
					zipOutputStream = null;
				}	
				zipFile.delete();
			}
			
			//Zurückdrehen der Umbenennung des QuellZipFiles
			originalFileTemp.renameTo(zipFile);
			throw e;
		}
		finally {
			if (bufferedInputStream != null) {
				try {
					bufferedInputStream.close();
				}
				catch (Exception e) {
				}
				bufferedInputStream = null;
			}
		}
	}
	
	/**
	 * Auslesen einer ZipDatei
	 * @param zipFile
	 * @return alle Dateieinträge in einer Map
	 * @throws IOException
	 */
	public static Map<String, byte[]> readExistingZipFile(File zipFile) throws IOException {
		Map<String, byte[]> returnMap = new HashMap<String, byte[]>();
		
		ZipFile sourceZipFile = null;
		BufferedInputStream bufferedInputStream = null;
		ByteArrayOutputStream byteArrayOutputStream = null;
		
		try {
			sourceZipFile = new ZipFile(zipFile);
			
			// Lesen der Einträge
			Enumeration<? extends ZipEntry> srcEntries = sourceZipFile.entries();
			while (srcEntries.hasMoreElements()) {
				ZipEntry sourceZipFileEntry = srcEntries.nextElement();
			
				bufferedInputStream = new BufferedInputStream(sourceZipFile.getInputStream(sourceZipFileEntry));
				byteArrayOutputStream = new ByteArrayOutputStream();
			
				byte[] bufferArray = new byte[1024];
				int byteBufferFillLength = bufferedInputStream.read(bufferArray);
				while (byteBufferFillLength > -1) {
					byteArrayOutputStream.write(bufferArray, 0, byteBufferFillLength);
					byteBufferFillLength = bufferedInputStream.read(bufferArray);
				}
				
				returnMap.put(sourceZipFileEntry.getName(), byteArrayOutputStream.toByteArray());
			
				byteArrayOutputStream.close();
				byteArrayOutputStream = null;
				
				bufferedInputStream.close();
				bufferedInputStream = null;
			}
			
			sourceZipFile.close();
			
			return returnMap;
		}
		finally {
			if (bufferedInputStream != null) {
				try {
					bufferedInputStream.close();
				}
				catch (Exception e) {
				}
				bufferedInputStream = null;
			}
			
			if (byteArrayOutputStream != null) {
				try {
					byteArrayOutputStream.close();
				}
				catch (Exception e) {
				}
				byteArrayOutputStream = null;
			}
			
			if (sourceZipFile != null) {
				try {
					sourceZipFile.close();
				}
				catch (Exception e) {
				}
				sourceZipFile = null;
			}
		}
	}
}
