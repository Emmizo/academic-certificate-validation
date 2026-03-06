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
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
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
     * Generates a styled A4 PDF representation of the given certificate and returns it as bytes.
     * <p>
     * The PDF is intentionally human‑friendly and does not expose low‑level cryptographic
     * details such as hashes or signatures. Verification should be performed via the
     * public portal using the certificate ID.
     */
    public byte[] renderCertificatePdf(Certificate certificate) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document doc = new Document(PageSize.A4, 50, 50, 72, 72);
        PdfWriter writer = PdfWriter.getInstance(doc, baos);

        Color primary = new Color(0x16, 0x46, 0x55);
        Color muted = new Color(0x55, 0x65, 0x81);
        Color lightRule = new Color(0xE2, 0xE8, 0xF0);

        Font titleFont = new Font(Font.TIMES_ROMAN, 26, Font.BOLD, primary);
        Font subTitleFont = new Font(Font.TIMES_ROMAN, 11, Font.ITALIC, muted);
        Font nameFont = new Font(Font.TIMES_ROMAN, 20, Font.BOLD, primary);
        Font bodyFont = new Font(Font.TIMES_ROMAN, 12, Font.NORMAL, new Color(0x33, 0x33, 0x33));
        Font labelFont = new Font(Font.TIMES_ROMAN, 10, Font.BOLD, primary);
        Font valueFont = new Font(Font.TIMES_ROMAN, 10, Font.NORMAL, Color.BLACK);
        Font sigLabelFont = new Font(Font.TIMES_ROMAN, 9, Font.NORMAL, muted);

        Font footerFont = new Font(Font.TIMES_ROMAN, 9, Font.NORMAL, muted);
        HeaderFooter footer = new HeaderFooter(
                new Phrase("To validate this document, use the Certificate ID on the public verification portal.", footerFont),
                false
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setBorder(Rectangle.TOP);
        doc.setFooter(footer);

        doc.open();

        // Double border for a formal certificate look
        PdfContentByte cb = writer.getDirectContent();
        float borderMargin = 36f;
        float topBorderOffset = 130f;
        float outerLeft = doc.left() + borderMargin;
        float outerBottom = doc.bottom() + borderMargin;
        float outerRight = doc.right() - borderMargin;
        float outerTop = doc.top() - topBorderOffset;
        cb.setLineWidth(1.5f);
        cb.setColorStroke(primary);
        cb.rectangle(outerLeft, outerBottom, outerRight - outerLeft, outerTop - outerBottom);
        cb.stroke();
        float inset = 6f;
        cb.setLineWidth(0.75f);
        cb.setColorStroke(lightRule);
        cb.rectangle(outerLeft + inset, outerBottom + inset, (outerRight - outerLeft) - 2 * inset, (outerTop - outerBottom) - 2 * inset);
        cb.stroke();
        cb.setColorStroke(Color.BLACK);

        // Heading
        Paragraph heading = new Paragraph("Academic Certificate", titleFont);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingBefore(80f);
        heading.setSpacingAfter(12f);
        doc.add(heading);

        Paragraph sub = new Paragraph("Digitally signed and verifiable via CertSign", subTitleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(24f);
        doc.add(sub);

        // Thin divider line
        PdfPTable dividerTable = new PdfPTable(1);
        dividerTable.setWidthPercentage(40);
        dividerTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        dividerTable.setSpacingAfter(32f);
        PdfPCell dividerCell = new PdfPCell(new Phrase(" "));
        dividerCell.setBorder(PdfPCell.BOTTOM);
        dividerCell.setBorderWidth(0.5f);
        dividerCell.setBorderColor(lightRule);
        dividerCell.setFixedHeight(8f);
        dividerTable.addCell(dividerCell);
        doc.add(dividerTable);

        // Main certificate text
        Paragraph intro = new Paragraph("This is to certify that", bodyFont);
        intro.setAlignment(Element.ALIGN_CENTER);
        intro.setSpacingAfter(16f);
        doc.add(intro);

        Paragraph name = new Paragraph(
                certificate.getStudentName() != null ? certificate.getStudentName() : "____________________",
                nameFont
        );
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingAfter(16f);
        doc.add(name);

        String degreeText = certificate.getDegree() != null ? certificate.getDegree() : "____________________";
        Paragraph degreePara = new Paragraph(
                "has successfully completed the " + degreeText + " program.",
                bodyFont
        );
        degreePara.setAlignment(Element.ALIGN_CENTER);
        degreePara.setSpacingAfter(10f);
        doc.add(degreePara);

        String institutionText = certificate.getInstitution() != null ? certificate.getInstitution() : "____________________";
        Paragraph institutionPara = new Paragraph(
                "at " + institutionText + ".",
                bodyFont
        );
        institutionPara.setAlignment(Element.ALIGN_CENTER);
        institutionPara.setSpacingAfter(20f);
        doc.add(institutionPara);

        // Signature line (professional touch)
        Paragraph sigLine = new Paragraph("_________________________", bodyFont);
        sigLine.setAlignment(Element.ALIGN_CENTER);
        sigLine.setSpacingBefore(40f);
        sigLine.setSpacingAfter(4f);
        doc.add(sigLine);
        Paragraph sigLabel = new Paragraph("Authorized Signature", sigLabelFont);
        sigLabel.setAlignment(Element.ALIGN_CENTER);
        sigLabel.setSpacingAfter(16f);
        doc.add(sigLabel);

        // Thin rule above metadata block
        PdfPTable ruleTable = new PdfPTable(1);
        ruleTable.setWidthPercentage(100);
        ruleTable.setSpacingBefore(0f);
        ruleTable.setSpacingAfter(12f);
        PdfPCell ruleCell = new PdfPCell(new Phrase(" "));
        ruleCell.setBorder(PdfPCell.TOP);
        ruleCell.setBorderWidth(0.5f);
        ruleCell.setBorderColor(lightRule);
        ruleCell.setFixedHeight(2f);
        ruleTable.addCell(ruleCell);
        doc.add(ruleTable);

        // Metadata row at bottom: Certificate ID | Student ID | Issue Date
        PdfPTable metaTable = new PdfPTable(3);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingBefore(100f); // pushes block to bottom of bordered area
        metaTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        metaTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        metaTable.getDefaultCell().setVerticalAlignment(Element.ALIGN_BOTTOM);

        addMetadataCell(metaTable, "Certificate ID", certificate.getCertificateId(), labelFont, valueFont);
        addMetadataCell(metaTable, "Student ID", certificate.getStudentId(), labelFont, valueFont);
        addMetadataCell(
                metaTable,
                "Issue Date",
                certificate.getIssueDate() != null ? DATE_FORMAT.format(certificate.getIssueDate()) : "",
                labelFont,
                valueFont
        );
        doc.add(metaTable);

        doc.close();
        return baos.toByteArray();
    }

    /**
     * Adds a single metadata cell with label above value (used in a 3‑column footer table).
     */
    private void addMetadataCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(label + "\n", labelFont));
        p.add(new Phrase(value != null ? value : "", valueFont));

        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}

