// SDLC Phase: Implementation
// Component: CertificatePdfService
// Requirements covered: FR-05, NFR-03
// Description: Renders a human-readable PDF representation of a certificate
package com.certsign.service;

import com.certsign.model.Certificate;
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
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class CertificatePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates a styled A4 PDF representation of the given certificate,
     * including primary details and cryptographic metadata, and returns it as bytes.
     */
    public byte[] renderCertificatePdf(Certificate certificate) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Color primary = new Color(0x16, 0x46, 0x55);
        Color muted = new Color(0x55, 0x65, 0x81);

        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, primary);
        Font subTitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, muted);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, primary);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);

        Paragraph heading = new Paragraph("Academic Certificate", titleFont);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingAfter(4f);
        doc.add(heading);

        Paragraph sub = new Paragraph("Digitally signed and verifiable via CertSign", subTitleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20f);
        doc.add(sub);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10f);
        infoTable.setSpacingAfter(10f);
        infoTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

        addRow(infoTable, "Certificate ID", certificate.getCertificateId(), labelFont, valueFont);
        addRow(infoTable, "Student Name", certificate.getStudentName(), labelFont, valueFont);
        addRow(infoTable, "Student ID", certificate.getStudentId(), labelFont, valueFont);
        addRow(infoTable, "Degree / Program", certificate.getDegree(), labelFont, valueFont);
        addRow(infoTable, "Institution", certificate.getInstitution(), labelFont, valueFont);
        addRow(infoTable, "Issue Date",
                certificate.getIssueDate() != null ? DATE_FORMAT.format(certificate.getIssueDate()) : "",
                labelFont, valueFont);
        doc.add(infoTable);

        Paragraph cryptoHeading = new Paragraph("Cryptographic Details", labelFont);
        cryptoHeading.setSpacingBefore(10f);
        cryptoHeading.setSpacingAfter(6f);
        doc.add(cryptoHeading);

        Paragraph hash = new Paragraph("Document Hash (SHA-256): " +
                safe(certificate.getDocumentHash()), valueFont);
        hash.setSpacingAfter(4f);
        doc.add(hash);

        Paragraph sig = new Paragraph("Digital Signature (Base64, truncated): " +
                abbreviate(safe(certificate.getDigitalSignature()), 160), valueFont);
        sig.setSpacingAfter(8f);
        doc.add(sig);

        PdfPTable metaTable = new PdfPTable(3);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingBefore(4f);
        metaTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        addRow(metaTable, "Key Pair ID", certificate.getKeyPair() != null ? String.valueOf(certificate.getKeyPair().getId()) : "-", labelFont, valueFont);
        addRow(metaTable, "Signed At", certificate.getCreatedAt() != null ? DATE_TIME_FORMAT.format(certificate.getCreatedAt()) : "-", labelFont, valueFont);
        addRow(metaTable, "Issued By", certificate.getIssuedBy() != null ? certificate.getIssuedBy().getUsername() : "-", labelFont, valueFont);
        doc.add(metaTable);

        Paragraph footer = new Paragraph(
                "To validate this document, use the Certificate ID on the public verification portal.",
                new Font(Font.HELVETICA, 9, Font.NORMAL, muted));
        footer.setSpacingBefore(24f);
        doc.add(footer);

        doc.close();
        return baos.toByteArray();
    }

    /**
     * Adds a two‑column label/value row to the provided PDF table.
     */
    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPaddingBottom(4f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPaddingBottom(4f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * Truncates a long string to {@code maxLen} characters and appends ellipsis if needed.
     */
    private String abbreviate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    /**
     * Returns a non‑null string, converting {@code null} to the empty string.
     */
    private String safe(String v) {
        return v == null ? "" : v;
    }
}

