package com.immo2n;

import com.google.gson.Gson;
import com.immo2n.Core.FileServer;
import com.immo2n.DataClasses.SelectedFile;
import com.immo2n.Etc.AppConfig;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class App extends Application {

    public static String bucketChecksum = "EMPTY";
    public static final HashMap<String, File> selectedFiles = new HashMap<>();
    public static final List<SelectedFile> selectedFilesPublic = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static String selectionCheckKey = null, selectionCheckKeyHold;
    private static Integer lastIndex = 0;
    private static Server appServer;
    private static Thread appUiThread, fileServerThread;
    private static String serverPin = generatePin();

    private static ProgressBar progressBar;
    private static FileServer fileServer;

    @Override
    public void stop() throws Exception {
        super.stop();
        if (appServer != null && appServer.isStarted()) {
            appServer.stop();
        }
        if (appUiThread != null && appUiThread.isAlive()) {
            appUiThread.join();
        }
        if (fileServer != null) {
            fileServer.stop();
        }
        if (fileServerThread != null && fileServerThread.isAlive()) {
            fileServerThread.join();
        }
    }

    @Override
    public void start(Stage stage) {

        Installer installer = new Installer();
        boolean needInstall = installer.isInstalled();
        String defaultLocation = Installer.staticPath;

        if(needInstall){
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Directories", "*"));
            fileChooser.setInitialDirectory(new File(defaultLocation));
            File selectedDirectory = fileChooser.showSaveDialog(stage);
            if (selectedDirectory != null) {
                String downloadUrl = "https://example.com/resource.zip";
                new Thread(() -> downloadZip(selectedDirectory, downloadUrl, stage)).start();
            }
        }
        else startApp(stage);
    }

    private void downloadZip(File selectedDirectory, String downloadUrl, Stage stage) {

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        StackPane root = new StackPane();
        root.getChildren().add(progressBar);
        Scene scene = new Scene(root, 400, 200);

        stage.setTitle("Downloading Resources...");
        stage.setScene(scene);
        Platform.runLater(stage::show);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();

        Response response;
        try {
            response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Failed to download file: " + response);
            }

            long contentLength = response.body().contentLength();
            InputStream inputStream = response.body().byteStream();

            File outputFile = new File(selectedDirectory.getAbsoluteFile(), "resources.zip");
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                long totalBytesRead = 0;
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    double progress = (double) totalBytesRead / contentLength;
                    Platform.runLater(() -> progressBar.setProgress(progress));
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Download Completed!", ButtonType.OK);
                    alert.showAndWait();

                    System.out.println("CAN UNZIP HERE!!!");

                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Download Failed: " + e.getMessage(), ButtonType.OK);
                    alert.showAndWait();
                });
                log.error(e.toString());
            }

        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error downloading file: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            });
            log.error(e.toString());
        } finally {
            //startApp(stage);
        }

    }

    private void startApp(Stage stage) {
        startServers();

        WebView webView = new WebView();
        webView.getEngine().load("http://localhost:" + AppConfig.APP_PORT);

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> webView.getEngine().reload());

        BorderPane root = new BorderPane();
        root.setCenter(webView);

        Scene scene = new Scene(root, 1000, 800);
        stage.setScene(scene);
        stage.setMinHeight(800);
        stage.setMinWidth(600);
        stage.setTitle("Sharlet");
        stage.show();
    }

    public static void main(String[] args) {
        startServers();
        launch(args);
    }

    private static void startServers() {
        appUiThread = getAppUIthread();
        fileServerThread = new Thread(() -> {
            try {
                fileServer = new FileServer(
                        "192.168.0.198",
                        AppConfig.FILE_SERVER_PORT,
                        serverPin
                );
                fileServer.start();
            }
            catch (Exception e) {
                log.error("e", e);
            }
        });
        appUiThread.start();
        fileServerThread.start();
    }

    public static Thread getAppUIthread() {
        return new Thread(() -> {

            try {
                appServer = new Server(AppConfig.APP_PORT);

                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                context.setContextPath("/");

                context.setResourceBase(AppConfig.staticPath);
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
                        selectedFiles.clear();
                        selectedFilesPublic.clear();
                        lastIndex = 0;
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("OK");
                    }
                }), "/clear-all");

                context.addServlet(new ServletHolder(new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        String setNew = request.getParameter("new");
                        if(setNew != null && !setNew.isEmpty()) {
                            serverPin = generatePin();
                            fileServer.changeServerPin(serverPin);
                        }
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write(serverPin);
                    }
                }), "/pin");

                context.addServlet(new ServletHolder(new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        String hash = request.getParameter("hash");
                        if(hash == null || hash.isEmpty()) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            response.getWriter().write("HASH_MISSING");
                            return;
                        }
                        File removedFile = selectedFiles.remove(hash);
                        if (removedFile != null) {
                            selectedFilesPublic.removeIf(selectedFile -> selectedFile.getHash().equals(hash));
                            lastIndex = selectedFilesPublic.size();
                        }
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("OK");
                    }
                }), "/clear");

                context.addServlet(new ServletHolder(new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        String hash = request.getParameter("hash");
                        if(hash == null || hash.isEmpty()) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            response.getWriter().write("HASH_MISSING");
                            return;
                        }
                        File file = selectedFiles.get(hash);
                        if (file == null) {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            response.getWriter().write("FILE_NOT_FOUND");
                            return;
                        }
                        if(Desktop.isDesktopSupported()){
                            Desktop.getDesktop().open(file);
                        }
                        else {
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            response.getWriter().write("PREVIEW_NOT_SUPPORTED");
                        }
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("OK");
                    }
                }), "/preview");

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
                    for (File file : files) {
                        if (file.isDirectory()) {
                            addFilesFromDirectory(file);
                        } else {
                            addFileToSelection(file);
                        }
                    }
                    fileServer.syncFilesContexts();
                    bucketChecksum = App.md5(Integer.toString(App.selectedFilesPublic.size()));
                    selectionCheckKey = selectionCheckKeyHold;
                } else {
                    selectionCheckKeyHold = null;
                }
            });

            fileAdderThread.start();

        });
    }

    private static void addFilesFromDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addFilesFromDirectory(file);
                } else {
                    addFileToSelection(file);
                }
            }
        }
    }

    private static void addFileToSelection(File file) {
        String hash = md5(file.getName());
        if(selectedFiles.containsKey(hash)) return;
        SelectedFile selectedFile = new SelectedFile();
        selectedFile.setName(file.getName());
        selectedFile.setSize(String.valueOf(file.length()));
        selectedFile.setHash(hash);
        selectedFiles.put(hash, file);
        selectedFilesPublic.add(selectedFile);
    }

    private static List<SelectedFile> getNewFiles(int index) {
        List<SelectedFile> newFiles = new ArrayList<>();
        for (int i = index; i < selectedFilesPublic.size(); i++) {
            newFiles.add(selectedFilesPublic.get(i));
        }
        return newFiles;
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Sharlet process: MD5 algorithm not found.");
            return null;
        }
    }

    public static String generatePin() {
        Random random = new Random();
        int pin = 10000 + random.nextInt(90000);
        return String.valueOf(pin);
    }

    private void showDownloadProgress(Stage stage, File resDir) {
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        StackPane root = new StackPane();
        root.getChildren().add(progressBar);
        Scene scene = new Scene(root, 400, 200);

        stage.setTitle("Downloading Resources...");
        stage.setScene(scene);
        stage.show();

        App.progressBar = progressBar;
    }

    private void downloadAndUnzipResources(String downloadUrl, File destinationDir) {
        new Thread(() -> {
            try {
                // Download the zip file
                HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                long totalFileSize = connection.getContentLengthLong();
                InputStream inputStream = connection.getInputStream();

                // Prepare the destination zip file
                File zipFile = new File(destinationDir, "resources.zip");
                FileOutputStream outputStream = new FileOutputStream(zipFile);

                // Download the file in chunks and update progress
                byte[] buffer = new byte[1024];
                long downloaded = 0;
                int bytesRead;

                // Progress reporting thread
                long finalDownloaded = downloaded;
                Thread progressThread = new Thread(() -> {
                    while (finalDownloaded < totalFileSize) {
                        double progress = (double) finalDownloaded / totalFileSize;
                        Platform.runLater(() -> {
                            progressBar.setProgress(progress);
                        });
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                progressThread.start();

                // Write data to the zip file
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                }

                inputStream.close();
                outputStream.close();

                // Once the zip file is downloaded, unzip it
                unzip(zipFile, destinationDir);

                Platform.runLater(() -> {
                    // Proceed with UI after download
                    System.out.println("Download complete. Unzipping...");
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void unzip(File zipFile, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                File file = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parentDir = file.getParentFile();
                    if (parentDir != null) parentDir.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zipIn.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

}
