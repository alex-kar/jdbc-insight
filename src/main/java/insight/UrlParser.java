package insight;

import java.net.URI;
import java.util.Objects;
import java.util.Properties;

public class UrlParser {

    public static Properties parse(String url) {
        URI uri = URI.create(url.substring(5));
        Properties props = new Properties();
        if (!Objects.isNull(uri.getQuery())) {
            for (String param : uri.getQuery().split("&")) {
                String[] keyValue = param.split("=");
                props.put(keyValue[0], keyValue[1]);
            }
        }
        return props;
    }

}
