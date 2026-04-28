package com.certsign.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final MailSender mailSender;
    private final JavaMailSender javaMailSender;

    public MailService(
            ObjectProvider<MailSender> mailSenderProvider,
            ObjectProvider<JavaMailSender> javaMailSenderProvider
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.javaMailSender = javaMailSenderProvider.getIfAvailable();
    }

    public boolean send(String to, String subject, String body) {
        if (mailSender == null) {
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        return true;
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
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(
                    attachmentFilename,
                    new org.springframework.core.io.ByteArrayResource(attachmentBytes),
                    contentType
            );
            javaMailSender.send(mimeMessage);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
