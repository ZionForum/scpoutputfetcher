# SCP Log Viewer

A Java-based real-time log monitoring application that allows you to view and track log files on remote Linux machines through SSH/SCP connection.

## Features

- **Multi-Tab Support**: Monitor multiple log files simultaneously in different tabs
- **Real-time Monitoring**: Automatic refresh to show live log updates
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
  - Clear log files directly from the interface (supports full clear or bottom-only clear)
  - Export logs with timestamps and connection details
  - Duplicate line filtering with configuration persistence
  - Word wrap option with configuration persistence
  - Timestamp display toggle
- **Connection Status**: Real-time connection status and last update time display
- **User-friendly Interface**:
  - Intuitive tab management with custom naming
  - Right-click context menu for copy and select operations
  - Customizable UI with dark mode support
  - Auto-connect feature for tabs
- **Security Features**:
  - Password encryption using Base64 encoding with shift cipher
  - No plain-text password storage
  - Secure SSH/SCP communication
- **Special Log Commands**: Support for special escape codes (\u001B[C] and \u001B[CB]) for clearing logs

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
       "host": "your.host.ip",
       "port": 22,
       "user": "username",
       "password": "encrypted_password",
       "logFile": "/path/to/your/log",
       "tabName": "Log 1",
       "textColorRGB": -16777216,
       "backgroundColorRGB": -1,
       "fontName": "Monospaced",
       "fontSize": 12,
       "fontStyle": 0,
       "wordWrap": false,
       "filterDuplicates": false,
       "showTimestamp": true,
       "autoConnect": false
     }],
     "darkMode": false,
     "globalHost": "localhost",
     "globalPort": 22,
     "globalUsername": "",
     "globalPassword": ""
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
   - Clear logs (full or bottom-only)
   - Toggle word wrap
   - Toggle duplicate filtering

## Security Features

- SSH key verification
- No plain-text password storage

## Keyboard Shortcuts

- **Ctrl+T**: Create new tab
- **Ctrl+W**: Close current tab
- **Alt+Up**: Fast scroll up
- **Alt+Down**: Fast scroll down
- **Ctrl+S**: Save current tab settings
- **F2**: Rename current tab
- **Ctrl+F**: Show search dialog
- **Ctrl+E**: Export log
- **Ctrl+L**: Clear log
- **Ctrl+Enter**: Save and connect

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
