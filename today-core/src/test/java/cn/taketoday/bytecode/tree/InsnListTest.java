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
package cn.taketoday.bytecode.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InsnList}.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public class InsnListTest {

  private final InsnNode insn1 = new InsnNode(0);
  private final InsnNode insn2 = new InsnNode(1);

  @Test
  public void testSize_emptyList() {
    assertEquals(0, newInsnList().size());
  }

  @Test
  public void testGetFirst_emptyList() {
    assertEquals(null, newInsnList().getFirst());
  }

  @Test
  public void testGetLast_emptyList() {
    assertEquals(null, newInsnList().getLast());
  }

  @Test
  public void testGet_outOfBounds() {
    Executable get = () -> newInsnList().get(0);

    assertThrows(IndexOutOfBoundsException.class, get);
  }

  @Test
  public void testContains() {
    assertFalse(newInsnList().contains(new InsnNode(0)));
  }

  @Test
  public void testIndexOf_noSuchElement() {
    InsnList insnList = newInsnList();

    Executable indexOf = () -> insnList.indexOf(new InsnNode(0));

    assertThrows(NoSuchElementException.class, indexOf);
  }

  @Test
  public void testIndexOf() {
    InsnList insnList = newInsnList(insn1, insn2);

    int index1 = insnList.indexOf(insn1);
    int index2 = insnList.indexOf(insn2);

    assertEquals(0, index1);
    assertEquals(1, index2);
  }

  @Test
  public void testAccept_cloneListVisitor() {
    InsnList insnList = newInsnList();
    insnList.add(new InsnNode(55));
    insnList.add(new InsnNode(77));
    InsnList dstInsnList = new InsnList();

    insnList.accept(
            new MethodVisitor() {
              @Override
              public void visitInsn(final int opcode) {
                dstInsnList.add(new InsnNode(opcode));
              }
            });

    assertEquals(2, dstInsnList.size());
    assertEquals(55, dstInsnList.get(0).opcode);
    assertEquals(77, dstInsnList.get(1).opcode);
  }

  @Test
  public void testIteratorNext_noSuchElement() {
    ListIterator<AbstractInsnNode> iterator = newInsnList().iterator();

    Executable next = () -> iterator.next();

    assertThrows(NoSuchElementException.class, next);
  }

  @Test
  public void testIteratorNext_nonEmptyList() {
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);

    AbstractInsnNode insn = iterator.next();

    assertEquals(insn2, insn);
    assertFalse(iterator.hasNext());
    assertEquals(2, iterator.nextIndex());
    assertTrue(iterator.hasPrevious());
    assertEquals(1, iterator.previousIndex());
  }

  @Test
  public void testIteratorPrevious_noSuchElement() {
    ListIterator<AbstractInsnNode> iterator = newInsnList().iterator();

    Executable previous = () -> iterator.previous();

    assertThrows(NoSuchElementException.class, previous);
  }

  @Test
  public void testIteratorPrevious_nonEmptyList() {
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);

    AbstractInsnNode insn = iterator.previous();

    assertEquals(insn1, insn);
    assertTrue(iterator.hasNext());
    assertEquals(0, iterator.nextIndex());
    assertFalse(iterator.hasPrevious());
    assertEquals(-1, iterator.previousIndex());
  }

  @Test
  public void testIteratorAdd_emptyList() {
    InsnNode insn = new InsnNode(0);
    InsnList insnList = newInsnList();

    insnList.iterator().add(insn);

    assertArrayEquals(new AbstractInsnNode[] { insn }, insnList.toArray());
  }

  @Test
  public void testIteratorAdd_firstInsn() {
    InsnNode insn = new InsnNode(0);
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);

    iterator.add(insn);

    assertArrayEquals(new AbstractInsnNode[] { insn1, insn, insn2 }, insnList.toArray());
  }

  @Test
  public void testIteratorRemove_illegalState() {
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);

    Executable remove = () -> iterator.remove();

    assertThrows(IllegalStateException.class, remove);
  }

  @Test
  public void testIteratorRemove_afterNext() {
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);
    iterator.next();

    iterator.remove();

    assertArrayEquals(new AbstractInsnNode[] { insn1 }, insnList.toArray());
  }

  @Test
  public void testIteratorRemove_afterPrevious() {
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);
    iterator.previous();

    iterator.remove();

    assertArrayEquals(new AbstractInsnNode[] { insn2 }, insnList.toArray());
  }

  @Test
  public void testIteratorAdd_lastInsn() {
    InsnNode insn = new InsnNode(0);
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(2);

    iterator.add(insn);

    assertArrayEquals(new AbstractInsnNode[] { insn1, insn2, insn }, insnList.toArray());
  }

  @Test
  public void testIteratorSet_illegalState() {
    InsnNode insn = new InsnNode(0);
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);

    Executable set = () -> iterator.set(insn);

    assertThrows(IllegalStateException.class, set);
  }

  @Test
  public void testIteratorSet_afterNext() {
    InsnNode insn = new InsnNode(0);
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);
    iterator.next();

    iterator.set(insn);

    assertArrayEquals(new AbstractInsnNode[] { insn1, insn }, insnList.toArray());
  }

  @Test
  public void testIteratorSet_afterPrevious() {
    InsnNode insn = new InsnNode(0);
    InsnList insnList = newInsnList(insn1, insn2);
    ListIterator<AbstractInsnNode> iterator = insnList.iterator(1);
    iterator.previous();

    iterator.set(insn);

    assertArrayEquals(new AbstractInsnNode[] { insn, insn2 }, insnList.toArray());
  }

  @Test
  public void testIterator_cacheIsNull() {
    InsnList insnList = newInsnList(insn1, insn2);
    insnList.iterator();
    assertNull(insnList.cache);
  }

  @Test
  public void testToArray_emptyList() {
    assertEquals(0, newInsnList().toArray().length);
  }

  @Test
  public void testToArray_nonEmptyList() {
    InsnList insnList = newInsnList(insn1, insn2);

    AbstractInsnNode[] insnArray = insnList.toArray();

    assertArrayEquals(new AbstractInsnNode[] { insn1, insn2 }, insnArray);
  }

  @Test
  public void testSet_noSuchElement() {
    InsnList insnList = newInsnList();

    Executable set = () -> insnList.set(new InsnNode(0), new InsnNode(0));

    assertThrows(NoSuchElementException.class, set);
  }

  @Test
  public void testSet_singleInsn() {
    InsnList insnList = newInsnList();
    insnList.add(insn1);
    AbstractInsnNode insn = new InsnNode(0);

    insnList.set(insn1, insn);

    assertEquals(1, insnList.size());
    assertEquals(insn, insnList.getFirst());
  }

  @Test
  public void testSet_firstInsn() {
    InsnList insnList = newInsnList(insn1, insn2);
    AbstractInsnNode insn = new InsnNode(0);

    insnList.set(insn1, insn);

    assertEquals(2, insnList.size());
    assertEquals(insn, insnList.getFirst());
  }

  @Test
  public void testSet_lastInsn() {
    InsnList insnList = newInsnList(insn1, insn2);
    AbstractInsnNode insn = new InsnNode(0);
    insnList.toArray();

    insnList.set(insn2, insn);

    assertEquals(2, insnList.size());
    assertEquals(insn, insnList.getLast());
  }

  @Test
  public void testAdd_illegalArgument() {
    InsnList insnList = newInsnList();
    newInsnList(insn1, insn2);

    Executable add = () -> insnList.add(insn1);

    assertThrows(IllegalArgumentException.class, add);
  }

  @Test
  public void testAdd_inEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);

    insnList.add(insn);

    assertEquals(1, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn, insnList.getLast());
    assertEquals(insn, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertEquals(0, insnList.indexOf(insn));
    assertArrayEquals(new AbstractInsnNode[] { insn }, insnList.toArray());
    assertEquals(null, insn.getPrevious());
    assertEquals(null, insn.getNext());
  }

  @Test
  public void testAdd_inNonEmptyList() {
    InsnList insnList = newInsnList();
    insnList.add(insn1);
    InsnNode insn = new InsnNode(0);

    insnList.add(insn);

    assertEquals(2, insnList.size());
    assertEquals(insn, insnList.getLast());
    assertEquals(1, insnList.indexOf(insn));
    assertEquals(insn, insnList.get(1));
    assertTrue(insnList.contains(insn));
  }

  @Test
  public void testAddList_illegalArgument() {
    InsnList insnList = newInsnList();

    Executable add = () -> insnList.add(insnList);

    assertThrows(IllegalArgumentException.class, add);
  }

  @Test
  public void testAddList_inEmptyList_emptyList() {
    InsnList insnList = newInsnList();

    insnList.add(newInsnList());

    assertEquals(0, insnList.size());
    assertEquals(null, insnList.getFirst());
    assertEquals(null, insnList.getLast());
    assertArrayEquals(new AbstractInsnNode[0], insnList.toArray());
  }

  @Test
  public void testAddList_inEmptyList_nonEmptyList() {
    InsnList insnList = newInsnList();

    insnList.add(newInsnList(insn1, insn2));

    assertEquals(2, insnList.size());
    assertEquals(insn1, insnList.getFirst());
    assertEquals(insn2, insnList.getLast());
    assertEquals(insn1, insnList.get(0));
    assertTrue(insnList.contains(insn1));
    assertTrue(insnList.contains(insn2));
    assertEquals(0, insnList.indexOf(insn1));
    assertEquals(1, insnList.indexOf(insn2));
    assertArrayEquals(new AbstractInsnNode[] { insn1, insn2 }, insnList.toArray());
  }

  @Test
  public void testAddList_inNonEmptyList_nonEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);

    insnList.add(newInsnList(insn1, insn2));

    assertEquals(3, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn2, insnList.getLast());
    assertEquals(insn, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertTrue(insnList.contains(insn1));
    assertTrue(insnList.contains(insn2));
    assertEquals(0, insnList.indexOf(insn));
    assertEquals(1, insnList.indexOf(insn1));
    assertEquals(2, insnList.indexOf(insn2));
    assertArrayEquals(new AbstractInsnNode[] { insn, insn1, insn2 }, insnList.toArray());
  }

  @Test
  public void testInsert_illegalArgument() {
    InsnList insnList = newInsnList();
    newInsnList(insn1, insn2);

    Executable insert = () -> insnList.insert(insn1);

    assertThrows(IllegalArgumentException.class, insert);
  }

  @Test
  public void testInsert_inEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);

    insnList.insert(insn);

    assertEquals(1, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn, insnList.getLast());
    assertEquals(insn, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertEquals(0, insnList.indexOf(insn));
    assertArrayEquals(new AbstractInsnNode[] { insn }, insnList.toArray());
  }

  @Test
  public void testInsert_inNonEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(new InsnNode(0));

    insnList.insert(insn);

    assertEquals(2, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertEquals(0, insnList.indexOf(insn));
  }

  @Test
  public void testInsertList_illegalArgument() {
    InsnList insnList = newInsnList();

    Executable insert = () -> insnList.insert(insnList);

    assertThrows(IllegalArgumentException.class, insert);
  }

  @Test
  public void testInsertList_inEmptyList_emptyList() {
    InsnList insnList = newInsnList();

    insnList.insert(newInsnList());

    assertEquals(0, insnList.size());
    assertEquals(null, insnList.getFirst());
    assertEquals(null, insnList.getLast());
    assertArrayEquals(new AbstractInsnNode[0], insnList.toArray());
  }

  @Test
  public void testInsertList_inEmptyList_nonEmptyList() {
    InsnList insnList = newInsnList();

    insnList.insert(newInsnList(insn1, insn2));

    assertEquals(2, insnList.size(), 2);
    assertEquals(insn1, insnList.getFirst());
    assertEquals(insn2, insnList.getLast());
    assertEquals(insn1, insnList.get(0));
    assertTrue(insnList.contains(insn1));
    assertTrue(insnList.contains(insn2));
    assertEquals(0, insnList.indexOf(insn1));
    assertEquals(1, insnList.indexOf(insn2));
    assertArrayEquals(new AbstractInsnNode[] { insn1, insn2 }, insnList.toArray());
  }

  @Test
  public void testInsertList_inNonEmptyList_nonEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);

    insnList.insert(newInsnList(insn1, insn2));

    assertEquals(3, insnList.size());
    assertEquals(insn1, insnList.getFirst());
    assertEquals(insn, insnList.getLast());
    assertEquals(insn1, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertTrue(insnList.contains(insn1));
    assertTrue(insnList.contains(insn2));
    assertEquals(0, insnList.indexOf(insn1));
    assertEquals(1, insnList.indexOf(insn2));
    assertEquals(2, insnList.indexOf(insn));
    assertArrayEquals(new AbstractInsnNode[] { insn1, insn2, insn }, insnList.toArray());
  }

  @Test
  public void testInsertAfter_noSuchElement() {
    InsnList insnList = newInsnList(insn1, insn2);

    Executable insert = () -> insnList.insert(new InsnNode(0), new InsnNode(0));

    assertThrows(NoSuchElementException.class, insert);
  }

  @Test
  public void testInsertAfter_lastInsn() {
    InsnList insnList = newInsnList(insn1, insn2);
    InsnNode insn = new InsnNode(0);

    insnList.insert(insn2, insn);

    assertEquals(3, insnList.size());
    assertEquals(insn1, insnList.getFirst());
    assertEquals(insn, insnList.getLast());
    assertEquals(insn1, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertEquals(2, insnList.indexOf(insn));
    assertArrayEquals(new AbstractInsnNode[] { insn1, insn2, insn }, insnList.toArray());
  }

  @Test
  public void testInsertAfter_notLastInsn() {
    InsnList insnList = newInsnList(insn1, insn2);
    InsnNode insn = new InsnNode(0);

    insnList.insert(insn1, insn);

    assertEquals(3, insnList.size());
    assertEquals(insn1, insnList.getFirst());
    assertEquals(insn2, insnList.getLast());
    assertEquals(insn1, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertEquals(1, insnList.indexOf(insn));
    assertArrayEquals(new AbstractInsnNode[] { insn1, insn, insn2 }, insnList.toArray());
  }

  @Test
  public void testInsertListAfter_noSuchElement() {
    InsnList insnList = newInsnList(insn1, insn2);

    Executable insert = () -> insnList.insert(new InsnNode(0), newInsnList());

    assertThrows(NoSuchElementException.class, insert);
  }

  @Test
  public void testInsertListAfter_lastInsn_emptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);

    insnList.insert(insn, newInsnList());

    assertEquals(1, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn, insnList.getLast());
    assertArrayEquals(new AbstractInsnNode[] { insn }, insnList.toArray());
  }

  @Test
  public void testInsertListAfter_lastInsn_nonEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);

    insnList.insert(insn, newInsnList(insn1, insn2));

    assertEquals(3, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn2, insnList.getLast());
    assertEquals(insn, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertTrue(insnList.contains(insn1));
    assertTrue(insnList.contains(insn2));
    assertEquals(0, insnList.indexOf(insn));
    assertEquals(1, insnList.indexOf(insn1));
    assertEquals(2, insnList.indexOf(insn2));
    assertArrayEquals(new AbstractInsnNode[] { insn, insn1, insn2 }, insnList.toArray());
  }

  @Test
  public void testInsertListAfter_notLastInsn_nonEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);
    insnList.add(new InsnNode(0));

    insnList.insert(insn, newInsnList(insn1, insn2));

    assertEquals(4, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertTrue(insnList.contains(insn1));
    assertTrue(insnList.contains(insn2));
    assertEquals(0, insnList.indexOf(insn));
    assertEquals(1, insnList.indexOf(insn1));
    assertEquals(2, insnList.indexOf(insn2));
  }

  @Test
  public void testInsertBefore_noSuchElement() {
    InsnList insnList = newInsnList(insn1, insn2);

    Executable insertBefore = () -> insnList.insertBefore(new InsnNode(0), new InsnNode(0));

    assertThrows(NoSuchElementException.class, insertBefore);
  }

  @Test
  public void testInsertBefore_firstInsn() {
    InsnList insnList = newInsnList(insn1, insn2);
    InsnNode insn = new InsnNode(0);

    insnList.insertBefore(insn1, insn);

    assertEquals(3, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn2, insnList.getLast());
    assertEquals(insn, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertEquals(0, insnList.indexOf(insn));
    assertArrayEquals(new AbstractInsnNode[] { insn, insn1, insn2 }, insnList.toArray());
  }

  @Test
  public void testInsertBefore_notFirstInsn() {
    InsnList insnList = newInsnList(insn1, insn2);
    InsnNode insn = new InsnNode(0);

    insnList.insertBefore(insn2, insn);

    assertEquals(3, insnList.size());
    assertEquals(insn1, insnList.getFirst());
    assertEquals(insn2, insnList.getLast());
    assertEquals(insn, insnList.get(1));
    assertTrue(insnList.contains(insn));
    assertEquals(1, insnList.indexOf(insn));
    assertArrayEquals(new AbstractInsnNode[] { insn1, insn, insn2 }, insnList.toArray());
  }

  @Test
  public void testInsertListBefore_noSuchElement() {
    InsnList insnList = newInsnList(insn1, insn2);

    Executable insertBefore = () -> insnList.insertBefore(new InsnNode(0), newInsnList());

    assertThrows(NoSuchElementException.class, insertBefore);
  }

  @Test
  public void testInsertListBefore_firstInsn_emptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);

    insnList.insertBefore(insn, newInsnList());

    assertEquals(1, insnList.size());
    assertEquals(insn, insnList.getFirst());
    assertEquals(insn, insnList.getLast());
    assertArrayEquals(new AbstractInsnNode[] { insn }, insnList.toArray());
  }

  @Test
  public void testInsertListBefore_firstInsn_nonEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);

    insnList.insertBefore(insn, newInsnList(insn1, insn2));

    assertEquals(3, insnList.size());
    assertEquals(insn1, insnList.getFirst());
    assertEquals(insn, insnList.getLast());
    assertEquals(insn1, insnList.get(0));
    assertTrue(insnList.contains(insn));
    assertTrue(insnList.contains(insn1));
    assertTrue(insnList.contains(insn2));
    assertEquals(2, insnList.indexOf(insn));
    assertEquals(0, insnList.indexOf(insn1));
    assertEquals(1, insnList.indexOf(insn2));
    assertArrayEquals(new AbstractInsnNode[] { insn1, insn2, insn }, insnList.toArray());
  }

  @Test
  public void testInsertListBefore_notFirstInsn_nonEmptyList() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(new InsnNode(0));
    insnList.add(insn);

    insnList.insertBefore(insn, newInsnList(insn1, insn2));

    assertEquals(4, insnList.size());
    assertEquals(insn1, insnList.get(1));
    assertEquals(insn2, insnList.get(2));
    assertTrue(insnList.contains(insn));
    assertTrue(insnList.contains(insn1));
    assertTrue(insnList.contains(insn2));
    assertEquals(3, insnList.indexOf(insn));
    assertEquals(1, insnList.indexOf(insn1));
    assertEquals(2, insnList.indexOf(insn2));
  }

  @Test
  public void testRemove_noSuchElement() {
    InsnList insnList = newInsnList(insn1, insn2);

    Executable remove = () -> insnList.remove(new InsnNode(0));

    assertThrows(NoSuchElementException.class, remove);
  }

  @Test
  public void testRemove_singleInsn() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);

    insnList.remove(insn);

    assertEquals(0, insnList.size());
    assertEquals(null, insnList.getFirst());
    assertEquals(null, insnList.getLast());
    assertFalse(insnList.contains(insn));
    assertArrayEquals(new AbstractInsnNode[0], insnList.toArray());
    assertEquals(null, insn.getPrevious());
    assertEquals(null, insn.getNext());
  }

  @Test
  public void testRemove_firstInsn() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(insn);
    insnList.add(new InsnNode(0));

    insnList.remove(insn);

    assertFalse(insnList.contains(insn));
    assertEquals(null, insn.getPrevious());
    assertEquals(null, insn.getNext());
  }

  @Test
  public void testRemove_middleInsn() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(new InsnNode(0));
    insnList.add(insn);
    insnList.add(new InsnNode(0));

    insnList.remove(insn);

    assertFalse(insnList.contains(insn));
    assertEquals(null, insn.getPrevious());
    assertEquals(null, insn.getNext());
  }

  @Test
  public void testRemove_lastInsn() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(new InsnNode(0));
    insnList.add(insn);

    insnList.remove(insn);

    assertFalse(insnList.contains(insn));
    assertEquals(null, insn.getPrevious());
    assertEquals(null, insn.getNext());
  }

  @Test
  public void testClear() {
    InsnList insnList = newInsnList();
    InsnNode insn = new InsnNode(0);
    insnList.add(new InsnNode(0));
    insnList.add(insn);
    insnList.add(new InsnNode(0));

    insnList.clear();

    assertEquals(0, insnList.size());
    assertEquals(null, insnList.getFirst());
    assertEquals(null, insnList.getLast());
    assertFalse(insnList.contains(insn));
    assertArrayEquals(new AbstractInsnNode[0], insnList.toArray());
    assertEquals(null, insn.getPrevious());
    assertEquals(null, insn.getNext());
  }

  @Test
  public void testResetLabels() {
    InsnList insnList = newInsnList();
    LabelNode labelNode = new LabelNode();
    insnList.add(new InsnNode(55));
    insnList.add(labelNode);
    insnList.add(new InsnNode(77));
    Label label = labelNode.getLabel();

    insnList.resetLabels();

    assertNotNull(label);
    assertNotSame(label, labelNode.getLabel());
  }

  private static InsnList newInsnList() {
    return new CheckedInsnList();
  }

  private static InsnList newInsnList(final InsnNode insnNode1, final InsnNode insnNode2) {
    InsnList insnList = new CheckedInsnList();
    insnList.add(insnNode1);
    insnList.add(insnNode2);
    return insnList;
  }

  /** An {@link InsnList} which checks that its methods are properly used. */
  static class CheckedInsnList extends InsnList {

    @Override
    public int indexOf(final AbstractInsnNode insnNode) {
      if (!contains(insnNode)) {
        throw new NoSuchElementException();
      }
      return super.indexOf(insnNode);
    }

    @Override
    public void set(final AbstractInsnNode oldInsnNode, final AbstractInsnNode newInsnNode) {
      if (!contains(oldInsnNode)) {
        throw new NoSuchElementException();
      }
      if (newInsnNode.index != -1) {
        throw new IllegalArgumentException();
      }
      super.set(oldInsnNode, newInsnNode);
    }

    @Override
    public void add(final AbstractInsnNode insnNode) {
      if (insnNode.index != -1) {
        throw new IllegalArgumentException();
      }
      super.add(insnNode);
    }

    @Override
    public void add(final InsnList insnList) {
      if (insnList == this) {
        throw new IllegalArgumentException();
      }
      super.add(insnList);
    }

    @Override
    public void insert(final AbstractInsnNode insnNode) {
      if (insnNode.index != -1) {
        throw new IllegalArgumentException();
      }
      super.insert(insnNode);
    }

    @Override
    public void insert(final InsnList insnList) {
      if (insnList == this) {
        throw new IllegalArgumentException();
      }
      super.insert(insnList);
    }

    @Override
    public void insert(final AbstractInsnNode previousInsn, final AbstractInsnNode insnNode) {
      if (!contains(previousInsn)) {
        throw new NoSuchElementException();
      }
      if (insnNode.index != -1) {
        throw new IllegalArgumentException();
      }
      super.insert(previousInsn, insnNode);
    }

    @Override
    public void insert(final AbstractInsnNode previousInsn, final InsnList insnList) {
      if (!contains(previousInsn)) {
        throw new NoSuchElementException();
      }
      if (insnList == this) {
        throw new IllegalArgumentException();
      }
      super.insert(previousInsn, insnList);
    }

    @Override
    public void insertBefore(final AbstractInsnNode nextInsn, final AbstractInsnNode insnNode) {
      if (!contains(nextInsn)) {
        throw new NoSuchElementException();
      }
      if (insnNode.index != -1) {
        throw new IllegalArgumentException();
      }
      super.insertBefore(nextInsn, insnNode);
    }

    @Override
    public void insertBefore(final AbstractInsnNode nextInsn, final InsnList insnList) {
      if (!contains(nextInsn)) {
        throw new NoSuchElementException();
      }
      if (insnList == this) {
        throw new IllegalArgumentException();
      }
      super.insertBefore(nextInsn, insnList);
    }

    @Override
    public void remove(final AbstractInsnNode insnNode) {
      if (!contains(insnNode)) {
        throw new NoSuchElementException();
      }
      super.remove(insnNode);
    }

    @Override
    public void clear() {
      removeAll(true);
      super.clear();
    }
  }
}
