package infra.persistence.support;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import infra.persistence.VersionIncrementStrategy;

/**
 * Default implementation of {@link VersionIncrementStrategy}.
 * <p>
 * This strategy supports incrementing numeric versions ({@link Integer}, {@link Long}, {@link Short})
 * by adding 1, and updating {@link Instant} versions to the current time.
 * For unsupported types, it returns {@code null}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/6 10:19
 */
public class DefaultVersionIncrementStrategy implements VersionIncrementStrategy {

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
      return Instant.now();
    }

    if (currentVersion instanceof LocalDateTime) {
      return LocalDateTime.now();
    }

    if (currentVersion instanceof ZonedDateTime zoned) {
      return ZonedDateTime.now(zoned.getZone());
    }

    if (currentVersion instanceof OffsetDateTime offset) {
      return OffsetDateTime.now(offset.getOffset());
    }

    return null;
  }

}
