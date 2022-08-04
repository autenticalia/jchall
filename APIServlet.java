package de.maultaschenfabrikle.kurts.warehouse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class APIServlet extends HttpServlet {

	private static final long serialVersionUID = 4622813460640568846L;
	private LogManager logManager = LogManager.getInstance();

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		Logger logger = logManager.getLogger(LoginServlet.class.getName());

		String body = new String();
		for (String line; (line = request.getReader().readLine()) != null; body += line);		
		logger.info(String.format("doPost(), body='%s'", body));

		SAXReader reader = new SAXReader();
		Document doc = null;
		String responseXML = "";

		try {
			
			InputStream stream = new ByteArrayInputStream(body.getBytes());
			doc = reader.read(stream);
			Element rootElement = doc.getRootElement();
			Node methodNode = rootElement.selectSingleNode("method");
			
			if (null != methodNode) {

				String method = methodNode.getStringValue();
				logger.info(String.format("method: '%s'", method));
				String sql = "";
				ResultSet rs = null;

				switch (method) {
				case "listUsers":					
					sql = "SELECT username, firstname, lastname FROM profiles";
					rs = DBHelper.executeQuery(sql);

					responseXML = "<xml><users>";					
					while (rs.next() ) {
						responseXML += "<user>";
						responseXML += String.format("<username>%s</username>", rs.getString("username"));
						responseXML += String.format("<firstname>%s</firstname>", rs.getString("firstname"));
						responseXML += String.format("<lastname>%s</lastname>", rs.getString("lastname"));
						responseXML += "</user>";
					}

					responseXML += "</users></xml>";
					
					rs.getStatement().close();

					break;
				case "listStorage":
					sql = "SELECT name, amount FROM storage";
					rs = DBHelper.executeQuery(sql);

					responseXML = "<xml><storage>";					
					while (rs.next() ) {
						responseXML += "<article>";
						responseXML += String.format("<name>%s</name>", rs.getString("name"));
						responseXML += String.format("<amount>%s</amount>", rs.getInt("amount"));
						responseXML += "</article>";
					}

					responseXML += "</storage></xml>";					
					rs.getStatement().close();

					break;

				default:
					responseXML = "<xml><error>unknown method</error></xml>";

				}

			} else {
				responseXML = "<xml><error>missing method</error></xml>";

			}

		} catch (DocumentException | NullPointerException e) {
			responseXML = "<xml><error>parsing problem</error></xml>";
			e.printStackTrace();
		} catch (SQLException e) {
			responseXML = "<xml><error>SQL error</error></xml>";
		}
		
		response.getWriter().println(responseXML);
		
		if (responseXML.length() > 60) {
			responseXML = responseXML.substring(0, 60);
		}
			
		logger.info(String.format("responseXML: '%s'", responseXML));
		
	}

}