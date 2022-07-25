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
package cn.taketoday.bytecode.util;

import cn.taketoday.bytecode.AnnotationVisitor;

/**
 * An {@link AnnotationVisitor} that prints the annotations it visits with a {@link Printer}.
 *
 * @author Eric Bruneton
 */
public final class TraceAnnotationVisitor extends AnnotationVisitor {

  /** The printer to convert the visited annotation into text. */
  private final Printer printer;

  /**
   * Constructs a new {@link TraceAnnotationVisitor}.
   *
   * @param printer the printer to convert the visited annotation into text.
   */
  public TraceAnnotationVisitor(final Printer printer) {
    this(null, printer);
  }

  /**
   * Constructs a new {@link TraceAnnotationVisitor}.
   *
   * @param annotationVisitor the annotation visitor to which to delegate calls. May be {@literal
   * null}.
   * @param printer the printer to convert the visited annotation into text.
   */
  public TraceAnnotationVisitor(final AnnotationVisitor annotationVisitor, final Printer printer) {
    super(annotationVisitor);
    this.printer = printer;
  }

  @Override
  public void visit(final String name, final Object value) {
    printer.visit(name, value);
    super.visit(name, value);
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    printer.visitEnum(name, descriptor, value);
    super.visitEnum(name, descriptor, value);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    Printer annotationPrinter = printer.visitAnnotation(name, descriptor);
    return new TraceAnnotationVisitor(super.visitAnnotation(name, descriptor), annotationPrinter);
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    Printer arrayPrinter = printer.visitArray(name);
    return new TraceAnnotationVisitor(super.visitArray(name), arrayPrinter);
  }

  @Override
  public void visitEnd() {
    printer.visitAnnotationEnd();
    super.visitEnd();
  }
}
