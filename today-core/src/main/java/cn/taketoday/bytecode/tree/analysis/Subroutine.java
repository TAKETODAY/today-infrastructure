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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.bytecode.tree.JumpInsnNode;
import cn.taketoday.bytecode.tree.LabelNode;

/**
 * A method subroutine (corresponds to a JSR instruction).
 *
 * @author Eric Bruneton
 */
final class Subroutine {

  /** The start of this subroutine. */
  public final LabelNode start;

  /**
   * The local variables that are read or written by this subroutine. The i-th element is true if
   * and only if the local variable at index i is read or written by this subroutine.
   */
  public final boolean[] localsUsed;

  /** The JSR instructions that jump to this subroutine. */
  public final List<JumpInsnNode> callers;

  /**
   * Constructs a new {@link Subroutine}.
   *
   * @param start the start of this subroutine.
   * @param maxLocals the local variables that are read or written by this subroutine.
   * @param caller a JSR instruction that jump to this subroutine.
   */
  Subroutine(final LabelNode start, final int maxLocals, final JumpInsnNode caller) {
    this.start = start;
    this.localsUsed = new boolean[maxLocals];
    this.callers = new ArrayList<>();
    callers.add(caller);
  }

  /**
   * Constructs a copy of the given {@link Subroutine}.
   *
   * @param subroutine the subroutine to copy.
   */
  Subroutine(final Subroutine subroutine) {
    this.start = subroutine.start;
    this.localsUsed = subroutine.localsUsed.clone();
    this.callers = new ArrayList<>(subroutine.callers);
  }

  /**
   * Merges the given subroutine into this subroutine. The local variables read or written by the
   * given subroutine are marked as read or written by this one, and the callers of the given
   * subroutine are added as callers of this one (if both have the same start).
   *
   * @param subroutine another subroutine. This subroutine is left unchanged by this method.
   * @return whether this subroutine has been modified by this method.
   */
  public boolean merge(final Subroutine subroutine) {
    boolean changed = false;
    boolean[] localsUsed = this.localsUsed;
    boolean[] subroutineLocalsUsed = subroutine.localsUsed;
    for (int i = 0; i < localsUsed.length; ++i) {
      if (subroutineLocalsUsed[i] && !localsUsed[i]) {
        localsUsed[i] = true;
        changed = true;
      }
    }
    if (subroutine.start == start) {
      List<JumpInsnNode> callers = this.callers;
      for (JumpInsnNode caller : subroutine.callers) {
        if (!callers.contains(caller)) {
          callers.add(caller);
          changed = true;
        }
      }
    }
    return changed;
  }
}
