package insight;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

class UrlParserTest {

    @MethodSource("parseArgs")
    @ParameterizedTest
    public void parseTest(ParseArgs args) {

        Properties actual = UrlParser.parse(args.url);

        Assertions.assertEquals(args.expected.get(), actual);
    }

    private static Stream<ParseArgs> parseArgs() {
        return Stream.of(
                new ParseArgs("jdbc:postgresql://localhost:5432/database", Properties::new),
                new ParseArgs("jdbc:postgresql://localhost:5432/database?param1=value1", () -> {
                    Properties props = new Properties();
                    props.put("param1", "value1");
                    return props;
                }),
                new ParseArgs("jdbc:postgresql://localhost:5432/database?param1=value1&param2=value2", () -> {
                    Properties props = new Properties();
                    props.put("param1", "value1");
                    props.put("param2", "value2");
                    return props;
                })
        );
    }

    record ParseArgs(String url, Supplier<Properties> expected) {}
}