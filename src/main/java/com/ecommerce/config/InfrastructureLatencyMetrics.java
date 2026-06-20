package com.ecommerce.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas de infraestrutura no estilo Micrometer:
 * <ul>
 *   <li>{@link Gauge} + callback — valor lido no scrape (disponível, última latência)</li>
 *   <li>{@link Counter} — total de probes com sucesso/falha (só cresce)</li>
 *   <li>{@link Timer} — histograma de latência (no Prometheus vira _bucket/_sum/_count)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InfrastructureLatencyMetrics {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    private final AtomicBoolean postgresAvailable = new AtomicBoolean(false);
    private final AtomicBoolean redisAvailable = new AtomicBoolean(false);
    private final AtomicLong lastPostgresPingNanos = new AtomicLong(0);
    private final AtomicLong lastRedisPingNanos = new AtomicLong(0);

    private Counter postgresProbeSuccess;
    private Counter postgresProbeFailure;
    private Counter redisProbeSuccess;
    private Counter redisProbeFailure;
    private Timer postgresPingTimer;
    private Timer redisPingTimer;

    @PostConstruct
    void registerMeters() {
        Gauge.builder("app.postgres.available", postgresAvailable, available -> available.get() ? 1.0 : 0.0)
                .description("1 se o último probe PostgreSQL passou, 0 caso contrário")
                .register(meterRegistry);

        Gauge.builder("app.redis.available", redisAvailable, available -> available.get() ? 1.0 : 0.0)
                .description("1 se o último probe Redis passou, 0 caso contrário")
                .register(meterRegistry);

        Gauge.builder("app.postgres.last_ping.seconds", lastPostgresPingNanos,
                        nanos -> nanos.get() > 0 ? nanos.get() / 1_000_000_000.0 : 0.0)
                .description("Latência do último probe PostgreSQL (segundos)")
                .register(meterRegistry);

        Gauge.builder("app.redis.last_ping.seconds", lastRedisPingNanos,
                        nanos -> nanos.get() > 0 ? nanos.get() / 1_000_000_000.0 : 0.0)
                .description("Latência do último probe Redis (segundos)")
                .register(meterRegistry);

        postgresProbeSuccess = Counter.builder("app.postgres.probe.total")
                .tag("outcome", "success")
                .description("Probes PostgreSQL bem-sucedidos")
                .register(meterRegistry);

        postgresProbeFailure = Counter.builder("app.postgres.probe.total")
                .tag("outcome", "failure")
                .description("Probes PostgreSQL com falha")
                .register(meterRegistry);

        redisProbeSuccess = Counter.builder("app.redis.probe.total")
                .tag("outcome", "success")
                .description("Probes Redis bem-sucedidos")
                .register(meterRegistry);

        redisProbeFailure = Counter.builder("app.redis.probe.total")
                .tag("outcome", "failure")
                .description("Probes Redis com falha")
                .register(meterRegistry);

        postgresPingTimer = buildPingTimer(
                "app.postgres.ping",
                "Histograma de latência dos probes PostgreSQL");

        redisPingTimer = buildPingTimer(
                "app.redis.ping",
                "Histograma de latência dos probes Redis");
    }

    private Timer buildPingTimer(String name, String description) {
        return Timer.builder(name)
                .description(description)
                .publishPercentiles(0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @Scheduled(fixedRateString = "${management.metrics.probe-interval-ms:30000}")
    public void probeInfrastructure() {
        probePostgres();
        probeRedis();
    }

    private void probePostgres() {
        long start = System.nanoTime();
        boolean ok = false;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
            ok = true;
        } catch (Exception e) {
            log.warn("Probe PostgreSQL falhou: {}", e.getMessage());
        }
        long elapsed = System.nanoTime() - start;
        recordPostgres(ok, elapsed);
    }

    private void probeRedis() {
        long start = System.nanoTime();
        boolean ok = false;
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            ok = pong != null && "PONG".equalsIgnoreCase(pong);
            if (!ok) {
                log.warn("Probe Redis resposta inesperada: {}", pong);
            }
        } catch (Exception e) {
            log.warn("Probe Redis falhou: {}", e.getMessage());
        }
        long elapsed = System.nanoTime() - start;
        recordRedis(ok, elapsed);
    }

    private void recordPostgres(boolean ok, long elapsedNanos) {
        postgresAvailable.set(ok);
        lastPostgresPingNanos.set(elapsedNanos);
        if (ok) {
            postgresProbeSuccess.increment();
        } else {
            postgresProbeFailure.increment();
        }
        postgresPingTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    private void recordRedis(boolean ok, long elapsedNanos) {
        redisAvailable.set(ok);
        lastRedisPingNanos.set(elapsedNanos);
        if (ok) {
            redisProbeSuccess.increment();
        } else {
            redisProbeFailure.increment();
        }
        redisPingTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
    }
}
