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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;

/**
 * A node that represents a stack map frame. These nodes are pseudo instruction nodes in order to be
 * inserted in an instruction list. In fact these nodes must(*) be inserted <i>just before</i> any
 * instruction node <b>i</b> that follows an unconditionnal branch instruction such as GOTO or
 * THROW, that is the target of a jump instruction, or that starts an exception handler block. The
 * stack map frame types must describe the values of the local variables and of the operand stack
 * elements <i>just before</i> <b>i</b> is executed. <br>
 * <br>
 * (*) this is mandatory only for classes whose version is greater than or equal to {@link
 * Opcodes#V1_6}.
 *
 * @author Eric Bruneton
 */
public class FrameNode extends AbstractInsnNode {

  /**
   * The type of this frame. Must be {@link Opcodes#F_NEW} for expanded frames, or {@link
   * Opcodes#F_FULL}, {@link Opcodes#F_APPEND}, {@link Opcodes#F_CHOP}, {@link Opcodes#F_SAME} or
   * {@link Opcodes#F_APPEND}, {@link Opcodes#F_SAME1} for compressed frames.
   */
  public int type;

  /**
   * The types of the local variables of this stack map frame. Elements of this list can be Integer,
   * String or LabelNode objects (for primitive, reference and uninitialized types respectively -
   * see {@link MethodVisitor}).
   */
  public List<Object> local;

  /**
   * The types of the operand stack elements of this stack map frame. Elements of this list can be
   * Integer, String or LabelNode objects (for primitive, reference and uninitialized types
   * respectively - see {@link MethodVisitor}).
   */
  public List<Object> stack;

  private FrameNode() {
    super(-1);
  }

  /**
   * Constructs a new {@link FrameNode}.
   *
   * @param type the type of this frame. Must be {@link Opcodes#F_NEW} for expanded frames, or
   * {@link Opcodes#F_FULL}, {@link Opcodes#F_APPEND}, {@link Opcodes#F_CHOP}, {@link
   * Opcodes#F_SAME} or {@link Opcodes#F_APPEND}, {@link Opcodes#F_SAME1} for compressed frames.
   * @param numLocal number of local variables of this stack map frame.
   * @param local the types of the local variables of this stack map frame. Elements of this list
   * can be Integer, String or LabelNode objects (for primitive, reference and uninitialized
   * types respectively - see {@link MethodVisitor}).
   * @param numStack number of operand stack elements of this stack map frame.
   * @param stack the types of the operand stack elements of this stack map frame. Elements of this
   * list can be Integer, String or LabelNode objects (for primitive, reference and
   * uninitialized types respectively - see {@link MethodVisitor}).
   */
  public FrameNode(
          final int type,
          final int numLocal,
          final Object[] local,
          final int numStack,
          final Object[] stack) {
    super(-1);
    this.type = type;
    switch (type) {
      case Opcodes.F_NEW:
      case Opcodes.F_FULL:
        this.local = Util.asArrayList(numLocal, local);
        this.stack = Util.asArrayList(numStack, stack);
        break;
      case Opcodes.F_APPEND:
        this.local = Util.asArrayList(numLocal, local);
        break;
      case Opcodes.F_CHOP:
        this.local = Util.asArrayList(numLocal);
        break;
      case Opcodes.F_SAME:
        break;
      case Opcodes.F_SAME1:
        this.stack = Util.asArrayList(1, stack);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public int getType() {
    return FRAME;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    switch (type) {
      case Opcodes.F_APPEND -> methodVisitor.visitFrame(type, local.size(), asArray(local), 0, null);
      case Opcodes.F_CHOP -> methodVisitor.visitFrame(type, local.size(), null, 0, null);
      case Opcodes.F_SAME -> methodVisitor.visitFrame(type, 0, null, 0, null);
      case Opcodes.F_SAME1 -> methodVisitor.visitFrame(type, 0, null, 1, asArray(stack));
      case Opcodes.F_NEW, Opcodes.F_FULL -> methodVisitor.visitFrame(type, local.size(), asArray(local), stack.size(), asArray(stack));
      default -> throw new IllegalArgumentException();
    }
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    FrameNode clone = new FrameNode();
    clone.type = type;
    List<Object> local = this.local;
    if (local != null) {
      ArrayList<Object> cloneLocal = new ArrayList<>();
      clone.local = cloneLocal;
      for (Object localElement : local) {
        if (localElement instanceof LabelNode) {
          localElement = clonedLabels.get(localElement);
        }
        cloneLocal.add(localElement);
      }
    }
    List<Object> stack = this.stack;
    if (stack != null) {
      ArrayList<Object> cloneStack = new ArrayList<>();
      clone.stack = cloneStack;
      for (Object stackElement : stack) {
        if (stackElement instanceof LabelNode) {
          stackElement = clonedLabels.get(stackElement);
        }
        cloneStack.add(stackElement);
      }
    }
    return clone;
  }

  private static Object[] asArray(final List<Object> list) {
    Object[] array = new Object[list.size()];
    for (int i = 0, n = array.length; i < n; ++i) {
      Object o = list.get(i);
      if (o instanceof LabelNode) {
        o = ((LabelNode) o).getLabel();
      }
      array[i] = o;
    }
    return array;
  }
}
