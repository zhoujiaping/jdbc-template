package org.sirenia.app;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.Test;

public class MySqlTest {
	@Test
	public void test() throws ClassNotFoundException, SQLException {
		String username = "root";
		String password = "";
		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8&useOldAliasMetaDataBehavior=true";
		Class.forName(driver);
		Connection connection = DriverManager.getConnection(url, username, password);
		PreparedStatement ps0 = connection.prepareStatement("select 1 from sys_user");
		ResultSet rs = ps0.executeQuery();
		while (rs.next()) {
			int count = rs.getMetaData().getColumnCount();
			for (int i = 1; i <= count; i++) {
				Object o = rs.getObject(i);
				System.out.print(rs.getMetaData().getColumnLabel(i)+",");
				System.out.println(o);
				String schemaname = rs.getMetaData().getSchemaName(i);
				System.out.println(schemaname);
			}
		}
		System.out.println("+++++++++++++++++");
		DatabaseMetaData md = connection.getMetaData();
		System.out.println(md.getDatabaseProductName());// 获取数据库名：MySQL
		ResultSet mdrs = md.getPrimaryKeys(null, null, "sys_user");
		ResultSetMetaData rsmd0 = mdrs.getMetaData();
		connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		while (mdrs.next()) {
			int count = mdrs.getMetaData().getColumnCount();
			for (int i = 1; i <= count; i++) {
				Object o = mdrs.getObject(i);
				System.out.print(rsmd0.getColumnLabel(i)+",");
				System.out.println(o);
				String schemaname = rsmd0.getSchemaName(i);
				System.out.println(schemaname);
			}
		}
		System.out.println("-------------------");

		PreparedStatement ps = connection.prepareStatement("select t1.*,t2.* from sys_user t1 left join sys_user t2 on t1.id=t2.id where 1=1");
		rs = ps.executeQuery();
		ResultSetMetaData rsmd = rs.getMetaData();
		int cc = rsmd.getColumnCount();
		System.out.println(cc);
		for (int i = 1; i <= cc; i++) {
			System.out.println(rsmd.getColumnName(i));
			String ctn = rsmd.getColumnTypeName(i);
			System.out.println(ctn);// BIGINT VARCHAR TIMESTAMP INT
			int cds = rsmd.getColumnDisplaySize(i);
			System.out.println(cds);// 20 36
			String cl = rsmd.getColumnLabel(i);
			System.out.println(cl);// id name create_time status
			int ct = rsmd.getColumnType(i);
			System.out.println(ct);// -5 12
			String cn = rsmd.getCatalogName(i);
			System.out.println(cn);// test test
			String ccn = rsmd.getColumnClassName(i);
			System.out.println(ccn);// java.lang.Long java.lang.String
									// java.sql.Timestamp java.lang.Integer
			String tn = rsmd.getTableName(i);
			System.out.println(tn);// sys_user sys_user
			String sn = rsmd.getSchemaName(i);
			System.out.println(sn);//
			System.out.println("===============");
		}
		while(rs.next()){
			System.out.println(rs.getObject("sys_user.id"));
		}
		connection.close();
	}
	@Test
	public void test1(){
		
	}
}