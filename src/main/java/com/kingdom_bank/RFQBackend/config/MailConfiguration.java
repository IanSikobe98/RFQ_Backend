package com.kingdom_bank.RFQBackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfiguration {

    @Value("${spring.mail.host:\"smtp.tangazoletu.com\"}")
    private String host;
    @Value("${spring.mail.port:587}")
    private int port;
    @Value("${spring.mail.username:\"bulksms@tangazoletu.com\"}")
    private String username;
    @Value("${spring.mail.password:\"prsp@TANGAZOLETU15\"}")
    private String password;

    @Value("${spring.mail.properties.mail.transport.protocol:smtp}")
    private String transportProtocol;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private String starttlsEnable;

    @Value("${spring.mail.properties.mail.debug:true}")
    private boolean debug;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:*}")
    private String smtpSslTrust;

    @Bean(name = "getMailSender")
    public JavaMailSender getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", transportProtocol);
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.debug", debug);
        props.put("mail.smtp.ssl.trust", smtpSslTrust);

        return mailSender;
    }

}
