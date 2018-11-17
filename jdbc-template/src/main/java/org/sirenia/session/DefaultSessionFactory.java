package org.sirenia.session;

import java.sql.Connection;

import javax.sql.DataSource;

public class DefaultSessionFactory implements SessionFactory {
	private DataSource dataSource;
	private ThreadLocal<Session> sessionHolder = new ThreadLocal<>();

	public DataSource getDataSource() {
		return dataSource;
	}
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	public Session getSession() {
		return getSession(false);
	}
	public Session getSession(boolean createIfNotExsists) {
		try {
			Session session = sessionHolder.get();
			if (session != null) {
				return session;
			}
			// session不存在
			// 参考org.springframework.jdbc.datasource.DataSourceTransactionManager.doGetTransaction()
			if (createIfNotExsists) {
				Connection conn = dataSource.getConnection();
				session = new Session(conn);
				sessionHolder.set(session);
				return session;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void closeSession(Session session) {
		try {
			sessionHolder.remove();
			Connection conn = session.getConn();
			if(conn!=null && !conn.isClosed()){
				conn.close();
				session.clearConn();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
