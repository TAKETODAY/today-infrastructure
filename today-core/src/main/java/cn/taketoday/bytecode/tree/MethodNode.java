/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.TypePath;
import cn.taketoday.util.ObjectUtils;

/**
 * A node that represents a method.
 *
 * @author Eric Bruneton
 */
public class MethodNode extends MethodVisitor {

  /**
   * The method's access flags (see {@link Opcodes}). This field also indicates if the method is
   * synthetic and/or deprecated.
   */
  public int access;

  /** The method's name. */
  public String name;

  /** The method's descriptor (see {@link Type}). */
  public String desc;

  /** The method's signature. May be {@literal null}. */
  public String signature;

  /** The internal names of the method's exception classes (see {@link Type#getInternalName()}). */
  public String[] exceptions;

  /** The method parameter info (access flags and name). */
  public List<ParameterNode> parameters;

  /** The runtime visible annotations of this method. May be {@literal null}. */
  public List<AnnotationNode> visibleAnnotations;

  /** The runtime invisible annotations of this method. May be {@literal null}. */
  public List<AnnotationNode> invisibleAnnotations;

  /** The runtime visible type annotations of this method. May be {@literal null}. */
  public List<TypeAnnotationNode> visibleTypeAnnotations;

  /** The runtime invisible type annotations of this method. May be {@literal null}. */
  public List<TypeAnnotationNode> invisibleTypeAnnotations;

  /** The non standard attributes of this method. May be {@literal null}. */
  public List<Attribute> attrs;

  /**
   * The default value of this annotation interface method. This field must be a {@link Byte},
   * {@link Boolean}, {@link Character}, {@link Short}, {@link Integer}, {@link Long}, {@link
   * Float}, {@link Double}, {@link String} or {@link Type}, or an two elements String array (for
   * enumeration values), a {@link AnnotationNode}, or a {@link List} of values of one of the
   * preceding types. May be {@literal null}.
   */
  public Object annotationDefault;

  /**
   * The number of method parameters than can have runtime visible annotations. This number must be
   * less or equal than the number of parameter types in the method descriptor (the default value 0
   * indicates that all the parameters described in the method descriptor can have annotations). It
   * can be strictly less when a method has synthetic parameters and when these parameters are
   * ignored when computing parameter indices for the purpose of parameter annotations (see
   * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18).
   */
  public int visibleAnnotableParameterCount;

  /**
   * The runtime visible parameter annotations of this method. These lists are lists of {@link
   * AnnotationNode} objects. May be {@literal null}.
   */
  public List<AnnotationNode>[] visibleParameterAnnotations;

  /**
   * The number of method parameters than can have runtime invisible annotations. This number must
   * be less or equal than the number of parameter types in the method descriptor (the default value
   * 0 indicates that all the parameters described in the method descriptor can have annotations).
   * It can be strictly less when a method has synthetic parameters and when these parameters are
   * ignored when computing parameter indices for the purpose of parameter annotations (see
   * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18).
   */
  public int invisibleAnnotableParameterCount;

  /**
   * The runtime invisible parameter annotations of this method. These lists are lists of {@link
   * AnnotationNode} objects. May be {@literal null}.
   */
  public List<AnnotationNode>[] invisibleParameterAnnotations;

  /** The instructions of this method. */
  public InsnList instructions;

  /** The try catch blocks of this method. */
  public List<TryCatchBlockNode> tryCatchBlocks;

  /** The maximum stack size of this method. */
  public int maxStack;

  /** The maximum number of local variables of this method. */
  public int maxLocals;

  /** The local variables of this method. May be {@literal null} */
  public List<LocalVariableNode> localVariables;

  /** The visible local variable annotations of this method. May be {@literal null} */
  public List<LocalVariableAnnotationNode> visibleLocalVariableAnnotations;

  /** The invisible local variable annotations of this method. May be {@literal null} */
  public List<LocalVariableAnnotationNode> invisibleLocalVariableAnnotations;

  /** Whether the accept method has been called on this object. */
  private boolean visited;

  /**
   * Constructs an uninitialized {@link MethodNode}.
   */
  public MethodNode() {
    this.instructions = new InsnList();
  }

