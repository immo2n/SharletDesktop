package com.immo2n;

import com.immo2n.Etc.AppConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        webView.getEngine().load("http://localhost:" + AppConfig.APP_PORT);

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> webView.getEngine().reload());  // Reload the webpage when clicked

        BorderPane root = new BorderPane();
        root.setCenter(webView);
        root.setTop(refreshButton);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setMinHeight(600);
        stage.setMinWidth(400);
        String windowTitle = "Sharlet";
        stage.setTitle(windowTitle);
        stage.show();
    }

    public static void main(String[] args) {
        Thread appUiThread = getAppUIthread();
        appUiThread.start();
        launch(args);
    }

    public static Thread getAppUIthread() {
        return new Thread(() -> {
            try {
                Server server = new Server(AppConfig.APP_PORT);

                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                context.setContextPath("/");

                context.setResourceBase("src/main/resources/static");
                context.addServlet(DefaultServlet.class, "/");

                context.addServlet(new ServletHolder(new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        Platform.runLater(App::openFileDialog);
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("TRIGGERED_FILE_DIALOGUE");
                    }
                }), "/select-files");

                server.setHandler(context);
                server.start();
                server.join();

            } catch (Exception e) {
                e.printStackTrace();  // Handle the exception (could log or rethrow)
            }
        });
    }

    public static void openFileDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select files to send...");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(new Stage());

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File selectedFile : selectedFiles) {
                System.out.println("File selected: " + selectedFile.getAbsolutePath());
            }
        } else {
            System.out.println("No files were selected.");
        }
    }
}
