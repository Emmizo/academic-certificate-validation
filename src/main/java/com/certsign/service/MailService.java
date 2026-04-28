package com.certsign.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final MailSender mailSender;

    public MailService(ObjectProvider<MailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
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
}
