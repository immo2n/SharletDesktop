package com.immo2n.Core;

import com.google.gson.Gson;
import com.immo2n.App;
import com.immo2n.DataClasses.FileServerInfo;
import com.immo2n.DataClasses.SelectedFile;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class FileServer {
    private static final Logger log = LoggerFactory.getLogger(FileServer.class);
    protected static String staticLocation = "src/main/resources/static";
    protected static String serverPin = null;
    private final HttpServer server;
    private final Gson gson = new Gson();
    public FileServer(String ip, int port, String serverPin) throws IOException {
        this.server = HttpServer.create(
                new InetSocketAddress(ip, port),
                0
        );
        FileServer.serverPin = serverPin;
        /* Set up the receiver server default contexts */
        setupReceiver(server);

        server.createContext("/sharlet-info", httpExchange -> {
            Headers h = httpExchange.getResponseHeaders();
            h.add("Cache-Control", "no-cache");
            h.add("Content-Type", "application/json");
            Identity identity = new Identity();
            identity.setServerAddress(ip);
            identity.setServerPort(String.valueOf(port));
            identity.setServerName("Sharlet Desktop - File Server");
            identity.setRequireAuth(true);
            String response = gson.toJson(identity);
            sendResponse(httpExchange, response);
        });
    }

    private void setupReceiver(HttpServer server) {
        //Css
        server.createContext("/css/main.css", loadStaticFile(staticLocation + "/receiver/css/main.css"));
        server.createContext("/font.css", loadStaticFile(staticLocation + "/receiver/css/font.css"));
        server.createContext("/trebuc.woff", loadStaticFile(staticLocation + "/receiver/css/trebuc.woff"));
        server.createContext("/Trebuchet-MS-Italic.woff", loadStaticFile(staticLocation + "/receiver/css/Trebuchet-MS-Italic.woff"));
        //Plugin
        server.createContext("/plugins/fontawesome/css/all.min.css", loadStaticFile(staticLocation + "/receiver/plugins/fontawesome/css/all.min.css"));
        server.createContext("/plugins/fontawesome/webfonts/fa-solid-900.ttf", loadStaticFile(staticLocation + "/receiver/plugins/fontawesome/webfonts/fa-solid-900.ttf"));
        server.createContext("/plugins/fontawesome/webfonts/fa-solid-900.woff2", loadStaticFile(staticLocation + "/receiver/plugins/fontawesome/webfonts/fa-solid-900.woff2"));
        //Images
        server.createContext("/img/logo.png", loadStaticFile(staticLocation + "/receiver/img/logo.png"));

        server.createContext("/img/favs/apple-icon-57x57.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-57x57.png"));
        server.createContext("/img/favs/apple-icon-60x60.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-60x60.png"));
        server.createContext("/img/favs/apple-icon-72x72.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-72x72.png"));
        server.createContext("/img/favs/apple-icon-76x76.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-76x76.png"));
        server.createContext("/img/favs/apple-icon-114x114.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-114x114.png"));
        server.createContext("/img/favs/apple-icon-120x120.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-120x120.png"));
        server.createContext("/img/favs/apple-icon-144x144.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-144x144.png"));
        server.createContext("/img/favs/apple-icon-152x152.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-152x152.png"));
        server.createContext("/img/favs/apple-icon-180x180.png", loadStaticFile(staticLocation + "/receiver/img/favs/apple-icon-180x180.png"));
        server.createContext("/img/favs/android-icon-192x192.png", loadStaticFile(staticLocation + "/receiver/img/favs/android-icon-192x192.png"));
        server.createContext("/img/favs/android-icon-144x144.png", loadStaticFile(staticLocation + "/receiver/img/favs/android-icon-144x144.png"));
        server.createContext("/img/favs/android-icon-96x96.png", loadStaticFile(staticLocation + "/receiver/img/favs/android-icon-96x96.png"));
        server.createContext("/img/favs/android-icon-72x72.png", loadStaticFile(staticLocation + "/receiver/img/favs/android-icon-72x72.png"));
        server.createContext("/img/favs/android-icon-48x48.png", loadStaticFile(staticLocation + "/receiver/img/favs/android-icon-48x48.png"));
        server.createContext("/img/favs/android-icon-36x36.png", loadStaticFile(staticLocation + "/receiver/img/favs/android-icon-36x36.png"));

        server.createContext("/img/favs/favicon-32x32.png", loadStaticFile(staticLocation + "/receiver/img/favs/favicon-32x32.png"));
        server.createContext("/img/favs/favicon-96x96.png", loadStaticFile(staticLocation + "/receiver/img/favs/favicon-96x96.png"));
        server.createContext("/img/favs/favicon-16x16.png", loadStaticFile(staticLocation + "/receiver/img/favs/favicon-16x16.png"));
        server.createContext("/img/favs/manifest.json", loadStaticFile(staticLocation + "/receiver/img/favs/manifest.json"));

        //Js
        server.createContext("/js/connect.js", loadStaticFile(staticLocation + "/receiver/js/connect.js"));
        server.createContext("/js/main.js", loadStaticFile(staticLocation + "/receiver/js/main.js"));
        server.createContext("/js/query.js", loadStaticFile(staticLocation + "/receiver/js/query.js"));
        server.createContext("/js/jszip.min.js", loadStaticFile(staticLocation + "/receiver/js/jszip.min.js"));
        server.createContext("/js/saver.js", loadStaticFile(staticLocation + "/receiver/js/saver.js"));
        //HTML
        server.createContext("/", loadStaticFile(staticLocation + "/receiver/index.html"));
        server.createContext("/bucket_error.html", loadStaticFile(staticLocation + "/receiver/bucket_error.html"));
        setupServerAuth();
        setBucket();
        setInfo();
        setupServerIndex(server);
        //PC SERVER COMPONENTS ENDS
    }

    private void setInfo() {
        server.createContext("/info", httpExchange -> {
            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody());
            BufferedReader br = new BufferedReader(isr);
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            br.close();
            isr.close();
            String pValue = Commons.extractParameterValue(requestBody.toString(), "p");
            if (null == pValue || !pValue.equals(serverPin)) {
                String response = "WRONG-PIN";
                sendResponse(httpExchange, response);
                return;
            }
            FileServerInfo info = new FileServerInfo();
            info.setLinkSpeed("1000 Mbps");
            info.setSsid("Sharlet Desktop - File Server");
            info.setBucketChecksum(App.bucketChecksum);
            sendResponse(httpExchange, gson.toJson(info));
        });
    }

    private void setupServerAuth() {
        server.createContext("/auth", httpExchange -> {
            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody());
            BufferedReader br = new BufferedReader(isr);
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            br.close();
            isr.close();
            String pValue = Commons.extractParameterValue(requestBody.toString(), "p");
            if (null == pValue || !pValue.equals(serverPin)) {
                String response = "WRONG-PIN";
                httpExchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream responseBody = httpExchange.getResponseBody();
                responseBody.write(response.getBytes(StandardCharsets.UTF_8));
                responseBody.close();
                return;
            }
            String ajax_data = "PASS";
            httpExchange.sendResponseHeaders(200, ajax_data.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(ajax_data.getBytes());
            os.close();
        });
    }

    public static void setupServerIndex(HttpServer server) {
        if(null != serverPin){
            try {
                server.removeContext("/x-" + serverPin + "/receive/");
            }
            catch (Exception e){
                log.error(e.toString());
            }
        }
        server.createContext("/x-" + serverPin + "/receive/", loadStaticFile(staticLocation + "/receiver/receive/index.html"));
    }

    private static HttpHandler loadStaticFile(String path) {
        return httpExchange -> {
            LoadFile loadFile = new LoadFile(path);
            loadFile.handle(httpExchange);
        };
    }

    public void setBucket() {
        server.createContext("/bucket", httpExchange -> {
            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody());
            BufferedReader br = new BufferedReader(isr);
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            br.close();
            isr.close();
            String pValue = Commons.extractParameterValue(requestBody.toString(), "p");
            if (null == pValue || !pValue.equals(serverPin)) {
                String response = "WRONG-PIN";
                sendResponse(httpExchange, response);
                return;
            }
            /*
            * Send path info to the client
            * */
            String response = gson.toJson(App.selectedFilesPublic);
            sendResponse(httpExchange, response);
        });
    }

    public void syncFilesContexts() {
        for (SelectedFile file : App.selectedFilesPublic) {
            if(serverPin != null){
                try {
                    server.removeContext("/file/" + serverPin + "/" + file.getHash());
                } catch (Exception e) {
                    log.error(e.toString());
                }
            }
            server.createContext("/file/" + serverPin + "/" + file.getHash(), loadDynamicFile(file));
        }
    }

    private HttpHandler loadDynamicFile(SelectedFile file) {
        return httpExchange -> {
            String hash = file.getHash();
            if(App.selectedFiles.containsKey(hash)){
                File mainFile = App.selectedFiles.get(hash);
                LoadFile loadFile = new LoadFile(mainFile.getAbsolutePath());
                loadFile.handle(httpExchange);
            }
            else {
                String response = "COULDN'T SERVE: FILE IS EMPTY OR DELETED OR PIN CHANGED";
                sendResponse(httpExchange, response);
            }
        };
    }

    private void sendResponse(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream responseBody = httpExchange.getResponseBody();
        responseBody.write(response.getBytes(StandardCharsets.UTF_8));
        responseBody.close();
    }

    public void start() {
        server.start();
    }
    public void stop() {
        server.stop(0);
    }
}
