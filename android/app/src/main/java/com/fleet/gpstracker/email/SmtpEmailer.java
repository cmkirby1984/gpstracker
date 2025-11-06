package com.fleet.gpstracker.email;

import android.util.Log;

import com.fleet.gpstracker.models.GpsLocation;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * SMTP Email sender for GPS location updates
 * Uses JavaMail API to send emails
 */
public class SmtpEmailer {
    private static final String TAG = "SmtpEmailer";

    private String smtpHost;
    private int smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private boolean useSSL;
    private boolean useTLS;
    private String fromEmail;
    private String[] toEmails;

    /**
     * Constructor
     * TODO: Load configuration from SharedPreferences or config file
     */
    public SmtpEmailer() {
        // Default configuration - UPDATE THESE VALUES
        this.smtpHost = "smtp.gmail.com";  // TODO: Configure SMTP server
        this.smtpPort = 587;                // TODO: Configure port (587 for TLS, 465 for SSL)
        this.smtpUser = "your-email@gmail.com";  // TODO: Configure username
        this.smtpPassword = "your-app-password";  // TODO: Configure password
        this.useSSL = false;
        this.useTLS = true;
        this.fromEmail = "your-email@gmail.com";  // TODO: Configure from address
        this.toEmails = new String[]{"recipient@example.com"};  // TODO: Configure recipients
    }

    /**
     * Configure SMTP settings
     */
    public void configure(String host, int port, String user, String password,
                         boolean ssl, boolean tls, String from, String[] to) {
        this.smtpHost = host;
        this.smtpPort = port;
        this.smtpUser = user;
        this.smtpPassword = password;
        this.useSSL = ssl;
        this.useTLS = tls;
        this.fromEmail = from;
        this.toEmails = to;
    }

    /**
     * Send GPS location update email
     * @param location GPS location data
     * @return true if email sent successfully
     */
    public boolean sendLocationUpdate(GpsLocation location) {
        if (location == null || !location.isValid()) {
            Log.e(TAG, "Invalid location data");
            return false;
        }

        try {
            // Create email message
            String subject = EmailTemplate.generateSubject(location);
            String body = EmailTemplate.generateBody(location);

            return sendEmail(subject, body);

        } catch (Exception e) {
            Log.e(TAG, "Error sending location email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send email with custom subject and body
     * @param subject Email subject
     * @param body Email body (HTML)
     * @return true if sent successfully
     */
    public boolean sendEmail(String subject, String body) {
        try {
            // Create properties
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", "true");

            if (useSSL) {
                props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }

            if (useTLS) {
                props.put("mail.smtp.starttls.enable", "true");
            }

            // Create session
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPassword);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));

            // Add recipients
            for (String toEmail : toEmails) {
                message.addRecipient(Message.RecipientType.TO,
                                    new InternetAddress(toEmail));
            }

            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");

            // Send message
            Transport.send(message);

            Log.i(TAG, "Email sent successfully to " + toEmails.length + " recipients");
            return true;

        } catch (MessagingException e) {
            Log.e(TAG, "Email sending failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Test email configuration
     * @return true if test email sent successfully
     */
    public boolean sendTestEmail() {
        String subject = "Fleet GPS Tracker - Test Email";
        String body = "<html><body>" +
                     "<h2>Test Email</h2>" +
                     "<p>This is a test email from Fleet GPS Tracker application.</p>" +
                     "<p>If you received this email, your SMTP configuration is correct.</p>" +
                     "<p>Timestamp: " + new java.util.Date().toString() + "</p>" +
                     "</body></html>";

        Log.i(TAG, "Sending test email...");
        return sendEmail(subject, body);
    }

    // Getters and setters
    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public boolean isUseTLS() {
        return useTLS;
    }

    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String[] getToEmails() {
        return toEmails;
    }

    public void setToEmails(String[] toEmails) {
        this.toEmails = toEmails;
    }
}
