package com.hakyung.barleyssal_spring.application.auth.service.password;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendPasswordResetMail(String email, String resetUrl) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Flowerable 비밀번호 재설정");
        message.setText(
                "아래 링크를 클릭하여 비밀번호를 재설정하세요.\n\n"
                        + resetUrl
                        + "\n\n10분 후 만료됩니다."
        );

        mailSender.send(message);
    }
}