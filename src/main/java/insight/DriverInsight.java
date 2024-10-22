package insight;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.lang.reflect.Proxy;
import java.net.*;
import java.sql.*;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class DriverInsight implements Driver {
    private static final String URL_PREFIX = "jdbc:insight:";

    private final OtelFactory otelFactory;

    public DriverInsight() {
        this.otelFactory = new OtelFactory();
    }

    public DriverInsight(OtelFactory otelFactory) {
        this.otelFactory = otelFactory;
    }

    @Override
    public Connection connect(String url, Properties properties) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }

        String targetUrl = removeUrlPrefix(url);
        Properties urlProps = UrlParser.parse(targetUrl);
        Driver driver;
        String jdbcPath = (String) urlProps.get("jdbcPath");
        String mainClass = (String) urlProps.get("mainClass");
        if (!Objects.isNull(jdbcPath) && !Objects.isNull(mainClass)) {
            try {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file:" + jdbcPath)}, this.getClass().getClassLoader());
                driver = (Driver) Class.forName(mainClass, true, classLoader).newInstance();
            } catch (MalformedURLException | ClassNotFoundException |
                     InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            driver = DriverManager.getDriver(targetUrl);
        }
        if (Objects.nonNull(driver)) {
            if (driver.acceptsURL(targetUrl)) {
                Tracer driverTracer = otelFactory.initTracer("DriverInsight");
                Span driverSpan = driverTracer.spanBuilder(driver.getClass().getCanonicalName()).startSpan();
                try (Scope dirverScope = driverSpan.makeCurrent()) {
                    Tracer connTracer = otelFactory.initTracer("Connection");
                    Span connSpan = connTracer.spanBuilder(targetUrl).startSpan();
                    try (Scope connScope = connSpan.makeCurrent()) {
                        return wrapWithProxy(driver.connect(targetUrl, properties), connTracer, Context.current());
                    } finally {
                        connSpan.end();
                    }
                } finally {
                    driverSpan.end();
                }
            }
        }
        throw new SQLException("No suitable driver found for " + targetUrl);
    }

    private Connection wrapWithProxy(Connection conn, Tracer tracer, Context parentContext) {
        Connection proxy = (Connection) Proxy.newProxyInstance(
                conn.getClass().getClassLoader(),
                conn.getClass().getInterfaces(),
                new GenericInvocationHandler(conn, tracer, parentContext)
        );
        return proxy;
    }


    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return Objects.nonNull(url) && url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    private String removeUrlPrefix(String url) {
        return url.replace(URL_PREFIX, "jdbc:");
    }
}
