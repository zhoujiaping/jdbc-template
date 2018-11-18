package org.sirenia.model;

import java.util.List;

public class NestNode {
	private List<NestNode> children;
	private NestNode parentNode;
	private String parentProp;
	private String table;
	private String rowKey;
	public List<NestNode> getChildren() {
		return children;
	}
	public void setChildren(List<NestNode> children) {
		this.children = children;
	}
	public NestNode getParentNode() {
		return parentNode;
	}
	public void setParentNode(NestNode parentNode) {
		this.parentNode = parentNode;
	}
	public String getParentProp() {
		return parentProp;
	}
	public void setParentProp(String parentProp) {
		this.parentProp = parentProp;
	}
	public String getTable() {
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	public String getRowKey() {
		return rowKey;
	}
	public void setRowKey(String rowKey) {
		this.rowKey = rowKey;
	}
	
}
