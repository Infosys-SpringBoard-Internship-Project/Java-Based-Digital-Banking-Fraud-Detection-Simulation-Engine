package com.example.infosys_project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean startTlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean startTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:smtp.gmail.com}")
    private String sslTrust;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:20000}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:20000}")
    private int timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:20000}")
    private int writeTimeout;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(trim(host));
        mailSender.setPort(port);
        mailSender.setUsername(trim(username));
        mailSender.setPassword(stripWhitespace(password));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnable));
        props.put("mail.smtp.starttls.required", String.valueOf(startTlsRequired));
        props.put("mail.smtp.ssl.trust", trim(sslTrust));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.auth.mechanisms", "LOGIN PLAIN");
        props.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(timeout));
        props.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));

        String maskedUser = mask(trim(username));
        log.info("MailConfig initialized. host='{}', port={}, username='{}', auth={}, starttls={}, timeouts(ms)=[{}/{}/{}]",
                trim(host), port, maskedUser, smtpAuth, startTlsEnable, connectionTimeout, timeout, writeTimeout);

        return mailSender;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int at = value.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at - 1);
    }

    private String stripWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "");
    }
}
