# Communication Protocol Specification

## Overview
This document defines the communication protocol between the ESP32 trailer device and the Android tablet.

## Transport Layer
- **Protocol**: Bluetooth Classic
- **Profile**: SPP (Serial Port Profile)
- **UUID**: 00001101-0000-1000-8000-00805F9B34FB
- **Baud Rate**: 9600 (for Bluetooth serial emulation)

## Message Format

### Structure
Messages use pipe-delimited format for simplicity and easy parsing:

```
SERIAL|LATITUDE|LONGITUDE|HEADING|TIMESTAMP|END\n
```

### Field Definitions

| Field | Type | Format | Example | Description |
|-------|------|--------|---------|-------------|
| SERIAL | String | Alphanumeric | `TRAILER_001` | Unique identifier for the trailer |
| LATITUDE | Float | Decimal degrees, 6 decimals | `49.123456` | GPS latitude (-90 to +90) |
| LONGITUDE | Float | Decimal degrees, 6 decimals | `-123.456789` | GPS longitude (-180 to +180) |
| HEADING | Float | Degrees, 2 decimals | `275.50` | Direction of travel (0-360) |
| TIMESTAMP | String | ISO 8601 | `2025-11-05T14:30:00` | UTC time from GPS |
| END | String | Literal "END" | `END` | Message terminator |
| \n | Character | Newline | - | Line terminator |

### Complete Example
```
TRAILER_001|49.123456|-123.456789|275.50|2025-11-05T14:30:00|END\n
```

## Message Flow

### Normal Operation
```
ESP32 → Tablet: TRAILER_001|49.123456|-123.456789|275.50|2025-11-05T14:30:00|END\n
[5 minute wait]
ESP32 → Tablet: TRAILER_001|49.123567|-123.456801|276.20|2025-11-05T14:35:00|END\n
[5 minute wait]
...continues...
```

### No GPS Fix
When ESP32 has no GPS fix, it does NOT send a message. It waits until GPS is available.

### Connection Lost
If Bluetooth disconnects:
1. ESP32 continues reading GPS (doesn't send anywhere)
2. Tablet shows "Disconnected" status
3. Both devices attempt automatic reconnection
4. When reconnected, ESP32 sends next scheduled update

## Validation Rules

### Sender (ESP32) Requirements
- Must wait for valid GPS fix before sending
- Must send complete message (all fields)
- Must include END terminator
- Must send newline character after END
- Latitude must be between -90 and 90
- Longitude must be between -180 and 180
- Heading must be between 0 and 360

### Receiver (Tablet) Requirements
- Must validate message contains all 6 fields plus terminator
- Must validate GPS coordinates are in valid ranges
- Must handle malformed messages gracefully (log and discard)
- Must handle connection interruptions
- Must not crash on unexpected data

## Error Handling

### Invalid Message Examples

**Missing Fields:**
```
TRAILER_001|49.123456|-123.456789|END\n
❌ Missing heading and timestamp
```

**Invalid Coordinates:**
```
TRAILER_001|95.123456|-123.456789|275.50|2025-11-05T14:30:00|END\n
❌ Latitude > 90 degrees (impossible)
```

**No Terminator:**
```
TRAILER_001|49.123456|-123.456789|275.50|2025-11-05T14:30:00\n
❌ Missing END marker
```

### Receiver Response to Errors
- Log error to console/debug output
- Discard invalid message
- Continue listening for next message
- Do NOT send email for invalid data
- Do NOT crash or stop service

## Timing Specifications

| Event | Timing |
|-------|--------|
| Update Interval | 5 minutes (300 seconds) |
| GPS Acquisition Timeout | 60 seconds max |
| Bluetooth Connection Timeout | 30 seconds |
| Message Send Timeout | 5 seconds |
| Reconnection Retry Delay | 10 seconds |

## Future Enhancements

### Possible Protocol Extensions

**Acknowledgment Messages:**
```
Tablet → ESP32: ACK\n
```
Would confirm receipt and allow ESP32 to verify tablet received data.

**Status Requests:**
```
Tablet → ESP32: STATUS?\n
ESP32 → Tablet: STATUS|GPS_OK|BATTERY_75|SIGNAL_GOOD\n
```
Would allow tablet to query ESP32 health.

**Multiple Trailers:**
Current design supports multiple trailers by unique serial numbers. 
One tablet can monitor multiple ESP32 devices.

**Compressed Format:**
Could switch to binary format to reduce bandwidth:
```
[1 byte: message type][4 bytes: lat][4 bytes: lon][2 bytes: heading][4 bytes: timestamp]
```

## Bluetooth Pairing

### Initial Setup
1. Power on ESP32
2. ESP32 becomes discoverable as "ESP32_TRAILER_001"
3. From tablet, scan for Bluetooth devices
4. Select "ESP32_TRAILER_001"
5. Enter PIN if prompted (default: 1234)
6. Devices are now paired

### Subsequent Connections
- Should reconnect automatically
- No re-pairing required
- If pairing info lost, repeat initial setup

## Security Considerations

### Current Implementation
- Basic Bluetooth pairing with PIN
- No encryption of message content
- No authentication beyond pairing

### Potential Security Issues
- Messages sent in plain text over Bluetooth
- Anyone paired to ESP32 could receive data
- No verification that tablet is authorized receiver

### Future Security Enhancements
- Use Bluetooth encryption (built into BT Classic)
- Add message signing/HMAC
- Implement device whitelisting
- Encrypt GPS coordinates in message payload

## Testing Checklist

### ESP32 Testing
- [ ] GPS acquires fix within 60 seconds
- [ ] Message format is correct
- [ ] Sends every 5 minutes consistently
- [ ] Handles GPS loss gracefully
- [ ] Reconnects after Bluetooth disconnect
- [ ] All coordinate values in valid ranges

### Tablet Testing
- [ ] Discovers and pairs with ESP32
- [ ] Receives messages successfully
- [ ] Parses all fields correctly
- [ ] Rejects invalid messages
- [ ] Reconnects after disconnect
- [ ] Sends emails for valid data only
- [ ] Handles multiple messages correctly
- [ ] Background service keeps running

### Integration Testing
- [ ] End-to-end: GPS → Bluetooth → Email
- [ ] Power cycle ESP32 mid-operation
- [ ] Move trailer beyond Bluetooth range
- [ ] Leave system running for 24 hours
- [ ] Test with multiple trailers
- [ ] Test with poor GPS signal conditions
