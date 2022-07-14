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
import cn.taketoday.core.bytecode.FieldVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.TypePath;
import cn.taketoday.core.bytecode.TypeReference;

/**
 * A {@link FieldVisitor} that checks that its methods are properly used.
 *
 * @author Eric Bruneton
 */
public class CheckFieldAdapter extends FieldVisitor {

  /** Whether the {@link #visitEnd} method has been called. */
  private boolean visitEndCalled;

  /**
   * Constructs a new {@link CheckFieldAdapter}.
   *
   * @param fieldVisitor the field visitor to which this adapter must delegate calls.
   */
  public CheckFieldAdapter(final FieldVisitor fieldVisitor) {
    super(fieldVisitor);
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
