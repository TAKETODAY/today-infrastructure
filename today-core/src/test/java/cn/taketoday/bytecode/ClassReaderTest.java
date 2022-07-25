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
package cn.taketoday.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for {@link ClassReader}.
 *
 * @author Eric Bruneton
 */
public class ClassReaderTest extends AsmTest implements Opcodes {

  @Test
  public void testReadByte() throws IOException {
    ClassReader classReader = new ClassReader(getClass().getName());

    assertEquals(classReader.classFileBuffer[0] & 0xFF, classReader.readByte(0));
  }

  @Test
  public void testGetItem() throws IOException {
    ClassReader classReader = new ClassReader(getClass().getName());

    int item = classReader.getItem(1);

    assertTrue(item >= 10);
    assertTrue(item < classReader.header);
  }

  @Test
  public void testGetAccess() throws Exception {
    String name = getClass().getName();

    assertEquals(ACC_PUBLIC | ACC_SUPER, new ClassReader(name).getAccess());
  }

  @Test
  public void testGetClassName() throws Exception {
    String name = getClass().getName();

    assertEquals(name.replace('.', '/'), new ClassReader(name).getClassName());
  }

  @Test
  public void testGetSuperName() throws Exception {
    ClassReader thisClassReader = new ClassReader(getClass().getName());
    ClassReader objectClassReader = new ClassReader(Object.class.getName());

    assertEquals(AsmTest.class.getName().replace('.', '/'), thisClassReader.getSuperName());
    assertNull(objectClassReader.getSuperName());
  }

  @Test
  public void testGetInterfaces() throws Exception {
    ClassReader classReader = new ClassReader(getClass().getName());

    String[] interfaces = classReader.getInterfaces();

    assertNotNull(interfaces);
    assertEquals(1, interfaces.length);
    assertEquals(Opcodes.class.getName().replace('.', '/'), interfaces[0]);
  }

  @Test
  public void testGetInterfaces_empty() throws Exception {
    ClassReader classReader = new ClassReader(Opcodes.class.getName());

    String[] interfaces = classReader.getInterfaces();

    assertNotNull(interfaces);
  }

  /** Tests {@link ClassReader#ClassReader(byte[])}. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testByteArrayConstructor(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());

    assertNotEquals(0, classReader.getAccess());
    assertEquals(classParameter.getInternalName(), classReader.getClassName());
    if (classParameter.getInternalName().equals("module-info")) {
      assertNull(classReader.getSuperName());
    }
    else {
      assertTrue(classReader.getSuperName().startsWith("java"));
    }
    assertNotNull(classReader.getInterfaces());
  }

  /** Tests {@link ClassReader#ClassReader(byte[], int, int)} and the basic ClassReader accessors. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testByteArrayConstructor_withOffset(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    byte[] byteBuffer = new byte[classFile.length + 1];
    System.arraycopy(classFile, 0, byteBuffer, 1, classFile.length);

    ClassReader classReader = new ClassReader(byteBuffer, 1, classFile.length);

    assertNotEquals(0, classReader.getAccess());
    assertEquals(classParameter.getInternalName(), classReader.getClassName());
    if (classParameter.getInternalName().equals("module-info")) {
      assertNull(classReader.getSuperName());
    }
    else {
      assertTrue(classReader.getSuperName().startsWith("java"));
    }
    assertNotNull(classReader.getInterfaces());
    AtomicInteger classVersion = new AtomicInteger(0);
    classReader.accept(
            new ClassVisitor() {
              @Override
              public void visit(
                      final int version,
                      final int access,
                      final String name,
                      final String signature,
                      final String superName,
                      final String[] interfaces) {
                classVersion.set(version);
              }
            },
            0);
    assertTrue((classVersion.get() & 0xFFFF) >= (Opcodes.V1_1 & 0xFFFF));
  }

  /**
   * Tests that constructing a ClassReader fails if the class version or constant pool is invalid or
   * not supported.
   */
  @ParameterizedTest
  @EnumSource(InvalidClass.class)
  public void testByteArrayConstructor_invalidClassHeader(final InvalidClass invalidClass) {
    assumeTrue(
            invalidClass == InvalidClass.INVALID_CLASS_VERSION
                    || invalidClass == InvalidClass.INVALID_CP_INFO_TAG);

    Executable constructor = () -> new ClassReader(invalidClass.getBytes());

    assertThrows(IllegalArgumentException.class, constructor);
  }

