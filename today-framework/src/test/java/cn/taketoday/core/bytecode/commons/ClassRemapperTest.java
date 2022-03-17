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
package cn.taketoday.core.bytecode.commons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Locale;

import cn.taketoday.core.bytecode.AnnotationValueHolder;
import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.ClassFile;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.ClassWriter;
import cn.taketoday.core.bytecode.ConstantDynamic;
import cn.taketoday.core.bytecode.Handle;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.core.bytecode.tree.InvokeDynamicInsnNode;
import cn.taketoday.core.bytecode.tree.LdcInsnNode;
import cn.taketoday.core.bytecode.util.CheckMethodAdapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ClassRemapper}.
 *
 * @author Eric Bruneton
 */
public class ClassRemapperTest extends AsmTest {

  @Test
  public void testVisit() {
    ClassNode classNode = new ClassNode();
    ClassRemapper classRemapper =
            new ClassRemapper(classNode, new SimpleRemapper("pkg/C", "new/pkg/C"));

    classRemapper.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "pkg/C", null, "java/lang/Object", null);

    assertEquals("new/pkg/C", classNode.name);
  }

  @Test
  public void testVisitAnnotation() {
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
  public void testVisitInnerClass() {
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
  public void testVisitInnerClass_localInnerClass() {
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
  public void testVisitAttribute_moduleHashes() {
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

    classRemapper.visitAttribute(
            new ModuleHashesAttribute("algorithm", Arrays.asList("pkg.C"), Arrays.asList(new byte[0])));

    assertEquals("C", classNode.name);
    assertEquals("new.pkg.C", ((ModuleHashesAttribute) classNode.attrs.get(0)).modules.get(0));
  }

  @Test
  public void testVisitLdcInsn_constantDynamic() {
    ClassNode classNode = new ClassNode();
    ClassRemapper classRemapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {
                      @Override
                      public String mapInvokeDynamicMethodName(final String name, final String descriptor) {
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
  public void testInvokeDynamicInsn_field() {
    ClassNode classNode = new ClassNode();
    ClassRemapper classRemapper =
            new ClassRemapper(
                    classNode,
                    new Remapper() {
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
    assertEquals("demo", invokeDynamic.bsm.getName());
  }

  /** Tests that classes transformed with a ClassRemapper can be loaded and instantiated. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAllMethods_precompiledClass(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter classWriter = new ClassWriter(0);
    UpperCaseRemapper upperCaseRemapper = new UpperCaseRemapper(classParameter.getInternalName());
    ClassRemapper classRemapper =
            newClassRemapper(classWriter, upperCaseRemapper);

    Executable accept = () -> classReader.accept(classRemapper, 0);

    assertDoesNotThrow(accept);
    Executable newInstance = () -> new ClassFile(classWriter.toByteArray()).newInstance();
    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    }
    else {
      assertDoesNotThrow(newInstance);
    }
  }

  /**
   * Tests that classes transformed with a ClassNode and ClassRemapper can be loaded and
   * instantiated.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAllMethods_precompiledClass_fromClassNode(
          final PrecompiledClass classParameter) {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    ClassWriter classWriter = new ClassWriter(0);
    UpperCaseRemapper upperCaseRemapper = new UpperCaseRemapper(classParameter.getInternalName());
    ClassRemapper classRemapper =
            newClassRemapper(classWriter, upperCaseRemapper);

    Executable accept = () -> classNode.accept(classRemapper);

    assertDoesNotThrow(accept);
    Executable newInstance = () -> new ClassFile(classWriter.toByteArray()).newInstance();
    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    }
    else {
      assertDoesNotThrow(newInstance);
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

  ClassRemapper newClassRemapper(
          final ClassVisitor classVisitor, final Remapper remapper) {
    return new ClassRemapper(classVisitor, remapper);
  }

  static class UpperCaseRemapper extends Remapper {

    private static final Locale LOCALE = Locale.ENGLISH;

    private final String internalClassName;
    private final String remappedInternalClassName;

    UpperCaseRemapper(final String internalClassName) {
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
    public String mapInvokeDynamicMethodName(final String name, final String descriptor) {
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
