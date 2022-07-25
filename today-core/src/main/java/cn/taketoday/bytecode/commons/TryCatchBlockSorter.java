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

package cn.taketoday.bytecode.commons;

import java.util.Comparator;
import java.util.List;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.tree.TryCatchBlockNode;

/**
 * A {@link MethodVisitor} adapter to sort the exception handlers. The handlers are sorted in a
 * method innermost-to-outermost. This allows the programmer to add handlers without worrying about
 * ordering them correctly with respect to existing, in-code handlers.
 *
 * <p>Behavior is only defined for properly-nested handlers. If any "try" blocks overlap (something
 * that isn't possible in Java code) then this may not do what you want. In fact, this adapter just
 * sorts by the length of the "try" block, taking advantage of the fact that a given try block must
 * be larger than any block it contains).
 *
 * @author Adrian Sampson
 */
public class TryCatchBlockSorter extends MethodNode {

  /**
   * Constructs a new {@link TryCatchBlockSorter}.
   *
   * @param methodVisitor the method visitor to which this visitor must delegate method calls. May
   * be {@literal null}.
   * @param access the method's access flags (see {@link Opcodes}). This parameter also indicates if
   * the method is synthetic and/or deprecated.
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param signature the method's signature. May be {@literal null} if the method parameters,
   * return type and exceptions do not use generic types.
   * @param exceptions the internal names of the method's exception classes (see {@link
   * Type#getInternalName()}). May be {@literal null}.
   */
  public TryCatchBlockSorter(
          final MethodVisitor methodVisitor,
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final String[] exceptions) {
    super(access, name, descriptor, signature, exceptions);
    this.mv = methodVisitor;
  }

  @Override
  public void visitEnd() {
    // Sort the TryCatchBlockNode elements by the length of their "try" block.
    final List<TryCatchBlockNode> tryCatchBlocks = this.tryCatchBlocks;
    if (tryCatchBlocks != null) {
      tryCatchBlocks.sort(new Comparator<>() {
        @Override
        public int compare(
                final TryCatchBlockNode tryCatchBlockNode1,
                final TryCatchBlockNode tryCatchBlockNode2) {
          return blockLength(tryCatchBlockNode1) - blockLength(tryCatchBlockNode2);
        }

        private int blockLength(final TryCatchBlockNode tryCatchBlockNode) {
          int endIndex = instructions.indexOf(tryCatchBlockNode.end);
          int startIndex = instructions.indexOf(tryCatchBlockNode.start);
          return endIndex - startIndex;
        }
      });

      // Update the 'target' of each try catch block annotation.
      final int size = tryCatchBlocks.size();
      for (int i = 0; i < size; ++i) {
        tryCatchBlocks.get(i).updateIndex(i);
      }
    }
    if (mv != null) {
      accept(mv);
    }
  }
}
