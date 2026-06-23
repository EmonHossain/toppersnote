package com.sharenote.verification;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.sharenote.verification.messaging.EmailVerificationMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
public class VerificationEmailTemplate {

  private static final DateTimeFormatter EXPIRATION_FORMATTER = DateTimeFormatter
      .ofPattern("MMM d, uuuu 'at' HH:mm 'UTC'").withZone(ZoneOffset.UTC);

  // Renders a responsive HTML body with escaped user-controlled values.
  public String renderHtml(EmailVerificationMessage message) {
    String firstName = HtmlUtils.htmlEscape(message.recipientFirstName());
    String verificationUrl = HtmlUtils.htmlEscape(message.verificationUrl());
    String expiresAt = HtmlUtils.htmlEscape(EXPIRATION_FORMATTER.format(message.expiresAt()));

    Map<String, Object> context = Map.of(
        "firstName", firstName,
        "verificationUrl", verificationUrl,
        "expiresAt", expiresAt);

    try {
      ClassPathResource resource = new ClassPathResource("email-verification-template.html");
      String emailTemplateHtml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      return Mustache.compiler().compile(emailTemplateHtml).execute(context);
    } catch (IOException | MustacheException e) {
      log.warn(
          "Falling back to inline verification email template after classpath template rendering failed, reasonType={}",
          e.getClass().getSimpleName());
    }

    return getFalbackRenderedHtmlEmail(firstName, verificationUrl, expiresAt);
  }

  private String getFalbackRenderedHtmlEmail(String firstName, String verificationUrl, String expiresAt) {
    return """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Verify your ShareNote email</title>
        </head>
        <body style="margin:0;background:#f4f7fb;font-family:Inter,Segoe UI,Arial,sans-serif;color:#172033;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                 style="background:#f4f7fb;padding:32px 12px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                       style="max-width:620px;background:#ffffff;border-radius:20px;overflow:hidden;
                              box-shadow:0 12px 40px rgba(31,42,68,.12);">
                  <tr>
                    <td style="padding:34px 40px;background:linear-gradient(135deg,#5b5ce2,#7c3aed);color:#ffffff;">
                      <div style="font-size:24px;font-weight:800;letter-spacing:-.4px;">ShareNote</div>
                      <div style="margin-top:8px;font-size:14px;opacity:.88;">Learn together. Share smarter.</div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:40px;">
                      <h1 style="margin:0 0 16px;font-size:28px;line-height:1.25;color:#172033;">
                        Verify your university email
                      </h1>
                      <p style="margin:0 0 18px;font-size:16px;line-height:1.65;color:#526078;">
                        Hi %s, confirm your email address to unlock ShareNote uploads, downloads,
                        study groups, and class resources.
                      </p>
                      <table role="presentation" cellspacing="0" cellpadding="0" style="margin:28px 0;">
                        <tr>
                          <td style="border-radius:12px;background:#5b5ce2;">
                            <a href="%s" target="_blank"
                               style="display:inline-block;padding:15px 26px;color:#ffffff;text-decoration:none;
                                      font-size:16px;font-weight:700;border-radius:12px;">
                              Verify email address
                            </a>
                          </td>
                        </tr>
                      </table>
                      <p style="margin:0 0 20px;font-size:14px;line-height:1.6;color:#69768c;">
                        This secure link expires on <strong>%s</strong>. If the button does not work,
                        copy and paste this URL into your browser:
                      </p>
                      <p style="margin:0;padding:14px 16px;background:#f4f5ff;border-radius:10px;
                                font-size:12px;line-height:1.55;word-break:break-all;color:#4c51a8;">%s</p>
                      <p style="margin:26px 0 0;font-size:13px;line-height:1.6;color:#8791a5;">
                        If you did not create a ShareNote account, you can safely ignore this email.
                        Never share this verification link with anyone.
                      </p>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:22px 40px;background:#f8f9fc;font-size:12px;color:#8b95a7;text-align:center;">
                      ShareNote · Academic knowledge, shared securely
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(firstName, verificationUrl, expiresAt, verificationUrl);
  }

  // Renders a plain-text alternative for clients that do not display HTML.
  public String renderPlainText(EmailVerificationMessage message) {
    return """
        Hi %s,

        Verify your university email to unlock ShareNote uploads, downloads, study groups, and class resources:

        %s

        This link expires on %s.

        If you did not create a ShareNote account, ignore this email. Never share this verification link.
        """.formatted(
        message.recipientFirstName(),
        message.verificationUrl(),
        EXPIRATION_FORMATTER.format(message.expiresAt()));
  }
}
