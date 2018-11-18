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
import org.sirenia.model.Form;
import org.sirenia.model.NestConf;
import org.sirenia.model.NestNode;
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
	private static final char labelSeperator = '$';
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
	public Form queryForForm(String sql,JSONObject params){
		ParsedSql parsedSql = null;
		if(params!=null){
			//设置参数
			parsedSql = SqlUtils.parseSql(sql,params);
			sql = parsedSql.getSql();
		}
		try (PreparedStatement ps = conn.prepareStatement(sql);) {
			logger.debug("sql=>{}",sql);
			String paramValues = null;
			if(params!=null){
				List<String> names = parsedSql.getParamNames();
				for(int i=0;i<names.size();i++){
					ps.setObject(i+1, params.get(names.get(i)));
				}
				paramValues = names.stream().map(name->params.get(name)).collect(Collectors.toList()).toString();
			}
			logger.debug("params=>{}",paramValues);
			ResultSet rs = ps.executeQuery();
			try{
				ResultSetMetaData rsmd = rs.getMetaData();
				int cc = rsmd.getColumnCount();
				List<String> props = new ArrayList<>(cc);
				List<String> tables = new ArrayList<>(cc);
				List<String> columns = new ArrayList<>(cc);
				for (int i = 1; i <= cc; i++) {
					String label = rsmd.getColumnLabel(i).toLowerCase();
					String tableName = null;
					int sepIndex = label.indexOf(labelSeperator);
					if(sepIndex>0){
						tableName = label.substring(0, sepIndex);
						label = label.substring(sepIndex+1);
					}else{
						tableName = rsmd.getTableName(i).toLowerCase();
					}
					String propName = Propertys.underline2camel(label);
					props.add(propName);
					tables.add(tableName);
					columns.add(label);
				}
				Form form = new Form();
				List<List<Object>> valuesArray = new ArrayList<>();
				while (rs.next()) {
					List<Object> values = new ArrayList<>(cc);
					for (int i = 1; i <= cc; i++) {
						Object value = rs.getObject(i);
						values.add(value);
					}
					valuesArray.add(values);
				}
				form.setColumns(columns);
				form.setProps(props);
				form.setTables(tables);
				form.setValuesArray(valuesArray);
				return form;
			}finally{
				rs.close();
			}
		} catch (Exception e) {
			logger.error("",e);
			throw new RuntimeException(e);
		}
	}
	/**
	 * 用户可以根据字符串构建nestConf，也可以通过反射根据class构建nestConf。
	 * 建议使用字符串方式，使用反射方式局限性大。
	 * @param sql
	 * @param params
	 * @param nestConf
	 * @return
	 */
	public JSONArray queryWithNest(String sql,JSONObject params,NestConf nestConf){
		JSONArray res = new JSONArray();
		Form form = queryForForm(sql,params);
		List<List<Object>> valuesArray = form.getValuesArray();
		if(valuesArray.isEmpty()){
			return res;
		}
		Map<String,List<Integer>> columnIndexsMap = new HashMap<>();//每个表的字段对应哪些下标
		List<String> tables = form.getTables();
		NestNode rootNode = nestConf.getRoot();
		Map<String,NestNode> nodeMap = nestConf.getMap();
		List<String> columns = form.getColumns();
		
		Map<String,Integer> collectKeyIndexs = new HashMap<>();//每个表的rowKey对应的下标 table+rowKeyValue->rowKeyColumnIndex
		Set<Entry<String,NestNode>> entrySet = nodeMap.entrySet();
		for(Entry<String,NestNode> entry : entrySet){
			NestNode node = entry.getValue();
			String rowKey = node.getRowKey();
			if(rowKey!=null){
				String tableRowKey = getTableRowKey(entry.getKey(), rowKey);
				collectKeyIndexs.put(tableRowKey,-1);
			}
		}
		for(int i=0;i<tables.size();i++){
			String table = tables.get(i).toString();
			List<Integer> columnIndexs = columnIndexsMap.get(table);
			if(columnIndexs==null){
				columnIndexs = new ArrayList<>();
				columnIndexsMap.put(table, columnIndexs);
			}
			columnIndexs.add(i);
			//
			String column = columns.get(i).toString();
			String possibleTableRowKey = getTableRowKey(table,column);
			if(collectKeyIndexs.containsKey(possibleTableRowKey)){
				collectKeyIndexs.put(possibleTableRowKey, i);
			}
		}
		Map<String,JSONObject> collected = new HashMap<>();
		for(int i=0;i<valuesArray.size();i++){
			//遍历树，深度优先遍历
			List<Object> values = valuesArray.get(i);
			JSONObject nestRes = generateNestResult(form,values,columnIndexsMap,null,rootNode,collected,collectKeyIndexs);
			if(nestRes!=null){//如果驱动表非重复的行
				res.add(nestRes);
			}
		}
		return res;
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
	public JSONArray queryWithNest(String sql,JSONObject params,List<String> config){
		NestConf nestConf = NestConf.parseNestConfig(config);
		return queryWithNest(sql, params, nestConf);
	}
	private String getTableRowKey(String table,String rowKey){
		return table+"."+rowKey;
	}
	private String getDataKey(String table,Object rowKeyValue){
		return table+"."+rowKeyValue;
	}
	private JSONObject generateNestResult(Form form, List<Object> values,
			Map<String, List<Integer>> columnIndexsMap, JSONObject parentNode,NestNode node,Map<String,JSONObject> collected, Map<String, Integer> collectKeyIndexs) {
		String table = node.getTable();
		String rowKey = node.getRowKey();
		String tableRowKey = getTableRowKey(table,rowKey);
		int collectKeyIndex = collectKeyIndexs.get(tableRowKey);
		if(collectKeyIndex<0){
			return null;
		}
		Object rowKeyValue = values.get(collectKeyIndex);
		if(rowKeyValue==null){
			return null;
		}
		String collectedKey =getDataKey(table,rowKeyValue);
		JSONObject res = collected.get(collectedKey);
		boolean rowExists = res!=null;
		if(!rowExists){
			res = new JSONObject();
			List<Integer> columnIndexs = columnIndexsMap.get(table);
			List<String> props = form.getProps();
			for(int i=0;i<columnIndexs.size();i++){
				int columIndex = columnIndexs.get(i);
				String prop = props.get(columIndex);
				Object value = values.get(columIndex);
				res.put(prop, value);
			}
			collected.put(collectedKey, res);
		}
		List<NestNode> children = node.getChildren();
		if(children!=null){
			for(int i=0;i<children.size();i++){
				NestNode childNode = children.get(i);
				String parentProp = childNode.getParentProp();
				JSONObject child = generateNestResult(form,values,columnIndexsMap,res,childNode,collected,collectKeyIndexs);
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
		List<String> config = new ArrayList<>();
		config.add("t1:id,t2:orderList:id,t4:itemList");
		config.add("t1:id,t3:addr");
		NestConf res = NestConf.parseNestConfig(config);
		System.out.println(JSONObject.toJSONString(res,true));
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
			logger.debug("sql=>{}",sql);
			String paramValues = null;
			if(params!=null){
				List<String> names = ParsedSql.getParamNames();
				for(int i=0;i<names.size();i++){
					ps.setObject(i+1, params.get(names.get(i)));
				}
				paramValues = names.stream().map(name->params.get(name)).collect(Collectors.toList()).toString();
			}
			logger.debug("params=>{}",paramValues);
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
			logger.debug("sql=>{}",sql);
			String values = null;
			if(params!=null){
				List<String> names = parsedSql.getParamNames();
				for(int i=0;i<names.size();i++){
					ps.setObject(i+1, params.get(names.get(i)));
				}
				values = names.stream().map(name->params.get(name)).collect(Collectors.toList()).toString();
			}
			logger.debug("params=>{}",values);
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
