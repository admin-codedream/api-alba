package com.api.alba.email;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

@Service
public class EmailService {

    private final static String SENDER_EMAIL = "codedream.contact@gmail.com";
    private final static String SENDER_NAME = "코드드림";

    private Session init() {
        Properties props = System.getProperties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Authenticator auth = new EmailAuth();
        return Session.getDefaultInstance(props, auth);
    }

    @Async
    public void sendEmail(EmailDto emailDto) {
        try {
            Session session = init();
            MimeMessage msg = new MimeMessage(session);

            // 발신자 이메일 설정
            msg.setSentDate(new Date());
            msg.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));

            // 수신자 이메일
            InternetAddress toAddress = new InternetAddress(emailDto.getRecipient());
            msg.setRecipient(Message.RecipientType.TO, toAddress);
            // 이메일 제목
            msg.setSubject(emailDto.getEmailTitle(), "UTF-8");
            // 이메일 내용
            msg.setText(emailDto.getEmailContent(), "UTF-8", "html");

            //메일 발송
            Transport.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }


}
