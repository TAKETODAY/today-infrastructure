/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.app.loader.zip;

import java.util.BitSet;

/**
 * Tracks entries that have a name that should be offset by a specific amount. This class
 * is used with nested directory zip files so that entries under the directory are offset
 * correctly. META-INF entries are copied directly and have no offset.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class NameOffsetLookups {

  public static final NameOffsetLookups NONE = new NameOffsetLookups(0, 0);

  private final int offset;

  private final BitSet enabled;

  NameOffsetLookups(int offset, int size) {
    this.offset = offset;
    this.enabled = (size != 0) ? new BitSet(size) : null;
  }

  void swap(int i, int j) {
    if (this.enabled != null) {
      boolean temp = this.enabled.get(i);
      this.enabled.set(i, this.enabled.get(j));
      this.enabled.set(j, temp);
    }
  }

  int get(int index) {
    return isEnabled(index) ? this.offset : 0;
  }

  int enable(int index, boolean enable) {
    if (this.enabled != null) {
      this.enabled.set(index, enable);
    }
    return (!enable) ? 0 : this.offset;
  }

  boolean isEnabled(int index) {
    return (this.enabled != null && this.enabled.get(index));
  }

  boolean hasAnyEnabled() {
    return this.enabled != null && this.enabled.cardinality() > 0;
  }

  NameOffsetLookups emptyCopy() {
    return new NameOffsetLookups(this.offset, this.enabled.size());
  }

}
