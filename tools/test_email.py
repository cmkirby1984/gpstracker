#!/usr/bin/env python3
"""
Email Configuration Tester
Tests SMTP email sending configuration
"""

import smtplib
import sys
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from datetime import datetime

def test_email(smtp_host, smtp_port, smtp_user, smtp_password, from_email, to_email, use_tls=True):
    """Test email sending"""

    print("=" * 60)
    print("Email Configuration Test")
    print("=" * 60)
    print()
    print(f"SMTP Host: {smtp_host}")
    print(f"SMTP Port: {smtp_port}")
    print(f"Username: {smtp_user}")
    print(f"From: {from_email}")
    print(f"To: {to_email}")
    print(f"TLS: {'Yes' if use_tls else 'No'}")
    print()

    try:
        # Create message
        msg = MIMEMultipart()
        msg['From'] = from_email
        msg['To'] = to_email
        msg['Subject'] = "Fleet GPS Tracker - Test Email"

        body = f"""
        <html>
        <body>
        <h2>Test Email from Fleet GPS Tracker</h2>
        <p>This is a test email to verify your SMTP configuration.</p>
        <p>If you received this email, your configuration is correct!</p>
        <p>Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        </body>
        </html>
        """

        msg.attach(MIMEText(body, 'html'))

        # Connect and send
        print("Connecting to SMTP server...")
        server = smtplib.SMTP(smtp_host, smtp_port)

        if use_tls:
            print("Starting TLS...")
            server.starttls()

        print("Logging in...")
        server.login(smtp_user, smtp_password)

        print("Sending email...")
        server.send_message(msg)
        server.quit()

        print()
        print("✓ Email sent successfully!")
        print()
        return True

    except Exception as e:
        print()
        print(f"✗ Email sending failed: {str(e)}")
        print()
        return False

def main():
    if len(sys.argv) < 7:
        print("Usage: python test_email.py <smtp_host> <smtp_port> <smtp_user> <smtp_password> <from_email> <to_email> [use_tls]")
        print()
        print("Example:")
        print("  python test_email.py smtp.gmail.com 587 user@gmail.com app_password user@gmail.com recipient@example.com true")
        sys.exit(1)

    smtp_host = sys.argv[1]
    smtp_port = int(sys.argv[2])
    smtp_user = sys.argv[3]
    smtp_password = sys.argv[4]
    from_email = sys.argv[5]
    to_email = sys.argv[6]
    use_tls = sys.argv[7].lower() == 'true' if len(sys.argv) > 7 else True

    test_email(smtp_host, smtp_port, smtp_user, smtp_password, from_email, to_email, use_tls)

if __name__ == "__main__":
    main()
