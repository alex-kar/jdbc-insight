package insight;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static insight.PropsParser.JDBC_CLASS;
import static insight.PropsParser.JDBC_PATH;
import static org.junit.jupiter.api.Named.named;

class UrlParserTest {

    @MethodSource("parseArgs")
    @ParameterizedTest
    public void parseTest(ParseArgs args) {
        Properties actual = PropsParser.parse(new Properties(), args.url);

        Assertions.assertEquals(args.expected.get(), actual);
    }

    private static Stream<Named<ParseArgs>> parseArgs() {
        return Stream.of(
                named("No parameters",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database", Properties::new)),
                named("Single parameter",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database?jdbcpath=jdbc_path", () -> {
                            Properties props = new Properties();
                            props.put(JDBC_PATH, "jdbc_path");
                            return props;
                        })),
                named("Multiple parameters",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database?jdbcpath=jdbc_path&jdbcclass=jdbc_class", () -> {
                            Properties props = new Properties();
                            props.put(JDBC_PATH, "jdbc_path");
                            props.put(JDBC_CLASS, "jdbc_class");
                            return props;
                        })),
                named("Case insensitive parameters",
                        new ParseArgs("jdbc:postgresql://localhost:5432/database?JDBCPATH=jdbc_path&JDBCCLASS=jdbc_class", () -> {
                            Properties props = new Properties();
                            props.put(JDBC_PATH, "jdbc_path");
                            props.put(JDBC_CLASS, "jdbc_class");
                            return props;
                        }))
        );
    }

    record ParseArgs(String url, Supplier<Properties> expected) {}
}