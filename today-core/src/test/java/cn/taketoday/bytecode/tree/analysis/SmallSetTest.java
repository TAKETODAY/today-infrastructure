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
package cn.taketoday.bytecode.tree.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SmallSet}.
 *
 * @author Eric Bruneton
 */
public class SmallSetTest {

  private static final Object ELEMENT1 = new Object();
  private static final Object ELEMENT2 = new Object();
  private static final Object ELEMENT3 = new Object();
  private static final Object ELEMENT4 = new Object();

  @Test
  public void testUnion_oneElement_emptySet() {
    SmallSet<Object> set1 = new SmallSet<>(ELEMENT1);
    SmallSet<Object> set2 = new SmallSet<>();

    Set<Object> union1 = set1.union(set2);
    Set<Object> union2 = set2.union(set1);

    assertEquals(set1, union1);
    assertEquals(set1, union2);
  }

  @Test
  public void testUnion_oneElement_oneElement() {
    SmallSet<Object> set1 = new SmallSet<>(ELEMENT1);
    SmallSet<Object> set2 = new SmallSet<>(ELEMENT1);

    Set<Object> union1 = set1.union(set2);
    Set<Object> union2 = set2.union(set1);

    assertEquals(union1, union2);
    assertEquals(union1, new HashSet<Object>(Arrays.asList(ELEMENT1)));
  }

  @Test
  public void testUnion_oneElement_twoElements_superSet() {
    SmallSet<Object> set1 = newSmallSet(ELEMENT1, ELEMENT2);
    SmallSet<Object> set2 = new SmallSet<>(ELEMENT1);

    Set<Object> union1 = set1.union(set2);
    Set<Object> union2 = set2.union(set1);

    assertEquals(union1, union2);
    assertEquals(union1, new HashSet<Object>(Arrays.asList(ELEMENT1, ELEMENT2)));
  }

  @Test
  public void testUnion_twoElements_twoElements_equalSets() {
    SmallSet<Object> set1 = newSmallSet(ELEMENT1, ELEMENT2);
    SmallSet<Object> set2 = newSmallSet(ELEMENT2, ELEMENT1);

    Set<Object> union1 = set1.union(set2);
    Set<Object> union2 = set2.union(set1);

    assertEquals(union1, union2);
    assertEquals(union1, new HashSet<Object>(Arrays.asList(ELEMENT1, ELEMENT2)));
  }

  @Test
  public void testUnion_twoElements_oneElement_distinctSets() {
    SmallSet<Object> set1 = newSmallSet(ELEMENT1, ELEMENT2);
    SmallSet<Object> set2 = new SmallSet<>(ELEMENT3);

    Set<Object> union1 = set1.union(set2);
    Set<Object> union2 = set2.union(set1);

    assertEquals(union1, union2);
    assertEquals(union1, new HashSet<Object>(Arrays.asList(ELEMENT1, ELEMENT2, ELEMENT3)));
  }

  @Test
  public void testUnion_twoElements_twoElements_distincSets() {
    SmallSet<Object> set1 = newSmallSet(ELEMENT1, ELEMENT2);
    SmallSet<Object> set2 = newSmallSet(ELEMENT3, ELEMENT4);

    Set<Object> union1 = set1.union(set2);
    Set<Object> union2 = set2.union(set1);

    assertEquals(union1, union2);
    assertEquals(
            union1, new HashSet<Object>(Arrays.asList(ELEMENT1, ELEMENT2, ELEMENT3, ELEMENT4)));
  }

  @Test
  public void testIterator_next_firstElement() {
    Iterator<Object> iterator = newSmallSet(ELEMENT1, ELEMENT2).iterator();

    Object element = iterator.next();

    assertEquals(ELEMENT1, element);
    assertTrue(iterator.hasNext());
  }

  @Test
  public void testIterator_next_secondElement() {
    Iterator<Object> iterator = newSmallSet(ELEMENT1, ELEMENT2).iterator();
    iterator.next();

    Object element = iterator.next();

    assertEquals(ELEMENT2, element);
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testIterator_next_noSuchElement() {
    Iterator<Object> iterator = newSmallSet(ELEMENT1, ELEMENT2).iterator();
    iterator.next();
    iterator.next();

    Executable next = () -> iterator.next();

    assertThrows(NoSuchElementException.class, next);
  }

  @Test
  public void testIterator_remove() {
    Iterator<Object> iterator = newSmallSet(ELEMENT1, ELEMENT2).iterator();
    iterator.next();

    assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
  }

  private static SmallSet<Object> newSmallSet(final Object element1, final Object element2) {
    return (SmallSet<Object>) new SmallSet<Object>(element1).union(new SmallSet<Object>(element2));
  }
}
