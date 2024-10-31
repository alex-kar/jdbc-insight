package insight;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.lang.reflect.Proxy;
import java.net.*;
import java.sql.*;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import static insight.PropsParser.JDBC_CLASS;
import static insight.PropsParser.JDBC_PATH;

public class DriverInsight implements Driver {
    private static final String URL_PREFIX = "jdbc:insight:";

    private final OtelFactory otelFactory;
    private Tracer driverTracer;

    public DriverInsight() {
        this.otelFactory = new OtelFactory();
    }

    public DriverInsight(OtelFactory otelFactory) {
        this.otelFactory = otelFactory;
    }

    private void init(OtelFactory otelFactory) {
        this.driverTracer = otelFactory.initTracer("DriverInsight");
    }

    @Override
    public Connection connect(String url, Properties properties) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        init(otelFactory);
        Span driverSpan = driverTracer.spanBuilder("connect").startSpan();
        String targetUrl = removeUrlPrefix(url);
        Map<String, String> urlProps = PropsParser.parse(properties, targetUrl);
        String jdbcPath = urlProps.get(JDBC_PATH);
        String mainClass = urlProps.get(JDBC_CLASS);
        Driver driver = loadDriver(jdbcPath, mainClass, targetUrl);
        try (Scope dirverScope = driverSpan.makeCurrent()) {
            try {
                Span delegateSpan = driverTracer.spanBuilder(driver.getClass().getCanonicalName()).startSpan();
                try (Scope delegateScope = driverSpan.makeCurrent()) {
                    Tracer connTracer = otelFactory.initTracer("Connection");
                    Span connSpan = connTracer.spanBuilder(targetUrl).startSpan();
                    try (Scope connScope = connSpan.makeCurrent()) {
                        return wrapWithProxy(driver.connect(targetUrl, properties), connTracer, Context.current(), otelFactory);
                    } finally {
                        connSpan.end();
                    }
                } finally {
                    delegateSpan.end();
                }
            } catch (Exception e) {
                driverSpan.recordException(e, Attributes.of(AttributeKey.booleanKey("exception.escaped"), true));
                driverSpan.setAttribute(AttributeKey.booleanKey("error"), true);
                throw e;
            } finally {
                driverSpan.end();
            }
        }
    }

    private Driver loadDriver(String jdbcPath, String mainClass, String targetUrl) throws SQLException {
        if (!Objects.isNull(jdbcPath) && !Objects.isNull(mainClass)) {
            try {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file:" + jdbcPath)}, this.getClass().getClassLoader());
                return (Driver) Class.forName(mainClass, true, classLoader).newInstance();
            } catch (MalformedURLException e) {
                throw new SQLException("JDBC Insight failed to load delegate driver at " + jdbcPath, e);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new SQLException("JDBC Insight failed to instantiate " + mainClass, e);
            }
        }
        try {
            return DriverManager.getDriver(targetUrl);
        } catch (SQLException e) {
            throw new SQLException("JDBC Insight was loaded, but no suitable driver was found for " + targetUrl);
        }
    }

    private Connection wrapWithProxy(Connection conn, Tracer tracer, Context parentContext, OtelFactory otelFactory) {
        Connection proxy = (Connection) Proxy.newProxyInstance(
                conn.getClass().getClassLoader(),
                conn.getClass().getInterfaces(),
                new GenericInvocationHandler(conn, tracer, parentContext, otelFactory)
        );
        return proxy;
    }


    @Override
    public boolean acceptsURL(String url) throws SQLException {
        if (Objects.isNull(url)) {
            throw new SQLException("URL is null");
        }
        return url.startsWith(URL_PREFIX);
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
