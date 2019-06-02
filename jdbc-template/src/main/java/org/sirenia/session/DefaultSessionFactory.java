package org.sirenia.session;

import java.sql.Connection;

import javax.sql.DataSource;

public class DefaultSessionFactory implements SessionFactory {
	private DataSource dataSource;

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
			Session session = SessionHolder.get();
			if (session != null) {
				return session;
			}
			// session不存在
			// 参考org.springframework.jdbc.datasource.DataSourceTransactionManager.doGetTransaction()
			if (createIfNotExsists) {
				Connection conn = dataSource.getConnection();
				session = new Session(conn);
				SessionHolder.set(session);
				return session;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void closeSession(Session session) throws Exception {
		session.close();
	}
}
