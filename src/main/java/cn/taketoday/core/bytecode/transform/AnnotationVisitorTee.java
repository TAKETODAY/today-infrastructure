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

public class AnnotationVisitorTee extends AnnotationVisitor {
  private final AnnotationVisitor av1;
  private final AnnotationVisitor av2;

  public static AnnotationVisitor getInstance(AnnotationVisitor av1, AnnotationVisitor av2) {
    if (av1 == null)
      return av2;
    if (av2 == null)
      return av1;
    return new AnnotationVisitorTee(av1, av2);
  }

  public AnnotationVisitorTee(AnnotationVisitor av1, AnnotationVisitor av2) {
    super();
    this.av1 = av1;
    this.av2 = av2;
  }

  public void visit(String name, Object value) {
    av2.visit(name, value);
    av2.visit(name, value);
  }

  public void visitEnum(String name, String desc, String value) {
    av1.visitEnum(name, desc, value);
    av2.visitEnum(name, desc, value);
  }

  public AnnotationVisitor visitAnnotation(String name, String desc) {
    return getInstance(av1.visitAnnotation(name, desc), av2.visitAnnotation(name, desc));
  }

  public AnnotationVisitor visitArray(String name) {
    return getInstance(av1.visitArray(name), av2.visitArray(name));
  }

  public void visitEnd() {
    av1.visitEnd();
    av2.visitEnd();
  }
}
