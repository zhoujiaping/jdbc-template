package org.sirenia.model;

import java.util.Map;

public class NestConf {
	private Map<String,NestNode> map;
	private NestNode root;
	public Map<String, NestNode> getMap() {
		return map;
	}
	public void setMap(Map<String, NestNode> map) {
		this.map = map;
	}
	public NestNode getRoot() {
		return root;
	}
	public void setRoot(NestNode root) {
		this.root = root;
	}
	
}
