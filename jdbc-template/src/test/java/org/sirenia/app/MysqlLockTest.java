package org.sirenia.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sirenia.util.Callback1;

public class MysqlLockTest {
	@BeforeClass
	public static void init() throws ClassNotFoundException, SQLException {
		String driver = "com.mysql.jdbc.Driver";
		Class.forName(driver);
	}
	private Connection conn(){
		try{
			String username = "";
			String password = "";
			String url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8";
			Connection conn = DriverManager.getConnection(url, username, password);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			//conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			conn.setAutoCommit(false);
			return conn;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	public void withConnection(Callback1<Connection> callback){
		String username = "";
		String password = "";
		String url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8";
		try(Connection conn = DriverManager.getConnection(url, username, password);){
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			//conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			conn.setAutoCommit(false);
			callback.apply(conn);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	public void withTx(){
		
		
	}
	private List<Map<String,Object>> select(Connection conn,String sql){
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int cc = rsmd.getColumnCount();
			List<Map<String,Object>> list = new ArrayList<>();
			while(rs.next()){
				Map<String,Object> map = new HashMap<>();
				for(int i=1;i<=cc;i++){
					String label = rsmd.getColumnLabel(i);
					Object value = rs.getObject(label);
					map.put(label, value);
				}
				list.add(map);
			}
			return list;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally {
			try{
				if(rs!=null){
					rs.close();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}finally {
				try {
					if(ps!=null){
						ps.close();
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	private int update(Connection conn,String sql){
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}finally {
			try{
				if(ps!=null){
					ps.close();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	public void withTryCatch(){}
	//update不会阻塞select
	//update会阻塞update
	@Test
	public void updateAndSelect() throws SQLException{
		Connection conn1 = conn();
		Connection conn2 = conn();
		update(conn2, "update sys_user set nick='111' where id = 1");
		int count = update(conn1, "update sys_user set nick='xxx' where id = 2");
		System.out.println(count);
		List<?> res = select(conn2, "select * from sys_user where id = 1");
		System.out.println(res);
		//conn1.commit();
		//conn2.commit();
		conn1.close();
		conn2.close();
	}
	/**
	 * 线程一：select  ...                               select, commit
	 * 线程二 : ........          update, commit 
	 * 线程二在线程一的两次查询之间发生update并提交。
	 * 如果事务隔离级别是 可重复读，那么第二次读到的结果和第一次读到的结果一样，不受线程二更新的影响。
	 * 如果事务隔离级别是 读已提交，那么第二次读到的结果，是线程二更新提交之后的结果。
	 * @throws SQLException
	 */
	@Test
	public void updateAndSelect2() throws SQLException{
		Connection conn1 = conn();
		Connection conn2 = conn();
		List<?> res = select(conn2, "select * from sys_user where id = 1 LOCK IN SHARE MODE");
		System.out.println(res);
		int count = update(conn1, "update sys_user set nick='16' where id = 1");
		System.out.println(count);
		conn1.commit();
		res = select(conn2, "select * from sys_user where id = 1");
		System.out.println(res);
		count = update(conn2, "update sys_user set nick='15' where id = 1 and nick='14'");
		System.out.println(count);
		conn2.commit();
		conn1.close();
		conn2.close();
	}
	
}
