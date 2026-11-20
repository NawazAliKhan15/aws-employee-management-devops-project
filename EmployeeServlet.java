
import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class EmployeeServlet extends HttpServlet {

    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Employee Management</title>");
        out.println("<link rel='stylesheet' href='style.css'>");
        out.println("</head><body><div class='container'>");

        out.println("<h1>Employee Management</h1>");
        out.println("<p class='subtitle'>Manage employee records stored in AWS RDS</p>");

        out.println("<h2>Add New Employee</h2>");
        out.println("<form method='POST' action='employees'>");
        out.println("<input type='hidden' name='action' value='add'>");
        out.println("<input type='text' name='name' placeholder='Employee name' required>");
        out.println("<input type='text' name='role' placeholder='Employee role' required>");
        out.println("<button type='submit'>Add Employee</button>");
        out.println("</form>");

        out.println("<h2>Employee Records</h2>");
        out.println("<table>");
        out.println("<tr><th>ID</th><th>Name</th><th>Role</th><th>Action</th></tr>");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection con = DriverManager.getConnection(
                    DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(
                    "SELECT id, name, role FROM employees ORDER BY id")) {

                while (rs.next()) {
                    int id = rs.getInt("id");

                    out.println("<tr>");
                    out.println("<td>" + id + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("name")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("role")) + "</td>");
                    out.println("<td>");
                    out.println("<form method='POST' action='employees' " +
                        "onsubmit=\"return confirm('Delete this employee?');\">");
                    out.println("<input type='hidden' name='action' value='delete'>");
                    out.println("<input type='hidden' name='id' value='" + id + "'>");
                    out.println("<button type='submit'>Delete</button>");
                    out.println("</form>");
                    out.println("</td>");
                    out.println("</tr>");
                }
            }

        } catch (Exception e) {
            out.println("<tr><td colspan='4'>Unable to load employee records.</td></tr>");
            e.printStackTrace();
        }

        out.println("</table></div></body></html>");
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection con = DriverManager.getConnection(
                    DB_URL, DB_USER, DB_PASSWORD)) {

                if ("add".equals(action)) {

                    String name = request.getParameter("name");
                    String role = request.getParameter("role");

                    if (name == null || role == null ||
                        name.trim().isEmpty() || role.trim().isEmpty()) {
                        response.sendError(400, "Name and role are required.");
                        return;
                    }

                    String sql =
                        "INSERT INTO employees (name, role) VALUES (?, ?)";

                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, name.trim());
                        ps.setString(2, role.trim());
                        ps.executeUpdate();
                    }

                } else if ("delete".equals(action)) {

                    String idValue = request.getParameter("id");
                    int id;

                    try {
                        id = Integer.parseInt(idValue);
                    } catch (Exception ex) {
                        response.sendError(400, "Invalid employee ID.");
                        return;
                    }

                    String sql = "DELETE FROM employees WHERE id = ?";

                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }

                } else {
                    response.sendError(400, "Invalid action.");
                    return;
                }
            }

            response.sendRedirect(request.getContextPath() + "/employees");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Unable to update employee records.");
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}

