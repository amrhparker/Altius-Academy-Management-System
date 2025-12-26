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

public class FinancialReportPDFServlet extends HttpServlet {

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
        response.setHeader("Content-Disposition", "attachment; filename=FinancialReport.pdf");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {

            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            document.add(new Paragraph("Financial Report")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Summary
            Statement stmt = conn.createStatement();
            String summaryQuery = "SELECT SUM(total_fee) AS totalRevenue, SUM(paid) AS totalCollected, "
                    + "SUM(outstanding) AS totalOutstanding, COUNT(DISTINCT student_name) AS totalEnrollment "
                    + "FROM payments WHERE DATE_FORMAT(payment_date,'%Y-%m') = '" + monthParam + "'";
            ResultSet rsSummary = stmt.executeQuery(summaryQuery);

            Table summaryTable = new Table(new float[]{150, 150});
            if (rsSummary.next()) {
                summaryTable.addCell("Total Revenue");
                summaryTable.addCell("RM " + rsSummary.getDouble("totalRevenue"));
                summaryTable.addCell("Total Collected");
                summaryTable.addCell("RM " + rsSummary.getDouble("totalCollected"));
                summaryTable.addCell("Outstanding Fees");
                summaryTable.addCell("RM " + rsSummary.getDouble("totalOutstanding"));
                summaryTable.addCell("Total Enrollment");
                summaryTable.addCell(rsSummary.getInt("totalEnrollment") + "");
            }
            document.add(summaryTable);
            document.add(new Paragraph("\n"));

            // Detail Table
            Table detailTable = new Table(new float[]{100, 120, 80, 80, 80});
            detailTable.addHeaderCell("Student");
            detailTable.addHeaderCell("Package");
            detailTable.addHeaderCell("Total Fee");
            detailTable.addHeaderCell("Paid");
            detailTable.addHeaderCell("Outstanding");

            String detailQuery = "SELECT student_name, package_name, total_fee, paid, outstanding "
                    + "FROM payments WHERE DATE_FORMAT(payment_date,'%Y-%m') = '" + monthParam + "'";
            ResultSet rsDetail = stmt.executeQuery(detailQuery);

            while (rsDetail.next()) {
                detailTable.addCell(rsDetail.getString("student_name"));
                detailTable.addCell(rsDetail.getString("package_name"));
                detailTable.addCell("RM " + rsDetail.getDouble("total_fee"));
                detailTable.addCell("RM " + rsDetail.getDouble("paid"));
                detailTable.addCell("RM " + rsDetail.getDouble("outstanding"));
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
        processRequest(request, response); // keep calling processRequest
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response); // keep calling processRequest
    }

    @Override
    public String getServletInfo() {
        return "Financial Report PDF Generator";
    }
}
