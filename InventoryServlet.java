package de.maultaschenfabrikle.kurts.warehouse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

public class InventoryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private LogManager logManager = LogManager.getInstance();

	// this method is only called automatically via cron
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Logger logger = logManager.getLogger(InventoryServlet.class.getName());
		
		BufferedWriter writer = null;
		ResultSet rs = null;
		
		try {

			logger.debug("fetching data for inventory file");

			String sql = String.format("select * from storage");
			rs = DBHelper.executeQuery(sql);

			writer = new BufferedWriter(new FileWriter("inventory.csv"));
			
			while (rs.next()) {

				int amount = rs.getInt("amount");
				String name = rs.getString("name");
				String replacement = rs.getString("replacement");

				String line = String.format("%s;%d;%s\n", name, amount, replacement);
				logger.debug(String.format("writing line '%s' to inventory.csv", line.replace('\n', ' ')));
				
				writer.write(line);

			}

		} catch (SQLException e) {
			logger.error(e.toString());
		}
		finally {
			if (null != writer) {
				writer.close();
			}
			if (null != rs) {
				try {
					rs.getStatement().close();
				} catch (SQLException e) {
					logger.error(e.toString());
				}
			}
		}

	}
}
