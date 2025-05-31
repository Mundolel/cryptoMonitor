package esp.proyecto.esptest.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class EmailService {
    // Configura estos según tu proveedor SMTP
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 465;
    private static final String SMTP_USER = "dickusbiggua@gmail.com";
    private static final String SMTP_PASS = "ddvw mrnt ecbx pzvl";  // usa App Password si es Gmail

    private final Session session;

    public EmailService() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });
    }

    /**
     * Envía un email de forma asíncrona.
     * @param to      Destinatario
     * @param subject Asunto
     * @param body    Cuerpo (HTML o texto plano)
     */
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String body) {
        return CompletableFuture.runAsync(() -> {
            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SMTP_USER));
                message.setRecipients(
                        Message.RecipientType.TO, InternetAddress.parse(to)
                );
                message.setSubject(subject);
                message.setContent(body, "text/html; charset=UTF-8");

                Transport.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
