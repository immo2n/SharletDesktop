package com.immo2n.Core;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.apache.commons.net.io.Util.copyStream;

public class LoadFile {
    private final String path;

    public LoadFile(String path){
        this.path = path;
    }

    public void handle(HttpExchange t) throws IOException {

        File file = new File(path);
        String content_type = "text/html; charset=UTF-8";

        if(!file.exists()){
            String response = "COULDN'T SERVE: FILE IS EMPTY OR DELETED";
            t.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream responseBody = t.getResponseBody();
            responseBody.write(response.getBytes(StandardCharsets.UTF_8));
            responseBody.close();
            return;
        }
        else {
            String ct = Files.probeContentType(Paths.get(file.getPath()));
            if (null != ct && !ct.isEmpty()) {
                content_type = ct;
            }
        }

        Headers h = t.getResponseHeaders();
        h.add("Cache-Control", "no-cache");
        h.add("Content-Type", content_type);
        t.sendResponseHeaders(200, file.length());

        FileInputStream fis;
        OutputStream os = t.getResponseBody();
        fis = new FileInputStream(file);
        copyStream(fis, os);
        os.close();
        fis.close();
        t.close();
    }
}
