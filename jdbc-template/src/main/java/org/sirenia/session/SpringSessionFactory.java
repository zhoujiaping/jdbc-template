package org.sirenia.session;

import java.lang.reflect.Method;
import java.sql.Connection;

import javax.sql.DataSource;

//import org.springframework.jdbc.datasource.ConnectionHolder;
//import org.springframework.jdbc.datasource.DataSourceTransactionManager;
//import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SpringSessionFactory implements SessionFactory{
	private Object txManager;//DataSourceTransactionManager
	private ThreadLocal<Session> sessionHolder = new ThreadLocal<>();
	private Class<?> transactionSynchronizationManagerClass;
	//private Map<String,Method> methodCache = new ConcurrentHashMap<>();
	{
		try {
			transactionSynchronizationManagerClass = Class.forName("import org.springframework.transaction.support.TransactionSynchronizationManager");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public void setTxManager(Object txManager) {
		this.txManager = txManager;
	}
	@Override
	public Session getSession() {
		return getSession(false);
	}
	@Override
	public Session getSession(boolean createIfNotExsists) {
		try{
			Session session = sessionHolder.get();
			if (session != null) {
				return session;
			}
			// session不存在
			// 参考org.springframework.jdbc.datasource.DataSourceTransactionManager.doGetTransaction()
			if (createIfNotExsists) {
				Method getResourceMethod = transactionSynchronizationManagerClass.getMethod("getResource", DataSource.class);
				Object conHolder = getResourceMethod.invoke(null);
				if (conHolder == null) {// 连接不存在
					Class<?> txManagerClass = txManager.getClass();
					Method getTransactionMethod = txManagerClass.getMethod("getTransaction", Class.forName("org.springframework.transaction.TransactionDefinition"));
					getTransactionMethod.invoke(txManager, null);
					conHolder = getResourceMethod.invoke(null);
				}
				Class<?> connectionHolderClass = conHolder.getClass();
				Method getConnectionMethod = connectionHolderClass.getMethod("getConnection");
				Connection conn = (Connection) getConnectionMethod.invoke(conHolder);
				session = new Session(conn);
				sessionHolder.set(session);
				return session;
			} else {
				return null;
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	/*private Method getMethod(Class<?> clazz,String methodName,Class<?> ...argsTypes){
		StringBuffer argsSignature = new StringBuffer();
		if(argsTypes!=null){
			for(int i=0;i<argsTypes.length;i++){
				argsSignature.append(argsTypes[i].getName());
			}
		}
		String signature = clazz.getName()+"."+methodName+"("+argsSignature.toString()+")";
		if(methodCache.containsKey(signature)){
			return methodCache.get(signature);
		}
		try {
			Method method = clazz.getMethod(methodName, argsTypes);
			methodCache.put(signature, method);
			return method;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}*/
	@Override
	public void closeSession(Session session){
		sessionHolder.remove();
		session.clearConn();
	}
}
