package com.certsign.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from-name:IPRC Tumba College}")
    private String fromName;

    @Value("${app.mail.from-address:noreply@tumbacollege.rw}")
    private String fromAddress;

    public MailService(ObjectProvider<JavaMailSender> javaMailSenderProvider) {
        this.javaMailSender = javaMailSenderProvider.getIfAvailable();
    }

    public boolean send(String to, String subject, String body) {
        return sendWithAttachment(to, subject, body, null, null, null);
    }

    public boolean sendWithAttachment(
            String to,
            String subject,
            String body,
            String attachmentFilename,
            byte[] attachmentBytes,
            String contentType
    ) {
        if (javaMailSender == null) {
            return false;
        }
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            if (attachmentFilename != null && attachmentBytes != null) {
                helper.addAttachment(
                        attachmentFilename,
                        new org.springframework.core.io.ByteArrayResource(attachmentBytes),
                        contentType
                );
            }
            javaMailSender.send(mimeMessage);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
