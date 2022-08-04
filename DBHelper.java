package de.maultaschenfabrikle.kurts.warehouse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;


public class DBHelper {

	private static Connection dbConnection = null;

	private static Connection getConnection() {

		Logger logger = LogManager.getInstance().getLogger(DBHelper.class.getName());
		
		try {

			if (dbConnection == null || !dbConnection.isValid(30)) {

				if (dbConnection != null) {
					dbConnection.close();
				}

				logger.debug("creating new DB connection");
				Context initContext = new InitialContext();
				Context envContext = (Context) initContext.lookup("java:comp/env");
				DataSource ds = (DataSource) envContext.lookup("jdbc/WarehouseDB");
				dbConnection = ds.getConnection();
			}

		} catch (Exception e) {
			logger.error("error getting sql connection: " + e);
		}

		return dbConnection;
	}

	public static ResultSet executeQuery(String query) {

		Logger logger = LogManager.getInstance().getLogger(DBHelper.class.getName());		
		ResultSet rs = null;

		try {
			Statement statement = DBHelper.getConnection().createStatement();	
			statement.setQueryTimeout(1);

			logger.debug(String.format("executing SQL query >> %s <<", query));
			rs = statement.executeQuery(query);
		} catch (Exception e) {
			logger.error("executing query " + e);
		}
		
		return rs;

	}

	public static ResultSet executeUpdate(String query) {

		Logger logger = LogManager.getInstance().getLogger(DBHelper.class.getName());		
		ResultSet rs = null;

		try {

			Statement statement = DBHelper.getConnection().createStatement();

			logger.debug(String.format("executing SQL update >> %s <<", query));
			statement.executeUpdate(query);
			logger.debug("done");

			statement.close();

		} catch (Exception e) {
			logger.error("executing update " + e);
		}

		return rs;

	}

}
