package insight;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

class DriverTest {

    @CsvSource({
            "jdbc:postgresql://127.0.0.1:5432/postgres, false",
            "jdbc:insight:postgresql://127.0.0.1:5432/postgres, true",
    })
    @ParameterizedTest
    public void acceptsURLTest(String url, boolean expected) throws SQLException {
        boolean actual = new DriverInsight().acceptsURL(url);

        Assertions.assertEquals(actual, expected);
    }

    @Test
    public void whenDetectWrongUrl_thenReturnNull_Test() throws SQLException {
        Connection conn =
                new DriverInsight().connect("jdbc:postgresql://127.0.0.1:5432/postgres", new Properties());

        Assertions.assertNull(conn);
    }
}