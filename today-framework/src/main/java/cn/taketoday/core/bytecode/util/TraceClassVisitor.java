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
package cn.taketoday.core.bytecode.util;

import java.io.PrintWriter;

import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.Attribute;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.FieldVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.ModuleVisitor;
import cn.taketoday.core.bytecode.RecordComponentVisitor;
import cn.taketoday.core.bytecode.TypePath;

/**
 * A {@link ClassVisitor} that prints the classes it visits with a {@link Printer}. This class
 * visitor can be used in the middle of a class visitor chain to trace the class that is visited at
 * a given point in this chain. This may be useful for debugging purposes.
 *
 * <p>When used with a {@link Textifier}, the trace printed when visiting the {@code Hello} class is
 * the following:
 *
 * <pre>
 * // class version 49.0 (49) // access flags 0x21 public class Hello {
 *
 * // compiled from: Hello.java
 *
 * // access flags 0x1
 * public &lt;init&gt; ()V
 * ALOAD 0
 * INVOKESPECIAL java/lang/Object &lt;init&gt; ()V
 * RETURN
 * MAXSTACK = 1 MAXLOCALS = 1
 *
 * // access flags 0x9
 * public static main ([Ljava/lang/String;)V
 * GETSTATIC java/lang/System out Ljava/io/PrintStream;
 * LDC &quot;hello&quot;
 * INVOKEVIRTUAL java/io/PrintStream println (Ljava/lang/String;)V
 * RETURN
 * MAXSTACK = 2 MAXLOCALS = 1
 * }
 * </pre>
 *
 * <p>where {@code Hello} is defined by:
 *
 * <pre>
 * public class Hello {
 *
 *   public static void main(String[] args) {
 *     System.out.println(&quot;hello&quot;);
 *   }
 * }
 * </pre>
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public final class TraceClassVisitor extends ClassVisitor {

  /** The print writer to be used to print the class. May be {@literal null}. */
  private final PrintWriter printWriter;

  /** The printer to convert the visited class into text. */
  // DontCheck(MemberName): can't be renamed (for backward binary compatibility).
  public final Printer p;

  /**
   * Constructs a new {@link TraceClassVisitor}.
   *
   * @param printWriter the print writer to be used to print the class. May be {@literal null}.
   */
  public TraceClassVisitor(final PrintWriter printWriter) {
    this(null, printWriter);
  }

  /**
   * Constructs a new {@link TraceClassVisitor}.
   *
   * @param classVisitor the class visitor to which to delegate calls. May be {@literal null}.
   * @param printWriter the print writer to be used to print the class. May be {@literal null}.
   */
  public TraceClassVisitor(final ClassVisitor classVisitor, final PrintWriter printWriter) {
    this(classVisitor, new Textifier(), printWriter);
  }

  /**
   * Constructs a new {@link TraceClassVisitor}.
   *
   * @param classVisitor the class visitor to which to delegate calls. May be {@literal null}.
   * @param printer the printer to convert the visited class into text.
   * @param printWriter the print writer to be used to print the class. May be {@literal null}.
   */
  public TraceClassVisitor(
          final ClassVisitor classVisitor, final Printer printer, final PrintWriter printWriter) {
    super(classVisitor);
    this.printWriter = printWriter;
    this.p = printer;
  }

  @Override
  public void visit(
          final int version,
          final int access,
          final String name,
          final String signature,
          final String superName,
          final String[] interfaces) {
    p.visit(version, access, name, signature, superName, interfaces);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public void visitSource(final String file, final String debug) {
    p.visitSource(file, debug);
    super.visitSource(file, debug);
  }

  @Override
  public ModuleVisitor visitModule(final String name, final int flags, final String version) {
    Printer modulePrinter = p.visitModule(name, flags, version);
    return new TraceModuleVisitor(super.visitModule(name, flags, version), modulePrinter);
  }

  @Override
  public void visitNestHost(final String nestHost) {
    p.visitNestHost(nestHost);
    super.visitNestHost(nestHost);
  }

  @Override
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    p.visitOuterClass(owner, name, descriptor);
    super.visitOuterClass(owner, name, descriptor);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitClassAnnotation(descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitAnnotation(descriptor, visible), annotationPrinter);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitClassTypeAnnotation(typeRef, typePath, descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitTypeAnnotation(typeRef, typePath, descriptor, visible), annotationPrinter);
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    p.visitClassAttribute(attribute);
    super.visitAttribute(attribute);
  }

  @Override
  public void visitNestMember(final String nestMember) {
    p.visitNestMember(nestMember);
    super.visitNestMember(nestMember);
  }

  @Override
  public void visitPermittedSubclass(final String permittedSubclass) {
    p.visitPermittedSubclass(permittedSubclass);
    super.visitPermittedSubclass(permittedSubclass);
  }

  @Override
  public void visitInnerClass(
          final String name, final String outerName, final String innerName, final int access) {
    p.visitInnerClass(name, outerName, innerName, access);
    super.visitInnerClass(name, outerName, innerName, access);
  }

  @Override
  public RecordComponentVisitor visitRecordComponent(
          final String name, final String descriptor, final String signature) {
    Printer recordComponentPrinter = p.visitRecordComponent(name, descriptor, signature);
    return new TraceRecordComponentVisitor(
            super.visitRecordComponent(name, descriptor, signature), recordComponentPrinter);
  }

  @Override
  public FieldVisitor visitField(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final Object value) {
    Printer fieldPrinter = p.visitField(access, name, descriptor, signature, value);
    return new TraceFieldVisitor(
            super.visitField(access, name, descriptor, signature, value), fieldPrinter);
  }

  @Override
  public MethodVisitor visitMethod(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final String[] exceptions) {
    Printer methodPrinter = p.visitMethod(access, name, descriptor, signature, exceptions);
    return new TraceMethodVisitor(
            super.visitMethod(access, name, descriptor, signature, exceptions), methodPrinter);
  }

  @Override
  public void visitEnd() {
    p.visitClassEnd();
    if (printWriter != null) {
      p.print(printWriter);
      printWriter.flush();
    }
    super.visitEnd();
  }
}
