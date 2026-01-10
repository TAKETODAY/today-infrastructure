/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.util;

/**
 * A standard set of {@link DataSize} units.
 *
 * <p>The unit prefixes used in this class are
 * <a href="https://en.wikipedia.org/wiki/Binary_prefix">binary prefixes</a>
 * indicating multiplication by powers of 2. The following table displays the
 * enum constants defined in this class and corresponding values.
 *
 * <p>
 * <table border="1">
 * <tr><th>Constant</th><th>Data Size</th><th>Power&nbsp;of&nbsp;2</th><th>Size in Bytes</th></tr>
 * <tr><td>{@link #BYTES}</td><td>1B</td><td>2^0</td><td>1</td></tr>
 * <tr><td>{@link #KILOBYTES}</td><td>1KB</td><td>2^10</td><td>1,024</td></tr>
 * <tr><td>{@link #MEGABYTES}</td><td>1MB</td><td>2^20</td><td>1,048,576</td></tr>
 * <tr><td>{@link #GIGABYTES}</td><td>1GB</td><td>2^30</td><td>1,073,741,824</td></tr>
 * <tr><td>{@link #TERABYTES}</td><td>1TB</td><td>2^40</td><td>1,099,511,627,776</td></tr>
 * </table>
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.1.3
 */
public enum DataUnit {

  /** Bytes. */
  BYTES("B", DataSize.ofBytes(1)),

  /** Kilobytes. */
  KILOBYTES("KB", DataSize.ofKilobytes(1)),

  /** Megabytes. */
  MEGABYTES("MB", DataSize.ofMegabytes(1)),

  /** Gigabytes. */
  GIGABYTES("GB", DataSize.ofGigabytes(1)),

  /** Terabytes. */
  TERABYTES("TB", DataSize.ofTerabytes(1));

  private final String suffix;

  private final DataSize size;

  DataUnit(String suffix, DataSize size) {
    this.suffix = suffix;
    this.size = size;
  }

  DataSize size() {
    return this.size;
  }

  /**
   * Return the {@link DataUnit} matching the specified {@code suffix}.
   *
   * @param suffix one of the standard suffixes
   * @return the {@link DataUnit} matching the specified {@code suffix}
   * @throws IllegalArgumentException if the suffix does not match the suffix
   * of any of this enum's constants
   */
  public static DataUnit fromSuffix(String suffix) {
    for (DataUnit candidate : values()) {
      if (candidate.suffix.equalsIgnoreCase(suffix)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Unknown unit '%s'".formatted(suffix));
  }

}
