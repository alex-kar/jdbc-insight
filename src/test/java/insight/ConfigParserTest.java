package insight;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static insight.ConfigParser.*;
import static org.junit.jupiter.api.Named.named;

class ConfigParserTest {

    @MethodSource("parseArgs")
    @ParameterizedTest
    public void parseTest(ParseArgs args) {
        Map<String,String> actual = ConfigParser.parse(args.properties.get(), args.url);

        Assertions.assertEquals(args.expected, actual);
    }

    private static Stream<Named<ParseArgs>> parseArgs() {
        return Stream.of(
                named("No parameters",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database", Properties::new, Map.of())),
                named("Single Url parameter",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database?jdbcPath=jdbc_path",
                                Properties::new,
                                Map.of(JDBC_PATH, "jdbc_path"))),
                named("Multiple Url parameters",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database?jdbcPath=jdbc_path&jdbcClass=jdbc_class",
                                Properties::new,
                                Map.of(JDBC_PATH, "jdbc_path", JDBC_CLASS, "jdbc_class"))),
                named("Case insensitive Url parameters",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database?JDBCpath=jdbc_path&JDBCclass=jdbc_class",
                                Properties::new,
                                Map.of(JDBC_PATH, "jdbc_path", JDBC_CLASS, "jdbc_class"))),
                named("Ignore unknown parameters",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database?unknown=value",
                                () -> {
                                    Properties props = new Properties();
                                    props.put("unknown", "value");
                                    return props;
                                },
                                Map.of())),
                named("Empty Properties",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database",
                                Properties::new,
                                Map.of())),
                named("Single value Properties",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database",
                                () -> {
                                    Properties props = new Properties();
                                    props.put("jdbcPath", "jdbc_path");
                                    return props;
                                },
                                Map.of(JDBC_PATH, "jdbc_path"))),
                named("Multiple values Properties",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database",
                                () -> {
                                    Properties props = new Properties();
                                    props.put("jdbcPath", "jdbc_path");
                                    props.put("jdbcClass", "jdbc_class");
                                    return props;
                                },
                                Map.of(JDBC_PATH, "jdbc_path", JDBC_CLASS, "jdbc_class"))),
                named("Case insensitive Properties",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database",
                                () -> {
                                    Properties props = new Properties();
                                    props.put("JDBCpath", "jdbc_path");
                                    props.put("JDBCclass", "jdbc_class");
                                    return props;
                                },
                                Map.of(JDBC_PATH, "jdbc_path", JDBC_CLASS, "jdbc_class"))),
                named("Url parameters take precedence over Properties",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database?jdbcPath=path_from_url&jdbcClass=class_from_url",
                                () -> {
                                    Properties props = new Properties();
                                    props.put("jdbcPath", "path_from_properties");
                                    props.put("jdbcClass", "class_from_properties");
                                    return props;
                                },
                                Map.of(JDBC_PATH, "path_from_url", JDBC_CLASS, "class_from_url")))
        );
    }

    record ParseArgs(String url, Supplier<Properties> properties, Map<String, String> expected) {}
}