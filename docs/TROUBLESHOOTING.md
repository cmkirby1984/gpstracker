# Troubleshooting Guide

Solutions for common issues with Fleet GPS Tracking System.

## ESP32 Firmware Issues

### Won't Flash Firmware

**Symptom**: Upload fails with errors

**Solutions**:
1. Check USB cable (must support data, not just charging)
2. Hold BOOT button while uploading
3. Verify correct COM port: `platformio device list`
4. Install USB drivers (CP210x or CH340)
5. Try different USB port
6. Lower upload speed in `platformio.ini`:
   ```ini
   upload_speed = 115200
   ```

### No Serial Output

**Symptom**: Serial monitor blank

**Solutions**:
1. Check baud rate matches (115200)
2. Verify correct COM port
3. Press EN/Reset button on ESP32
4. Check USB cable connection
5. Try different terminal program

### Boot Loop

**Symptom**: Continuous restarts

**Solutions**:
1. Check power supply (needs stable 5V, 500mA+)
2. Review code for infinite loops
3. Check for memory leaks
4. Disable watchdog temporarily:
   ```cpp
   #define ENABLE_WATCHDOG 0
   ```
5. Check for short circuits in wiring

## GPS Module Issues

### No GPS Data

**Symptom**: "GPS module not responding"

**Solutions**:
1. Check wiring:
   - ESP32 GPIO16 ← GPS TX (correct direction!)
   - ESP32 GPIO17 → GPS RX
2. Verify 3.3V power (NOT 5V!)
3. Check GPS module LED is on
4. Try swapping TX/RX connections
5. Test GPS separately with serial adapter
6. Verify `GPS_BAUD_RATE 9600`

### No GPS Fix

**Symptom**: GPS responding but no location

**Solutions**:
1. Move outdoors with clear sky view
2. Wait longer (cold start: 30-60 seconds)
3. Check GPS antenna connection
4. Move away from buildings/trees
5. Try different location
6. Check GPS module isn't faulty:
   ```bash
   # In serial monitor, look for NMEA sentences
   # Should see $GPGGA, $GPRMC, etc.
   ```

### Weak GPS Signal

**Symptom**: Frequent fix loss, low satellite count

**Solutions**:
1. Improve antenna placement (outdoors, horizontal)
2. Move away from metal objects
3. Check antenna connection
4. Lower requirements temporarily:
   ```cpp
   #define MIN_SATELLITES 3
   #define MAX_HDOP 10.0
   ```

## Bluetooth Issues

### ESP32 Not Visible

**Symptom**: Can't find device in Bluetooth scan

**Solutions**:
1. Check Bluetooth initialized:
   ```
   # Look in serial output
   Bluetooth initialized successfully
   ```
2. Verify unique device name
3. Power cycle ESP32
4. Check no duplicate names nearby
5. Try from different Android device
6. Verify Bluetooth not disabled in code

### Can't Pair/Connect

**Symptom**: Pairing fails or times out

**Solutions**:
1. Verify MAC address correct
2. Remove old pairing from tablet
3. Enter PIN if prompted (default: 1234)
4. Check Bluetooth permissions granted
5. Restart both devices
6. Check ESP32 logs for errors

### Connection Drops

**Symptom**: Connected then disconnects

**Solutions**:
1. Keep devices closer (< 10m)
2. Remove obstacles between devices
3. Check for interference (WiFi, other BT)
4. Power cycle both devices
5. Check ESP32 not resetting (power issue)
6. Look for errors in serial logs

## Android App Issues

### App Crashes on Launch

**Symptom**: Immediate crash when opening

**Solutions**:
1. Check logcat for errors:
   ```bash
   adb logcat | grep AndroidRuntime
   ```
2. Grant all required permissions
3. Clear app data: Settings → Apps → Fleet GPS Tracker → Clear Data
4. Reinstall app
5. Check Android version >= 7.0

### Permissions Not Working

**Symptom**: Permission dialogs don't appear

**Solutions**:
1. Manually grant in Settings:
   - Settings → Apps → Fleet GPS Tracker → Permissions
2. Check Android version for permission requirements
3. Reinstall app (fresh permissions)

### Messages Not Received

**Symptom**: Connected but no data

**Solutions**:
1. Check ESP32 sending (serial monitor)
2. Verify message format correct
3. Check for parsing errors in logcat
4. Try test message:
   ```
   GPS001|37.774900|-122.419400|270.50|2025-11-06T12:00:00Z|END
   ```
5. Verify Bluetooth data actually transmitted

### Database Errors

**Symptom**: App errors related to database

**Solutions**:
1. Clear app data (WARNING: loses history)
2. Check storage not full
3. Update Room database version
4. Check logcat for SQL errors
5. Reinstall app

## Email Issues

### Emails Not Sending

**Symptom**: No emails received

**Solutions**:
1. Test SMTP config:
   ```bash
   python tools/test_email.py smtp.gmail.com 587 user@gmail.com app_password from@gmail.com to@example.com
   ```
2. Check internet connection
3. Verify SMTP credentials correct
4. For Gmail, use App Password (not regular password):
   - Google Account → Security → 2FA → App Passwords
5. Check spam folder
6. Verify recipient addresses correct
7. Check email in Android logcat:
   ```bash
   adb logcat | grep SmtpEmailer
   ```

### Gmail Authentication Failed

**Symptom**: "Authentication failed" errors

