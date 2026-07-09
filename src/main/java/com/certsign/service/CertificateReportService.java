package com.certsign.service;

import com.certsign.model.Certificate;
import com.certsign.model.CertificateApprovalStatus;
import com.certsign.repository.CertificateRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CertificateReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CertificateRepository certificateRepository;

    public CertificateReportService(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    public CertificateReport buildReport(String mode, String date, String month, Integer year) {
        ReportPeriod period = resolvePeriod(mode, date, month, year);
        List<Certificate> certificates = certificateRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                period.start(),
                period.endExclusive()
        );
        long pending = certificates.stream()
                .filter(c -> c.getApprovalStatus() == CertificateApprovalStatus.PENDING_APPROVAL)
                .count();
        long approved = certificates.stream()
                .filter(c -> c.getApprovalStatus() == CertificateApprovalStatus.APPROVED)
                .count();
        long rejected = certificates.stream()
                .filter(c -> c.getApprovalStatus() == CertificateApprovalStatus.REJECTED)
                .count();
        return new CertificateReport(period, certificates, certificates.size(), pending, approved, rejected);
    }

    public byte[] renderReportPdf(CertificateReport report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(doc, baos);

        Color primary = new Color(0x16, 0x46, 0x55);
        Color light = new Color(0xE2, 0xE8, 0xF0);
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, primary);
        Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, primary);
        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);

        doc.open();
        Paragraph title = new Paragraph("Certificate Generation Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(8f);
        doc.add(title);

        Paragraph period = new Paragraph(report.period().label(), bodyFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(18f);
        doc.add(period);

        PdfPTable summary = new PdfPTable(4);
        summary.setWidthPercentage(100);
        summary.setSpacingAfter(18f);
        addSummaryCell(summary, "Generated", String.valueOf(report.total()), labelFont, bodyFont, light);
        addSummaryCell(summary, "Pending", String.valueOf(report.pending()), labelFont, bodyFont, light);
        addSummaryCell(summary, "Approved", String.valueOf(report.approved()), labelFont, bodyFont, light);
        addSummaryCell(summary, "Rejected", String.valueOf(report.rejected()), labelFont, bodyFont, light);
        doc.add(summary);

        PdfPTable table = new PdfPTable(new float[]{1.4f, 1.9f, 1.9f, 2.7f, 1.6f, 1.6f, 1.7f});
        table.setWidthPercentage(100);
        addHeaderCell(table, "Generated", headerFont, primary);
        addHeaderCell(table, "Certificate ID", headerFont, primary);
        addHeaderCell(table, "Student ID", headerFont, primary);
        addHeaderCell(table, "Student Name", headerFont, primary);
        addHeaderCell(table, "Program", headerFont, primary);
        addHeaderCell(table, "Status", headerFont, primary);
        addHeaderCell(table, "Issue Date", headerFont, primary);

        if (report.certificates().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No certificates found for this period.", cellFont));
            empty.setColspan(7);
            empty.setPadding(10f);
            table.addCell(empty);
        } else {
            for (Certificate certificate : report.certificates()) {
                addCell(table, certificate.getCreatedAt() != null ? DATE_TIME_FORMAT.format(certificate.getCreatedAt()) : "", cellFont);
                addCell(table, certificate.getCertificateId(), cellFont);
                addCell(table, certificate.getStudentId(), cellFont);
                addCell(table, certificate.getStudentName(), cellFont);
                addCell(table, certificate.getDegree(), cellFont);
                addCell(table, certificate.getApprovalStatus() != null ? certificate.getApprovalStatus().name() : "", cellFont);
                addCell(table, certificate.getIssueDate() != null ? DATE_FORMAT.format(certificate.getIssueDate()) : "", cellFont);
            }
        }
        doc.add(table);
        doc.close();
        return baos.toByteArray();
    }

    private ReportPeriod resolvePeriod(String mode, String date, String month, Integer year) {
        String normalizedMode = mode == null ? "month" : mode.trim().toLowerCase();
        LocalDate startDate;
        LocalDate endDate;
        String label;

        if ("day".equals(normalizedMode)) {
            startDate = parseDateOrToday(date);
            endDate = startDate.plusDays(1);
            label = "Daily report: " + DATE_FORMAT.format(startDate);
        } else if ("year".equals(normalizedMode)) {
            Year selectedYear = Year.of(year != null ? year : LocalDate.now().getYear());
            startDate = selectedYear.atDay(1);
            endDate = selectedYear.plusYears(1).atDay(1);
            label = "Yearly report: " + selectedYear;
        } else {
            YearMonth selectedMonth = parseMonthOrCurrent(month);
            startDate = selectedMonth.atDay(1);
            endDate = selectedMonth.plusMonths(1).atDay(1);
            normalizedMode = "month";
            label = "Monthly report: " + MONTH_FORMAT.format(selectedMonth);
        }

        return new ReportPeriod(
                normalizedMode,
                label,
                startDate.atStartOfDay(),
                endDate.atStartOfDay(),
                DATE_FORMAT.format(startDate),
                DATE_FORMAT.format(endDate.minusDays(1))
        );
    }

    private LocalDate parseDateOrToday(String date) {
        try {
            return date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    private YearMonth parseMonthOrCurrent(String month) {
        try {
            return month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        } catch (Exception ex) {
            return YearMonth.now();
        }
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont, Color border) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(label + "\n", labelFont));
        p.add(new Phrase(value, valueFont));
        PdfPCell cell = new PdfPCell(p);
        cell.setPadding(8f);
        cell.setBorderColor(border);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    public record CertificateReport(
            ReportPeriod period,
            List<Certificate> certificates,
            long total,
            long pending,
            long approved,
            long rejected
    ) {}

    public record ReportPeriod(
            String mode,
            String label,
            LocalDateTime start,
            LocalDateTime endExclusive,
            String startDate,
            String endDate
    ) {}
}
