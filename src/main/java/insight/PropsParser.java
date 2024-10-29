package insight;

import java.net.URI;
import java.util.Objects;
import java.util.Properties;

public class PropsParser {
    public static final String JDBC_PATH = "jdbcpath";
    public static final String JDBC_CLASS = "jdbcclass";

    public static Properties parse(Properties props, String urlProps) {
        return parseUrl(urlProps);
    }

    //TODO: Add support for semicolon-separated parameters
    private static Properties parseUrl(String url) {
        URI uri = URI.create(url.substring(5));
        Properties props = new Properties();
        if (!Objects.isNull(uri.getQuery())) {
            for (String param : uri.getQuery().split("&")) {
                String[] keyValue = param.split("=");
                props.put(keyValue[0].toLowerCase(), keyValue[1]);
            }
        }
        return props;
    }
}
