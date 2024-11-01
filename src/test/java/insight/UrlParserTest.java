package insight;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static insight.PropsParser.*;
import static org.junit.jupiter.api.Named.named;

class UrlParserTest {

    @MethodSource("parseUrlArgs")
    @ParameterizedTest
    public void parseUrl(ParseUrl args) {
        Map<String,String> actual = PropsParser.parse(new Properties(), args.url);

        Assertions.assertEquals(args.expected, actual);
    }

    private static Stream<Named<ParseUrl>> parseUrlArgs() {
        return Stream.of(
                named("No parameters",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database", Map.of())),
                named("Single parameter",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database?jdbcpath=jdbc_path",
                                Map.of(
                                        JDBC_PATH, "jdbc_path"
                                ))),
                named("Multiple parameters",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database?jdbcpath=jdbc_path&jdbcclass=jdbc_class",
                                Map.of(
                                        JDBC_PATH, "jdbc_path",
                                        JDBC_CLASS, "jdbc_class"
                                ))),
                named("Case insensitive parameters",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database?JDBCPATH=jdbc_path&JDBCCLASS=jdbc_class",
                                Map.of(
                                        JDBC_PATH, "jdbc_path",
                                        JDBC_CLASS, "jdbc_class"
                                ))),
                named("Ignore unknown parameters",
                        new ParseUrl("jdbc:postgresql://localhost:5432/database?unknown=value",
                                Map.of())
                ));
    }


    @MethodSource("parsePropsArgs")
    @ParameterizedTest
    public void parsePropsTest(ParseProps args) {
        String url = "jdbc:postgresql://localhost:5432/database";
        Properties props = args.properties.get();

        Map<String,String> actual = PropsParser.parse(props, url);

        Assertions.assertEquals(args.expected, actual);
    }

    private static Stream<Named<ParseProps>> parsePropsArgs() {
        return Stream.of(
                named("No parameters",
                        new ParseProps(Properties::new, Map.of())),
                named("Single parameter",
                        new ParseProps(
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    return props;
                                },
                                Map.of(
                                        JDBC_PATH, "jdbc_path"
                                ))),
                named("Multiple parameters",
                        new ParseProps(
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    props.put(JDBC_CLASS, "jdbc_class");
                                    return props;
                                },
                                Map.of(
                                        JDBC_PATH, "jdbc_path",
                                        JDBC_CLASS, "jdbc_class"
                                ))),
                named("Case insensitive parameters",
                        new ParseProps(
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "jdbc_path");
                                    props.put(JDBC_CLASS, "jdbc_class");
                                    return props;
                                },
                                Map.of(
                                        JDBC_PATH, "jdbc_path",
                                        JDBC_CLASS, "jdbc_class"
                                ))),
                named("Ignore unknown parameters",
                        new ParseProps(
                                () -> {
                                    Properties props = new Properties();
                                    props.put("unknown", "value");
                                    return props;
                                },
                                Map.of()))
        );
    }

    @MethodSource("mergePropsArgs")
    @ParameterizedTest
    public void mergePropsTest(MergeProps args) {
        String url = args.url;
        Properties props = args.properties.get();

        Map<String,String> actual = PropsParser.parse(props, url);

        Assertions.assertEquals(args.expected, actual);
    }

    private static Stream<Named<MergeProps>> mergePropsArgs() {
        return Stream.of(
                named("Url arguments override Properties",
                        new MergeProps(
                                "jdbc:postgresql://localhost:5432/database?jdbcPath=path_from_url&jdbcClass=class_from_url",
                                () -> {
                                    Properties props = new Properties();
                                    props.put(JDBC_PATH, "path_from_properties");
                                    props.put(JDBC_CLASS, "class_from_properties");
                                    return props;
                                },
                                Map.of(
                                        JDBC_PATH, "path_from_url",
                                        JDBC_CLASS, "class_from_url"
                                )))
        );
    }

    record ParseUrl(String url, Map<String, String> expected) {}

    record ParseProps(Supplier<Properties> properties, Map<String, String> expected) {}

    record MergeProps(String url, Supplier<Properties> properties, Map<String, String> expected) {}
}