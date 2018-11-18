package org.sirenia.model;

import java.util.List;

public class ParsedSql {
	private String sql;
	private List<String> paramNames;
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	public List<String> getParamNames() {
		return paramNames;
	}
	public void setParamNames(List<String> paramNames) {
		this.paramNames = paramNames;
	}
}
