package net.mitask;

import net.mitask.requests.Router;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        Kizuna app = Kizuna.builder()
                .setHttpPort(8080)
                .setHttpsPort(8443)
                .setTemplatesDir(Paths.get("templates"))
                .setCertificatePath(Paths.get("public.crt"))
                .setPrivateKeyPath(Paths.get("private.key"))
//                .setNotFoundHandler((_, res) -> res.redirect("/hello"))
                .setErrorHandler((_, res, data) -> {
                    res.setStatus(500);
                    res.sendText("Oops! Something went wrong!\n" + Arrays.toString(data));
                })
                .build();

        app.serveStatic("/assets", Paths.get("assets/"));

        app.get("/", (_, res) -> {
//            String str1 = Arrays.toString(req.headers.entrySet().toArray());
//            String str2 = Arrays.toString(req.cookies.entrySet().toArray());
//            res.sendText(str1 + "\n\n" + str2);

            res.sendFile("index.html");
        });

        app.get("/hello", (req, res) -> {
            String name = req.queryParams.getOrDefault("name", "World");
            res.sendTemplate("hello.jte", Map.of("name", name));
        });

        var userRouter = new Router();
        userRouter.get("/:testingId", (req, res) -> {
            String name = req.urlParams.get("testingId");
            res.sendTemplate("hello.jte", Map.of("name", name));
        });
        app.use("/user", userRouter);

        app.post("/data", (req, res) -> {
            System.out.println(req.toJson());
            res.sendText("Received: " + req.body);
        });

        app.addMiddleware((req, res, chain) -> {
            System.out.println("Request: " + req.method + " " + req.path);
            chain.next(req, res);
        });

        app.get("/api/data", (_, res) -> {
            Map<String, String> data = Map.of("message", "Hello, world!");
            res.sendJson(data);
        }).use((req, res, chain) -> {
            String apiKey = req.queryParams.get("apiKey");
            if(!"12345".equals(apiKey)) {
                res.setStatus(403);
                res.sendText("Forbidden: Invalid API Key");
            } else {
                chain.next(req, res);
            }
        });

        app.listen();
    }
}