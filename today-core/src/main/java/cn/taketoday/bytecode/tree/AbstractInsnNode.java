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

/**
 * A node that represents a bytecode instruction. <i>An instruction can appear at most once in at
 * most one {@link InsnList} at a time</i>.
 *
 * @author Eric Bruneton
 */
public abstract class AbstractInsnNode {

  /** The type of {@link InsnNode} instructions. */
  public static final int INSN = 0;

  /** The type of {@link IntInsnNode} instructions. */
  public static final int INT_INSN = 1;

  /** The type of {@link VarInsnNode} instructions. */
  public static final int VAR_INSN = 2;

  /** The type of {@link TypeInsnNode} instructions. */
  public static final int TYPE_INSN = 3;

  /** The type of {@link FieldInsnNode} instructions. */
  public static final int FIELD_INSN = 4;

  /** The type of {@link MethodInsnNode} instructions. */
  public static final int METHOD_INSN = 5;

  /** The type of {@link InvokeDynamicInsnNode} instructions. */
  public static final int INVOKE_DYNAMIC_INSN = 6;

  /** The type of {@link JumpInsnNode} instructions. */
  public static final int JUMP_INSN = 7;

  /** The type of {@link LabelNode} "instructions". */
  public static final int LABEL = 8;

  /** The type of {@link LdcInsnNode} instructions. */
  public static final int LDC_INSN = 9;

  /** The type of {@link IincInsnNode} instructions. */
  public static final int IINC_INSN = 10;

  /** The type of {@link TableSwitchInsnNode} instructions. */
  public static final int TABLESWITCH_INSN = 11;

  /** The type of {@link LookupSwitchInsnNode} instructions. */
  public static final int LOOKUPSWITCH_INSN = 12;

  /** The type of {@link MultiANewArrayInsnNode} instructions. */
  public static final int MULTIANEWARRAY_INSN = 13;

  /** The type of {@link FrameNode} "instructions". */
  public static final int FRAME = 14;

  /** The type of {@link LineNumberNode} "instructions". */
  public static final int LINE = 15;

  /** The opcode of this instruction. */
  protected int opcode;

  /**
   * The runtime visible type annotations of this instruction. This field is only used for real
   * instructions (i.e. not for labels, frames, or line number nodes). This list is a list of {@link
   * TypeAnnotationNode} objects. May be {@literal null}.
   */
  public List<TypeAnnotationNode> visibleTypeAnnotations;

  /**
   * The runtime invisible type annotations of this instruction. This field is only used for real
   * instructions (i.e. not for labels, frames, or line number nodes). This list is a list of {@link
   * TypeAnnotationNode} objects. May be {@literal null}.
   */
  public List<TypeAnnotationNode> invisibleTypeAnnotations;

  /** The previous instruction in the list to which this instruction belongs. */
  AbstractInsnNode previousInsn;

  /** The next instruction in the list to which this instruction belongs. */
  AbstractInsnNode nextInsn;

  /**
   * The index of this instruction in the list to which it belongs. The value of this field is
   * correct only when {@link InsnList#cache} is not null. A value of -1 indicates that this
   * instruction does not belong to any {@link InsnList}.
   */
  int index;

  /**
   * Constructs a new {@link AbstractInsnNode}.
   *
   * @param opcode the opcode of the instruction to be constructed.
   */
  protected AbstractInsnNode(final int opcode) {
    this.opcode = opcode;
    this.index = -1;
  }

  /**
   * Returns the opcode of this instruction.
   *
   * @return the opcode of this instruction.
   */
  public int getOpcode() {
    return opcode;
  }

  /**
   * Returns the type of this instruction.
   *
   * @return the type of this instruction, i.e. one the constants defined in this class.
   */
  public abstract int getType();

  /**
   * Returns the previous instruction in the list to which this instruction belongs, if any.
   *
   * @return the previous instruction in the list to which this instruction belongs, if any. May be
   * {@literal null}.
   */
  public AbstractInsnNode getPrevious() {
    return previousInsn;
  }

