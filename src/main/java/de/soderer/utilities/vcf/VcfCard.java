package de.soderer.utilities.vcf;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VcfCard {
	private String lastName = null;
	private String firstName = null;
	private String additionalFirstName = null;
	private String namePrefix = null;
	private String nameSuffix = null;
	private String formattedName = null;
	private List<String> organization = null;
	private String role = null;
	private String title = null;
	private String photoUrl = null;
	private byte[] photoData = null;
	private Date latestUpdate = null;
	private String url = null;
	private String note = null;
	private Date birthday = null;
	private boolean birthdayWithoutYear = false;
	private final List<VcfAttributedValue> telephoneNumbers = new ArrayList<>();
	private final List<VcfAttributedAddress> adresses = new ArrayList<>();
	private final List<VcfAttributedValue> emails = new ArrayList<>();

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public String getAdditionalFirstName() {
		return additionalFirstName;
	}

	public void setAdditionalFirstName(final String additionalFirstName) {
		this.additionalFirstName = additionalFirstName;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(final String namePrefix) {
		this.namePrefix = namePrefix;
	}

	public String getNameSuffix() {
		return nameSuffix;
	}

	public void setNameSuffix(final String nameSuffix) {
		this.nameSuffix = nameSuffix;
	}

	public String getFormattedName() {
		return formattedName;
	}

	public void setFormattedName(final String formattedName) {
		this.formattedName = formattedName;
	}

	public List<String> getOrganization() {
		return organization;
	}

	public void setOrganization(final List<String> organization) {
		this.organization = organization;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void setPhotoUrl(final String photoUrl) {
		this.photoUrl = photoUrl;
	}

	public byte[] getPhotoData() {
		return photoData;
	}

	public void setPhotoData(final byte[] photoData) {
		this.photoData = photoData;
	}

	public void addTelephoneNumber(final VcfAttributedValue telephoneNumber) {
		telephoneNumbers.add(telephoneNumber);
	}

	public List<VcfAttributedValue> getTelephoneNumbers() {
		return telephoneNumbers;
	}

	public void addAddress(final VcfAttributedAddress adress) {
		adresses.add(adress);
	}

	public List<VcfAttributedAddress> getAdresses() {
		return adresses;
	}

	public void addEmail(final VcfAttributedValue email) {
		emails.add(email);
	}

	public List<VcfAttributedValue> getEmails() {
		return emails;
	}

	public Date getLatestUpdate() {
		return latestUpdate;
	}

	public void setLatestUpdate(final Date latestUpdate) {
		this.latestUpdate = latestUpdate;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public Date getBirthday() {
		return birthday;
	}

	public boolean isBirthdayWithoutYear() {
		return birthdayWithoutYear;
	}

	public void setBirthday(final Date birthday) {
		this.birthday = birthday;
	}

	public void setBirthday(final Date birthday, final boolean birthdayWithoutYear) {
		this.birthday = birthday;
		this.birthdayWithoutYear = birthdayWithoutYear;
	}
}
