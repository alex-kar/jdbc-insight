package insight;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.*;
import java.sql.DatabaseMetaData;
import java.util.Properties;

@ExtendWith(MockitoExtension.class)
@Testcontainers
public class InsightTest {

    @Container
    private final PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Test
    public void test() throws SQLException {

        DriverManager.registerDriver(new DriverInsight());
//        DriverManager.registerDriver(new org.postgresql.Driver());

        String url = String.format("jdbc:insight:postgresql://%s:%s/%s",
                container.getHost(), container.getFirstMappedPort(), container.getDatabaseName());

        Properties props = new Properties();
        props.put("user", container.getUsername());
        props.put("password", container.getPassword());

        try (Connection conn = DriverManager.getConnection(url, props)) {

            DatabaseMetaData databaseMetaData = conn.getMetaData();
            ResultSet catalogs = databaseMetaData.getCatalogs();
            while (catalogs.next()) {
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("select 1;")) {
                ResultSetMetaData resultSetMetaData = rs.getMetaData();
                resultSetMetaData.getColumnTypeName(1);
                while (rs.next()) {
                    System.out.println(rs.getInt(1));
                }
            }
        }
    }
}
