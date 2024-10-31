package insight;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static insight.PropsParser.JDBC_CLASS;
import static insight.PropsParser.JDBC_PATH;
import static org.junit.jupiter.api.Named.named;

class UrlParserTest {

    @MethodSource("parseUrlArgs")
    @ParameterizedTest
    public void parseUrl(ParseUrl args) {
        Map<String,String> actual = PropsParser.parse(new Properties(), args.url);

        Assertions.assertEquals(args.expected.get(), actual);
    }

    private static Stream<Named<ParseUrl>> parseUrlArgs() {
        return Stream.of(
                named("No parameters",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database", Properties::new)),
                named("Single parameter",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database?jdbcpath=jdbc_path", () -> {
                            Properties props = new Properties();
                            props.put(JDBC_PATH, "jdbc_path");
                            return props;
                        })),
                named("Multiple parameters",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database?jdbcpath=jdbc_path&jdbcclass=jdbc_class", () -> {
                            Properties props = new Properties();
                            props.put(JDBC_PATH, "jdbc_path");
                            props.put(JDBC_CLASS, "jdbc_class");
                            return props;
                        })),
                named("Case insensitive parameters",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database?JDBCPATH=jdbc_path&JDBCCLASS=jdbc_class", () -> {
                            Properties props = new Properties();
                            props.put(JDBC_PATH, "jdbc_path");
                            props.put(JDBC_CLASS, "jdbc_class");
                            return props;
                        }))
        );
    }


    @MethodSource("parsePropsArgs")
    @ParameterizedTest
    public void parsePropsTest(ParseProps args) {
        String url = "jdbc:postgresql://localhost:5432/database";
        Properties props = args.properties.get();

        Map<String,String> actual = PropsParser.parse(props, url);

        Assertions.assertEquals(args.expected.get(), actual);
    }

    private static Stream<Named<ParseProps>> parsePropsArgs() {
        return Stream.of(
                named("No parameters",
                        new ParseProps(Properties::new, Properties::new)),
                named("Single parameter",
                        new ParseProps(
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    return props;
                                },
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    return props;
                        })),
                named("Multiple parameters",
                        new ParseProps(
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    props.put(JDBC_CLASS, "jdbc_class");
                                    return props;
                                },
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    props.put(JDBC_CLASS, "jdbc_class");
                                    return props;
                                })),
                named("Case insensitive parameters",
                        new ParseProps(
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    props.put(JDBC_CLASS, "jdbc_class");
                                    return props;
                                },
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    props.put(JDBC_CLASS, "jdbc_class");
                                    return props;
                                }))
        );
    }

    record ParseUrl(String url, Supplier<Properties> expected) {}

    record ParseProps(Supplier<Properties> properties, Supplier<Properties> expected) {}
}