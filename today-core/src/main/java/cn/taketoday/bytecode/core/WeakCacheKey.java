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
package cn.taketoday.bytecode.core;

import java.lang.ref.WeakReference;

/**
 * Allows to check for object equality, yet the class does not keep strong
 * reference to the target. {@link #equals(Object)} returns true if and only if
 * the reference is not yet expired and target objects are equal in terms of
 * {@link #equals(Object)}.
 * <p>
 * This an internal class, thus it might disappear in future cglib releases.
 *
 * @param <T> type of the reference
 */
@SuppressWarnings({ "rawtypes" })
public class WeakCacheKey<T> extends WeakReference<T> {
  private final int hash;

  public WeakCacheKey(T referent) {
    super(referent);
    this.hash = referent.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WeakCacheKey)) {
      return false;
    }
    Object ours = get();
    Object theirs = ((WeakCacheKey) obj).get();
    return ours != null && ours.equals(theirs);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    T t = get();
    return t == null ? "Clean WeakIdentityKey, hash: " + hash : t.toString();
  }
}
