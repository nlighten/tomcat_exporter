package nl.nlighten.prometheus.tomcat;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class TestServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // deliberately create a session to test session metrics
        req.getSession();


        Connection conn = null;
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/db");
            conn = ds.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("select 1");
            rs = st.executeQuery("select * from NON_EXISTING_TABLE");

        } catch (Exception e) {
           // ignore
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        PrintWriter out = resp.getWriter();
        out.println("Hello world!");
        out.close();
    }
}
