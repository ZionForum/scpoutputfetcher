# SCP Log Viewer

A Java-based real-time log monitoring application that allows you to view and track log files on remote Linux machines through SSH/SCP connection.

## Features

- **Multi-Tab Support**: Monitor multiple log files simultaneously in different tabs
- **Real-time Monitoring**: Automatic refresh every 2 seconds to show live log updates
- **SSH/SCP Integration**: Secure connection to remote Linux machines using JSch library
- **Text Customization**:
  - Adjustable font family, size, and style
  - Customizable text and background colors
  - Dark mode support
- **Search Functionality**:
  - Case-sensitive search option
  - Wrap-around search
  - Forward and backward search directions
- **Log Management**:
  - Clear log files directly from the interface
  - Export logs with timestamps and connection details
  - Duplicate line filtering
- **Connection Status**: Real-time connection status and last update time display
- **User-friendly Interface**:
  - Intuitive tab management
  - Right-click context menu for copy and select operations
  - Customizable UI with dark mode support

## Dependencies

- Java Runtime Environment (JRE) 8 or higher
- Required Libraries:
  - JSch (Java Secure Channel) for SSH/SCP functionality
  - JSON library for configuration management
  - Swing/AWT for GUI components

## Setup and Configuration

1. Ensure Java is installed on your system
2. Download the latest release of SCP Log Viewer
3. Configuration is stored in `log_viewer_config.json`:

   ```json
   {
     "tabs": [{
       "textColorRGB": -16777216,
       "tabName": "Log 1",
       "fontName": "Monospaced",
       "backgroundColorRGB": -1,
       "logFile": "/path/to/your/log",
       "host": "your.host.ip",
       "fontSize": 12,
       "fontStyle": 0,
       "user": "username"
     }],
     "darkMode": false
   }
   ```

## Usage

1. Launch the application
2. Enter connection details:
   - Host: Remote Linux machine IP/hostname
   - Username: SSH username
   - Password: SSH password
   - Log File: Full path to the log file on remote machine
3. Click "Connect" to start monitoring
4. Use the toolbar for additional functions:
   - Add new tabs
   - Search within logs
   - Customize text appearance
   - Export logs
   - Toggle dark mode
   - Clear logs

## Security Features

- SSH key verification
- Secure password handling
- No plain-text password storage

## Keyboard Shortcuts

- **Ctrl+F**: Open search dialog
- **Ctrl+C**: Copy selected text
- **Ctrl+A**: Select all text

## System Requirements

- Operating System: Windows/Linux/macOS
- Memory: Minimum 256MB RAM
- Disk Space: 50MB minimum
- Network: Stable internet connection for remote log monitoring

## Build from Source

1. Clone the repository
2. Ensure Java Development Kit (JDK) 8 or higher is installed
3. Build using your preferred Java IDE or build tool

## License

This project is open-source and available under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit pull requests.
