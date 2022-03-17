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

import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.Attribute;
import cn.taketoday.core.bytecode.RecordComponentVisitor;
import cn.taketoday.core.bytecode.TypePath;

/**
 * A {@link RecordComponentVisitor} that prints the record components it visits with a {@link
 * Printer}.
 *
 * @author Remi Forax
 */
public final class TraceRecordComponentVisitor extends RecordComponentVisitor {

  /** The printer to convert the visited record component into text. */
  public final Printer printer;

  /**
   * Constructs a new {@link TraceRecordComponentVisitor}.
   *
   * @param printer the printer to convert the visited record component into text.
   */
  public TraceRecordComponentVisitor(final Printer printer) {
    this(null, printer);
  }

  /**
   * Constructs a new {@link TraceRecordComponentVisitor}.
   *
   * @param recordComponentVisitor the record component visitor to which to delegate calls. May be
   * {@literal null}.
   * @param printer the printer to convert the visited record component into text.
   */
  public TraceRecordComponentVisitor(
          final RecordComponentVisitor recordComponentVisitor, final Printer printer) {
    super(recordComponentVisitor);
    this.printer = printer;
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    Printer annotationPrinter = printer.visitRecordComponentAnnotation(descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitAnnotation(descriptor, visible), annotationPrinter);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    Printer annotationPrinter =
            printer.visitRecordComponentTypeAnnotation(typeRef, typePath, descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitTypeAnnotation(typeRef, typePath, descriptor, visible), annotationPrinter);
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    printer.visitRecordComponentAttribute(attribute);
    super.visitAttribute(attribute);
  }

  @Override
  public void visitEnd() {
    printer.visitRecordComponentEnd();
    super.visitEnd();
  }
}
