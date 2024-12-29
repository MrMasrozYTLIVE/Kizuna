package net.mitask;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.*;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import lombok.Builder;
import net.mitask.requests.HttpRequest;
import net.mitask.requests.HttpResponse;
import net.mitask.requests.Route;
import net.mitask.requests.Router;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class Kizuna extends Router {
    private final int httpPort, httpsPort;
    private final TemplateEngine templateEngine;
    private final ExecutorService executorService;
    private SSLContext sslContext;
    private final Path certificatePath, privateKeyPath;
    private final Route.RouteHandler notFoundHandler;

    @Builder(setterPrefix = "set")
    private Kizuna(int httpPort, int httpsPort, Path templatesDir, Path privateKeyPath, Path certificatePath, Route.RouteHandler notFoundHandler) throws Exception {
        if(httpPort == 0 && httpsPort == 0) throw new IllegalStateException("Server must enable at least HTTP or SSL (No ports specified!)");

        this.httpPort = httpPort;
        this.httpsPort = httpsPort;

        this.privateKeyPath = privateKeyPath;
        this.certificatePath = certificatePath;
        if(httpsPort != 0) configureSSL();

        this.templateEngine = TemplateEngine.create(new DirectoryCodeResolver(templatesDir), ContentType.Html);
        this.templateEngine.setBinaryStaticContent(true);
        this.templateEngine.precompileAll();

        this.executorService = Executors.newCachedThreadPool();
        this.notFoundHandler = notFoundHandler;
    }

    private void configureSSL() throws Exception {
        if(privateKeyPath == null) throw new IllegalStateException("Private key path must be specified if SSL (HTTPS) port was specified in order to run HTTPS server!");
        if(certificatePath == null) throw new IllegalStateException("Certificate path must be specified if SSL (HTTPS) port was specified in order to run HTTPS server!");

        String privateKeyContent = Files.readString(privateKeyPath, StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        String certContent = Files.readString(certificatePath, StandardCharsets.UTF_8)
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] certBytes = Base64.getDecoder().decode(certContent);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("server-key", privateKey, null, new Certificate[]{certificate});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, null);

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
    }

    public void listen() throws IOException {
        if(this.httpPort != 0) {
            ServerSocket serverSocket = new ServerSocket(httpPort);
            executorService.submit(() -> startServerSocket(serverSocket, "HTTP"));
        }

        if(this.httpsPort != 0) {
            if(sslContext == null) throw new IllegalStateException("SSL is not configured. Pass private and public keys into builder before starting SSL.");

            ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket(httpsPort);
            executorService.submit(() -> startServerSocket(serverSocket, "HTTPS"));
        }
    }


    private void startServerSocket(ServerSocket serverSocket, String protocol) {
        try (serverSocket) {
            System.out.println(protocol + " server listening on port " + serverSocket.getLocalPort());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            String requestLine = in.readLine();
            if (requestLine == null) return;

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) return;

            String method = requestParts[0];
            String fullPath = requestParts[1];

            String path = fullPath.split("\\?")[0];

            String body = "";
            Map<String, String> queryParams = new HashMap<>();
            Map<String, String> headers = new HashMap<>();

            String line;
            while (!(line = in.readLine()).isEmpty()) {
                int colonIndex = line.indexOf(":");
                if (colonIndex > 0) {
                    String headerName = line.substring(0, colonIndex).trim();
                    String headerValue = line.substring(colonIndex + 1).trim();
                    headers.put(headerName, headerValue);
                }
            }

            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars);
                body = new String(bodyChars);
            }

            if (fullPath.contains("?")) {
                String queryString = fullPath.split("\\?")[1];
                for (String param : queryString.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        queryParams.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                    }
                }
            }

            Route matchedRoute = null;
            Map<String, String> urlParams = new HashMap<>();

            for (Route route : routes) {
                if (route.getMethod().name().equalsIgnoreCase(method) && route.matches(path, urlParams)) {
                    matchedRoute = route;
                    break;
                }
            }

            HttpRequest request = new HttpRequest(method, path, queryParams, body, urlParams);
            HttpResponse response = new HttpResponse(out, this.templateEngine);
            if (matchedRoute != null) {
                matchedRoute.getHandler().handle(request, response);
            } else {
                if(notFoundHandler == null) response.sendCustom(404, "text/html", "<html><body>Page not found!</body></html>");
                else notFoundHandler.handle(request, response);
            }

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
