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

package cn.taketoday.util;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable container for a key and a value, suitable for use
 * in creating and populating {@code Map} instances.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/26 23:28
 */
public final class KeyValueHolder<K, V> implements Map.Entry<K, V> {
  private final K key;

  private V value;

  public KeyValueHolder(K k, V v) {
    key = k;
    value = v;
  }

  /**
   * Gets the key from this holder.
   *
   * @return the key
   */
  @Override
  public K getKey() {
    return key;
  }

  /**
   * Gets the value from this holder.
   *
   * @return the value
   */
  @Override
  public V getValue() {
    return value;
  }

  /**
   * Replaces the value corresponding to this entry with the specified
   * value (optional operation).  (Writes through to the map.)  The
   * behavior of this call is undefined if the mapping has already been
   * removed from the map (by the iterator's {@code remove} operation).
   *
   * @param value new value to be stored in this entry
   * @return old value corresponding to the entry
   * @throws ClassCastException if the class of the specified value
   * prevents it from being stored in the backing map
   */
  @Override
  public V setValue(V value) {
    V old = this.value;
    this.value = value;
    return old;
  }

  /**
   * Compares the specified object with this entry for equality.
   * Returns {@code true} if the given object is also a map entry and
   * the two entries' keys and values are equal. Note that key and
   * value are non-null, so equals() can be called safely on them.
   */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    return o instanceof Map.Entry<?, ?> e
            && Objects.equals(key, e.getKey())
            && Objects.equals(value, e.getValue());
  }

  /**
   * Returns the hash code value for this map entry. The hash code
   * is {@code key.hashCode() ^ value.hashCode()}. Note that key and
   * value are non-null, so hashCode() can be called safely on them.
   */
  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  /**
   * Returns a String representation of this map entry.  This
   * implementation returns the string representation of this
   * entry's key followed by the equals character ("{@code =}")
   * followed by the string representation of this entry's value.
   *
   * @return a String representation of this map entry
   */
  @Override
  public String toString() {
    return key + "=" + value;
  }

}
