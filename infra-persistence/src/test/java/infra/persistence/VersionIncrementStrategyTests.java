package infra.persistence;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import infra.persistence.support.DefaultVersionIncrementStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/6 18:24
 */
class VersionIncrementStrategyTests {

  @Test
  void incrementVersion_integer_increments() {
    assertThat(createDefaults().nextVersion(5)).isEqualTo(6);
  }

  @Test
  void incrementVersion_long_increments() {
    assertThat(createDefaults().nextVersion(10L)).isEqualTo(11L);
  }

  @Test
  void incrementVersion_short_increments() {
    assertThat(createDefaults().nextVersion((short) 3)).isEqualTo((short) 4);
  }

  @Test
  void incrementVersion_zeroValue_increments() {
    assertThat(createDefaults().nextVersion(0)).isEqualTo(1);
  }

  @Test
  void incrementVersion_instant_returnsCurrentTime() {
    Instant before = Instant.now().minusMillis(1);
    Object result = createDefaults().nextVersion(Instant.now());
    assertThat(result).isInstanceOf(Instant.class);
    assertThat(((Instant) result).toEpochMilli()).isGreaterThanOrEqualTo(before.toEpochMilli());
  }

  @Test
  void strategy_defaults_handlesAllBuiltInTypes() {
    VersionIncrementStrategy strategy = createDefaults();

    int intv = 1;

    assertThat(strategy.nextVersion(null)).isNull();
    assertThat(strategy.nextVersion(intv)).isEqualTo(intv + 1);
    assertThat(strategy.nextVersion(5)).isEqualTo(6);
    assertThat(strategy.nextVersion(10L)).isEqualTo(11L);
    assertThat(strategy.nextVersion((short) 3)).isEqualTo((short) 4);
    assertThat(strategy.nextVersion(new Timestamp(0))).isNull();
    assertThat(strategy.nextVersion(Instant.now())).isInstanceOf(Instant.class);
  }

  private static VersionIncrementStrategy createDefaults() {
    return new DefaultVersionIncrementStrategy();
  }

}