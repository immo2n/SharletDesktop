package com.immo2n;

import com.google.gson.Gson;
import com.immo2n.DataClasses.SelectedFile;
import com.immo2n.Etc.AppConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class App extends Application {

    private static final List<File> selectedFiles = new ArrayList<>();
    private static final List<SelectedFile> selectedFilesPublic = new ArrayList<>();
    private static final Set<String> selectedFileNames = new HashSet<>();
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static String selectionCheckKey = null, selectionCheckKeyHold;
    private static Integer lastIndex = 0;
    private static Server appServer;
    private static Thread appUiThread;

    @Override
    public void stop() throws Exception {
        super.stop();
        if (appServer != null && appServer.isStarted()) {
            appServer.stop();
        }
        if (appUiThread != null && appUiThread.isAlive()) {
            appUiThread.join();
        }
    }

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        webView.getEngine().load("http://localhost:" + AppConfig.APP_PORT);

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> webView.getEngine().reload());

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
        appUiThread = getAppUIthread();
        appUiThread.start();
        launch(args);
    }

    public static Thread getAppUIthread() {
        return new Thread(() -> {
            try {
                appServer = new Server(AppConfig.APP_PORT);

                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                context.setContextPath("/");

                context.setResourceBase("src/main/resources/static");
                context.addServlet(DefaultServlet.class, "/");

                context.addServlet(new ServletHolder(new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        openFileDialog();
                        response.setStatus(HttpServletResponse.SC_OK);
                        selectionCheckKey = null;
                        selectionCheckKeyHold = UUID.randomUUID().toString();
                        response.getWriter().write(selectionCheckKeyHold);
                    }
                }), "/select-files");

                context.addServlet(new ServletHolder(new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        if (selectionCheckKeyHold == null) {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write("RESET");
                            return;
                        }
                        if (selectionCheckKey == null) {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write("NO_NEW");
                            return;
                        }

                        String key = request.getParameter("key");
                        String newSignal = request.getParameter("new");

                        if (newSignal != null && newSignal.equals("true")) {
                            lastIndex = 0;
                        }

                        if (key != null && key.equals(selectionCheckKey)) {
                            response.setStatus(HttpServletResponse.SC_OK);
                            List<SelectedFile> newFiles = getNewFiles(lastIndex);
                            lastIndex = selectedFilesPublic.size();
                            response.getWriter().write(new Gson().toJson(newFiles));
                            selectionCheckKey = null;

                        } else {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write("INVALID_KEY");
                        }
                    }
                }), "/selected-files");

                appServer.setHandler(context);
                appServer.start();
                appServer.join();

            } catch (Exception e) {
                log.error("e", e);
            }
        });
    }

    private static Thread fileAdderThread;

    public static void openFileDialog() {
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select files to send...");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

            List<File> files = fileChooser.showOpenMultipleDialog(new Stage());

            if(null != fileAdderThread && fileAdderThread.isAlive()) {
                try {
                    fileAdderThread.join();
                } catch (InterruptedException e) {
                    // Rare case
                    log.error("e: ", e);
                }
            }

            fileAdderThread = new Thread(() -> {
                if (files != null && !files.isEmpty()) {
                    List<SelectedFile> newFiles = new ArrayList<>();
                    for (File file : files) {
                        if (file.isDirectory()) {
                            addFilesFromDirectory(file, newFiles);
                        } else {
                            addFileToSelection(file, newFiles);
                        }
                    }

                    if (!newFiles.isEmpty()) {
                        selectedFilesPublic.addAll(newFiles);
                        selectedFiles.addAll(files);
                    }

                    selectionCheckKey = selectionCheckKeyHold;
                } else {
                    selectionCheckKeyHold = null;
                }
            });

            fileAdderThread.start();

        });
    }

    private static void addFilesFromDirectory(File directory, List<SelectedFile> newFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addFilesFromDirectory(file, newFiles);
                } else {
                    addFileToSelection(file, newFiles);
                }
            }
        }
    }

    private static void addFileToSelection(File file, List<SelectedFile> newFiles) {
        if (selectedFileNames.contains(file.getName())) return;
        SelectedFile selectedFile = new SelectedFile();
        selectedFile.setName(file.getName());
        selectedFile.setSize(String.valueOf(file.length()));
        newFiles.add(selectedFile);
        selectedFileNames.add(file.getName());
    }

    private static List<SelectedFile> getNewFiles(int index) {
        List<SelectedFile> newFiles = new ArrayList<>();
        for (int i = index; i < selectedFilesPublic.size(); i++) {
            newFiles.add(selectedFilesPublic.get(i));
        }
        return newFiles;
    }
}
