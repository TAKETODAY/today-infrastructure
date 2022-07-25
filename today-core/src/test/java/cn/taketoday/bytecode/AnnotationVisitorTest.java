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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link AnnotationVisitor}.
 *
 * @author Eric Bruneton
 */
public class AnnotationVisitorTest extends AsmTest {

  /**
   * Tests that ClassReader accepts visitor which return null AnnotationVisitor, and that returning
   * null AnnotationVisitor is equivalent to returning an EmptyAnnotationVisitor.
   */
  @ParameterizedTest
  @MethodSource("allClassesAndAllApis")
  public void testReadAndWrite_removeOrDeleteAnnotations(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter removedAnnotationsClassWriter = new ClassWriter(0);
    ClassWriter deletedAnnotationsClassWriter = new ClassWriter(0);
    ClassVisitor removeAnnotationsAdapter =
            new RemoveAnnotationsAdapter(removedAnnotationsClassWriter);
    ClassVisitor deleteAnnotationsAdapter =
            new DeleteAnnotationsAdapter(deletedAnnotationsClassWriter);

    Executable removeAnnotations = () -> classReader.accept(removeAnnotationsAdapter, 0);
    Executable deleteAnnotations = () -> classReader.accept(deleteAnnotationsAdapter, 0);

    assertDoesNotThrow(removeAnnotations);
    assertDoesNotThrow(deleteAnnotations);
    assertEquals(
            new ClassFile(removedAnnotationsClassWriter.toByteArray()),
            new ClassFile(deletedAnnotationsClassWriter.toByteArray())
    );
  }

  private static class EmptyAnnotationVisitor extends AnnotationVisitor {

    EmptyAnnotationVisitor() {
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
      return this;
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
      return this;
    }
  }

  private static class RemoveAnnotationsAdapter extends ClassVisitor {

    RemoveAnnotationsAdapter(final ClassVisitor classVisitor) {
      super(classVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
      return new EmptyAnnotationVisitor();
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef,
            final TypePath typePath,
            final String descriptor,
            final boolean visible) {
      return new EmptyAnnotationVisitor();
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      return new MethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions)) {

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
          return new EmptyAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
          return new EmptyAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return new EmptyAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
                final int parameter, final String descriptor, final boolean visible) {
          return new EmptyAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return new EmptyAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return new EmptyAnnotationVisitor();
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
          return new EmptyAnnotationVisitor();
        }
      };
    }
  }

  private static class DeleteAnnotationsAdapter extends ClassVisitor {

    DeleteAnnotationsAdapter(final ClassVisitor classVisitor) {
      super(classVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
      return null;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef,
            final TypePath typePath,
            final String descriptor,
            final boolean visible) {
      return null;
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      return new MethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions)) {

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
          return null;
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
          return null;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return null;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
                final int parameter, final String descriptor, final boolean visible) {
          return null;
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return null;
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(
                final int typeRef,
                final TypePath typePath,
                final String descriptor,
                final boolean visible) {
          return null;
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
          return null;
        }
      };
    }
  }
}
