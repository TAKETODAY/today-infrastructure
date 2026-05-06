package infra.persistence.support;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/6 19:14
 */
class DefaultVersionIncrementStrategyTests {

  @Test
  void defaultStrategy_integer_increments() {
    DefaultVersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy();
    assertThat(strategy.nextVersion(5)).isEqualTo(6);
  }

  @Test
  void defaultStrategy_long_increments() {
    DefaultVersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy();
    assertThat(strategy.nextVersion(10L)).isEqualTo(11L);
  }

  @Test
  void defaultStrategy_short_increments() {
    DefaultVersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy();
    assertThat(strategy.nextVersion((short) 3)).isEqualTo((short) 4);
  }

  @Test
  void defaultStrategy_instant_returnsNow() {
    DefaultVersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy();
    Instant before = Instant.now().minusMillis(1);
    Object result = strategy.nextVersion(Instant.now());
    assertThat(result).isInstanceOf(Instant.class);
    assertThat(((Instant) result).toEpochMilli()).isGreaterThanOrEqualTo(before.toEpochMilli());
  }

  @Test
  void defaultStrategy_localDateTime_returnsNow() {
    DefaultVersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy();
    Object result = strategy.nextVersion(LocalDateTime.now());
    assertThat(result).isInstanceOf(LocalDateTime.class);
  }

  @Test
  void defaultStrategy_zonedDateTime_returnsNowWithSameZone() {
    DefaultVersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy();
    ZoneId zone = ZoneId.of("Asia/Shanghai");
    Object result = strategy.nextVersion(ZonedDateTime.now(zone));
    assertThat(result).isInstanceOf(ZonedDateTime.class);
    assertThat(((ZonedDateTime) result).getZone()).isEqualTo(zone);
  }

  @Test
  void defaultStrategy_offsetDateTime_returnsNowWithSameOffset() {
    DefaultVersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy();
    ZoneOffset offset = ZoneOffset.ofHours(8);
    Object result = strategy.nextVersion(OffsetDateTime.now(offset));
    assertThat(result).isInstanceOf(OffsetDateTime.class);
    assertThat(((OffsetDateTime) result).getOffset()).isEqualTo(offset);
  }

  @Test
  void defaultStrategy_unsupportedType_returnsNull() {
    DefaultVersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy();
    assertThat(strategy.nextVersion("unsupported")).isNull();
  }

}