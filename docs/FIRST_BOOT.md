# First Boot Procedure

Step-by-step guide for initial system setup and testing.

## Prerequisites

- ESP32 wired according to [HARDWARE_SETUP.md](HARDWARE_SETUP.md)
- Firmware flashed to ESP32
- Android tablet with app installed
- SMTP email account configured

## Phase 1: ESP32 First Boot

### Step 1: Connect Serial Monitor

```bash
cd firmware
platformio device monitor
```

### Step 2: Power On ESP32

You should see:
```
========================================
  Fleet GPS Tracker System
  Firmware: 1.0.0
  Device: GPS001
========================================

Initializing GPS module...
GPS initialized in MOCK MODE  (if mock enabled)
Initializing Bluetooth...
Bluetooth initialized successfully
Device name: FleetGPS_001
System Ready!
```

**✓ Success**: System startup complete
**✗ Failure**: See troubleshooting section

### Step 3: Wait for GPS Fix

If using real GPS (not mock mode):
```
GPS module responding, waiting for fix...
GPS fix acquired!
Location: 37.774900, -122.419400
Satellites: 8
```

**Note**: First fix can take 30-60 seconds outdoors.

## Phase 2: Android App Setup

### Step 1: Install and Launch App

```bash
cd android
./gradlew installDebug
```

Launch "Fleet GPS Tracker" from app drawer.

### Step 2: Grant Permissions

App will request:
- Bluetooth permissions
- Notification permissions (Android 13+)

**Grant all permissions** for proper operation.

### Step 3: Configure ESP32 Connection

1. Note ESP32 Bluetooth MAC address from serial monitor
2. In app, go to Settings (TODO: Settings screen not yet implemented)
3. Enter ESP32 MAC address
4. Save settings

**For now**: Edit `ConfigManager` defaults in code or use preferences.

### Step 4: Configure Email

Set up SMTP email:
- SMTP Host: `smtp.gmail.com` (or your provider)
- Port: `587` (TLS) or `465` (SSL)
- Username: Your email
- Password: App password (for Gmail)
- From Email: Your email
- To Emails: Recipient addresses (comma-separated)

### Gmail Setup

1. Enable 2-factor authentication
2. Generate App Password:
   - Google Account → Security → App Passwords
   - Generate password for "Mail" / "Other"
3. Use generated password in app

## Phase 3: Connection Test

### Step 1: Connect to ESP32

1. In app, tap "Connect" button
2. Wait for connection (5-10 seconds)
3. Status should change to "Connected" (green)

**Expected Serial Output**:
```
Bluetooth connection established
```

### Step 2: Verify Message Reception

Wait for next GPS update (up to 5 minutes in production, faster in mock mode).

**Android App**:
- "Messages: 1" counter increments
- Last location display updates
- Toast notification appears

**Serial Monitor**:
```
--- GPS Update Cycle ---
Update sent successfully (#1)
Location: 37.774900, -122.419400
--- End Update Cycle ---
```

### Step 3: Verify Email Sending

If auto-email enabled:
- Check recipient email inbox
- Should receive formatted GPS update
- Includes Google Maps link

**Test Email Manually**:
```bash
cd tools
python test_email.py smtp.gmail.com 587 user@gmail.com app_password from@gmail.com to@example.com true
```

## Phase 4: System Validation

### Run Full Test Cycle

1. **ESP32 Sends Update**
   - Serial: "Update sent successfully"
   - LED blinks once

2. **Android Receives Data**
   - Status: Messages counter increments
   - Location display updates
   - Database entry created

3. **Email Sent**
   - "Email sent successfully" toast
   - Emails counter increments
   - Email received

### Verify in Serial Monitor

```bash
platformio device monitor
```

Look for:
- Regular GPS updates every 5 minutes
- "Update sent successfully" messages
- No errors or warnings

### Verify in Android App

- Connection status: Connected (green)
- Messages counter incrementing
- Emails counter incrementing
- Last location updating

## Phase 5: Extended Testing

### Leave Running for 1 Hour

Monitor for:
- Consistent message delivery
- No connection drops
- Successful email sending
- No crashes or errors

### Check Database

Use Android Studio Database Inspector:
1. Open Android Studio
2. View → Tool Windows → App Inspection
3. Select device and app
4. Browse `locations` table
5. Verify entries being created

## Configuration Checklist

Before production deployment:

### ESP32 Configuration
- [ ] `DEVICE_SERIAL` set to unique ID
- [ ] `BT_DEVICE_NAME` set uniquely
- [ ] `UPDATE_INTERVAL_MS` set appropriately
- [ ] `USE_MOCK_GPS` set to 0 (for real GPS)
- [ ] GPS pins verified correct
- [ ] Hardware connections solid

### Android Configuration
- [ ] ESP32 MAC address configured
- [ ] SMTP server settings correct
- [ ] Email recipients configured
- [ ] Permissions granted
- [ ] Auto-connect enabled (optional)
- [ ] Auto-send email enabled

### System Verification
- [ ] ESP32 boots successfully
- [ ] GPS fix acquired (if real GPS)
- [ ] Bluetooth connection stable
- [ ] Messages received reliably
- [ ] Emails sending successfully
- [ ] No errors in logs

## Troubleshooting First Boot

### ESP32 Won't Boot

- Check power supply
- Verify firmware flashed correctly
- Check serial baud rate (115200)
- Try power cycle

### GPS No Fix

- Move outdoors with clear sky view
- Wait 60+ seconds for cold start
- Check GPS wiring
- Verify GPS module LED active

### Bluetooth Not Visible

- Check Bluetooth enabled on tablet
- Verify unique device name
- Try ESP32 power cycle
- Check for duplicate device names

### App Won't Connect

- Verify MAC address correct
- Check Bluetooth permissions
- Ensure ESP32 is powered and advertising
- Try unpair and re-pair

### No Messages Received

- Check connection status in app
- Verify ESP32 sending (check serial)
- Check UPDATE_INTERVAL_MS not too long
- Try disconnecting and reconnecting

### Email Not Sending

- Test SMTP config with `test_email.py`
- Check internet connection
- Verify Gmail app password (not regular password)
- Check email in spam folder

## Next Steps

After successful first boot:

1. **Extended Testing**: Run for 24 hours
2. **Enclosure**: Install in weatherproof housing
3. **Mounting**: Install in vehicle/equipment
4. **Documentation**: Record MAC addresses and serials
5. **Deployment**: Begin operational use

## Support

For detailed troubleshooting, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
