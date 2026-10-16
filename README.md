# soderer-utilities

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)

**General purpose helpers for Java applications: text, files, I/O, dates, collections, workers, I18N and more.**

`soderer-utilities` is the common base of the `de.soderer` libraries and applications. Its core uses only the JDK.
Packages for remote file access and special archive formats use third party libraries, which are declared as
optional dependencies — only add them if you use those packages.

---

## Contents

### Core (`de.soderer.utilities`)

| Area | Classes |
|---|---|
| **Strings and text** | `Utilities`, `TextUtilities`, `StringUtilities`, `TextTable`, `QuotedPrintableCodec`, `PunycodeCodec`, `HtmlUtilities` |
| **Numbers** | `NumberUtilities`, `ExactNumber`, `BitUtilities`, `StochasticUtilities` |
| **Dates and times** | `DateUtilities`, `DateUtilitiesForDb` |
| **Files** | `FileUtilities`, `FileComparer`, `FileType`, `WildcardFilenameFilter`, `FileChangeWatcherThread` |
| **Streams and readers** | `IoUtilities`, `LineReader`, `BOMInputStream`, `EncodingDetector`, `CountingInputStream`, `ReaderInputStream`, `TempFileInputStream`, `RecordReader`, `SqlScriptReader`, `BasicReader` |
| **Properties files** | `PropertiesReader`, `PropertiesWriter`, `SectionedProperties`, `MapStringReader` |
| **Language texts (I18N)** | `LangResources` with built-in texts in English, German, French, Spanish and Italian |
| **Data stores** | `SecureDataStore` (password encrypted file), `UnsecureDataStore` (plain JSON file) |
| **Passwords and credentials** | `PasswordGenerator`, `Credentials`, `WrongPasswordException`, `MissingPasswordException` |
| **System** | `SystemUtilities`, `VersionInfo`, `DebugUtils`, `ImageUtilities` |
| **Value types** | `Version`, `Tuple`, `Triple`, `Result`, `LRUCache` |
| **Errors** | `ExceptionUtilities`, `VisibleException`, `UserError`, `ParameterException` |

### Subpackages

| Package | Content |
|---|---|
| `collection` | `CaseInsensitiveMap`, `CaseInsensitiveLinkedMap`, `CaseInsensitiveSet`, `IndexedLinkedHashMap`, `MultiValueCaseInsensitiveOrderedMap`, `UniqueFifoQueuedList`, `MapUtilities` |
| `worker` | `WorkerSimple` / `WorkerDual`: background jobs with progress reporting, cancel support and result; ready-made workers for copying, downloading and uploading files |
| `xml` | `XmlUtilities` (parsing, XPath, formatting), `IndentedXMLStreamWriter` |
| `zip` | `ZipUtilities` (JDK), `TarGzUtilities` (commons-compress), `Zip4jUtilities` (password protected zip files, zip4j) |
| `remote` | One interface `RemoteConnection` for SFTP/SCP (JSch), FTP (commons-net), SMB 2/3 (jcifs), SMB 1 (jcifs) and HTTP directory listings |
| `textbook` | `TextBookEditor` for editing very large text files page by page, with line actions (filter, count, drop lines) |
| `code` | `ClassUtilities` (reflection), `JavaSourceFromString` (compiling Java code at runtime) |
| `jarinjarloader` | Start class for applications packed as a single jar with their library jars inside |

## Requirements

- Java 17 or newer
- [json](https://github.com/hudeany/json) (`UnsecureDataStore`, `SecureDataStore`, `JwtUtilities`)

Optional, only for the packages that use them:

| Dependency | Needed for |
|---|---|
| commons-compress | `zip.TarGzUtilities` |
| zip4j | `zip.Zip4jUtilities` |
| commons-net | `remote.FtpConnection` |
| jsch | `remote.SftpConnection` |
| jcifs | `remote.SmbConnection` |
| jcifs | `remote.Smb1Connection` |
| network | `PunycodeCodec`, `remote.HttpFileConnection` |
| proxyautoconfig | proxy support of the `remote` connections |

## Installation

### Maven

```xml
<dependency>
	<groupId>de.soderer</groupId>
	<artifactId>soderer-utilities</artifactId>
	<version>VERSION</version>
</dependency>
```

### Gradle

```groovy
implementation "de.soderer:soderer-utilities:VERSION"
```

### Plain jar

Download `soderer-utilities-VERSION.jar` from the releases page and put it on the classpath together with the json jar
(and the optional jars of the packages you use).

## Examples

### Strings and numbers

```java
Utilities.isBlank("   ");                                                   // true
Utilities.getHumanReadableNumber(1536L, "B", false, 5, false, Locale.ENGLISH); // "1.5 KiB"
DateUtilities.getShortHumanReadableTimespan(Duration.ofSeconds(3725), false, false); // "1h 2m"
```

### Versions

```java
Version current = new Version("1.9.2");
Version available = new Version("1.10.0");
if (available.compareTo(current) > 0) {
	System.out.println("Update available: " + available);
}
```

### Case-insensitive collections

```java
Map<String, String> headers = new CaseInsensitiveMap<>();
headers.put("Content-Type", "text/plain");
headers.get("content-type"); // "text/plain"
```

### Language texts

```java
LangResources.get("cancel");                 // "Abbrechen" with a German default locale
LangResources.get("errorMessage", "Timeout"); // "Error:\nTimeout"
```

Applications add their own texts with `LanguageProperties*.properties` files in the classpath root, which take
precedence over the built-in texts. Libraries can contribute their own texts via `LangResources.registerBuiltInText(...)`.

### Encrypted data store

```java
SecureDataStore store = new SecureDataStore();
store.addEntry("serverPassword", "secret");
store.save(new File("data.store"), keystorePassword);

SecureDataStore loaded = new SecureDataStore();
loaded.load(new File("data.store"), keystorePassword);
String value = (String) loaded.getEntry("serverPassword");
```

### Background worker

```java
public class CountWorker extends WorkerSimple<Long> {
	@Override
	public Long work() throws Exception {
		itemsToDo = 1000;
		for (itemsDone = 0; itemsDone < itemsToDo && !cancel; itemsDone++) {
			// do something
			signalProgress();
		}
		return itemsDone;
	}
}
```

A `WorkerParentSimple` (for example the `ProgressDialog` of soderer-swing) receives the progress signals and can
cancel the worker.

### Passwords

```java
char[] password = PasswordGenerator.generatePassword(16);
```

## Related libraries

| Library | Content |
|---|---|
| [soderer-swing](https://github.com/hudeany/Swing) | Swing dialogs and the `DropDown` autocomplete combo |
| [soderer-console](https://github.com/hudeany/Console) | Console input, menus and ANSI output |
| [soderer-applicationbase](https://github.com/hudeany/ApplicationBase) | Self-updating application base, configuration dialog |
| [csv](https://github.com/hudeany/csv), [json](https://github.com/hudeany/json), [ProxyAutoConfig](https://github.com/hudeany/ProxyAutoConfig) | Data formats and proxy configuration |

## Building

The project is built with [Apache Ant](https://ant.apache.org/). Dependencies are downloaded automatically.

```sh
ant build_jar
```

This compiles the sources, runs the tests and creates the following files in `build/`:

- `soderer-utilities-VERSION.jar`
- `soderer-utilities-VERSION-sources.jar`
- `soderer-utilities-VERSION-javadoc.jar`

The version is read from the file `build.version`.

## License

Released under the [MIT License](LICENSE.txt).
