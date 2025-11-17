/*
 * Copyright 2017 - 2025 the original author or authors.
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
package infra.bytecode.commons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import infra.bytecode.AnnotationValueHolder;
import infra.bytecode.AnnotationVisitor;
import infra.bytecode.AsmTest;
import infra.bytecode.Attribute;
import infra.bytecode.ClassFile;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.bytecode.ClassWriter;
import infra.bytecode.ConstantDynamic;
import infra.bytecode.Handle;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.tree.ClassNode;
import infra.bytecode.tree.InvokeDynamicInsnNode;
import infra.bytecode.tree.LdcInsnNode;
import infra.bytecode.util.CheckMethodAdapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ClassRemapper}.
 *
 * @author Eric Bruneton
 */
class ClassRemapperTests extends AsmTest {

  @Test
  void testVisit() {
    ClassNode classNode = new ClassNode();
    ClassRemapper classRemapper =
            new ClassRemapper(
                    classNode,
                    new SimpleRemapper("pkg/C", "new/pkg/C"));

    classRemapper.visit(infra.bytecode.Opcodes.V1_5, Opcodes.ACC_PUBLIC, "pkg/C", null, "java/lang/Object", null);

    assertEquals("new/pkg/C", classNode.name);
  }

  @Test
  void testVisitAnnotation() {
    ClassNode classNode = new ClassNode();
    ClassRemapper remapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {
                      @Override
                      public String mapAnnotationAttributeName(final String descriptor, final String name) {
                        if ("Lpkg/A;".equals(descriptor)) {
                          return "new." + name;
                        }
                        return name;
                      }
                    });
    remapper.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "pkg/C", null, "java/lang/Object", null);
    AnnotationVisitor annotationVisitor = remapper.visitAnnotation("Lpkg/A;", true);
    annotationVisitor.visit("attribute", "value");

    assertEquals("new.attribute", classNode.visibleAnnotations.get(0).values.get(0));
  }

  @Test
  void testVisitInnerClass() {
    ClassNode classNode = new ClassNode();
    ClassRemapper remapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {
                      @Override
                      public String map(final String internalName) {
                        if ("pkg/C".equals(internalName)) {
                          return "a";
                        }
                        if ("pkg/C$Inner".equals(internalName)) {
                          return "a$b";
                        }
                        return internalName;
                      }
                    });
    remapper.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "pkg/C", null, "java/lang/Object", null);

    remapper.visitInnerClass("pkg/C$Inner", "pkg/C", "Inner", Opcodes.ACC_PUBLIC);

    assertEquals("a$b", classNode.innerClasses.get(0).name);
    assertEquals("a", classNode.innerClasses.get(0).outerName);
    assertEquals("b", classNode.innerClasses.get(0).innerName);
  }

  @Test
  void testVisitInnerClass_localInnerClass() {
    ClassNode classNode = new ClassNode();
    ClassRemapper remapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {
                      @Override
                      public String map(final String internalName) {
                        if ("pkg/C".equals(internalName)) {
                          return "a";
                        }
                        if ("pkg/C$1Inner".equals(internalName)) {
                          return "a$1b";
                        }
                        return internalName;
                      }
                    });
    remapper.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "pkg/C", null, "java/lang/Object", null);

    remapper.visitInnerClass("pkg/C$1Inner", "pkg/C", "Inner", Opcodes.ACC_PUBLIC);

    assertEquals("a$1b", classNode.innerClasses.get(0).name);
    assertEquals("a", classNode.innerClasses.get(0).outerName);
    assertEquals("b", classNode.innerClasses.get(0).innerName);
  }

  @Test
  void testVisitInnerClass_specialRemap() {
    ClassNode classNode = new ClassNode();
    ClassRemapper remapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {
                      @Override
                      public String map(final String internalName) {
                        if ("pkg/C".equals(internalName)) {
                          return "pkg2/C";
                        }
                        if ("pkg/C$$a".equals(internalName)) {
                          return "pkg2/C$$a";
                        }
                        return internalName;
                      }
                    });
    remapper.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "pkg/C", null, "java/lang/Object", null);

    remapper.visitInnerClass("pkg/C$$a", "pkg/C", "$a", Opcodes.ACC_PUBLIC);

    assertEquals("pkg2/C$$a", classNode.innerClasses.get(0).name);
    assertEquals("pkg2/C", classNode.innerClasses.get(0).outerName);
    assertEquals("$a", classNode.innerClasses.get(0).innerName);
  }

  @Test
  void testVisitAttribute_moduleHashes() {
    ClassNode classNode = new ClassNode();
    ClassRemapper classRemapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {
                      @Override
                      public String mapModuleName(final String name) {
                        return "new." + name;
                      }
                    });
    classRemapper.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "C", null, "java/lang/Object", null);

    // The ClassRemapper change the modules and the hashes so the lists have to be mutable.
    classRemapper.visitAttribute(
            new ModuleHashesAttribute("algorithm", Arrays.asList("pkg.C"), Arrays.asList(new byte[0])));

    assertEquals("C", classNode.name);
    assertEquals("new.pkg.C", ((ModuleHashesAttribute) classNode.attrs.get(0)).modules.get(0));
  }

  @Test
  void testVisitLdcInsn_constantDynamic_deprecated() {
    ClassNode classNode = new ClassNode();
    ClassRemapper classRemapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {

                      @Override
                      public String mapBasicInvokeDynamicMethodName(String name, String descriptor,
                              Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        return "new." + name;
                      }

                      @Override
                      public String map(final String internalName) {
                        if (internalName.equals("java/lang/String")) {
                          return "java/lang/Integer";
                        }
                        return internalName;
                      }
                    }) {
              /* inner class so it can access the protected constructor */
            };
    classRemapper.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "C", null, "java/lang/Object", null);
    MethodVisitor methodVisitor =
            classRemapper.visitMethod(Opcodes.ACC_PUBLIC, "hello", "()V", null, null);
    methodVisitor.visitCode();

    methodVisitor.visitLdcInsn(
            new ConstantDynamic(
                    "foo",
                    "Ljava/lang/String;",
                    new Handle(Opcodes.H_INVOKESTATIC, "BSMHost", "bsm", "()Ljava/lang/String;", false)));

    ConstantDynamic constantDynamic =
            (ConstantDynamic) ((LdcInsnNode) classNode.methods.get(0).instructions.get(0)).cst;
    assertEquals("new.foo", constantDynamic.getName());
    assertEquals("Ljava/lang/Integer;", constantDynamic.getDescriptor());
    assertEquals("()Ljava/lang/Integer;", constantDynamic.getBootstrapMethod().getDesc());
  }

  @Test
  void testVisitLdcInsn_constantDynamic() {
    ClassNode classNode = new ClassNode();
    ClassRemapper classRemapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {
                      @Override
                      public String mapBasicInvokeDynamicMethodName(
                              final String name,
                              final String descriptor,
                              final Handle bootstrapMethodHandle,
                              final Object... bootstrapMethodArguments) {
                        return "new." + name;
                      }

                      @Override
                      public String map(final String internalName) {
                        if (internalName.equals("java/lang/String")) {
                          return "java/lang/Integer";
                        }
                        return internalName;
                      }
                    }) {
              /* inner class so it can access the protected constructor */
            };
    classRemapper.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "C", null, "java/lang/Object", null);
    MethodVisitor methodVisitor =
            classRemapper.visitMethod(Opcodes.ACC_PUBLIC, "hello", "()V", null, null);
    methodVisitor.visitCode();

    methodVisitor.visitLdcInsn(
            new ConstantDynamic(
                    "foo",
                    "Ljava/lang/String;",
                    new Handle(Opcodes.H_INVOKESTATIC, "BSMHost", "bsm", "()Ljava/lang/String;", false)));

    ConstantDynamic constantDynamic =
            (ConstantDynamic) ((LdcInsnNode) classNode.methods.get(0).instructions.get(0)).cst;
    assertEquals("new.foo", constantDynamic.getName());
    assertEquals("Ljava/lang/Integer;", constantDynamic.getDescriptor());
    assertEquals("()Ljava/lang/Integer;", constantDynamic.getBootstrapMethod().getDesc());
  }

  @Test
  void testInvokeDynamicInsn_field() {
    ClassNode classNode = new ClassNode();
    ClassRemapper classRemapper =
            new ClassRemapper(

                    classNode,
                    new Remapper() {
                      @Override
                      public String mapBasicInvokeDynamicMethodName(
                              final String name,
                              final String descriptor,
                              final Handle bootstrapMethodHandle,
                              final Object... bootstrapMethodArguments) {
                        return "new." + name;
                      }

                      @Override
                      public String mapFieldName(
                              final String owner, final String name, final String descriptor) {
                        if ("a".equals(name)) {
                          return "demo";
                        }
                        return name;
                      }
                    });
    classRemapper.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "C", null, "java/lang/Object", null);
    MethodVisitor methodVisitor =
            classRemapper.visitMethod(Opcodes.ACC_PUBLIC, "hello", "()V", null, null);
    methodVisitor.visitCode();

    methodVisitor.visitInvokeDynamicInsn(
            "foo",
            "()Ljava/lang/String;",
            new Handle(Opcodes.H_GETFIELD, "pkg/B", "a", "Ljava/lang/String;", false));

    InvokeDynamicInsnNode invokeDynamic =
            (InvokeDynamicInsnNode) classNode.methods.get(0).instructions.get(0);
    assertEquals("new.foo", invokeDynamic.name);
    assertEquals("demo", invokeDynamic.bsm.getName());
  }

  /** Tests that classes transformed with an empty ClassRemapper are unchanged. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testEmptyClassRemapper_precompiledClass(
          final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassRemapper classRemapper =
            newClassRemapper(classWriter, new SimpleRemapper(Map.of()));

    Executable accept =
            () -> classReader.accept(classRemapper, new Attribute[] { new CodeComment() }, 0);

    if (classParameter.isMoreRecentThan(apiParameter)) {
      Exception exception = assertThrows(UnsupportedOperationException.class, accept);
      assertTrue(exception.getMessage().matches(UNSUPPORTED_OPERATION_MESSAGE_PATTERN));
    }
    else {
      assertDoesNotThrow(accept);
      assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
    }
  }

  /** Tests that inner class names are unchanged with by an empty ClassRemapper. */
  @Test
  void testEmptyClassRemapper_innerClassNames() {
    ClassWriter classFileWriter = new ClassWriter(0);
    classFileWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
            "Outer",
            null,
            "java/lang/Object",
            null);
    classFileWriter.visitInnerClass(
            "Outer$$Inner",
            "Outer",
            "$Inner",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE);
    classFileWriter.visitEnd();
    byte[] classFile = classFileWriter.toByteArray();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassRemapper classRemapper =
            new ClassRemapper(
                    classWriter, new SimpleRemapper(Map.of()));

    classReader.accept(classRemapper, new Attribute[] { new CodeComment() }, 0);

    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /** Tests that classes transformed with a ClassRemapper can be loaded and instantiated. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testAllMethods_precompiledClass(
          final PrecompiledClass classParameter, final Api apiParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter classWriter = new ClassWriter(0);
    UpperCaseRemapper upperCaseRemapper = new UpperCaseRemapper(classParameter.getInternalName());
    ClassRemapper classRemapper =
            newClassRemapper(classWriter, upperCaseRemapper);

    Executable accept = () -> classReader.accept(classRemapper, 0);

    if (classParameter.isMoreRecentThan(apiParameter)) {
      Exception exception = assertThrows(UnsupportedOperationException.class, accept);
      assertTrue(exception.getMessage().matches(UNSUPPORTED_OPERATION_MESSAGE_PATTERN));
    }
    else {
      assertDoesNotThrow(accept);
      Executable newInstance = () -> new ClassFile(classWriter.toByteArray()).newInstance();
      if (classParameter.isNotCompatibleWithCurrentJdk()) {
        assertThrows(UnsupportedClassVersionError.class, newInstance);
      }
      else {
        assertDoesNotThrow(newInstance);
      }
    }
  }

  /**
   * Tests that classes transformed with a ClassNode and ClassRemapper can be loaded and
   * instantiated.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testAllMethods_precompiledClass_fromClassNode(
          final PrecompiledClass classParameter, final Api apiParameter) {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    ClassWriter classWriter = new ClassWriter(0);
    UpperCaseRemapper upperCaseRemapper = new UpperCaseRemapper(classParameter.getInternalName());
    ClassRemapper classRemapper =
            newClassRemapper(classWriter, upperCaseRemapper);

    Executable accept = () -> classNode.accept(classRemapper);

    if (classParameter.isMoreRecentThan(apiParameter)) {
      Exception exception = assertThrows(UnsupportedOperationException.class, accept);
      assertTrue(exception.getMessage().matches(UNSUPPORTED_OPERATION_MESSAGE_PATTERN));
    }
    else {
      assertDoesNotThrow(accept);
      Executable newInstance = () -> new ClassFile(classWriter.toByteArray()).newInstance();
      if (classParameter.isNotCompatibleWithCurrentJdk()) {
        assertThrows(UnsupportedClassVersionError.class, newInstance);
      }
      else {
        assertDoesNotThrow(newInstance);
      }
    }
  }

  private static void checkDescriptor(final String descriptor) {
    CheckMethodAdapter checkMethodAdapter = new CheckMethodAdapter(null);
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitFieldInsn(Opcodes.GETFIELD, "Owner", "name", descriptor);
  }

  private static void checkInternalName(final String internalName) {
    CheckMethodAdapter checkMethodAdapter = new CheckMethodAdapter(null);
    checkMethodAdapter.version = Opcodes.V1_5;
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitFieldInsn(Opcodes.GETFIELD, internalName, "name", "I");
  }

  ClassRemapper newClassRemapper(final ClassVisitor classVisitor, final Remapper remapper) {
    return new ClassRemapper(classVisitor, remapper);
  }

  static class UpperCaseRemapper extends Remapper {

    private static final Locale LOCALE = Locale.ENGLISH;

    private final String internalClassName;
    private final String remappedInternalClassName;

    UpperCaseRemapper(final String internalClassName) {
      super();
      this.internalClassName = internalClassName;
      this.remappedInternalClassName =
              internalClassName.equals("module-info")
                      ? internalClassName
                      : internalClassName.toUpperCase(LOCALE);
    }

    String getRemappedClassName() {
      return remappedInternalClassName.replace('/', '.');
    }

    @Override
    public String mapDesc(final String descriptor) {
      checkDescriptor(descriptor);
      return super.mapDesc(descriptor);
    }

    @Override
    public String mapType(final String type) {
      if (type != null && !type.equals("module-info")) {
        checkInternalName(type);
      }
      return super.mapType(type);
    }

    @Override
    public String mapMethodName(final String owner, final String name, final String descriptor) {
      if (name.equals("<init>") || name.equals("<clinit>")) {
        return name;
      }
      return owner.equals(internalClassName) ? name.toUpperCase(LOCALE) : name;
    }

    @Override
    public String mapBasicInvokeDynamicMethodName(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
      return name.toUpperCase(LOCALE);
    }

    @Override
    public String mapFieldName(final String owner, final String name, final String descriptor) {
      return owner.equals(internalClassName) ? name.toUpperCase(LOCALE) : name;
    }

    @Override
    public String map(final String typeName) {
      return typeName.equals(internalClassName) ? remappedInternalClassName : typeName;
    }

    @Override
    public Object mapValue(final Object value) {
      if (value instanceof Boolean
              || value instanceof Byte
              || value instanceof Short
              || value instanceof Character
              || value instanceof Integer
              || value instanceof Long
              || value instanceof Double
              || value instanceof Float
              || value instanceof String
              || value instanceof Type
              || value instanceof Handle
              || value instanceof AnnotationValueHolder
              || value instanceof ConstantDynamic
              || value.getClass().isArray()) {
        return super.mapValue(value);
      }
      // If this happens, add support for the new type in Remapper.mapValue(), if needed.
      throw new IllegalArgumentException("Unsupported type of value: " + value);
    }
  }
}
