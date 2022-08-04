package de.maultaschenfabrikle.kurts.warehouse;

import java.io.IOException;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 115392349296577227L;
	private LogManager logManager = LogManager.getInstance();	
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		Logger logger = logManager.getLogger(LoginServlet.class.getName());		
		String username = request.getParameter("username");
		String password = request.getParameter("password");		
		
		logger.info(String.format("doPost(), username: '%s', password :'%s'", username, password));		
		long start = System.currentTimeMillis();

		database: try {

			String sql = String.format("select * from profiles where username = '%s'", username);
			ResultSet rs = DBHelper.executeQuery(sql);
			if (!rs.next()) {
				logger.debug(String.format("user inactive or password invalid"));
				SessionManager.setAttribute(request, "error", "user inactive or password invalid");
				
				rs.getStatement().close();
				break database;
			}

			String firstname = rs.getString("firstname");
			String login_id = rs.getString("login_id");
			
			logger.debug(String.format("firstname '%s'", firstname));
			logger.debug(String.format("login_id '%s'", login_id));
			
			rs.getStatement().close();

			sql = String.format("select * from logins where id = '%s'", login_id);
			rs = DBHelper.executeQuery(sql);
			if (!rs.next()) {
				logger.debug(String.format("user inactive or password invalid"));
				SessionManager.setAttribute(request, "error", "user inactive or password invalid");

				rs.getStatement().close();
				break database;
			}

			String sqlPassword = rs.getString("password");			
			String hashedPassword = Cryptography.computeSHA1(password);
			Boolean active = rs.getBoolean("active");
			
			logger.debug(String.format("password '%s' vs '%s'", hashedPassword, sqlPassword));
			logger.debug(String.format("isActive '%b'", active));
									
			if (active && hashedPassword.equals(sqlPassword)) {
				logger.info("authenticated successfully");

				SessionManager.setAttribute(request, "authenticated", true);
				SessionManager.setAttribute(request, "firstname", firstname);				
				
			}
			else {
				logger.debug(String.format("user inactive or password invalid"));
				SessionManager.setAttribute(request, "error", "user inactive or password invalid");
			}
			
			rs.getStatement().close();

		} catch (Exception e) {
			logger.error(e.toString());
		}

		// fix for the SQL injection reported by Mr. Haxxor (see Ticket 1927 / the respective commit)
		long finish = System.currentTimeMillis();
		long elapsed = (finish-start);
		
		try {
			Thread.sleep(3000-elapsed);
		} catch (InterruptedException e) {
			logger.error("thread sleep interrupted");
		}

		
		if (SessionManager.getAttribute(request, "authenticated") != null && (boolean) SessionManager.getAttribute(request, "authenticated")) {
			response.sendRedirect("index.jsp");
		} else {
			response.sendRedirect("login.jsp");
		}

	}	

}