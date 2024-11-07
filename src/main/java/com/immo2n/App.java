package com.immo2n;

import com.immo2n.Etc.AppConfig;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Create the WebView to display the webpage
        WebView webView = new WebView();
        webView.getEngine().load("http://localhost:" + AppConfig.APP_PORT);

        // Create a refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> webView.getEngine().reload());  // Reload the webpage when clicked

        // Set up the layout
        BorderPane root = new BorderPane();
        root.setCenter(webView);
        root.setTop(refreshButton);  // Add the button to the top of the window

        // Set up the scene
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setResizable(false);
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
                server.setHandler(context);

                server.start();
                server.join();
            } catch (Exception e) {
                // TODO: Handle this
            }
        });
    }
}
