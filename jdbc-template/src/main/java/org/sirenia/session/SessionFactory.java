package org.sirenia.session;

public interface SessionFactory {

	Session getSession();

	Session getSession(boolean createIfNotExsists);

	void closeSession(Session session) throws Exception;

}
