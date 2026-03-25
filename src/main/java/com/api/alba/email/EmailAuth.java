package com.api.alba.email;

import lombok.Getter;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

@Getter
public class EmailAuth extends Authenticator {

    PasswordAuthentication passwordAuthentication;

    public EmailAuth() {
        String mailId = "codedream.contact@gmail.com";
        String mailPassword = "ixwc feeg nkou sogu";

        passwordAuthentication = new PasswordAuthentication(mailId, mailPassword);
    }
}