**Solutions**:
1. Enable 2-factor authentication
2. Generate App Password
3. Use App Password in app (not account password)
4. Verify username is full email address
5. Check "Less secure app access" NOT required with App Password

### Email Delayed

**Symptom**: Emails arrive late

**Solutions**:
1. Check internet speed
2. Verify SMTP server responsive
3. Look for queuing issues
4. Check Android not battery-optimizing app
5. Disable battery optimization:
   - Settings → Apps → Fleet GPS Tracker → Battery → Unrestricted

## System Integration Issues

### No End-to-End Communication

**Symptom**: Everything seems working but no results

**Troubleshooting Flow**:
1. **ESP32 Side**:
   ```bash
   platformio device monitor
   # Look for:
   # - "GPS fix acquired"
   # - "Update sent successfully"
   ```

2. **Bluetooth**:
   - ESP32: "Data sent via Bluetooth"
   - Android: "Connected" status

3. **Android Side**:
   ```bash
   adb logcat | grep FleetGPS
   # Look for:
   # - "Received message"
   # - "Successfully parsed"
   # - "Email sent successfully"
   ```

4. **Email**:
   - Check spam folder
   - Verify recipient address
   - Check SMTP logs

### Intermittent Problems

**Symptom**: Works sometimes, not others

**Solutions**:
1. Check power supply stability
2. Monitor ESP32 temperature (overheating)
3. Check for memory leaks (increasing RAM usage)
4. Look for timing issues (race conditions)
5. Enable detailed logging:
   ```cpp
   #define LOG_LEVEL 5
   ```

## Performance Issues

### ESP32 Running Slow

**Solutions**:
1. Reduce log level
2. Check for blocking code
3. Review loop() efficiency
4. Disable unnecessary features
5. Check watchdog not triggering

### Android App Sluggish

**Solutions**:
1. Clear old database entries
2. Check memory usage (Android Studio Profiler)
3. Reduce UI update frequency
4. Check for memory leaks
5. Review background service efficiency

### High Battery Drain (ESP32)

**Solutions**:
1. Increase update interval
2. Enable sleep mode:
   ```cpp
   #define ENABLE_SLEEP_MODE 1
   ```
3. Reduce GPS polling frequency
4. Lower log level
5. Check for power-hungry loops

## Error Messages

### "GPS module not responding"

**Cause**: No data from GPS on UART

**Fix**: Check GPS wiring, power, baud rate

### "Bluetooth initialization failed"

**Cause**: Bluetooth module error

**Fix**: Check Bluetooth enabled in platformio.ini, restart ESP32

### "GPS fix lost"

**Cause**: Lost satellite lock

**Fix**: Move outdoors, check antenna, wait for reacquisition

### "Email sending failed"

**Cause**: SMTP error

**Fix**: Check credentials, internet, SMTP server

### "Message validation failed"

**Cause**: Invalid message format

**Fix**: Check message format, protocol implementation

### "Bluetooth permission denied"

**Cause**: Missing Android permissions

**Fix**: Grant Bluetooth permissions in app settings

## Advanced Debugging

### Enable Verbose Logging

**ESP32**:
```cpp
#define LOG_LEVEL 5
#define LOG_GPS_SENTENCES 1
#define LOG_BT_EVENTS 1
```

**Android**:
```bash
adb logcat | grep -E "FleetGPS|Bluetooth"
```

### Check GPS Raw Data

**ESP32**:
```cpp
// Add to gps_module.cpp update():
Serial.write(c);  // Echo raw GPS
```

### Monitor Bluetooth Traffic

**ESP32**:
```cpp
// Add to bluetooth_module.cpp:
LOG_HEX("BT", data, length);
```

### Database Inspection

**Android Studio**:
1. View → Tool Windows → App Inspection
2. Select device and app
3. Browse database tables

## Getting Help

### Information to Provide

When reporting issues, include:
1. ESP32 firmware version
2. Android app version
3. Hardware setup (ESP32 board, GPS module)
4. Complete error messages
5. Serial monitor output
6. Android logcat output
7. Steps to reproduce
8. Expected vs actual behavior

### Log Collection

**ESP32**:
```bash
platformio device monitor > esp32_log.txt
```

**Android**:
```bash
adb logcat > android_log.txt
```

### Common Log Locations

- ESP32: Serial output
- Android: Logcat
- Email errors: SMTP server logs

## Still Having Issues?

1. Re-read documentation carefully
2. Check all wiring with multimeter
3. Test components individually
4. Try fresh install (both ESP32 and Android)
5. Test with known-good hardware
6. Simplify setup (remove features until working)

## Prevention

### Before Deployment

- [ ] Complete all tests in [TESTING_CHECKLIST.md](TESTING_CHECKLIST.md)
- [ ] Verify configuration in [CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md)
- [ ] Run 24-hour reliability test
- [ ] Document configuration for each device

### Regular Maintenance

- Monitor error logs weekly
- Check battery levels (if applicable)
- Verify GPS fix acquisition times
- Review email delivery success rate
- Update firmware/app as needed

## Quick Reference

| Issue | First Check | Quick Fix |
|-------|-------------|-----------|
| No GPS | Wiring | Swap TX/RX |
| No BT | Visible? | Power cycle |
| No Email | Internet? | Test SMTP |
| Crash | Logcat | Clear data |
| No Power | Cable | Try another |
