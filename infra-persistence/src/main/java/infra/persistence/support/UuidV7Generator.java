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

import java.security.SecureRandom;
import java.util.UUID;

/**
 * RFC 9562 UUID version 7 generation with a 48-bit Unix millisecond timestamp
 * and 74 random bits. Thread-safe; no counter or strict monotonicity guarantee.
 * Clock rollback is reflected in the timestamp rather than hidden by logical time.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class UuidV7Generator {

  private static final SecureRandom random = new SecureRandom();

  private UuidV7Generator() {
  }

  static UUID generate(long timestamp) {
    if (timestamp < 0 || timestamp > 0xFFFFFFFFFFFFL) {
      throw new IllegalArgumentException("UUID v7 timestamp must fit in 48 unsigned bits");
    }
    long mostSignificantBits = (timestamp << 16) | 0x7000L | (random.nextLong() & 0xFFFL);
    long leastSignificantBits = (random.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
    return new UUID(mostSignificantBits, leastSignificantBits);
  }

}
