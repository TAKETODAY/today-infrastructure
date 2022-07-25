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

import java.util.Set;

import cn.taketoday.bytecode.tree.AbstractInsnNode;

/**
 * A {@link Value} which keeps track of the bytecode instructions that can produce it.
 *
 * @author Eric Bruneton
 */
public class SourceValue implements Value {

  /**
   * The size of this value, in 32 bits words. This size is 1 for byte, boolean, char, short, int,
   * float, object and array types, and 2 for long and double.
   */
  public final int size;

  /**
   * The instructions that can produce this value. For example, for the Java code below, the
   * instructions that can produce the value of {@code i} at line 5 are the two ISTORE instructions
   * at line 1 and 3:
   *
   * <pre>
   * 1: i = 0;
   * 2: if (...) {
   * 3:   i = 1;
   * 4: }
   * 5: return i;
   * </pre>
   */
  public final Set<AbstractInsnNode> insns;

  /**
   * Constructs a new {@link SourceValue}.
   *
   * @param size the size of this value, in 32 bits words. This size is 1 for byte, boolean, char,
   * short, int, float, object and array types, and 2 for long and double.
   */
  public SourceValue(final int size) {
    this(size, new SmallSet<AbstractInsnNode>());
  }

  /**
   * Constructs a new {@link SourceValue}.
   *
   * @param size the size of this value, in 32 bits words. This size is 1 for byte, boolean, char,
   * short, int, float, object and array types, and 2 for long and double.
   * @param insnNode an instruction that can produce this value.
   */
  public SourceValue(final int size, final AbstractInsnNode insnNode) {
    this.size = size;
    this.insns = new SmallSet<>(insnNode);
  }

  /**
   * Constructs a new {@link SourceValue}.
   *
   * @param size the size of this value, in 32 bits words. This size is 1 for byte, boolean, char,
   * short, int, float, object and array types, and 2 for long and double.
   * @param insnSet the instructions that can produce this value.
   */
  public SourceValue(final int size, final Set<AbstractInsnNode> insnSet) {
    this.size = size;
    this.insns = insnSet;
  }

  /**
   * Returns the size of this value.
   *
   * @return the size of this value, in 32 bits words. This size is 1 for byte, boolean, char,
   * short, int, float, object and array types, and 2 for long and double.
   */
  @Override
  public int getSize() {
    return size;
  }

  @Override
  public boolean equals(final Object value) {
    if (!(value instanceof SourceValue sourceValue)) {
      return false;
    }
    return size == sourceValue.size && insns.equals(sourceValue.insns);
  }

  @Override
  public int hashCode() {
    return insns.hashCode();
  }
}
