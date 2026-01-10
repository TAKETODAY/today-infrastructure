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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode;

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
