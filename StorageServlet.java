package de.maultaschenfabrikle.kurts.warehouse;

import java.io.IOException;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

public class StorageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private LogManager logManager = LogManager.getInstance();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Logger logger = logManager.getLogger(StorageServlet.class.getName());
		
		try {

			String method = request.getParameter("method");
			String newAmount = request.getParameter("amount");			
			Integer id = Integer.parseInt(request.getParameter("id"));

			logger.info(String.format("doGet(), method='%s', id='%d', amount='%s'", method, id, newAmount));

			String sql = String.format("select * from storage where id = %d", id);
			ResultSet rs = DBHelper.executeQuery(sql);

			if (!rs.next()) {
				logger.error("no product with this id in store");
			}

			int amount = rs.getInt("amount");
			String replacement = rs.getString("replacement");
			rs.getStatement().close();

			if (method.equals("dec")) {
				if (amount > 0) {
					amount = amount - 1;
					sql = String.format("UPDATE storage SET amount=%d WHERE id=%d", amount, id);
					rs = DBHelper.executeUpdate(sql);
				} else {
					String firstname = (String) SessionManager.getAttribute(request, "firstname");
					request.setAttribute("error", String.format(
							"%s, DID YOU MAKE PRODUCTS OUT OF THIN AIR AGAIN !?!<br><br>Don't worry Bossmen I %s",
							firstname.toUpperCase(), replacement));
				}
			}

			if (method.equals("inc")) {

				if (amount < 100) {
					amount = amount + 1;
					sql = String.format("UPDATE storage SET amount=%d WHERE id=%d", amount, id);
					DBHelper.executeUpdate(sql);
				}

				else {
					String firstname = (String) SessionManager.getAttribute(request, "firstname");
					request.setAttribute("error", String.format(
							"WHO SET UP THIS ORDER !?!<br>Our fridge already overflows. %s, you are not leaving today until everything is gone!<br>(Even if you need to eat all of the additional ingredients yourself)",
							firstname.toUpperCase()));
				}
			}

			if (method.equals("restock")) {

				if (amount != 0) {				
					logger.info("stock not empty, doing nothing");
				
				} else {					
					sql = String.format("UPDATE storage SET amount=%s WHERE id=%d", newAmount, id);
					DBHelper.executeUpdate(sql);
				}

			}

		} catch (

		Exception e) {
			logger.error(e.toString());
			//System.out.println();
		}

		logger.debug("forwarding to index.jsp");
		request.getRequestDispatcher("index.jsp").forward(request, response);

		return;

	}

}
