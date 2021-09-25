// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package cn.taketoday.core.bytecode.tree.analysis;

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
