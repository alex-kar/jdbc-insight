package insight;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.SQLException;

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

}