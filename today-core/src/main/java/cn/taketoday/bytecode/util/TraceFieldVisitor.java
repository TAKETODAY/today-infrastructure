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
package cn.taketoday.bytecode.util;

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.FieldVisitor;
import cn.taketoday.bytecode.TypePath;
import cn.taketoday.lang.Nullable;

/**
 * A {@link FieldVisitor} that prints the fields it visits with a {@link Printer}.
 *
 * @author Eric Bruneton
 */
public final class TraceFieldVisitor extends FieldVisitor {

  /** The printer to convert the visited field into text. */
  // DontCheck(MemberName): can't be renamed (for backward binary compatibility).
  public final Printer p;

  /**
   * Constructs a new {@link TraceFieldVisitor}.
   *
   * @param printer the printer to convert the visited field into text.
   */
  public TraceFieldVisitor(final Printer printer) {
    this(null, printer);
  }

  /**
   * Constructs a new {@link TraceFieldVisitor}.
   *
   * @param fieldVisitor the field visitor to which to delegate calls. May be {@literal null}.
   * @param printer the printer to convert the visited field into text.
   */
  public TraceFieldVisitor(@Nullable FieldVisitor fieldVisitor, final Printer printer) {
    super(fieldVisitor);
    this.p = printer;
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitFieldAnnotation(descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitAnnotation(descriptor, visible), annotationPrinter);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitFieldTypeAnnotation(typeRef, typePath, descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitTypeAnnotation(typeRef, typePath, descriptor, visible), annotationPrinter);
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    p.visitFieldAttribute(attribute);
    super.visitAttribute(attribute);
  }

  @Override
  public void visitEnd() {
    p.visitFieldEnd();
    super.visitEnd();
  }
}
