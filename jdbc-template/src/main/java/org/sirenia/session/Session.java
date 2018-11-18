package org.sirenia.session;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.sirenia.model.DbMetaData;
import org.sirenia.model.ParsedSql;
import org.sirenia.util.Propertys;
import org.sirenia.util.SqlUtils;
import org.sirenia.util.page.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class Session {
	private static final Logger logger = LoggerFactory.getLogger(Session.class);
	//缓存表名的主键名
	private static Map<String,String> tablePkColumnNameMapper = new ConcurrentHashMap<>();
	private DbMetaData dbMetaData = new DbMetaData();
	private static final JSONObject columnsMetaData = new JSONObject();
	private Connection conn;
	private boolean closed;
	public Session(Connection conn){
		this.conn = conn;
	}
	public void loadColumnsMetaData(String schema){
		String sql = "select * from information_schema.columns where table_schema=#{schema};";
		JSONObject params = new JSONObject();
		params.put("schema", schema);
		JSONArray metadata = query(sql,params );
		columnsMetaData.put(schema, metadata);
	}
	public JSONArray getColumnsMetaData(String schema) {
		if(schema==null){
			schema = getDbMetaData().getCatalog();
		}
		if(columnsMetaData.containsKey(schema)){
			return columnsMetaData.getJSONArray(schema);
		}
		loadColumnsMetaData(schema);
		return columnsMetaData.getJSONArray(schema);
	}
	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	/**
	 * 获取已存在的连接
	 * 
	 * @return
	 */
	public Connection getConn() {
		return conn;
	}
	public void clearConn(){
		this.conn=null;
	}
	/**
	 * 执行查询
	 * @param sql
	 * @return
	 */
	public JSONArray query(String sql) {
		return query(sql,null);
	}
	public void loadDbMetaData(){
		try{
			DatabaseMetaData md = conn.getMetaData();
			//String schema = conn.getSchema();
			String catalog = conn.getCatalog();
			//dbmetadata.put("schema", schema);
			dbMetaData.setCatalog(catalog);
			String databaseProductName = md.getDatabaseProductName();// 获取数据库名：MySQL
			dbMetaData.setDatabaseProductName(databaseProductName);
		}catch(Exception e){
			logger.error("加载数据库元数据异常",e);
			throw new RuntimeException();
		}
	}
	public DbMetaData getDbMetaData(){
		if(dbMetaData.getCatalog()!=null){
			return dbMetaData;
		}
		loadDbMetaData();
		return dbMetaData;
	}
	/**
	 * @param sql
	 * @param params
	 * @return
	 */
	public JSONObject queryWithForm(String sql,JSONObject params){
		ParsedSql parsedSql = null;
		if(params!=null){
			//设置参数
			parsedSql = SqlUtils.parseSql(sql,params);
			sql = parsedSql.getSql();
		}
		try (PreparedStatement ps = conn.prepareStatement(sql);) {
			logger.info("sql=>{}",sql);
			String paramValues = null;
			if(params!=null){
				List<String> names = parsedSql.getParamNames();
				for(int i=0;i<names.size();i++){
					ps.setObject(i+1, params.get(names.get(i)));
				}
				paramValues = names.stream().map(name->params.get(name)).collect(Collectors.toList()).toString();
			}
			logger.info("params=>{}",paramValues);
			ResultSet rs = ps.executeQuery();
			try{
				ResultSetMetaData rsmd = rs.getMetaData();
				int cc = rsmd.getColumnCount();
				JSONArray props = new JSONArray(cc);
				JSONArray tables = new JSONArray(cc);
				JSONArray columns = new JSONArray(cc);
				for (int i = 1; i <= cc; i++) {
					String label = rsmd.getColumnLabel(i);
					String tableName = null;
					int sepIndex = label.indexOf('$');
					if(sepIndex>0){
						tableName = label.substring(0, sepIndex);
						label = label.substring(sepIndex+1);
					}else{
						tableName = rsmd.getTableName(i);
					}
					String propName = Propertys.underline2camel(label);
					props.add(propName);
					tables.add(tableName);
					columns.add(label);
				}
				JSONObject res = new JSONObject();
				JSONArray valuesArray = new JSONArray();
				while (rs.next()) {
					JSONArray values = new JSONArray();
					for (int i = 1; i <= cc; i++) {
						Object value = rs.getObject(i);
						values.add(value);
					}
					valuesArray.add(values);
				}
				res.put("columns", columns);
				res.put("props", props);
				res.put("tables", tables);
				res.put("valuesArray", valuesArray);
				return res;
			}finally{
				rs.close();
			}
		} catch (Exception e) {
			logger.error("",e);
			throw new RuntimeException(e);
		}
	}
	/**
	 * 支持一对一、一对多、多对多、多对一查询。
	 * @param sql
	 * @param params
	 * @param config   表名:主键:父节点的属性名
	 * （表名也可以使用别名，如果支持的话。主键可以是其他能标识一条记录的字段名。根节点没有父节点，不用写属性名）
	 * t1:id,t2:id:orderList,t4:item_id:itemList
	 * t1:id,t3:addr_id:addr
	 * 如果您使用mysql，在连接数据库的url加上useOldAliasMetaDataBehavior=true，则可以使用表的别名。
	 * 如果您使用postgresql，由于其不支持别名，所以必须使用表名。这样在表的自连接的情况就无法使用该方法实现嵌套了。
	 * pg自连接查询的解决办法：将自连接的另一个表的字段别名命名为"新表名$列别名"
	 * 此外，该方法还有个缺陷，就是如果某一列通过计算得到或者不是从某个表查出，那么将不会计入结果。
	 * 这个可以通过将列的别名命名为"表名$列别名"方式解决。
	 * @return
	 */
	public JSONArray queryWithNest(String sql,JSONObject params,JSONArray config){
		JSONObject formRes = queryWithForm(sql,params);
		JSONArray valuesArray = formRes.getJSONArray("valuesArray");
		Map<String,List<Integer>> columnIndexsMap = new HashMap<>();//每个表的字段对应哪些下标
		JSONArray tables = formRes.getJSONArray("tables");
		JSONObject conf = parseNestConfig(config);
		JSONObject rootNode = conf.getJSONObject("root");
		JSONObject nodeMap = conf.getJSONObject("map");
		JSONArray columns = formRes.getJSONArray("columns");
		
		Map<String,Integer> collectKeyIndexs = new HashMap<>();//每个表的rowKey对应的下标 table+rowKeyValue->rowKeyColumnIndex
		Set<Entry<String,Object>> entrySet = nodeMap.entrySet();
		for(Entry<String,Object> entry : entrySet){
			JSONObject node = (JSONObject) entry.getValue();
			String rowKey = node.getString("rowKey");
			if(rowKey!=null){
				collectKeyIndexs.put(entry.getKey()+"."+rowKey,-1);
			}
		}
		for(int i=0;i<tables.size();i++){
			String table = tables.getString(i);
			List<Integer> columnIndexs = columnIndexsMap.get(table);
			if(columnIndexs==null){
				columnIndexs = new ArrayList<>();
				columnIndexsMap.put(table, columnIndexs);
			}
			columnIndexs.add(i);
			//
			String column = columns.getString(i);
			String possiblerowKey = table+"."+column;
			if(collectKeyIndexs.containsKey(possiblerowKey)){
				collectKeyIndexs.put(possiblerowKey, i);
			}
		}
		JSONArray res = new JSONArray();
		JSONObject collected = new JSONObject();
		for(int i=0;i<valuesArray.size();i++){
			//遍历树，深度优先遍历
			JSONArray values = valuesArray.getJSONArray(i);
			JSONObject nestRes = generateNestResult(formRes,values,columnIndexsMap,null,rootNode,collected,collectKeyIndexs);
			if(nestRes!=null){//如果驱动表非重复的行
				res.add(nestRes);
			}
		}
		return res;
	}
	private JSONObject generateNestResult(JSONObject formRes, JSONArray values,
			Map<String, List<Integer>> columnIndexsMap, JSONObject parentNode,JSONObject node,JSONObject collected, Map<String, Integer> collectKeyIndexs) {
		String table = node.getString("table");
		String rowKey = node.getString("rowKey");
		int collectKeyIndex = collectKeyIndexs.get(table+"."+rowKey);
		if(collectKeyIndex<0){
			return null;
		}
		Object rowKeyValue = values.get(collectKeyIndex);
		if(rowKeyValue==null){
			return null;
		}
		String collectedKey = table + "." + rowKeyValue;
		JSONObject res = collected.getJSONObject(collectedKey);
		boolean rowExists = res!=null;
		if(!rowExists){
			res = new JSONObject();
			List<Integer> columnIndexs = columnIndexsMap.get(table);
			JSONArray props = formRes.getJSONArray("props");
			for(int i=0;i<columnIndexs.size();i++){
				int columIndex = columnIndexs.get(i);
				String prop = props.getString(columIndex);
				Object value = values.get(columIndex);
				res.put(prop, value);
			}
			collected.put(collectedKey, res);
		}
		JSONArray children = node.getJSONArray("children");
		if(children!=null){
			for(int i=0;i<children.size();i++){
				JSONObject childNode = children.getJSONObject(i);
				String parentProp = childNode.getString("parentProp");
				JSONObject child = generateNestResult(formRes,values,columnIndexsMap,res,childNode,collected,collectKeyIndexs);
				if(parentProp.endsWith("List")){
					JSONArray list = res.getJSONArray(parentProp);
					if(list == null){
						list = new JSONArray();
						res.put(parentProp, list);
					}
					if(child!=null){
						list.add(child);
					}
				}else{
					if(child!=null){
						res.put(parentProp, child);
					}
				}
			}
		}
		if(rowExists){
			return null;
		}else{
			return res;
		}
	}
	public static void main(String[] args) {
		JSONArray config = new JSONArray();
		config.add("t1:id,t2:orderList:id,t4:itemList");
		config.add("t1:id,t3:addr");
		JSONObject res = parseNestConfig(config);
		System.out.println(JSONObject.toJSONString(res,true));
	}
	/**
	 * @param config   表名:主键:父节点的属性名
	 * （表名也可以使用别名，如果支持的话。主键可以是其他能标识一条记录的字段名。根节点没有父节点，不用写属性名）
	 * t1:id,t2:id:orderList,t4:item_id:itemList
	 * t1:id,t3:addr_id:addr
	 * @return JSONObject
	 * it is a tree
	 */
	private static JSONObject parseNestConfig(JSONArray config) {
		JSONObject res = null;
		if(config!=null && !config.isEmpty()){
			res = new JSONObject();
			JSONObject root = null;
			JSONObject map = new JSONObject();
			for(int i=0;i<config.size();i++){
				JSONObject parent = root;
				String line = config.getString(i).replaceAll("\\s", "");
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
					JSONObject node = new JSONObject();
					if(root==null){
						parent = root = node;
					}else{
						JSONArray children = parent.getJSONArray("children");
						if(children==null){
							children = new JSONArray();
							parent.put("children", children);
						}
						children.add(node);
						node.put("parentNode", parent);
						node.put("parentProp", prop);
					}
					node.put("table", table);
					node.put("rowKey", rowKey);
					map.put(table, node);//节点标记为已处理
					parent = node;//下一个节点的父节点
				}
			}
			res.put("root", root);
			res.put("map", map);
		}
		logger.info(JSONObject.toJSONString(res,true));
		return res;
	}
	/**
	 * 执行查询，sql支持#{}和${}占位符功能
	 * @param sql
	 * @param params
	 * @return
	 */
	public JSONArray query(String sql,JSONObject params) {
		ParsedSql ParsedSql = null;
		if(params!=null){
			//设置参数
			ParsedSql = SqlUtils.parseSql(sql,params);
			sql = ParsedSql.getSql();
		}
		try (PreparedStatement ps = conn.prepareStatement(sql);) {
			logger.info("sql=>{}",sql);
			String paramValues = null;
			if(params!=null){
				List<String> names = ParsedSql.getParamNames();
				for(int i=0;i<names.size();i++){
					ps.setObject(i+1, params.get(names.get(i)));
				}
				paramValues = names.stream().map(name->params.get(name)).collect(Collectors.toList()).toString();
			}
			logger.info("params=>{}",paramValues);
			ResultSet rs = ps.executeQuery();
			try{
				JSONArray list = collectResultSet(rs);
				return list;
			}finally{
				rs.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static JSONArray collectResultSet(ResultSet rs) {
		try{
			ResultSetMetaData rsmd = rs.getMetaData();
			int cc = rsmd.getColumnCount();
			JSONArray propNames = new JSONArray(cc);
			for (int i = 1; i <= cc; i++) {
				String label = rsmd.getColumnLabel(i);
				String propName = Propertys.underline2camel(label);
				propNames.add(propName);
			}
			JSONArray rows = new JSONArray();
			while (rs.next()) {
				JSONObject row = new JSONObject();
				for (int i = 1; i <= cc; i++) {
					String propName = propNames.getString(i);
					Object value = rs.getObject(propName);
					row.put(propName, value);
				}
				rows.add(row);
			}
			return rows;
		}catch(Exception e){
			throw new RuntimeException(e);
		}finally{
			try {
				rs.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
	public JSONObject queryWithPage(String sql,JSONObject params,Pageable pageable) {
		return queryWithPage(sql,null,params,pageable);
	}
	public JSONObject queryWithPage(String sql,String countSql,JSONObject params,Pageable pageable) {
		String dialect = getDbMetaData().getDatabaseProductName().toLowerCase();
		String pageSql = pageable.createPageSql(sql, dialect);
		JSONArray rows = query(pageSql,params);
		if(countSql == null){
			countSql = "select count(1) count from ("+sql+") t1";
		}
		JSONObject totalWraper = queryOne(countSql,params);
		JSONObject res = new JSONObject();
		res.put("total", totalWraper.getIntValue("count"));
		res.put("rows", rows);
		return res;
	}
	public JSONObject queryOne(String sql) {
		return queryOne(sql,null);
	}
	public JSONObject queryOne(String sql,JSONObject params) {
		JSONArray array = query(sql,params);
		if(array.size()>1){
			throw new RuntimeException("queryOne返回了【"+array.size()+"】条结果");
		}else if(array.isEmpty()){
			return null;
		}else{
			return array.getJSONObject(0);
		}
	}
	/**
	 * 执行更新
	 * @param sql
	 * @return
	 */
	public int update(String sql) {
		return update(sql,null);
	}
	/**
	 * 执行更新
	 * @param sql
	 * @return
	 */
	public int update(String sql,JSONObject params) {
		ParsedSql parsedSql = null;
		if(params!=null){
			//设置参数
			parsedSql = SqlUtils.parseSql(sql,params);
			sql = parsedSql.getSql();
		}
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			logger.info("sql=>{}",sql);
			String values = null;
			if(params!=null){
				List<String> names = parsedSql.getParamNames();
				for(int i=0;i<names.size();i++){
					ps.setObject(i+1, params.get(names.get(i)));
				}
				values = names.stream().map(name->params.get(name)).collect(Collectors.toList()).toString();
			}
			logger.info("params=>{}",values);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	public JSONArray getColumnsMetaData(String schema, String table) {
		JSONArray columnsMetaData = getColumnsMetaData(schema);
		if(table==null){
			return columnsMetaData;
		}
		JSONArray res = new JSONArray();
		for(int i=0;i<columnsMetaData.size();i++){
			JSONObject columnMetaData = columnsMetaData.getJSONObject(i);
			String tablename = columnMetaData.getString("tableName");
			if(tablename.equalsIgnoreCase(table)){
				res.add(columnMetaData);
			}
		}
		return res;
	}
	/**
	 * 让外部能够获取，可以自行设置
	 */
	public Map<String, String> getTablePkColumnNameMapper() {
		return tablePkColumnNameMapper;
	}
	/**
	 * 查询数据库获取表的主键
	 * @param session
	 * @param tablename
	 * @return
	 */
	public String getPkColumn(String tablename) {
		try {
			if(tablePkColumnNameMapper.containsKey(tablename)){
				return tablePkColumnNameMapper.get(tablename);
			}
			Connection connection = getConn();
			DatabaseMetaData md = connection.getMetaData();
			ResultSet mdrs = md.getPrimaryKeys(null, null, tablename);
			JSONArray list = Session.collectResultSet(mdrs);
			if(list.size()!=1){
				throw new RuntimeException("表【"+tablename+"】主键个数为【"+list.size()+"】");
			}
			String columnName = list.getJSONObject(0).getString("columnName");
			if(columnName==null || columnName.trim().equals("")){
				throw new RuntimeException("表【"+tablename+"】主键未找到");
			}
			tablePkColumnNameMapper.put(tablename, columnName);
			return columnName;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
