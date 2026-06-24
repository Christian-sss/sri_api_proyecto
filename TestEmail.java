import java.util.Properties;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class TestEmail {
    public static void main(String[] args) {
        String to = "julcortezm.jys@gmail.com";
        String from = "julcortezm.jys@gmail.com";
        String host = "smtp.gmail.com";

        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("julcortezm.jys@gmail.com", "egjlvqmmovkzqvhb");
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("Prueba de correo");
            message.setText("Este es un correo de prueba.");
            System.out.println("Enviando...");
            Transport.send(message);
            System.out.println("Enviado exitosamente!");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
