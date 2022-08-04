package de.maultaschenfabrikle.kurts.warehouse;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public class SecurityFilter implements Filter {

	private LogManager logManager = LogManager.getInstance();

	private int checkHeaders(HttpServletRequest hRequest, ArrayList<String> toolPatterns,
			ArrayList<String> techniquePatterns) {

		Logger logger = logManager.getLogger(SecurityFilter.class.getName());

		int sleepTime = 0;

		Enumeration<String> headerEnum = hRequest.getHeaderNames();
		while (headerEnum.hasMoreElements()) {
			String elementName = headerEnum.nextElement();
			String elementValue = hRequest.getHeader(elementName);

			elementName = elementName.toLowerCase();
			elementValue = elementValue.toLowerCase();

			for (String pattern : toolPatterns) {
				if (elementName.contains(pattern.toLowerCase()) || elementValue.contains(pattern.toLowerCase())) {
					logger.warn(String.format("security alert, detected tool pattern '%s' in header '%s':'%s'", pattern,
							elementName, elementValue));
					sleepTime += 15;
				}
			}

			for (String pattern : techniquePatterns) {
				if (elementName.contains(pattern.toLowerCase()) || elementValue.contains(pattern.toLowerCase())) {
					logger.warn(String.format("security alert, detected technique pattern '%s' in header '%s':'%s'",
							pattern, elementName, elementValue));
					sleepTime += 5;
				}
			}
		}

		return sleepTime;
	}

	private int checkParameters(HttpServletRequest hRequest, ArrayList<String> toolPatterns,
			ArrayList<String> techniquePatterns) {

		Logger logger = logManager.getLogger(SecurityFilter.class.getName());
		int sleepTime = 0;

		Enumeration<String> paramEnum = hRequest.getParameterNames();
		while (paramEnum.hasMoreElements()) {
			String elementName = paramEnum.nextElement();
			String elementValue = hRequest.getParameter(elementName);

			elementName = elementName.toLowerCase();
			elementValue = elementValue.toLowerCase();

			for (String pattern : toolPatterns) {
				if (elementName.contains(pattern.toLowerCase()) || elementValue.contains(pattern.toLowerCase())) {
					logger.warn(String.format("security alert, detected tool pattern '%s' in parameter '%s':'%s'",
							pattern, elementName, elementValue));
					sleepTime += 15;
				}
			}

			for (String pattern : techniquePatterns) {
				if (elementName.contains(pattern.toLowerCase()) || elementValue.contains(pattern.toLowerCase())) {
					logger.warn(String.format("security alert, detected technique pattern '%s' in parameter '%s':'%s'",
							pattern, elementName, elementValue));
					sleepTime += 5;
				}
			}
		}

		return sleepTime;
	}

	private ArrayList<String> getPatterns(String type) {

		Logger logger = logManager.getLogger(SecurityFilter.class.getName());
		ArrayList<String> result = new ArrayList<String>();

		try {
			String sql = String.format("select * from waf where typ = '%s'", type);
			ResultSet rs = DBHelper.executeQuery(sql);

			while (rs.next()) {
				result.add(rs.getString("pattern"));
			}

			rs.getStatement().close();
		} catch (SQLException e) {
			logger.error("SQL Exception: " + e);
		}

		return result;
	}

	private String getIP(HttpServletRequest hRequest) {

		String ipAddress = hRequest.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
			ipAddress = hRequest.getRemoteAddr();
		}
		
		// 2022.07.08 15:00
		// Sorry I had to fix this vector as it circumvents the intended solution
		// and to be honest I did not expect that Apache does not validate this Header for valid IPs
		// ramoliks
		if (ipAddress.contains(",")) {
			String ipAddresses[] = ipAddress.split(",");
			ipAddress = ipAddresses[ipAddresses.length - 1];
			ipAddress = ipAddress.stripLeading();
		}

		return ipAddress;
	}

	private void writeToDB(HttpServletRequest hRequest, int sleepTime) {

		if (sleepTime == 0) {
			return;
		}

		if (sleepTime > 30) {
			sleepTime = 30;
		}

		String ipAddress = getIP(hRequest);

		long delayTill = System.currentTimeMillis() + sleepTime * 60 * 1000;
		DBHelper.executeUpdate(String.format("INSERT INTO blocklist VALUES('%s', '%d')", ipAddress, delayTill));

	}

	private long isBlocked(HttpServletRequest hRequest) {
		
		Logger logger = logManager.getLogger(SecurityFilter.class.getName());
		long blockedMinutes = 0;
		String ipAddress = getIP(hRequest);

		String sql = String.format("select * from blocklist where ip = '%s'", ipAddress);
		ResultSet rs = DBHelper.executeQuery(sql);

		try {
			if (rs.next()) {
				blockedMinutes = (rs.getLong("timestamp") - System.currentTimeMillis())/1000/60;
			}
			
			rs.getStatement().close();
		} catch (SQLException e) {
			logger.error("SQL Exception: " + e);
		}
		
		logger.info(String.format("ip '%s' is blocked for '%d' minutes", ipAddress, blockedMinutes));
		
		if (blockedMinutes < 0) {
			blockedMinutes = 0;
			DBHelper.executeUpdate(String.format("DELETE FROM blocklist WHERE ip = '%s'", ipAddress));
		}
		
		return blockedMinutes;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		Logger logger = logManager.getLogger(SecurityFilter.class.getName());
		HttpServletRequest hRequest = (HttpServletRequest) request;
		ThreadContext.put("IP", getIP(hRequest));

		long blockedMinutes = isBlocked(hRequest);
		if (blockedMinutes > 0) {
			request.setAttribute("blockedMinutes", blockedMinutes);
			request.getRequestDispatcher("waf.jsp").forward(request, response);
			return;
		}
		
		ArrayList<String> toolPatterns = getPatterns("tool");
		ArrayList<String> techniquePatterns = getPatterns("technique");
		logger.debug(String.format("initialized security filter with '%d' tool and '%d' technique patterns\n",
				toolPatterns.size(), techniquePatterns.size()));

		int sleepTime = 0;
		sleepTime += checkHeaders(hRequest, toolPatterns, techniquePatterns);
		sleepTime += checkParameters(hRequest, toolPatterns, techniquePatterns);

		writeToDB(hRequest, sleepTime);
		if (sleepTime > 0) {
			request.setAttribute("blockedMinutes", sleepTime);
			request.getRequestDispatcher("waf.jsp").forward(request, response);
			return;
		}

		chain.doFilter(request, response);

	}

}
