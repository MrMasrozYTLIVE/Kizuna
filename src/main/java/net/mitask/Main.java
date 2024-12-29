package net.mitask;

import net.mitask.requests.Router;

import java.nio.file.Paths;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        Kizuna app = Kizuna.builder()
                .setHttpPort(8080)
                .setHttpsPort(8443)
                .setTemplatesDir(Paths.get("templates"))
                .setCertificatePath(Paths.get("public.crt"))
                .setPrivateKeyPath(Paths.get("private.key"))
//                .setNotFoundHandler((req, res) -> res.redirect("/hello"))
                .build();

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

        app.post("/data", (req, res) -> res.sendText("Received: " + req.body));


        app.listen();
    }
}