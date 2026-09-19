/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.persistence.support;

import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import infra.persistence.VersionIncrementStrategy;
import infra.util.Assert;

/**
 * Default implementation of {@link VersionIncrementStrategy}.
 *
 * <p>It advances the following version types:
 * <ul>
 *   <li>Numeric types ({@link Integer}, {@link Long}, {@link Short}) by adding {@code 1}.</li>
 *   <li>Date-time types ({@link Instant}, {@link LocalDateTime}, {@link ZonedDateTime},
 *       {@link OffsetDateTime}) by setting them to the current time read from the
 *       configured {@link Clock}.</li>
 * </ul>
 *
 * <p>For {@link ZonedDateTime} and {@link OffsetDateTime}, the zone or offset of the
 * current value is preserved. Since {@link LocalDateTime} carries no zone information,
 * its next value is derived directly from the configured clock's zone (UTC by default).
 *
 * <p>Any other type is unsupported and returns {@code null}, allowing a composed strategy
 * (see {@link VersionIncrementStrategy#and}) to handle it.
 *
 * <p>The time source defaults to {@link Clock#systemUTC()} and can be replaced through
 * {@link #DefaultVersionIncrementStrategy(Clock)}, which makes time-based versions
 * deterministic:
 *
 * <pre>{@code
 * Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
 * VersionIncrementStrategy strategy = new DefaultVersionIncrementStrategy(clock);
 * }</pre>
 *
 * <p>Instances are immutable and therefore thread-safe.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/6 10:19
 */
public class DefaultVersionIncrementStrategy implements VersionIncrementStrategy {

  private final Clock clock;

  /**
   * Create a strategy using {@link Clock#systemUTC()} as the time source.
   */
  public DefaultVersionIncrementStrategy() {
    this(Clock.systemUTC());
  }

  /**
   * Create a strategy using the given clock as the time source for date-time versions.
   *
   * <p>For {@link ZonedDateTime} and {@link OffsetDateTime} only the clock's current
   * instant is used, since the zone or offset is taken from the version value itself.
   * For {@link LocalDateTime} the clock's zone determines the value, so supply a clock
   * with the desired zone (e.g. {@link Clock#systemDefaultZone()}) if UTC is not intended.
   *
   * @param clock the clock to read the current time from; must not be {@code null}
   * @throws IllegalArgumentException if {@code clock} is {@code null}
   */
  public DefaultVersionIncrementStrategy(Clock clock) {
    Assert.notNull(clock, "clock is required");
    this.clock = clock;
  }

  @Override
  public @Nullable Object nextVersion(Object currentVersion) {
    if (currentVersion instanceof Integer i) {
      return i + 1;
    }
    if (currentVersion instanceof Long l) {
      return l + 1L;
    }
    if (currentVersion instanceof Short s) {
      return (short) (s + 1);
    }

    if (currentVersion instanceof Instant) {
      return Instant.now(clock);
    }

    if (currentVersion instanceof LocalDateTime) {
      return LocalDateTime.now(clock);
    }

    if (currentVersion instanceof ZonedDateTime zoned) {
      return ZonedDateTime.now(clock.withZone(zoned.getZone()));
    }

    if (currentVersion instanceof OffsetDateTime offset) {
      return OffsetDateTime.now(clock.withZone(offset.getOffset()));
    }

    return null;
  }

}
