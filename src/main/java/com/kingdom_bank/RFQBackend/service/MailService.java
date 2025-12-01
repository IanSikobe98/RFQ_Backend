package com.kingdom_bank.RFQBackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.ForkJoinPool;

@Service
@Slf4j
public class MailService {

    @Autowired
    @Qualifier("getMailSender")
    JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    public void sendMail(String to, String subject, String text) {
        log.info("Sending email to: [{}], subject: [{}], text: [{}] ", to, subject, text);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        ForkJoinPool.commonPool().execute(() -> {
            try {
                mailSender.send(message);
                log.info("Mail sent successfully ...");
            } catch (Exception e) {
                log.error("Error sending email : {}", e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
