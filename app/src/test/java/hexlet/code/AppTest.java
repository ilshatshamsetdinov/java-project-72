package hexlet.code;

import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.Transaction;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;
import io.ebean.Database;

import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.Url;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class AppTest {
    private static Javalin app;
    private static String baseUrl;
    private static Url existingUrl;
    private static Database database;
    private static MockWebServer server;
    private static Transaction transaction;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
        server = new MockWebServer();
        String expectedBody = Files.readString(Path.of("src/test/resources/test.html"));
        server.enqueue(new MockResponse().setBody(expectedBody));
        server.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        server.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
        database.script().run("/truncate.sql");
        database.script().run("/seed-test-db.sql");
    }
    @AfterEach
    final void afterEach() {
        transaction.rollback();
    }

    @Test
    void testIndex() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Анализатор страниц");
    }
    @Test
    void testAddCorrectUrl() {

        String testUrl = "https://ya.ru";

        // Выполняем POST запрос при помощи агента Unirest
        HttpResponse responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", testUrl)
                .asEmpty();

        // Проверяем статус ответа
        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains(testUrl);
        assertThat(body).contains("Страница успешно добавлена");

        // Проверяем, что ссылка добавлена в БД
        Url actualUrl = new QUrl()
                .name.equalTo(testUrl)
                .findOne();

        assertThat(actualUrl).isNotNull();
        assertThat(actualUrl.getName()).isEqualTo(testUrl);
    }

    @Test
    void testAddIncorrectUrl() {

        String testUrl = "ya.ru";

        // Выполняем POST запрос при помощи агента Unirest
        HttpResponse responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", testUrl)
                .asEmpty();

        // Проверяем статус ответа
        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains("Ссылка некорректная");

        // Проверяем, что ссылка не добавлена в БД
        Url actualUrl = new QUrl()
                .name.equalTo(testUrl)
                .findOne();

        assertThat(actualUrl).isNull();

    }

    @Test
    void testListUrls() {
        // Выполняем GET запрос на адрес http://localhost:port/urls
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        // Получаем тело ответа
        String content = response.getBody();

        // Проверяем код ответа
        assertThat(response.getStatus()).isEqualTo(200);
        // Проверяем, что страница содержит определенный текст
        assertThat(content.contains("https://vk.com"));
        assertThat(content.contains("https://github.com"));
    }

    @Test
    void testAddDuplicateUrl() {
        String testUrl = "https://vk.com";

        HttpResponse responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", testUrl)
                .asEmpty();

        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains(testUrl);
        assertThat(body).contains("Ссылка уже добавлена");

    }


    @Test
    void testShowUrl() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/1")
                .asString();
        // Проверяем код ответа
        assertThat(response.getStatus()).isEqualTo(200);
        // Проверяем, что страница содержит определенный текст
        assertThat(response.getBody()).contains("https://vk.com");
        assertThat(response.getBody()).contains("https://github.com");
    }

    @Test
    public void testCheckUrl() throws Exception {
        String serverUrl = server.url("/").toString();
        String correctServerUrl = serverUrl.substring(0, serverUrl.length() - 1);


        Unirest.post(baseUrl + "/urls")
                .field("url", serverUrl)
                .asEmpty();

        Url url = new QUrl()
                .name.equalTo(correctServerUrl)
                .findOne();

        assertThat(url).isNotNull();

        Unirest.post(baseUrl + "/urls/" + url.getId() + "/checks")
                .asEmpty();

        HttpResponse<String> responseResult = Unirest
                .get(baseUrl + "/urls/" + url.getId())
                .asString();

        String responseBody = responseResult.getBody();
        assertThat(responseBody).contains("Страница успешно проверена");
        assertThat(responseResult.getStatus()).isEqualTo(200);

        UrlCheck check = new QUrlCheck()
                .findList().get(0);

        assertThat(check).isNotNull();

        String content = responseResult.getBody();

        assertThat(content).contains("Title тестовой страницы");
        assertThat(content).contains("description тестовой страницы");
        assertThat(content).contains("Заголовок h1 тестовой страницы");
    }

}
