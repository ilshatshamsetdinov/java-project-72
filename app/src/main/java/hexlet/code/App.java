package hexlet.code;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;


public class App {
    public static Javalin getApp() {
        Javalin app = Javalin.create(JavalinConfig::enableDevLogging)
                .get("/", ctx -> ctx.result("Hello World"))
                .start(8000);
        return app;
    }
    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(8000);
    }
}

