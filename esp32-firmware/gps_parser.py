"""
GPS Parser Module for Neo-6M GPS Module
Parses NMEA sentences (specifically GPGGA and GPRMC)
"""

class GPSParser:
    def __init__(self):
        self.latitude = None
        self.longitude = None
        self.heading = None
        self.timestamp = None
        self.has_fix = False
        self.satellites = 0

    def parse_nmea_sentence(self, sentence):
        """
        Parse a single NMEA sentence
        Returns True if sentence was parsed successfully
        """
        try:
            if not sentence.startswith('$'):
                return False

            # Remove checksum if present
            if '*' in sentence:
                sentence = sentence.split('*')[0]

            parts = sentence.split(',')
            sentence_type = parts[0]

            if sentence_type == '$GPGGA' or sentence_type == '$GNGGA':
                return self._parse_gpgga(parts)
            elif sentence_type == '$GPRMC' or sentence_type == '$GNRMC':
                return self._parse_gprmc(parts)

            return False

        except Exception as e:
            print(f"Error parsing NMEA: {e}")
            return False

    def _parse_gpgga(self, parts):
        """
        Parse GPGGA sentence (GPS Fix Data)
        Format: $GPGGA,time,lat,N/S,lon,E/W,quality,numSV,HDOP,alt,M,sep,M,diffAge,diffStation*cs
        """
        try:
            if len(parts) < 15:
                return False

            # Quality indicator (0=no fix, 1=GPS fix, 2=DGPS fix)
            quality = parts[6]
            if quality == '' or quality == '0':
                self.has_fix = False
                return False

            # Parse time (HHMMSS.SSS)
            time_str = parts[1]
            if time_str:
                self.timestamp = self._format_time(time_str)

            # Parse latitude
            lat_str = parts[2]
            lat_dir = parts[3]
            if lat_str and lat_dir:
                self.latitude = self._convert_to_decimal(lat_str, lat_dir)

            # Parse longitude
            lon_str = parts[4]
            lon_dir = parts[5]
            if lon_str and lon_dir:
                self.longitude = self._convert_to_decimal(lon_str, lon_dir)

            # Number of satellites
            if parts[7]:
                self.satellites = int(parts[7])

            self.has_fix = True
            return True

        except Exception as e:
            print(f"Error parsing GPGGA: {e}")
            return False

    def _parse_gprmc(self, parts):
        """
        Parse GPRMC sentence (Recommended Minimum)
        Format: $GPRMC,time,status,lat,N/S,lon,E/W,speed,course,date,mag,E/W,mode*cs
        """
        try:
            if len(parts) < 12:
                return False

            # Status (A=active/valid, V=void/invalid)
            status = parts[2]
            if status != 'A':
                self.has_fix = False
                return False

            # Parse time
            time_str = parts[1]
            if time_str:
                self.timestamp = self._format_time(time_str)

            # Parse latitude
            lat_str = parts[3]
            lat_dir = parts[4]
            if lat_str and lat_dir:
                self.latitude = self._convert_to_decimal(lat_str, lat_dir)

            # Parse longitude
            lon_str = parts[5]
            lon_dir = parts[6]
            if lon_str and lon_dir:
                self.longitude = self._convert_to_decimal(lon_str, lon_dir)

            # Parse course/heading
            course_str = parts[8]
            if course_str:
                self.heading = float(course_str)

            self.has_fix = True
            return True

        except Exception as e:
            print(f"Error parsing GPRMC: {e}")
            return False

    def _convert_to_decimal(self, coord_str, direction):
        """
        Convert NMEA coordinate format to decimal degrees
        Input format: DDMM.MMMM (latitude) or DDDMM.MMMM (longitude)
        """
        try:
            # Find decimal point
            dot_pos = coord_str.index('.')

            # For latitude (DDMM.MMMM), degrees are first 2 digits
            # For longitude (DDDMM.MMMM), degrees are first 3 digits
            if len(coord_str) >= 10:  # Longitude
                degrees = int(coord_str[:3])
                minutes = float(coord_str[3:])
            else:  # Latitude
                degrees = int(coord_str[:2])
                minutes = float(coord_str[2:])

            decimal = degrees + (minutes / 60.0)

            # Apply direction
            if direction in ['S', 'W']:
                decimal = -decimal

            return round(decimal, 6)

        except Exception as e:
            print(f"Error converting coordinate: {e}")
            return None

    def _format_time(self, time_str):
        """
        Convert HHMMSS.SSS format to HH:MM:SS
        """
        try:
            if len(time_str) >= 6:
                hours = time_str[0:2]
                minutes = time_str[2:4]
                seconds = time_str[4:6]
                return f"{hours}:{minutes}:{seconds}"
            return time_str
        except:
            return time_str

    def get_location_data(self):
        """
        Return dictionary with current GPS data
        """
        return {
            'latitude': self.latitude,
            'longitude': self.longitude,
            'heading': self.heading if self.heading else 0.0,
            'timestamp': self.timestamp,
            'has_fix': self.has_fix,
            'satellites': self.satellites
        }

    def is_valid(self):
        """
        Check if we have a valid GPS fix with coordinate data
        """
        return (self.has_fix and
                self.latitude is not None and
                self.longitude is not None)
