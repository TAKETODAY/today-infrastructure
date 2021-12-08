/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.bytecode;

/**
 * Information about a class being parsed in a {@link ClassReader}.
 *
 * @author Eric Bruneton
 */
final class Context {

  /** The prototypes of the attributes that must be parsed in this class. */
  public Attribute[] attributePrototypes;

  /**
   * The options used to parse this class. One or more of {@link ClassReader#SKIP_CODE}, {@link
   * ClassReader#SKIP_DEBUG}, {@link ClassReader#SKIP_FRAMES}, {@link ClassReader#EXPAND_FRAMES} or
   * {@link ClassReader#EXPAND_ASM_INSNS}.
   */
  public int parsingOptions;

  /** The buffer used to read strings in the constant pool. */
  public char[] charBuffer;

  // Information about the current method, i.e. the one read in the current (or latest) call
  // to {@link ClassReader#readMethod()}.

  /** The access flags of the current method. */
  public int currentMethodAccessFlags;

  /** The name of the current method. */
  public String currentMethodName;

  /** The descriptor of the current method. */
  public String currentMethodDescriptor;

  /**
   * The labels of the current method, indexed by bytecode offset (only bytecode offsets for which a
   * label is needed have a non null associated Label).
   */
  public Label[] currentMethodLabels;

  // Information about the current type annotation target, i.e. the one read in the current
  // (or latest) call to {@link ClassReader#readAnnotationTarget()}.

  /**
   * The target_type and target_info of the current type annotation target, encoded as described in
   * {@link TypeReference}.
   */
  public int currentTypeAnnotationTarget;

  /** The target_path of the current type annotation target. */
  public TypePath currentTypeAnnotationTargetPath;

  /** The start of each local variable range in the current local variable annotation. */
  public Label[] currentLocalVariableAnnotationRangeStarts;

  /** The end of each local variable range in the current local variable annotation. */
  public Label[] currentLocalVariableAnnotationRangeEnds;

  /**
   * The local variable index of each local variable range in the current local variable annotation.
   */
  public int[] currentLocalVariableAnnotationRangeIndices;

  // Information about the current stack map frame, i.e. the one read in the current (or latest)
  // call to {@link ClassReader#readFrame()}.

  /** The bytecode offset of the current stack map frame. */
  public int currentFrameOffset;

  /**
   * The type of the current stack map frame. One of {@link Opcodes#F_FULL}, {@link
   * Opcodes#F_APPEND}, {@link Opcodes#F_CHOP}, {@link Opcodes#F_SAME} or {@link Opcodes#F_SAME1}.
   */
  public int currentFrameType;

  /**
   * The number of local variable types in the current stack map frame. Each type is represented
   * with a single array element (even long and double).
   */
  public int currentFrameLocalCount;

  /**
   * The delta number of local variable types in the current stack map frame (each type is
   * represented with a single array element - even long and double). This is the number of local
   * variable types in this frame, minus the number of local variable types in the previous frame.
   */
  public int currentFrameLocalCountDelta;

  /**
   * The types of the local variables in the current stack map frame. Each type is represented with
   * a single array element (even long and double), using the format described in {@link
   * MethodVisitor#visitFrame}. Depending on {@link #currentFrameType}, this contains the types of
   * all the local variables, or only those of the additional ones (compared to the previous frame).
   */
  public Object[] currentFrameLocalTypes;

  /**
   * The number stack element types in the current stack map frame. Each type is represented with a
   * single array element (even long and double).
   */
  public int currentFrameStackCount;

  /**
   * The types of the stack elements in the current stack map frame. Each type is represented with a
   * single array element (even long and double), using the format described in {@link
   * MethodVisitor#visitFrame}.
   */
  public Object[] currentFrameStackTypes;
}
