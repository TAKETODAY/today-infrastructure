/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.agent;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;

/**
 * ASM {@link ClassVisitor} that rewrites a known set of method invocations
 * to call instrumented bridge methods for {@link RecordedInvocationsPublisher recording purposes}.
 * <p>The bridge methods are located in the {@link InstrumentedBridgeMethods} class.
 *
 * @author Brian Clozel
 * @see InstrumentedMethod
 */
class InvocationsRecorderClassVisitor extends ClassVisitor implements Opcodes {

  private boolean isTransformed;

  private final ClassWriter classWriter;

  public InvocationsRecorderClassVisitor() {
    this(new ClassWriter(ClassWriter.COMPUTE_MAXS));
  }

  private InvocationsRecorderClassVisitor(ClassWriter classWriter) {
    super(classWriter);
    this.classWriter = classWriter;
  }

  public boolean isTransformed() {
    return this.isTransformed;
  }

  public byte[] getTransformedClassBuffer() {
    return this.classWriter.toByteArray();
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
    return new InvocationsRecorderMethodVisitor(mv);
  }

  @SuppressWarnings("deprecation")
  class InvocationsRecorderMethodVisitor extends MethodVisitor implements Opcodes {

    private static final String INSTRUMENTED_CLASS = InstrumentedBridgeMethods.class.getName().replace('.', '/');

    private static final Set<String> instrumentedMethods = new HashSet<>();

    static {
      for (InstrumentedMethod method : InstrumentedMethod.values()) {
        MethodReference methodReference = method.methodReference();
        instrumentedMethods.add(methodReference.getClassName().replace('.', '/')
                + "#" + methodReference.getMethodName());
      }
    }

    public InvocationsRecorderMethodVisitor(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      if (isOpcodeSupported(opcode) && shouldRecordMethodCall(owner, name)) {
        String instrumentedMethodName = rewriteMethodName(owner, name);
        mv.visitMethodInsn(INVOKESTATIC, INSTRUMENTED_CLASS, instrumentedMethodName,
                rewriteDescriptor(opcode, owner, name, descriptor), false);
        isTransformed = true;
      }
      else {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }
    }

    private boolean isOpcodeSupported(int opcode) {
      return Opcodes.INVOKEVIRTUAL == opcode || Opcodes.INVOKESTATIC == opcode;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
      for (int i = 0; i < bootstrapMethodArguments.length; i++) {
        if (bootstrapMethodArguments[i] instanceof Handle argumentHandle) {
          if (shouldRecordMethodCall(argumentHandle.getOwner(), argumentHandle.getName())) {
            String instrumentedMethodName = rewriteMethodName(argumentHandle.getOwner(), argumentHandle.getName());
            String newDescriptor = rewriteDescriptor(argumentHandle.getTag(), argumentHandle.getOwner(), argumentHandle.getName(), argumentHandle.getDesc());
            bootstrapMethodArguments[i] = new Handle(H_INVOKESTATIC, INSTRUMENTED_CLASS, instrumentedMethodName, newDescriptor, false);
            isTransformed = true;
          }
        }
      }
      super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    private boolean shouldRecordMethodCall(String owner, String method) {
      String methodReference = owner + "#" + method;
      return instrumentedMethods.contains(methodReference);
    }

    private String rewriteMethodName(String owner, String methodName) {
      int classIndex = owner.lastIndexOf('/');
      return owner.substring(classIndex + 1).toLowerCase() + methodName;
    }

    private String rewriteDescriptor(int opcode, String owner, String name, String descriptor) {
      return (opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.H_INVOKESTATIC) ? descriptor : "(L" + owner + ";" + descriptor.substring(1);
    }

  }

}