  /** Tests {@link ClassReader#ClassReader(String)} and the basic ClassReader accessors. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testStringConstructor(final PrecompiledClass classParameter)
          throws IOException {
    ClassReader classReader = new ClassReader(classParameter.getName());

    assertNotEquals(0, classReader.getAccess());
    assertEquals(classParameter.getInternalName(), classReader.getClassName());
    if (classParameter.getInternalName().equals("module-info")) {
      assertNull(classReader.getSuperName());
    }
    else {
      assertTrue(classReader.getSuperName().startsWith("java"));
    }
    assertNotNull(classReader.getInterfaces());
  }

  /**
   * Tests {@link ClassReader#ClassReader(InputStream)} and the basic ClassReader accessors.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testStreamConstructor(final PrecompiledClass classParameter)
          throws IOException {
    ClassReader classReader;
    try (InputStream inputStream =
            ClassLoader.getSystemResourceAsStream(
                    classParameter.getName().replace('.', '/') + ".class")) {
      classReader = new ClassReader(inputStream);
    }

    assertNotEquals(0, classReader.getAccess());
    assertEquals(classParameter.getInternalName(), classReader.getClassName());
    if (classParameter.getInternalName().equals("module-info")) {
      assertNull(classReader.getSuperName());
    }
    else {
      assertTrue(classReader.getSuperName().startsWith("java"));
    }
    assertNotNull(classReader.getInterfaces());
  }

  @Test
  public void testStreamConstructor_nullStream() {
    Executable constructor = () -> new ClassReader((InputStream) null);

    Exception exception = assertThrows(IOException.class, constructor);
    assertEquals("Class not found", exception.getMessage());
  }

  /** Tests {@link ClassReader#ClassReader(InputStream)} with an empty stream. */
  @Test
  public void testStreamConstructor_emptyStream() throws IOException {
    try (InputStream inputStream =
            new InputStream() {

              @Override
              public int available() {
                return 0;
              }

              @Override
              public int read() {
                return -1;
              }
            }) {
      Executable streamConstructor = () -> new ClassReader(inputStream);

      assertTimeoutPreemptively(
              Duration.ofMillis(100),
              () -> assertThrows(ArrayIndexOutOfBoundsException.class, streamConstructor));
    }
  }

  /** Tests the ClassReader accept method with an empty visitor and SKIP_DEBUG. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAccept_emptyVisitor_skipDebug(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassVisitor classVisitor = new EmptyClassVisitor();

    Executable accept = () -> classReader.accept(classVisitor, ClassReader.SKIP_DEBUG);

    assertDoesNotThrow(accept);
  }

  /** Tests the ClassReader accept method with an empty visitor and EXPAND_FRAMES. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAccept_emptyVisitor_expandFrames(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassVisitor classVisitor = new EmptyClassVisitor();

    Executable accept = () -> classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);

    assertDoesNotThrow(accept);
  }

  /** Tests the ClassReader accept method with an empty visitor and SKIP_FRAMES. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAccept_emptyVisitor_skipFrames(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassVisitor classVisitor = new EmptyClassVisitor();

    Executable accept = () -> classReader.accept(classVisitor, ClassReader.SKIP_FRAMES);

    assertDoesNotThrow(accept);
  }

  /** Tests the ClassReader accept method with an empty visitor and SKIP_CODE. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAccept_emptyVisitor_skipCode(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassVisitor classVisitor = new EmptyClassVisitor();

    Executable accept = () -> classReader.accept(classVisitor, ClassReader.SKIP_CODE);

    // jdk8.ArtificialStructures contains structures which require ASM5, but only inside the method
    // code. Here we skip the code, so this class can be read with ASM4. Likewise for
    // jdk11.AllInstructions.
    assertDoesNotThrow(accept);
  }

  /**
   * Tests the ClassReader accept method with a visitor that skips fields, methods, members,
   * modules, nest host, permitted subclasses and record.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAccept_emptyVisitor_skipFieldMethodAndModuleContent(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassVisitor classVisitor =
            new EmptyClassVisitor() {
              @Override
              public void visit(
                      final int version,
                      final int access,
                      final String name,
                      final String signature,
                      final String superName,
                      final String[] interfaces) {
                // access may contain ACC_RECORD
              }

              @Override
              public ModuleVisitor visitModule(
                      final String name, final int access, final String version) {
                return null;
              }

              @Override
              public RecordComponentVisitor visitRecordComponent(
                      final String name, final String descriptor, final String signature) {
                return null;
              }

              @Override
              public FieldVisitor visitField(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final Object value) {
                return null;
              }

              @Override
              public MethodVisitor visitMethod(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final String[] exceptions) {
                return null;
              }

              @Override
              public void visitNestHost(final String nestHost) { }

              @Override
              public void visitNestMember(final String nestMember) { }

              @Override
              public void visitPermittedSubclass(final String permittedSubclass) { }
            };

    Executable accept = () -> classReader.accept(classVisitor, 0);

    assertDoesNotThrow(accept);
  }

  /** Tests the ClassReader accept method with a class whose content is invalid. */
  @ParameterizedTest
  @EnumSource(InvalidClass.class)
  public void testAccept_emptyVisitor_invalidClass(final InvalidClass invalidClass) {
    assumeFalse(
            invalidClass == InvalidClass.INVALID_CLASS_VERSION
                    || invalidClass == InvalidClass.INVALID_CP_INFO_TAG);
    ClassReader classReader = new ClassReader(invalidClass.getBytes());

    Executable accept =
            () -> classReader.accept(new EmptyClassVisitor(), 0);

    if (invalidClass == InvalidClass.INVALID_CONSTANT_POOL_INDEX
            || invalidClass == InvalidClass.INVALID_CONSTANT_POOL_REFERENCE
            || invalidClass == InvalidClass.INVALID_BYTECODE_OFFSET) {
      Exception exception = assertThrows(ArrayIndexOutOfBoundsException.class, accept);
      Matcher matcher = Pattern.compile("\\d+").matcher(exception.getMessage());
      assertTrue(matcher.find() && Integer.parseInt(matcher.group()) > 0);
    }
    else {
      assertThrows(IllegalArgumentException.class, accept);
    }
  }

