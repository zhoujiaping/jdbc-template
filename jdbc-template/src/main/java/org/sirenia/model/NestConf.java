package org.sirenia.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

public class NestConf {
	private static final Logger logger = LoggerFactory.getLogger(NestConf.class);

	
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
	/**
	 * @param config   表名:主键:父节点的属性名
	 * （表名也可以使用别名，如果支持的话。主键可以是其他能标识一条记录的字段名。根节点没有父节点，不用写属性名）
	 * t1:id,t2:id:orderList,t4:item_id:itemList
	 * t1:id,t3:addr_id:addr
	 * @return JSONObject
	 * it is a tree
	 */
	public static NestConf parseNestConfig(List<String> config) {
		NestConf res = null;
		if(config!=null && !config.isEmpty()){
			res = new NestConf();
			NestNode root = null;
			Map<String,NestNode> map = new HashMap<>();
			for(int i=0;i<config.size();i++){
				NestNode parent = root;
				String line = config.get(i).replaceAll("\\s", "");
				if(line.equals("")){
					continue;
				}
				String[] tableDescriptions = line.split(",");
				for(int j=0;j<tableDescriptions.length;j++){
					String[] tableDescription = tableDescriptions[j].split(":");
					String table = tableDescription[0];
					if(map.containsKey(table)){
						continue;
					}
					String prop = null;
					String rowKey = null;
					if(tableDescription.length==2){
						rowKey = tableDescription[1];
					}else{
						rowKey = tableDescription[1];
						prop = tableDescription[2];
					}
					NestNode node = new NestNode();
					if(root==null){
						parent = root = node;
					}else{
						List<NestNode> children = parent.getChildren();
						if(children==null){
							children = new ArrayList<>();
							parent.setChildren(children);
						}
						children.add(node);
						node.setParentNode(parent);
						node.setParentProp(prop);
					}
					node.setTable(table);
					node.setRowKey(rowKey);
					map.put(table, node);//节点标记为已处理
					parent = node;//下一个节点的父节点
				}
			}
			res.setMap(map);
			res.setRoot(root);
		}
		logger.debug(JSONObject.toJSONString(res,true));
		return res;
	}
}
