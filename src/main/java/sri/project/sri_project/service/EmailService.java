package sri.project.sri_project.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender emailSender;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void enviarCorreoLogin(String emailDestino, String nombre) throws Exception {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("julcortezm.jys@gmail.com");
        helper.setTo(emailDestino);
        helper.setSubject("¡Bienvenido a la evolución agrícola con SRI!");

        String contenido = "<h2 style='color: #1f5f27; margin-top: 0;'>¡Hola " + nombre + "!</h2>" +
                "<p style='color: #17351d; font-size: 16px; line-height: 1.5;'>" +
                "En el <strong>Sistema de Riego Inteligente</strong> estamos comprometidos con el futuro de tus tierras. Nos emociona que formes parte de nuestra plataforma para optimizar, cuidar y potenciar tus cultivos mediante tecnología IoT avanzada.</p>" +
                "<p style='color: #17351d; font-size: 16px; line-height: 1.5;'>" +
                "A través de lecturas precisas y automatización a tu medida, te ayudamos a tomar las mejores decisiones para tus plantas mientras reduces el impacto ambiental y ahorras recursos valiosos. Tu dedicación, potenciada por nuestros sensores.</p>" +
                "<p style='color: #3d6138; font-size: 16px; font-weight: bold; margin-top: 30px;'>" +
                "Atentamente,<br>El Equipo de SRI</p>";

        helper.setText(construirHtmlEmail(contenido), true);
        emailSender.send(message);
    }

    public void enviarCorreoRegistro(String emailDestino, String nombre) throws Exception {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("julcortezm.jys@gmail.com");
        helper.setTo(emailDestino);
        helper.setSubject("🌱 ¡Te damos la bienvenida a SRI! Tu automatización agrícola comienza hoy");

        String contenido = "<h2 style='color: #1f5f27; margin-top: 0;'>¡Hola " + nombre + "!</h2>" +
                "<p style='color: #17351d; font-size: 16px; line-height: 1.5;'>" +
                "Es un placer darte la bienvenida al <strong>Sistema de Riego Inteligente (SRI)</strong>. Nos emociona acompañarte en este paso hacia una agricultura digital, eficiente y sostenible.</p>" +
                "<p style='color: #17351d; font-size: 16px; line-height: 1.5;'>" +
                "A partir de ahora, tienes el control total de tu entorno agrícola en la palma de tu mano. Gracias a nuestra integración con tecnología IoT, podrás:</p>" +
                "<ul style='color: #17351d; font-size: 16px; line-height: 1.5;'>" +
                "<li>Monitorear en tiempo real los niveles de humedad del suelo y la disponibilidad de agua en tus tanques.</li>" +
                "<li>Automatizar los ciclos de riego adaptados específicamente a las necesidades de cada uno de tus perfiles de cultivo.</li>" +
                "<li>Optimizar el consumo de recursos, maximizando la salud y el rendimiento de tus plantaciones de manera inteligente.</li>" +
                "</ul>" +
                "<p style='color: #17351d; font-size: 16px; line-height: 1.5;'>" +
                "Estamos aquí para ayudarte a transformar tus datos de campo en decisiones acertadas. ¡Explora tu panel de control y configura tu primer cultivo activo para empezar!</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:8080/login' style='background-color: #2f7d32; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 12px; font-weight: bold; display: inline-block;'>Ir a mi Panel de Control</a>" +
                "</div>" +
                "<p style='color: #3d6138; font-size: 16px; font-weight: bold;'>" +
                "Atentamente,<br>El Equipo de SRI</p>";

        helper.setText(construirHtmlEmail(contenido), true);
        emailSender.send(message);
    }

    private String construirHtmlEmail(String contenido) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='margin: 0; padding: 0; font-family: \"Inter\", Arial, sans-serif; background-color: #eff7eb;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #eff7eb; padding: 40px 20px;'>" +
                "<tr><td align='center'>" +
                "<table width='100%' max-width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(47, 125, 50, 0.1); max-width: 600px;'>" +
                "<tr><td style='background: linear-gradient(90deg, #2f7d32, #78bd63, #2f7d32); height: 6px;'></td></tr>" +
                "<tr><td style='padding: 40px 30px; text-align: center; border-bottom: 1px solid #eaf6e6;'>" +
                "<h1 style='color: #2f7d32; margin: 0; font-size: 24px; font-weight: 800;'>Sistema de Riego Inteligente</h1>" +
                "</td></tr>" +
                "<tr><td style='padding: 30px 40px; text-align: left;'>" +
                contenido +
                "</td></tr>" +
                "<tr><td style='padding: 20px 40px; background-color: #f7fbf4; text-align: center; border-top: 1px solid #eaf6e6;'>" +
                "<p style='color: #7ca66f; font-size: 13px; margin: 0;'>© " + java.time.Year.now().getValue() + " Sistema de Riego Inteligente. Todos los derechos reservados.</p>" +
                "</td></tr>" +
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    public void enviarReportePorCorreoConAdjunto(String emailDestino, String tituloReporte, byte[] pdfContent) throws Exception {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom("julcortezm.jys@gmail.com");
        helper.setTo(emailDestino);
        helper.setSubject("Nuevo reporte generado: " + tituloReporte);
        helper.setText("Hola,\n\n" +
                "Adjunto encontrarás el reporte que solicitaste: " + tituloReporte + ".\n" +
                "Puedes acceder a más detalles dentro de la plataforma del Sistema de Riego Inteligente.\n\n" +
                "Atentamente,\nEl Equipo de SRI");

        // Adjuntar el PDF
        helper.addAttachment("Reporte_Sistema_Riego.pdf", new ByteArrayResource(pdfContent));

        emailSender.send(message);
    }
}
