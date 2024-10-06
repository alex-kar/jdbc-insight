package insight;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.sql.*;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class Driver implements java.sql.Driver {
    private static final String URL_PREFIX = "jdbc:insight:";

    @Override
    public Connection connect(String url, Properties props) throws SQLException {
        String targetUrl = removeUrlPrefix(url);
        java.sql.Driver driver = DriverManager.getDriver(targetUrl);
        if (Objects.nonNull(driver)) {
            if (driver.acceptsURL(targetUrl)) {
                return new Connection(driver.connect(targetUrl, props), initOtel());
            }
        }
        throw new SQLException("No suitable driver found for " + targetUrl);
    }

    private OpenTelemetry initOtel() {
        OtlpGrpcSpanExporter grpcSpanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://127.0.0.1:4317")
                .build();

        LoggingSpanExporter loggingSpanExporter = new LoggingSpanExporter();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.builder(grpcSpanExporter).build())
                .addSpanProcessor(SimpleSpanProcessor.builder(loggingSpanExporter).build())
                .setResource(Resource.getDefault().merge(Resource.builder()
                        .put("service.name", "insight")
                        .build()))
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        return openTelemetry;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        if (Objects.isNull(url) || !url.startsWith(URL_PREFIX)) {
            return false;
        }
        String targetUrl = removeUrlPrefix(url);
        java.sql.Driver driver = DriverManager.getDriver(targetUrl);
        boolean result = Objects.nonNull(driver);
        return result;
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
