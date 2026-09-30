package com.oncall.alert.service;

import com.oncall.alert.config.AppProperties;
import com.oncall.alert.model.AlertReport;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    public void sendAlertReport(String recipientEmail, AlertReport report) {
        try {
            Context context = new Context();
            context.setVariable("report", report);

            String htmlBody = templateEngine.process("alert-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.getFromEmail());
            helper.setTo(recipientEmail);
            helper.setSubject(String.format(
                    "[%s] Document Generation Errors — %d errors in last %d min",
                    report.getReportId(),
                    report.getTotalErrorCount(),
                    appProperties.getLogWindowMinutes()));
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Alert email sent to {} for report {}",
                    recipientEmail, report.getReportId());

        } catch (Exception e) {
            log.error("Failed to send alert email to {}", recipientEmail, e);
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }
}
