package org.sirenia.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sirenia.util.Callback00;
import org.sirenia.util.Callback10;
import org.sirenia.util.Callback11;
import org.sirenia.util.GenericTokenParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
/**
 * 和Session的主要区别，Session会绑定线程。
 * 用SessionFactory获取线程，除非调用了关闭Session，否则用的是同一个连接。
 * DbClient不绑定线程，一个DbClient一个连接，可以创建多个DbClient。
 * @author Administrator
 *
 */
public class DbClient {
	private static final Logger logger = LoggerFactory.getLogger(DbClient.class);
	private String username;
	private String password;
	private String url;
	private Connection conn;
	private static List<String> drivers = new ArrayList<>();
	private static boolean loadedDriver = false;
	static{
		drivers.add("org.postgresql.Driver");
		drivers.add("com.mysql.jdbc.Driver");
		//and so on
		drivers.forEach(driver->{
			try{
				Class.forName(driver);
				loadedDriver = true;
			}catch(Exception e){
				logger.info("driver not found: {}",driver);
			}
		});
		if(!loadedDriver){
			throw new RuntimeException("未提供支持的jdbc驱动");
		}
	}
	/**
	 * 获取已存在的连接
	 * @return
	 */
	public Connection getConn(){
		return conn;
	}
	/**
	 * 加载驱动
	 * @param driver
	 */
	private static void loadDriver(String driver) {
		// String driver = "com.mysql.jdbc.Driver";
		try {
			Class.forName(driver);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 私有化构造函数
	 */
	private DbClient() {
	}
	/**
	 * 通过工厂方法获取实例
	 * @param username
	 * @param password
	 * @param url
	 * @return
	 */
	public static DbClient newClient(String username, String password, String url) {
		DbClient client = new DbClient();
		client.url = url;
		client.username = username;
		client.password = password;
		return client;
	}
	/**
	 * 打开连接，执行回调，关闭连接
	 * @param callback
	 */
	public void withConn(Callback10<Connection> callback) {
		try (Connection conn = DriverManager.getConnection(url, username, password);) {
			this.conn = conn;
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			conn.setAutoCommit(false);
			callback.apply(conn);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 打开连接，执行回调，关闭连接
	 * @param callback
	 */
	public <R>R withConn(Callback11<R,Connection> callback) {
		try (Connection conn = DriverManager.getConnection(url, username, password);) {
			this.conn = conn;
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			conn.setAutoCommit(false);
			return callback.apply(conn);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 无入参版本
	 * 如果已经打开连接并且未关闭：执行回调，提交事务（异常则回滚事务）
	 * 如果已经打开连接并且已关闭：抛出异常
	 * 如果未打开连接：打开连接，执行回调，提交事务，关闭连接
	 * @param callback
	 */
	public void withTx(Callback00 callback) {
		if(this.conn==null){
			withConn(client->{
				withTxInternal(callback);
			});
		}else{
			withTxInternal(callback);
		}
	}
	/**
	 * 有入参版本
	 * 如果已经打开连接并且未关闭：执行回调，提交事务（异常则回滚事务）
	 * 如果已经打开连接并且已关闭：抛出异常
	 * 如果未打开连接：打开连接，执行回调，提交事务，关闭连接
	 * @param callback
	 */
	public void withTx(Callback10<DbClient> callback) {
		if(this.conn==null){
			withConn(client->{
				withTxInternal(callback);
			});
		}else{
			withTxInternal(callback);
		}
	}
	private void withTxInternal(Callback00 callback) {
		try {
			callback.apply();
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				throw new RuntimeException(e);
			}
			throw new RuntimeException(e);
		}
	}
	private void withTxInternal(Callback10<DbClient> callback) {
		try {
			callback.apply(this);
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				throw new RuntimeException(e);
			}
			throw new RuntimeException(e);
		}
	}
	/**
	 * 执行查询
	 * @param sql
	 * @return
	 */
	public JSONArray query(String sql) {
		return query(sql,null);
	}
	/**
	 * 执行查询，sql支持#{}和${}占位符功能
	 * @param sql
	 * @param params
	 * @return
	 */
	public JSONArray query(String sql,JSONObject params) {
		JSONObject parseRes = null;
		if(params!=null){
			//设置参数
			parseRes = parseSql(sql,params);
			sql = parseRes.getString("sql");
		}
		logger.info("sql=>{}",sql);
		logger.info("params=>{}",params);
		try (PreparedStatement ps = conn.prepareStatement(sql);) {
			if(params!=null){
				JSONArray names = parseRes.getJSONArray("names");
				for(int i=0;i<names.size();i++){
					ps.setObject(i+1, params.get(names.get(i)));
				}
			}
			ResultSet rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int cc = rsmd.getColumnCount();
			JSONArray list = new JSONArray();
			while (rs.next()) {
				JSONObject map = new JSONObject();
				for (int i = 1; i <= cc; i++) {
					String label = rsmd.getColumnLabel(i);
					String key = underline2camel(label);
					Object value = rs.getObject(label);
					map.put(key, value);
				}
				list.add(map);
			}
			rs.close();
			logger.info("result size=>{}",list.size());
			return list;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private JSONObject parseSql(String sql, JSONObject params) {
		JSONObject res = new JSONObject();
		JSONArray names = new JSONArray();
		sql = new GenericTokenParser("#{","}",(token)->{
			names.add(token);
			return "?";
		}).parse(sql);
		sql = new GenericTokenParser("${","}",(token)->{
			return String.valueOf(params.get(token));
		}).parse(sql);
		res.put("sql", sql);
		res.put("names", names);
		return res;
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
		JSONObject parseRes = null;
		if(params!=null){
			//设置参数
			parseRes = parseSql(sql,params);
			sql = parseRes.getString("sql");
		}
		logger.info("sql=>{}",sql);
		logger.info("params=>{}",params);
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			if(params!=null){
				JSONArray names = parseRes.getJSONArray("names");
				for(int i=0;i<names.size();i++){
					ps.setObject(i+1, params.get(names.get(i)));
				}
			}
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 驼峰命名转下划线命名
	 * */
	public static String camel2underline(String prop){
		return prop.replaceAll("(?<Uper>[A-Z])", "_${Uper}").toLowerCase();
	}
	/**
	 * 下划线转驼峰命名
	 * @param prop
	 * @return
	 */
	public static String underline2camel(String column){
		if(column==null){ 
		return null; 
		} 
		String[] array = column.split("_(?=[a-z])");
		if(array.length==1){
			return column;
		}
		System.out.println(String.join(",", array));
		for(int i=1;i<array.length;i++){
			array[i] = array[i].substring(0, 1).toUpperCase()+array[i].substring(1);
		}
		return String.join("", array); 
	}
	public static void main(String[] args) {
		//加载驱动
		DbClient.loadDriver("com.mysql.jdbc.Driver");
		
		//根据配置创建客户端
		String username = "root";
		String password = "";
		String url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8";
		DbClient.newClient(username, password, url)
		//在事务中执行（如果没有创建连接，则会自动创建连接，如果客户端已经创建连接，则使用已创建的连接，执行完毕自动关闭连接）
		.withTx(client->{
			//sql参数
			JSONObject params=new JSONObject();
			params.put("nick", "%h%");
			params.put("fetchSize", "1");
			//sql语句,支持#{}和${}占位符
			String sql = "select * from sys_user where nick like #{nick} limit ${fetchSize} ";
			//执行查询
			JSONArray res = client.query(sql,params);
			//结果处理
			res.forEach(row->{
				System.out.println(row);
			});
		});
		System.out.println("====");
		//根据配置创建客户端
		DbClient client = DbClient.newClient(username, password, url);
		//在连接中执行（执行完毕自动关闭连接）
		client.withConn(conn->{
			client.query("select * from sys_user").forEach(row->{
				System.out.println(row);
			});
		});
	}
}

