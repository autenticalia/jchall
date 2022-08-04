package de.maultaschenfabrikle.kurts.warehouse;

import java.io.ObjectStreamException;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.tomcat.SessionStore;

public class SessionManager extends SessionStore {

	private static HashMap<String, HashMap<String, Object>> sessions = new HashMap<String, HashMap<String, Object>>();
	private LogManager logManager = LogManager.getInstance();

	public static void setAttribute(HttpServletRequest request, String key, Object value) {
		HttpSession session = request.getSession();
		String id = session.getId();
		
		SessionManager.setAttribute(id, key, value);
		session.setAttribute(key, value);
	}
	
	public static void setAttribute(String id, String key, Object value) {
		Logger logger = LogManager.getInstance().getLogger(SessionManager.class.getName());		
		logger.debug(String.format("setting '%s' -> '%s' in session '%s'", key, value, id));
		
		HashMap<String, Object> attributes = sessions.get(id);
		if (null == attributes) {
			attributes = new HashMap<String, Object>();
			sessions.put(id, attributes);
		}

		attributes.put(key, value);		
	}

	public static Object getAttribute(HttpServletRequest request, String key) {		
		HttpSession session = request.getSession();
		return session.getAttribute(key);
	}
	
	public static void invalidate(HttpServletRequest request) {
		Logger logger = LogManager.getInstance().getLogger(SessionManager.class.getName());
		logger.debug("invalidating session");
		HttpSession session = request.getSession();
		session.invalidate();
	}

	public void print() {
		Logger logger = logManager.getLogger(SessionManager.class.getName());

		logger.warn("printing current session store");
		//logger.warn(");

		for (Entry<String, HashMap<String, Object>> entry : SessionManager.sessions.entrySet()) {
			String id = entry.getKey();
			HashMap<String, Object> value = entry.getValue();

			value.forEach((k, v) -> logger.debug(String.format("%s: %s -> %s", id, k, v)));
		}
	}

	// This method gets automatically called when Tomcat restarts (possible through the implemented interfaces in the super class).
	// Tomcat fills the attribute storedSessions, with the information from the previous run.
	// Therefore this method takes care to copy the old session information to the current object state.
	// Note: the logic is needed for the regular container restarts.
	private Object readResolve() throws ObjectStreamException {

		for (Entry<String, HashMap<String, Object>> entry : storedSessions.entrySet()) {
			String id = entry.getKey();
			HashMap<String, Object> value = entry.getValue();

			value.forEach((k, v) -> SessionManager.setAttribute(id, k, v));
		}
		
		print();
		
		return this;
		
	}

}
