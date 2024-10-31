package insight;

import java.net.URI;
import java.util.*;

public class PropsParser {
    public static final String JDBC_PATH = "jdbcpath";
    public static final String JDBC_CLASS = "jdbcclass";

    private static final Set<String> supportedProps = Set.of(JDBC_PATH, JDBC_CLASS);

    public static Map<String, String> parse(Properties props, String urlProps) {
        Map<String, String> fromUrl = parseUrl(urlProps);
        Map<String, String> fromProps = parseProps(props);
        return override(fromProps, fromUrl);
    }

    //TODO: Add support for semicolon-separated parameters
    private static Map<String, String> parseUrl(String url) {
        URI uri = URI.create(url.substring(5));
        Map<String, String> resultMap = new HashMap<>();
        if (!Objects.isNull(uri.getQuery())) {
            for (String param : uri.getQuery().split("&")) {
                String[] keyValue = param.split("=");
                if (supportedProps.contains(keyValue[0].toLowerCase())) {
                    resultMap.put(keyValue[0].toLowerCase(), keyValue[1]);
                }
            }
        }
        return resultMap;
    }

    private static Map<String, String> parseProps(Properties props) {
        Map<String, String> result = new HashMap<>();
        props.forEach((k,v) -> {
            if (k instanceof String key && v instanceof String value) {
                if (supportedProps.contains(key)) {
                    result.put(key, value);
                }
            }
        });
        return result;
    }

    private static Map<String, String> override(Map<String, String> baseProperties,
                                                Map<String, String> overrideProperties) {
        Map<String, String> resultMap = new HashMap<>();
        resultMap.putAll(baseProperties);
        resultMap.putAll(overrideProperties);
        return resultMap;
    }
}
