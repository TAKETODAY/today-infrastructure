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
package infra.bytecode.util;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.Attribute;
import infra.bytecode.Opcodes;
import infra.bytecode.RecordComponentVisitor;
import infra.bytecode.TypePath;
import infra.bytecode.TypeReference;

/**
 * A {@link RecordComponentVisitor} that checks that its methods are properly used.
 *
 * @author Eric Bruneton
 * @author Remi Forax
 */
public class CheckRecordComponentAdapter extends RecordComponentVisitor {

  /** Whether the {@link #visitEnd()} method has been called. */
  private boolean visitEndCalled;

  /**
   * Constructs a new {@link CheckRecordComponentAdapter}.
   *
   * @param recordComponentVisitor the record component visitor to which this adapter must delegate
   * calls.
   */
  public CheckRecordComponentAdapter(final RecordComponentVisitor recordComponentVisitor) {
    super(recordComponentVisitor);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    checkVisitEndNotCalled();
    // Annotations can only appear in V1_5 or more classes.
    CheckMethodAdapter.checkDescriptor(Opcodes.V1_5, descriptor, false);
    return new CheckAnnotationAdapter(super.visitAnnotation(descriptor, visible));
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    checkVisitEndNotCalled();
    int sort = new TypeReference(typeRef).getSort();
    if (sort != TypeReference.FIELD) {
      throw new IllegalArgumentException(
              "Invalid type reference sort 0x" + Integer.toHexString(sort));
    }
    CheckClassAdapter.checkTypeRef(typeRef);
    CheckMethodAdapter.checkDescriptor(Opcodes.V1_5, descriptor, false);
    return new CheckAnnotationAdapter(
            super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    checkVisitEndNotCalled();
    if (attribute == null) {
      throw new IllegalArgumentException("Invalid attribute (must not be null)");
    }
    super.visitAttribute(attribute);
  }

  @Override
  public void visitEnd() {
    checkVisitEndNotCalled();
    visitEndCalled = true;
    super.visitEnd();
  }

  private void checkVisitEndNotCalled() {
    if (visitEndCalled) {
      throw new IllegalStateException("Cannot call a visit method after visitEnd has been called");
    }
  }
}
