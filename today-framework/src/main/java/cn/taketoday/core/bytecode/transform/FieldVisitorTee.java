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
package cn.taketoday.core.bytecode.transform;

import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.Attribute;
import cn.taketoday.core.bytecode.FieldVisitor;
import cn.taketoday.core.bytecode.TypePath;

public class FieldVisitorTee extends FieldVisitor {

  private final FieldVisitor fv1, fv2;

  public FieldVisitorTee(FieldVisitor fv1, FieldVisitor fv2) {
//		super(Constant.ASM_API);
    this.fv1 = fv1;
    this.fv2 = fv2;
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return AnnotationVisitorTee.getInstance(fv1.visitAnnotation(desc, visible), fv2.visitAnnotation(desc, visible));
  }

  public void visitAttribute(Attribute attr) {
    fv1.visitAttribute(attr);
    fv2.visitAttribute(attr);
  }

  public void visitEnd() {
    fv1.visitEnd();
    fv2.visitEnd();
  }

  public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
    return AnnotationVisitorTee.getInstance(fv1.visitTypeAnnotation(typeRef, typePath, desc, visible),
                                            fv2.visitTypeAnnotation(typeRef, typePath, desc, visible));
  }
}
