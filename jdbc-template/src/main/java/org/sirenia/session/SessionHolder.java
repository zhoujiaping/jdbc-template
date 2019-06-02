package org.sirenia.session;

public class SessionHolder {
	private static ThreadLocal<Session> tl = new ThreadLocal<>();

	public static Session get() {
		return tl.get();
	}

	public static void set(Session session) {
		tl.set(session);
	}

	public static void remove() {
		tl.remove();
	}
	
}
