/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.beans;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public /* need it for class loading */ class FixedKeySet extends AbstractSet<String> {

  private final int size;
  private final Set<String> set;

  public FixedKeySet(String[] keys) {
    this.size = keys.length;
    HashSet<String> hashSet = new HashSet<>();
    Collections.addAll(hashSet, keys);
    this.set = Collections.unmodifiableSet(hashSet);
  }

  public Iterator<String> iterator() {
    return set.iterator();
  }

  public int size() {
    return size;
  }
}
