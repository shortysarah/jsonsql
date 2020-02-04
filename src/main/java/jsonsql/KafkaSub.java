package jsonsql;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.utils.AppInfoParser;

import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

// copy pasted from here
// https://github.com/micronaut-projects/micronaut-kafka/blob/master/kafka/src/main/java/io/micronaut/configuration/kafka/graal/KafkaSubstitutions.java

@TargetClass(className = "org.apache.kafka.common.utils.Crc32C$Java9ChecksumFactory")
final class Java9ChecksumFactory {

    @Substitute
    public Checksum create() {
        return new CRC32();
    }

}

// Replace unsupported compression types
@TargetClass(className = "org.apache.kafka.common.record.CompressionType")
final class CompressionTypeSubs {

    // @Alias @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    // public static CompressionType LZ4 = CompressionType.GZIP;

    @Alias @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static CompressionType SNAPPY = CompressionType.GZIP;

    @Alias @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static CompressionType ZSTD = CompressionType.GZIP;
}

// Replace JMX metrics, no operable on GraalVM
@TargetClass(className = "org.apache.kafka.common.metrics.JmxReporter")
@Substitute
final class NoopReporter implements MetricsReporter {

    @Substitute
    public NoopReporter() {
    }

    @Substitute
    public NoopReporter(String prefix) {
    }

    @Override
    @Substitute
    public void init(List<KafkaMetric> metrics) {
    }

    @Override
    @Substitute
    public void metricChange(KafkaMetric metric) {
    }

    @Override
    @Substitute
    public void metricRemoval(KafkaMetric metric) {
    }

    @Override
    @Substitute
    public void close() {
    }

    @Override
    @Substitute
    public void configure(Map<String, ?> configs) {
    }
}

@TargetClass(AppInfoParser.class)
final class AppInfoParserNoJMX {

    @Substitute
    public static void registerAppInfo(String prefix, String id, Metrics metrics, long nowMs) {
        // no-op
    }

    @Substitute
    public static void unregisterAppInfo(String prefix, String id, Metrics metrics) {
        // no-op
    }
}
