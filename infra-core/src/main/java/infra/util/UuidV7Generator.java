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

package infra.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RFC 9562 UUID version 7 generation with a 48-bit Unix millisecond timestamp
 * with 12 bits of sub-millisecond precision and a 62-bit random sequence.
 * Thread-safe; UUIDs increase monotonically in state-update order within each
 * generator instance, not necessarily in concurrent invocation completion order.
 * Logical time advances when necessary to preserve monotonicity, including on
 * clock rollback, and may therefore lead the wall clock.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class UuidV7Generator implements UuidGenerator {

  private static final SecureRandom random = new SecureRandom();

  private final AtomicReference<State> lastState = new AtomicReference<>(new State(Instant.EPOCH, Long.MIN_VALUE));

  @Override
  public UUID generateId() {
    State state = lastState.updateAndGet(previous ->
            previous.next(Instant.now(), random.nextLong() & 0x3FFFFFFFFFFFFFFFL));
    long mostSignificantBits = (state.timestamp().toEpochMilli() << 16) | 0x7000L | state.subMillis();
    long leastSignificantBits = state.sequence() | 0x8000000000000000L;
    return new UUID(mostSignificantBits, leastSignificantBits);
  }

  record State(Instant timestamp, long sequence, long subMillis) {

    State(Instant timestamp, long sequence) {
      this(timestamp, sequence, subMillis(timestamp));
    }

    State next(Instant now, long nextSequence) {
      long millis = timestamp.toEpochMilli();
      long nowMillis = now.toEpochMilli();
      if (nowMillis > millis || (nowMillis == millis && subMillis(now) > subMillis)) {
        return new State(now, nextSequence);
      }
      // ceil(1_000_000 / 4096) nanoseconds always advances the encoded timestamp.
      Instant nextTimestamp = nextSequence > sequence ? timestamp : timestamp.plusNanos(245);
      return new State(nextTimestamp, nextSequence);
    }

    private static long subMillis(Instant timestamp) {
      return (timestamp.getNano() % 1_000_000L) * 4096L / 1_000_000L;
    }

  }

}
