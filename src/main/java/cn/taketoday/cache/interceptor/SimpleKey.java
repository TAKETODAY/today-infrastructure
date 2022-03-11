/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.cache.interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * A simple key as returned from the {@link SimpleKeyGenerator}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
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
  public SimpleKey(Object... elements) {
    Assert.notNull(elements, "Elements must not be null");
    this.params = elements.clone();
    // Pre-calculate hashCode field
    this.hashCode = Arrays.deepHashCode(this.params);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other ||
            (other instanceof SimpleKey && Arrays.deepEquals(this.params, ((SimpleKey) other).params)));
  }

  @Override
  public final int hashCode() {
    // Expose pre-calculated hashCode field
    return this.hashCode;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
  }

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    // Re-calculate hashCode field on deserialization
    this.hashCode = Arrays.deepHashCode(this.params);
  }

}
