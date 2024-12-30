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
import net.mitask.requests.*;
import net.mitask.util.HttpMethod;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

public class Kizuna extends Router {
    private final int httpPort, httpsPort;
    private final TemplateEngine templateEngine;
    private final ExecutorService executorService;
    private SSLContext sslContext;
    private final Path certificatePath, privateKeyPath;
    private final Route.RouteHandler notFoundHandler;
    private final Route.AdvancedRouteHandler errorHandler;
    private final List<Middleware.MiddlewareHandler> middlewares = new ArrayList<>();

    @Builder(setterPrefix = "set")
    private Kizuna(int httpPort, int httpsPort, Path templatesDir, Path privateKeyPath, Path certificatePath, Route.RouteHandler notFoundHandler, Route.AdvancedRouteHandler errorHandler) throws Exception {
        if(httpPort == 0 && httpsPort == 0) throw new IllegalStateException("Server must enable at least HTTP or SSL (No ports specified!)");

        this.httpPort = httpPort;
        this.httpsPort = httpsPort;

        this.privateKeyPath = privateKeyPath;
        this.certificatePath = certificatePath;
        if(httpsPort != 0) configureSSL();

        TemplateEngine tempEngine = null;
        if(templatesDir != null) {
            tempEngine = TemplateEngine.create(new DirectoryCodeResolver(templatesDir), ContentType.Html);
            tempEngine.setBinaryStaticContent(true);
            tempEngine.precompileAll();
        } else {
            System.err.println("Due to TemplatesDir == null templateEngine (Rendering JTE templates) will not work!");
        }

        this.templateEngine = tempEngine;
        this.executorService = Executors.newCachedThreadPool();
        this.notFoundHandler = notFoundHandler;
        this.errorHandler = errorHandler;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            System.out.println("Server shutting down...");
        }));
    }

    public void addMiddleware(Middleware.MiddlewareHandler middleware) {
        middlewares.add(middleware);
    }

    public void serveStatic(String basePath, Path staticDir) {
        get(basePath + "/*", (req, res) -> {
            String filePath = staticDir.toString() + req.path.replace(basePath, "").replaceAll("..", ".");
            Path path = Paths.get(filePath);
            if(Files.exists(path) && !Files.isDirectory(path)) {
                res.sendFile(path.toString());
            } else {
                if(notFoundHandler == null) res.sendCustom(404, "text/html", "<html><body>File not found!</body></html>");
                else notFoundHandler.handle(req, res);
            }
        });
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
        try(serverSocket) {
            System.out.println(protocol + " server listening on port " + serverSocket.getLocalPort());
            while(true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try(OutputStream rawOut = clientSocket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(rawOut))
        ) {
            String requestLine = in.readLine();
            if (requestLine == null) return;

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) return;

            HttpMethod method = HttpMethod.valueOf(requestParts[0]);
            String fullPath = requestParts[1];

            String path = fullPath.split("\\?")[0];

            String body = "";
            Map<String, String> queryParams = new HashMap<>();
            Map<String, String> headers = new HashMap<>();
            Map<String, String> cookies = new HashMap<>();

            String line;
            while(!(line = in.readLine()).isEmpty()) {
                int colonIndex = line.indexOf(":");
                if(colonIndex > 0) {
                    String headerName = line.substring(0, colonIndex).trim();
                    String headerValue = line.substring(colonIndex + 1).trim();
                    headers.put(headerName, headerValue);
                }
            }

            if(headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars);
                body = new String(bodyChars);
            }

            if(headers.containsKey("Cookie")) {
                for(String cookie : headers.get("Cookie").split("; ")) {
                    String[] pair = cookie.split("=");
                    if(pair.length != 2) continue;
                    cookies.put(pair[0], pair[1]);
                }
            }

            if(fullPath.contains("?")) {
                String queryString = fullPath.split("\\?")[1];
                for(String param : queryString.split("&")) {
                    String[] pair = param.split("=");
                    if(pair.length != 2) continue;

                    queryParams.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                }
            }

            Route matchedRoute = null;
            Map<String, String> urlParams = new HashMap<>();

            for (Route route : routes) {
                if(route.getMethod() == method && route.matches(path, urlParams)) {
                    matchedRoute = route;
                    break;
                }
            }

            HttpRequest request = new HttpRequest(method, path, queryParams, urlParams, body, headers, cookies, clientSocket.getInetAddress().getHostAddress());
            HttpResponse response = new HttpResponse(out, rawOut, this.templateEngine);
            if(matchedRoute != null) {
                List<Middleware.MiddlewareHandler> combinedMiddlewares = new ArrayList<>(middlewares);
                combinedMiddlewares.addAll(matchedRoute.getMiddlewares());

                Middleware chain = new Middleware(combinedMiddlewares, matchedRoute.getHandler());
                try {
                    chain.next(request, response);
                } catch (Exception e) {
                    if(errorHandler != null) errorHandler.handle(request, response, e);
                }
            } else {
                if(notFoundHandler == null) response.sendCustom(404, "text/html", "<html><body>File not found!</body></html>");
                else notFoundHandler.handle(request, response);
            }

            out.flush();
        } catch (SocketException | SSLHandshakeException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