  /**
   * Returns the next instruction in the list to which this instruction belongs, if any.
   *
   * @return the next instruction in the list to which this instruction belongs, if any. May be
   * {@literal null}.
   */
  public AbstractInsnNode getNext() {
    return nextInsn;
  }

  /**
   * Makes the given method visitor visit this instruction.
   *
   * @param methodVisitor a method visitor.
   */
  public abstract void accept(MethodVisitor methodVisitor);

  /**
   * Makes the given visitor visit the annotations of this instruction.
   *
   * @param methodVisitor a method visitor.
   */
  protected final void acceptAnnotations(final MethodVisitor methodVisitor) {
    if (visibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : visibleTypeAnnotations) {
        typeAnnotation.accept(
                methodVisitor.visitInsnAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
      }
    }
    if (invisibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : invisibleTypeAnnotations) {
        typeAnnotation.accept(
                methodVisitor.visitInsnAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
      }
    }
  }

  /**
   * Returns a copy of this instruction.
   *
   * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
   * @return a copy of this instruction. The returned instruction does not belong to any {@link
   * InsnList}.
   */
  public abstract AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels);

  /**
   * Returns the clone of the given label.
   *
   * @param label a label.
   * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
   * @return the clone of the given label.
   */
  static LabelNode clone(final LabelNode label, final Map<LabelNode, LabelNode> clonedLabels) {
    return clonedLabels.get(label);
  }

  /**
   * Returns the clones of the given labels.
   *
   * @param labels a list of labels.
   * @param clonedLabels a map from LabelNodes to cloned LabelNodes.
   * @return the clones of the given labels.
   */
  static LabelNode[] clone(
          final List<LabelNode> labels, final Map<LabelNode, LabelNode> clonedLabels) {
    LabelNode[] clones = new LabelNode[labels.size()];
    for (int i = 0, n = clones.length; i < n; ++i) {
      clones[i] = clonedLabels.get(labels.get(i));
    }
    return clones;
  }

  /**
   * Clones the annotations of the given instruction into this instruction.
   *
   * @param insnNode the source instruction.
   * @return this instruction.
   */
  protected final AbstractInsnNode cloneAnnotations(final AbstractInsnNode insnNode) {
    List<TypeAnnotationNode> visibleTypeAnnotations = insnNode.visibleTypeAnnotations;
    if (visibleTypeAnnotations != null) {
      ArrayList<TypeAnnotationNode> thisVisibleTypeAnnotations = new ArrayList<>();
      this.visibleTypeAnnotations = thisVisibleTypeAnnotations;
      for (TypeAnnotationNode sourceAnnotation : visibleTypeAnnotations) {
        TypeAnnotationNode cloneAnnotation =
                new TypeAnnotationNode(
                        sourceAnnotation.typeRef, sourceAnnotation.typePath, sourceAnnotation.desc);
        sourceAnnotation.accept(cloneAnnotation);
        thisVisibleTypeAnnotations.add(cloneAnnotation);
      }
    }
    final List<TypeAnnotationNode> invisibleTypeAnnotations = insnNode.invisibleTypeAnnotations;
    if (invisibleTypeAnnotations != null) {
      ArrayList<TypeAnnotationNode> thisInvisibleTypeAnnotations = new ArrayList<>();
      this.invisibleTypeAnnotations = thisInvisibleTypeAnnotations;
      for (TypeAnnotationNode sourceAnnotation : invisibleTypeAnnotations) {
        TypeAnnotationNode cloneAnnotation =
                new TypeAnnotationNode(
                        sourceAnnotation.typeRef, sourceAnnotation.typePath, sourceAnnotation.desc);
        sourceAnnotation.accept(cloneAnnotation);
        thisInvisibleTypeAnnotations.add(cloneAnnotation);
      }
    }
    return this;
  }
}
