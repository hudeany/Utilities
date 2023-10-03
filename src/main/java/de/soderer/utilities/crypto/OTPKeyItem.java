package de.soderer.utilities.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TypedListener;

import de.soderer.utilities.BitUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.swt.ArrangementStyle;
import de.soderer.utilities.swt.LabeledFileText;
import de.soderer.utilities.swt.SwtUtilities;
import de.soderer.utilities.xml.XmlUtilities;

// TODO check key id duplicates
public class OTPKeyItem extends Composite {
	public static final int EOF = -1;

	protected LabeledFileText textField;
	protected int id = 0;
	private InputStream inputStream;
	private OutputStream outputStream;
	private final String version = "1.0";
	private String hashmethod = "SHA-512";
	private long keyLength;
	private long keyReadPosition;
	private String keysetID;
	private int keyAmount;
	private String dataHash;
	private String keyHash;
	private MessageDigest messageDigest;

	public OTPKeyItem(final Composite parent, final int style, final String labelText, final int id, final boolean showRemoveButton) throws Exception {
		super(parent, style | SWT.BORDER);

		final OTPKeyItem item = this;
		final Composite parentItem = parent;

		this.id = id;

		setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

		final Label keyFileLabel = new Label(this, SWT.NONE);
		keyFileLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		keyFileLabel.setText(labelText + (id > 0 ? " " + id : ""));

		textField = new LabeledFileText(getShell(), this, SWT.NONE, null, labelText, false, true, ArrangementStyle.Horizontal);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, showRemoveButton ? 1 : 2, 1));
		textField.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent arg0) {
				final Event event = new Event();
				event.widget = item;
				item.notifyListeners(SWT.Modify, event);
			}
		});

		if (showRemoveButton) {
			final Button removeButton = new Button(this, SWT.PUSH);
			removeButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
			removeButton.setText("X");
			removeButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					item.dispose();
					final int width = parentItem.getSize().x;
					parentItem.pack();
					parentItem.setSize(width, parentItem.getSize().y);
				}
			});
		}
	}

	public void setID(final int id) {
		this.id = id;
	}

	public String getHashmethod() {
		return hashmethod;
	}

	public int getKeyAmount() {
		return keyAmount;
	}

	public String getDataHash() {
		return dataHash;
	}

	public void setDataHash(final String dataHash) {
		this.dataHash = dataHash;
	}

	public void addModifyListener(final ModifyListener listener) throws Exception {
		checkWidget();
		if (listener == null) {
			throw new Exception("Missing listener");
		} else {
			final TypedListener typedListener = new TypedListener(listener);
			addListener(SWT.Modify, typedListener);
		}
	}

	public int getID() {
		return id;
	}

	public String getText() {
		return textField.getText();
	}

	public void setText(final String text) {
		textField.setText(text);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		textField.setEnabled(enabled);
	}

	public void openAndInitKeyInputStream() throws Exception {
		keyReadPosition = 0;
		inputStream = new FileInputStream(new File(getText()));

		final String leadIn = readLeadIn();
		if (!"<otp>".equals(leadIn)) {
			throw new Exception("invalid otp header");
		}
		final String keyVersion = XmlUtilities.removeFrameTag(readNextHeaderPart());
		if (!keyVersion.equals(keyVersion)) {
			throw new Exception("invalid otp header");
		}
		keysetID = XmlUtilities.removeFrameTag(readNextHeaderPart());
		if (Utilities.isBlank(keysetID)) {
			throw new Exception("invalid otp header");
		}
		final String keyAmountString = XmlUtilities.removeFrameTag(readNextHeaderPart());
		if (Utilities.isBlank(keyAmountString)) {
			throw new Exception("invalid otp header");
		} else {
			keyAmount = Integer.parseInt(keyAmountString);
		}
		final String keyIdString = XmlUtilities.removeFrameTag(readNextHeaderPart());
		if (Utilities.isBlank(keyIdString)) {
			throw new Exception("invalid otp header");
		} else {
			id = Integer.parseInt(keyIdString);
		}
		final String keyLengthString = XmlUtilities.removeFrameTag(readNextHeaderPart());
		if (Utilities.isBlank(keyLengthString)) {
			throw new Exception("invalid otp header");
		} else {
			keyLength = Integer.parseInt(keyLengthString);
		}
		hashmethod = XmlUtilities.removeFrameTag(readNextHeaderPart());
		if (Utilities.isBlank(hashmethod)) {
			throw new Exception("invalid otp header");
		}
		messageDigest = MessageDigest.getInstance(getHashmethod());
	}

	public void openAndInitKeyOutputStream(final String keysetIdToUse, final int keyAmountToUse, final long keyLengthToUse) throws Exception {
		keysetID = keysetIdToUse;
		keyAmount = keyAmountToUse;
		keyLength = keyLengthToUse;

		outputStream = new FileOutputStream(new File(getText()));
		final StringBuilder keyHead = new StringBuilder("<otp>");
		keyHead.append("<version>" + version + "</version>");
		keyHead.append("<keyset>" + keysetIdToUse + "</keyset>");
		keyHead.append("<keyamount>" + keyAmountToUse + "</keyamount>");
		keyHead.append("<keyid>" + id + "</keyid>");
		keyHead.append("<keylength>" + keyLengthToUse + "</keylength>");
		keyHead.append("<hashmethod>" + hashmethod + "</hashmethod>");
		outputStream.write(keyHead.toString().getBytes(StandardCharsets.UTF_8));
		messageDigest = MessageDigest.getInstance(getHashmethod());
	}

	public void finalizeKey() throws Exception {
		if (outputStream != null) {
			final StringBuilder keyTail = new StringBuilder();
			keyTail.append("<datahash>" + dataHash + "</datahash>");
			keyHash = BitUtilities.toHexString(messageDigest.digest());
			keyTail.append("<keyhash>" + keyHash + "</keyhash>");
			keyTail.append("</otp>");
			outputStream.write(keyTail.toString().getBytes(StandardCharsets.UTF_8));
		}
		Utilities.closeQuietly(outputStream);
		Utilities.closeQuietly(inputStream);
	}

	public void write(final byte[] data, final int offset, final int length) throws IOException {
		outputStream.write(data, offset, length);
		messageDigest.update(data, offset, length);
	}

	public int read(final byte[] data) throws Exception {
		final int readSize = inputStream.read(data);
		keyReadPosition += readSize;
		if (readSize == EOF) {
			return EOF;
		} else if (keyReadPosition > keyLength) {
			final int restDataLength = (int)(readSize - (keyReadPosition - keyLength));
			try {
				final byte[] tailData = new byte[4096];
				System.arraycopy(data, restDataLength, tailData, 0, readSize - restDataLength);
				inputStream.read(tailData, readSize - restDataLength, 4096 - (readSize - restDataLength));
				final String tail = new String(tailData, StandardCharsets.UTF_8);
				dataHash = XmlUtilities.removeFrameTag(tail.substring(0, tail.indexOf("</datahash>") + 11));
				keyHash = XmlUtilities.removeFrameTag(tail.substring(tail.indexOf("</datahash>") + 11, tail.indexOf("</keyhash>") + 10));
			} catch (final Exception e) {
				throw new Exception("invalid otp tail", e);
			}
			messageDigest.update(data, 0, restDataLength);
			final String keyHashRead = BitUtilities.toHexString(messageDigest.digest());
			if (Utilities.isBlank(keyHash) || !keyHash.equals(keyHashRead)) {
				throw new Exception("invalid key hash in key " + id);
			}
			return restDataLength;
		} else {
			messageDigest.update(data, 0, readSize);
			return readSize;
		}
	}

	public long available() {
		return keyLength;
	}

	private String readLeadIn() throws IOException {
		final int maxPartSize = 100;
		final StringBuilder dataString = new StringBuilder();
		int dataChar = inputStream.read();
		while (dataChar != EOF && (char) dataChar != '>' && dataString.length() < maxPartSize) {
			dataString.append((char) dataChar);
			dataChar = inputStream.read();
		}
		if (dataString.length() == maxPartSize) {
			return null;
		} else if ((char) dataChar == '>') {
			dataString.append((char) dataChar);
			return dataString.toString();
		} else {
			return dataString.toString();
		}
	}

	private String readNextHeaderPart() throws IOException {
		final int maxPartSize = 100;
		final StringBuilder dataString = new StringBuilder();
		int dataChar = inputStream.read();
		while (dataChar != EOF && (char) dataChar != '>' && dataString.length() < maxPartSize) {
			dataString.append((char) dataChar);
			dataChar = inputStream.read();
		}
		if (dataString.length() == maxPartSize) {
			return null;
		} else if ((char) dataChar == '>') {
			dataString.append((char) dataChar);
		} else {
			return null;
		}
		dataChar = inputStream.read();
		while (dataChar != EOF && (char) dataChar != '>' && dataString.length() < maxPartSize) {
			dataString.append((char) dataChar);
			dataChar = inputStream.read();
		}
		if (dataString.length() == maxPartSize) {
			return null;
		} else if ((char) dataChar == '>') {
			dataString.append((char) dataChar);
			return dataString.toString();
		} else {
			return null;
		}
	}
}
