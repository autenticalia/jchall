package de.maultaschenfabrikle.kurts.warehouse;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;


public class AuthenticationFilter implements Filter {

	private LogManager logManager = LogManager.getInstance();
	final String[] excluded = {"/static/logo.jpg", "/static/yelling-boss.png", "/static/login.css", "/login.jsp", "/loginServlet", "/apiServlet"};
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		Logger logger = logManager.getLogger(AuthenticationFilter.class.getName());
		
		HttpServletRequest hRequest = (HttpServletRequest) request;
		String servletPath = hRequest.getServletPath();
		Boolean requestAllowed = false;
		
		logger.debug(String.format("incoming request to '%s'", servletPath));

		for (String exclude : excluded) {
			if (servletPath.equals(exclude)) {
				logger.debug("skipping auth-check, path excluded");
				requestAllowed = true;
			}
		}		
		
		if (SessionManager.getAttribute(hRequest, "authenticated") != null && (Boolean) SessionManager.getAttribute(hRequest, "authenticated")) {
			logger.debug("user is authenticated");
			requestAllowed = true;			
		}
		
		if (!requestAllowed) {
			logger.debug("request not allowed, sending to login page");
			HttpServletResponse hResponse = (HttpServletResponse) response;			
			hResponse.sendRedirect("login.jsp");
		} else {
			chain.doFilter(request, response);
		}
		
		
	}

}
