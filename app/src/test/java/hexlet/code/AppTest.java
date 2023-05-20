package hexlet.code;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppTest {
    @Test
    public void appTest() {
        assertTrue(App.greeting().equals("hello world!"));
    }
}
