package org.sirenia.model;

public class DbMetaData {
	private String catalog;
	private String databaseProductName;
	
	public String getCatalog() {
		return catalog;
	}
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}
	public String getDatabaseProductName() {
		return databaseProductName;
	}
	public void setDatabaseProductName(String databaseProductName) {
		this.databaseProductName = databaseProductName;
	}
}
