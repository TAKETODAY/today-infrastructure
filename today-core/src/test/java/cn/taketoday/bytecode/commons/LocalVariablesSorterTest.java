/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package cn.taketoday.bytecode.commons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link LocalVariablesSorter}.
 *
 * @author Eric Bruneton
 */
public class LocalVariablesSorterTest extends AsmTest {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new LocalVariablesSorter(Opcodes.ACC_PUBLIC, "()V", new MethodNode()));
  }

  @Test
  public void testVisitFrame_emptyFrame() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_PUBLIC, "()V", new MethodNode());

    Executable visitFrame = () -> localVariablesSorter.visitFrame(Opcodes.F_NEW, 0, null, 0, null);

    assertDoesNotThrow(visitFrame);
  }

  @Test
  public void testVisitFrame_invalidFrameType() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_PUBLIC, "()V", new MethodNode());

    Executable visitFrame = () -> localVariablesSorter.visitFrame(Opcodes.F_FULL, 0, null, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals(
            "LocalVariablesSorter only accepts expanded frames (see ClassReader.EXPAND_FRAMES)",
            exception.getMessage());
  }

  @Test
  public void testNewLocal_boolean() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.BOOLEAN_TYPE);

    assertEquals(1, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_byte() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.BYTE_TYPE);

    assertEquals(1, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_char() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.CHAR_TYPE);

    assertEquals(1, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_short() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.SHORT_TYPE);

    assertEquals(1, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_int() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.INT_TYPE);

    assertEquals(1, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_float() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.FLOAT_TYPE);

    assertEquals(1, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_long() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.LONG_TYPE);
    assertEquals(2, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_double() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.DOUBLE_TYPE);

    assertEquals(2, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_object() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.forInternalName("pkg/Class"));

    assertEquals(1, localVariablesSorter.nextLocal);
  }

  @Test
  public void testNewLocal_array() {
    LocalVariablesSorter localVariablesSorter =
            new LocalVariablesSorter(Opcodes.ACC_STATIC, "()V", new MethodNode());

    localVariablesSorter.newLocal(Type.forDescriptor("[I"));

    assertEquals(1, localVariablesSorter.nextLocal);
  }

  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAllMethods_precompiledClass(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor localVariablesSorter =
            new LocalVariablesSorterClassAdapter(classWriter);

    Executable accept = () -> classReader.accept(localVariablesSorter, ClassReader.EXPAND_FRAMES);

    assertDoesNotThrow(accept);
    Executable newInstance = () -> new ClassFile(classWriter.toByteArray()).newInstance();
    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    }
    else {
      assertDoesNotThrow(newInstance);
    }
  }

  @Test
  public void testAllMethods_issue317586() throws FileNotFoundException, IOException {
    ClassReader classReader =
            new ClassReader(Files.newInputStream(Paths.get("src/test/resources/Issue317586.class")));
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor localVariablesSorter =
            new LocalVariablesSorterClassAdapter(classWriter);

    classReader.accept(localVariablesSorter, ClassReader.EXPAND_FRAMES);

    assertDoesNotThrow(() -> new ClassFile(classWriter.toByteArray()).newInstance());
  }

  static class LocalVariablesSorterClassAdapter extends ClassVisitor {

    LocalVariablesSorterClassAdapter(final ClassVisitor classVisitor) {
      super(classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      MethodVisitor methodVisitor =
              super.visitMethod(access, name, descriptor, signature, exceptions);
      return new LocalVariablesSorter(access, descriptor, methodVisitor) { };
    }
  }
}
