package insight;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;


public class OtelFactory {

    public Tracer initTracer(String serviceName) {
        OtlpGrpcSpanExporter grpcSpanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://127.0.0.1:4317")
                .build();

        LoggingSpanExporter loggingSpanExporter = new LoggingSpanExporter();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.builder(grpcSpanExporter).build())
                .addSpanProcessor(SimpleSpanProcessor.builder(loggingSpanExporter).build())
                .setResource(Resource.getDefault().merge(Resource.builder()
                        .put("service.name", serviceName)
                        .build()))
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        return openTelemetry.getTracerProvider().tracerBuilder("scope_name_" + serviceName).build();
    }

}