  /**
   * Tests the ClassReader accept method with default annotation, field, method and module visitors.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAccept_defaultAnnotationFieldMethodAndModuleVisitors(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassVisitor classVisitor =
            new EmptyClassVisitor() {

              @Override
              public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
                return new AnnotationVisitor() { };
              }

              @Override
              public AnnotationVisitor visitTypeAnnotation(
                      final int typeRef,
                      final TypePath typePath,
                      final String descriptor,
                      final boolean visible) {
                return new AnnotationVisitor() { };
              }

              @Override
              public ModuleVisitor visitModule(
                      final String name, final int access, final String version) {
                super.visitModule(name, access, version);
                return new ModuleVisitor() { };
              }

              @Override
              public RecordComponentVisitor visitRecordComponent(
                      final String name, final String descriptor, final String signature) {
                super.visitRecordComponent(name, descriptor, signature);
                return new RecordComponentVisitor() {
                  @Override
                  public AnnotationVisitor visitAnnotation(
                          final String descriptor, final boolean visible) {
                    return new AnnotationVisitor() { };
                  }

                  @Override
                  public AnnotationVisitor visitTypeAnnotation(
                          final int typeRef,
                          final TypePath typePath,
                          final String descriptor,
                          final boolean visible) {
                    return new AnnotationVisitor() { };
                  }
                };
              }

              @Override
              public FieldVisitor visitField(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final Object value) {
                return new FieldVisitor() { };
              }

              @Override
              public MethodVisitor visitMethod(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final String[] exceptions) {
                return new MethodVisitor() { };
              }
            };

    Executable accept = () -> classReader.accept(classVisitor, 0);

    assertDoesNotThrow(accept);
  }

  @Test
  public void testAccept_parameterAnnotationIndices() {
    ClassReader classReader = new ClassReader(PrecompiledClass.JDK5_LOCAL_CLASS.getBytes());
    AtomicInteger parameterIndex = new AtomicInteger(-1);
    ClassVisitor readParameterIndexVisitor =
            new ClassVisitor() {
              @Override
              public MethodVisitor visitMethod(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final String[] exceptions) {
                return new MethodVisitor(null) {
                  @Override
                  public AnnotationVisitor visitParameterAnnotation(
                          final int parameter, final String descriptor, final boolean visible) {
                    if (descriptor.equals("Ljava/lang/Deprecated;")) {
                      parameterIndex.set(parameter);
                    }
                    return null;
                  }
                };
              }
            };

    classReader.accept(readParameterIndexVisitor, 0);

    assertEquals(0, parameterIndex.get());
  }

  @Test
  public void testAccept_previewClass() {
    byte[] classFile = PrecompiledClass.JDK11_ALL_INSTRUCTIONS.getBytes();
    // Set the minor version to 65535.
    classFile[4] = (byte) 0xFF;
    classFile[5] = (byte) 0xFF;
    ClassReader classReader = new ClassReader(classFile);
    AtomicInteger classVersion = new AtomicInteger(0);
    ClassVisitor readVersionVisitor =
            new ClassVisitor() {
              @Override
              public void visit(
                      final int version,
                      final int access,
                      final String name,
                      final String signature,
                      final String superName,
                      final String[] interfaces) {
                classVersion.set(version);
              }
            };

    classReader.accept(readVersionVisitor, 0);

    assertEquals(Opcodes.V_PREVIEW, classVersion.get() & Opcodes.V_PREVIEW);
  }

  private static class EmptyClassVisitor extends ClassVisitor {

    final AnnotationVisitor annotationVisitor =
            new AnnotationVisitor() {

              @Override
              public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
                return this;
              }

              @Override
              public AnnotationVisitor visitArray(final String name) {
                return this;
              }
            };

    EmptyClassVisitor() {
      super();
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
      return annotationVisitor;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef,
            final TypePath typePath,
            final String descriptor,
            final boolean visible) {
      return annotationVisitor;
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
      return new FieldVisitor() {

        @Override
        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
          return annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return annotationVisitor;
        }
      };
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      return new MethodVisitor() {

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
          return annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
          return annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
                final int parameter, final String descriptor, final boolean visible) {
          return annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return annotationVisitor;
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
          return annotationVisitor;
        }
      };
    }
  }
}
