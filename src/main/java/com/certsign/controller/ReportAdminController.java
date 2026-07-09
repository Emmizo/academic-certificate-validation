package com.certsign.controller;

import com.certsign.service.CertificateReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportAdminController {

    private final CertificateReportService certificateReportService;

    public ReportAdminController(CertificateReportService certificateReportService) {
        this.certificateReportService = certificateReportService;
    }

    @GetMapping("/admin/reports")
    public String reports(@RequestParam(value = "mode", defaultValue = "month") String mode,
                          @RequestParam(value = "date", required = false) String date,
                          @RequestParam(value = "month", required = false) String month,
                          @RequestParam(value = "year", required = false) Integer year,
                          Model model) {
        var report = certificateReportService.buildReport(mode, date, month, year);
        model.addAttribute("report", report);
        model.addAttribute("mode", report.period().mode());
        model.addAttribute("date", date);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        return "admin/reports";
    }

    @GetMapping("/admin/reports/pdf")
    public ResponseEntity<byte[]> reportPdf(@RequestParam(value = "mode", defaultValue = "month") String mode,
                                            @RequestParam(value = "date", required = false) String date,
                                            @RequestParam(value = "month", required = false) String month,
                                            @RequestParam(value = "year", required = false) Integer year) {
        var report = certificateReportService.buildReport(mode, date, month, year);
        byte[] pdf = certificateReportService.renderReportPdf(report);
        String filename = "certificate-report-" + report.period().mode() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
