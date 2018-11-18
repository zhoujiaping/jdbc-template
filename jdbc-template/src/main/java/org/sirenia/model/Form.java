package org.sirenia.model;

import java.util.List;

public class Form {
	private List<String> columns;
	private List<String> props;
	private List<String> tables;
	private List<List<Object>> valuesArray;
	public List<String> getColumns() {
		return columns;
	}
	public void setColumns(List<String> columns) {
		this.columns = columns;
	}
	public List<String> getProps() {
		return props;
	}
	public void setProps(List<String> props) {
		this.props = props;
	}
	public List<String> getTables() {
		return tables;
	}
	public void setTables(List<String> tables) {
		this.tables = tables;
	}
	public List<List<Object>> getValuesArray() {
		return valuesArray;
	}
	public void setValuesArray(List<List<Object>> valuesArray) {
		this.valuesArray = valuesArray;
	}
	
	
}
