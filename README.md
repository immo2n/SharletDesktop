
# Sharlet

Sharlet is a simple JavaFX application that integrates with a web server (Jetty) to load a static HTML page inside a WebView. It provides a basic user interface to view the webpage locally with the ability to refresh the content through a button.

## Features

- **Webview**: Displays a webpage within a JavaFX application.
- **Jetty Server**: Uses Jetty to serve static files from the local filesystem.
- **Refresh Button**: A simple button to refresh the webpage inside the WebView.
- **Fixed Window Size**: The application window is fixed at 800x600 pixels and is not resizable.

---

## Prerequisites

- **JDK 11 or higher**: Required to compile and run the application.
- **Maven**: Used for managing dependencies.
- **JavaFX SDK**: To build and run JavaFX applications.

---

## Installation

1. **Clone the Repository**

   Start by cloning the repository:

   ```bash
   git clone https://github.com/immo2n/SharletDesktop.git
   cd sharlet
   ```

2. **Set up Maven**

   Ensure you have Maven installed on your system. You can verify this with:

   ```bash
   mvn -v
   ```

   If Maven is not installed, you can download it from [here](https://maven.apache.org/download.cgi).

3. **Install JavaFX SDK**

   Download and set up the JavaFX SDK for your platform from [here](https://openjfx.io/).

   For example, on Linux, you can use:

   ```bash
   sudo apt-get install openjfx
   ```

---

## Running the Application

1. **Compile the Project**

   Run the following Maven command to compile and package the application:

   ```bash
   mvn clean install
   ```

2. **Run the Application**

   To run the application, use:

   ```bash
   mvn javafx:run
   ```

   This will start the embedded Jetty server and launch the JavaFX application with the WebView displaying the local webpage.

---

## How It Works

- **Jetty Server**: The Jetty server serves static files from the `src/main/resources/static` directory. By default, it serves `App.html` at the root.
- **JavaFX WebView**: The webpage served by the Jetty server is displayed in a JavaFX WebView. The application loads the page using `webView.getEngine().load("http://localhost:PORT");`.
- **Refresh Button**: The "Refresh" button reloads the content of the webpage within the WebView when clicked, without needing to restart the application.

---

## File Structure

Here’s a breakdown of the project’s directory structure:

```plaintext
.
├── pom.xml                          # Maven project file
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── immo2n
│   │   │           └── App.java     # Main application file
│   │   ├── resources
│   │   │   └── static
│   │   │       └── App.html         # Static HTML file served by Jetty
└── README.md                        # Project overview and documentation
```

---

## Customization

- **Port Configuration**: You can modify the server port by updating the `AppConfig.APP_PORT` value in `AppConfig.java`.
- **Static Content**: Place any additional static content (CSS, JavaScript, images) in the `src/main/resources/static` directory. Jetty will serve them as static resources.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgements

- **Jetty**: Lightweight and fast HTTP server.
- **JavaFX**: A powerful framework for building desktop applications.