  /**
   * Constructs a new {@link MethodNode}.
   *
   * @param access the method's access flags (see {@link Opcodes}). This parameter also indicates if
   * the method is synthetic and/or deprecated.
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param signature the method's signature. May be {@literal null}.
   * @param exceptions the internal names of the method's exception classes (see {@link
   * Type#getInternalName()}). May be {@literal null}.
   */
  public MethodNode(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final String[] exceptions) {
    this.name = name;
    this.access = access;
    this.desc = descriptor;
    this.signature = signature;
    if (ObjectUtils.isNotEmpty(exceptions)) {
      this.exceptions = exceptions;
    }
    if ((access & Opcodes.ACC_ABSTRACT) == 0) {
      this.localVariables = new ArrayList<>(5);
    }
    this.instructions = new InsnList();
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the MethodVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visitParameter(final String name, final int access) {
    if (parameters == null) {
      parameters = new ArrayList<>(5);
    }
    parameters.add(new ParameterNode(name, access));
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    return new AnnotationNode(
            new ArrayList<>(0) {
              @Override
              public boolean add(final Object o) {
                annotationDefault = o;
                return super.add(o);
              }
            });
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    AnnotationNode annotation = new AnnotationNode(descriptor);
    if (visible) {
      visibleAnnotations = Util.add(visibleAnnotations, annotation);
    }
    else {
      invisibleAnnotations = Util.add(invisibleAnnotations, annotation);
    }
    return annotation;
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
    if (visible) {
      visibleTypeAnnotations = Util.add(visibleTypeAnnotations, typeAnnotation);
    }
    else {
      invisibleTypeAnnotations = Util.add(invisibleTypeAnnotations, typeAnnotation);
    }
    return typeAnnotation;
  }

  @Override
  public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
    if (visible) {
      visibleAnnotableParameterCount = parameterCount;
    }
    else {
      invisibleAnnotableParameterCount = parameterCount;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public AnnotationVisitor visitParameterAnnotation(
          final int parameter, final String descriptor, final boolean visible) {
    AnnotationNode annotation = new AnnotationNode(descriptor);
    if (visible) {
      if (visibleParameterAnnotations == null) {
        int params = Type.getArgumentCount(desc);
        visibleParameterAnnotations = (List<AnnotationNode>[]) new List<?>[params];
      }
      visibleParameterAnnotations[parameter] =
              Util.add(visibleParameterAnnotations[parameter], annotation);
    }
    else {
      if (invisibleParameterAnnotations == null) {
        int params = Type.getArgumentCount(desc);
        invisibleParameterAnnotations = (List<AnnotationNode>[]) new List<?>[params];
      }
      invisibleParameterAnnotations[parameter] =
              Util.add(invisibleParameterAnnotations[parameter], annotation);
    }
    return annotation;
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    attrs = Util.add(attrs, attribute);
  }

  @Override
  public void visitCode() {
    // Nothing to do.
  }

  @Override
  public void visitFrame(
          final int type,
          final int numLocal,
          final Object[] local,
          final int numStack,
          final Object[] stack) {
    instructions.add(
            new FrameNode(
                    type,
                    numLocal,
                    local == null ? null : getLabelNodes(local),
                    numStack,
                    stack == null ? null : getLabelNodes(stack)));
  }

  @Override
  public void visitInsn(final int opcode) {
    instructions.add(new InsnNode(opcode));
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    instructions.add(new IntInsnNode(opcode, operand));
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    instructions.add(new VarInsnNode(opcode, var));
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    instructions.add(new TypeInsnNode(opcode, type));
  }

  @Override
  public void visitFieldInsn(
          final int opcode, final String owner, final String name, final String descriptor) {
    instructions.add(new FieldInsnNode(opcode, owner, name, descriptor));
  }

  @Override
  public void visitMethodInsn(
          final int opcodeAndSource,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    int opcode = opcodeAndSource & ~Opcodes.SOURCE_MASK;
    instructions.add(new MethodInsnNode(opcode, owner, name, descriptor, isInterface));
  }

  @Override
  public void visitInvokeDynamicInsn(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object... bootstrapMethodArguments) {
    instructions.add(
            new InvokeDynamicInsnNode(
                    name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments));
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    instructions.add(new JumpInsnNode(opcode, getLabelNode(label)));
  }

  @Override
  public void visitLabel(final Label label) {
    instructions.add(getLabelNode(label));
  }

  @Override
  public void visitLdcInsn(final Object value) {
    instructions.add(new LdcInsnNode(value));
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    instructions.add(new IincInsnNode(var, increment));
  }

  @Override
  public void visitTableSwitchInsn(
          final int min, final int max, final Label dflt, final Label... labels) {
    instructions.add(new TableSwitchInsnNode(min, max, getLabelNode(dflt), getLabelNodes(labels)));
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    instructions.add(new LookupSwitchInsnNode(getLabelNode(dflt), keys, getLabelNodes(labels)));
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    instructions.add(new MultiANewArrayInsnNode(descriptor, numDimensions));
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    // Find the last real instruction, i.e. the instruction targeted by this annotation.
    AbstractInsnNode currentInsn = instructions.getLast();
    while (currentInsn.getOpcode() == -1) {
      currentInsn = currentInsn.getPrevious();
    }
    // Add the annotation to this instruction.
    TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
    if (visible) {
      currentInsn.visibleTypeAnnotations =
              Util.add(currentInsn.visibleTypeAnnotations, typeAnnotation);
    }
    else {
      currentInsn.invisibleTypeAnnotations =
              Util.add(currentInsn.invisibleTypeAnnotations, typeAnnotation);
    }
    return typeAnnotation;
  }

  @Override
  public void visitTryCatchBlock(
          final Label start, final Label end, final Label handler, final String type) {
    TryCatchBlockNode tryCatchBlock =
            new TryCatchBlockNode(getLabelNode(start), getLabelNode(end), getLabelNode(handler), type);
    tryCatchBlocks = Util.add(tryCatchBlocks, tryCatchBlock);
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    TryCatchBlockNode tryCatchBlock = tryCatchBlocks.get((typeRef & 0x00FFFF00) >> 8);
    TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
    if (visible) {
      tryCatchBlock.visibleTypeAnnotations =
              Util.add(tryCatchBlock.visibleTypeAnnotations, typeAnnotation);
    }
    else {
      tryCatchBlock.invisibleTypeAnnotations =
              Util.add(tryCatchBlock.invisibleTypeAnnotations, typeAnnotation);
    }
    return typeAnnotation;
  }

  @Override
  public void visitLocalVariable(
          final String name,
          final String descriptor,
          final String signature,
          final Label start,
          final Label end,
          final int index) {
    LocalVariableNode localVariable =
            new LocalVariableNode(
                    name, descriptor, signature, getLabelNode(start), getLabelNode(end), index);
    localVariables = Util.add(localVariables, localVariable);
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(
          final int typeRef,
          final TypePath typePath,
          final Label[] start,
          final Label[] end,
          final int[] index,
          final String descriptor,
          final boolean visible) {
    LocalVariableAnnotationNode localVariableAnnotation =
            new LocalVariableAnnotationNode(
                    typeRef, typePath, getLabelNodes(start), getLabelNodes(end), index, descriptor);
    if (visible) {
      visibleLocalVariableAnnotations =
              Util.add(visibleLocalVariableAnnotations, localVariableAnnotation);
    }
    else {
      invisibleLocalVariableAnnotations =
              Util.add(invisibleLocalVariableAnnotations, localVariableAnnotation);
    }
    return localVariableAnnotation;
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    instructions.add(new LineNumberNode(line, getLabelNode(start)));
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    this.maxStack = maxStack;
    this.maxLocals = maxLocals;
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  /**
   * Returns the LabelNode corresponding to the given Label. Creates a new LabelNode if necessary.
   * The default implementation of this method uses the {@link Label#info} field to store
   * associations between labels and label nodes.
   *
   * @param label a Label.
   * @return the LabelNode corresponding to label.
   */
  protected LabelNode getLabelNode(final Label label) {
    if (!(label.info instanceof LabelNode)) {
      label.info = new LabelNode();
    }
    return (LabelNode) label.info;
  }

  private LabelNode[] getLabelNodes(final Label[] labels) {
    LabelNode[] labelNodes = new LabelNode[labels.length];
    for (int i = 0, n = labels.length; i < n; ++i) {
      labelNodes[i] = getLabelNode(labels[i]);
    }
    return labelNodes;
  }

  private Object[] getLabelNodes(final Object[] objects) {
    Object[] labelNodes = new Object[objects.length];
    for (int i = 0, n = objects.length; i < n; ++i) {
      Object o = objects[i];
      if (o instanceof Label) {
        o = getLabelNode((Label) o);
      }
      labelNodes[i] = o;
    }
    return labelNodes;
  }

  // -----------------------------------------------------------------------------------------------
  // Accept method
  // -----------------------------------------------------------------------------------------------

  /**
   * Makes the given class visitor visit this method.
   *
   * @param classVisitor a class visitor.
   */
  public void accept(final ClassVisitor classVisitor) {
    MethodVisitor visitor = classVisitor.visitMethod(access, name, desc, signature, exceptions);
    if (visitor != null) {
      accept(visitor);
    }
  }

  /**
   * Makes the given method visitor visit this method.
   *
   * @param methodVisitor a method visitor.
   */
  public void accept(final MethodVisitor methodVisitor) {
    // Visit the parameters.
    if (parameters != null) {
      for (ParameterNode parameter : parameters) {
        parameter.accept(methodVisitor);
      }
    }
    // Visit the annotations.
    if (annotationDefault != null) {
      AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotationDefault();
      AnnotationNode.accept(annotationVisitor, null, annotationDefault);
      if (annotationVisitor != null) {
        annotationVisitor.visitEnd();
      }
    }
    if (visibleAnnotations != null) {
      for (AnnotationNode annotation : visibleAnnotations) {
        annotation.accept(methodVisitor.visitAnnotation(annotation.desc, true));
      }
    }
    if (invisibleAnnotations != null) {
      for (AnnotationNode annotation : invisibleAnnotations) {
        annotation.accept(methodVisitor.visitAnnotation(annotation.desc, false));
      }
    }
    if (visibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : visibleTypeAnnotations) {
        typeAnnotation.accept(
                methodVisitor.visitTypeAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
      }
    }
    if (invisibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : invisibleTypeAnnotations) {
        typeAnnotation.accept(
                methodVisitor.visitTypeAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
      }
    }
    if (visibleAnnotableParameterCount > 0) {
      methodVisitor.visitAnnotableParameterCount(visibleAnnotableParameterCount, true);
    }
    List<AnnotationNode>[] visibleParameterAnnotations = this.visibleParameterAnnotations;
    if (visibleParameterAnnotations != null) {
      for (int i = 0, n = visibleParameterAnnotations.length; i < n; ++i) {
        List<AnnotationNode> parameterAnnotations = visibleParameterAnnotations[i];
        if (parameterAnnotations == null) {
          continue;
        }
        for (AnnotationNode annotation : parameterAnnotations) {
          annotation.accept(methodVisitor.visitParameterAnnotation(i, annotation.desc, true));
        }
      }
    }
    if (invisibleAnnotableParameterCount > 0) {
      methodVisitor.visitAnnotableParameterCount(invisibleAnnotableParameterCount, false);
    }
    List<AnnotationNode>[] invisibleParameterAnnotations = this.invisibleParameterAnnotations;
    if (invisibleParameterAnnotations != null) {
      for (int i = 0, n = invisibleParameterAnnotations.length; i < n; ++i) {
        List<AnnotationNode> parameterAnnotations = invisibleParameterAnnotations[i];
        if (parameterAnnotations == null) {
          continue;
        }
        for (AnnotationNode annotation : parameterAnnotations) {
          annotation.accept(methodVisitor.visitParameterAnnotation(i, annotation.desc, false));
        }
      }
    }
    // Visit the non standard attributes.
    if (visited) {
      instructions.resetLabels();
    }
    if (attrs != null) {
      for (Attribute attr : attrs) {
        methodVisitor.visitAttribute(attr);
      }
    }
    // Visit the code.
    if (instructions.size() > 0) {
      methodVisitor.visitCode();
      // Visits the try catch blocks.
      List<TryCatchBlockNode> tryCatchBlocks = this.tryCatchBlocks;
      if (tryCatchBlocks != null) {
        for (int i = 0, n = tryCatchBlocks.size(); i < n; ++i) {
          tryCatchBlocks.get(i).updateIndex(i);
          tryCatchBlocks.get(i).accept(methodVisitor);
        }
      }
      // Visit the instructions.
      instructions.accept(methodVisitor);
      // Visits the local variables.
      if (localVariables != null) {
        for (LocalVariableNode localVariable : localVariables) {
          localVariable.accept(methodVisitor);
        }
      }
      // Visits the local variable annotations.
      if (visibleLocalVariableAnnotations != null) {
        for (LocalVariableAnnotationNode visibleLocalVariableAnnotation : visibleLocalVariableAnnotations) {
          visibleLocalVariableAnnotation.accept(methodVisitor, true);
        }
      }
      if (invisibleLocalVariableAnnotations != null) {
        for (LocalVariableAnnotationNode invisibleLocalVariableAnnotation : invisibleLocalVariableAnnotations) {
          invisibleLocalVariableAnnotation.accept(methodVisitor, false);
        }
      }
      methodVisitor.visitMaxs(maxStack, maxLocals);
      visited = true;
    }
    methodVisitor.visitEnd();
  }
}
