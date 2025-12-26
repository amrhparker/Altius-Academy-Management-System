import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AttendanceReportPDFServlet extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/yourDB";
    private static final String USER = "root";
    private static final String PASS = "password";

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String monthParam = request.getParameter("month"); // e.g., "2025-12"
        if (monthParam == null) monthParam = "2025-12";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=AttendanceReport.pdf");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {

            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            document.add(new Paragraph("Attendance Report")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Summary: total classes, total students, average attendance
            Statement stmt = conn.createStatement();
            String summaryQuery = "SELECT COUNT(DISTINCT class_id) AS totalClasses, "
                    + "COUNT(DISTINCT student_id) AS totalStudents, "
                    + "AVG(attended) AS avgAttendance "
                    + "FROM attendance "
                    + "WHERE DATE_FORMAT(attendance_date,'%Y-%m') = '" + monthParam + "'";
            ResultSet rsSummary = stmt.executeQuery(summaryQuery);

            Table summaryTable = new Table(new float[]{150, 150});
            if (rsSummary.next()) {
                summaryTable.addCell("Total Classes");
                summaryTable.addCell(rsSummary.getInt("totalClasses") + "");
                summaryTable.addCell("Total Students");
                summaryTable.addCell(rsSummary.getInt("totalStudents") + "");
                summaryTable.addCell("Average Attendance");
                summaryTable.addCell(String.format("%.2f%%", rsSummary.getDouble("avgAttendance")));
            }
            document.add(summaryTable);
            document.add(new Paragraph("\n"));

            // Detail Table
            Table detailTable = new Table(new float[]{100, 100, 100, 100});
            detailTable.addHeaderCell("Student");
            detailTable.addHeaderCell("Class");
            detailTable.addHeaderCell("Date");
            detailTable.addHeaderCell("Status");

            String detailQuery = "SELECT s.student_name, c.class_name, a.attendance_date, a.attended "
                    + "FROM attendance a "
                    + "JOIN students s ON a.student_id = s.student_id "
                    + "JOIN classes c ON a.class_id = c.class_id "
                    + "WHERE DATE_FORMAT(a.attendance_date,'%Y-%m') = '" + monthParam + "'";
            ResultSet rsDetail = stmt.executeQuery(detailQuery);

            while (rsDetail.next()) {
                detailTable.addCell(rsDetail.getString("student_name"));
                detailTable.addCell(rsDetail.getString("class_name"));
                detailTable.addCell(rsDetail.getDate("attendance_date").toString());
                detailTable.addCell(rsDetail.getBoolean("attended") ? "Present" : "Absent");
            }

            document.add(detailTable);
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Attendance Report PDF Generator";
    }
}
