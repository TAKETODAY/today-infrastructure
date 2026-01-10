/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.
package infra.bytecode.core;

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
