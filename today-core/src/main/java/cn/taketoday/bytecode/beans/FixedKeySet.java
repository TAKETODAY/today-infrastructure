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
package cn.taketoday.bytecode.beans;

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
