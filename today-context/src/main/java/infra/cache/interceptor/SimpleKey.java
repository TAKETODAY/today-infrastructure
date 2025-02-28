/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.cache.interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A simple key as returned from the {@link SimpleKeyGenerator}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleKeyGenerator
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SimpleKey implements Serializable {

  /**
   * An empty key.
   */
  public static final SimpleKey EMPTY = new SimpleKey();

  private final Object[] params;

  // Effectively final, just re-calculated on deserialization
  private transient int hashCode;

  /**
   * Create a new {@link SimpleKey} instance.
   *
   * @param elements the elements of the key
   */
  public SimpleKey(@Nullable Object... elements) {
    Assert.notNull(elements, "Elements is required");
    this.params = elements.clone();
    // Pre-calculate hashCode field
    this.hashCode = calculateHash(this.params);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof SimpleKey that
            && Arrays.deepEquals(this.params, that.params)));
  }

  @Override
  public final int hashCode() {
    // Expose pre-calculated hashCode field
    return this.hashCode;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " " + Arrays.deepToString(this.params);
  }

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    // Re-calculate hashCode field on deserialization
    this.hashCode = calculateHash(this.params);
  }

  /**
   * Calculate the hash of the key using its elements and
   * mix the result with the finalising function of MurmurHash3.
   */
  private static int calculateHash(@Nullable Object[] params) {
    int hash = Arrays.deepHashCode(params);
    hash = (hash ^ (hash >>> 16)) * 0x85ebca6b;
    hash = (hash ^ (hash >>> 13)) * 0xc2b2ae35;
    return hash ^ (hash >>> 16);
  }

}
