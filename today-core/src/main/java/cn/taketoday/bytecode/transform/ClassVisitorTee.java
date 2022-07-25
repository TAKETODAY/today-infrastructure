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
package cn.taketoday.bytecode.transform;

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.FieldVisitor;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.TypePath;

public class ClassVisitorTee extends ClassVisitor {

  private ClassVisitor cv1, cv2;

  public ClassVisitorTee(ClassVisitor cv1, ClassVisitor cv2) {
//		super(Constant.ASM_API);
    this.cv1 = cv1;
    this.cv2 = cv2;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    cv1.visit(version, access, name, signature, superName, interfaces);
    cv2.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitEnd() {
    cv1.visitEnd();
    cv2.visitEnd();
    cv1 = cv2 = null;
  }

  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    cv1.visitInnerClass(name, outerName, innerName, access);
    cv2.visitInnerClass(name, outerName, innerName, access);
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    FieldVisitor fv1 = cv1.visitField(access, name, desc, signature, value);
    FieldVisitor fv2 = cv2.visitField(access, name, desc, signature, value);
    if (fv1 == null)
      return fv2;
    if (fv2 == null)
      return fv1;
    return new FieldVisitorTee(fv1, fv2);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv1 = cv1.visitMethod(access, name, desc, signature, exceptions);
    MethodVisitor mv2 = cv2.visitMethod(access, name, desc, signature, exceptions);
    if (mv1 == null)
      return mv2;
    if (mv2 == null)
      return mv1;
    return new MethodVisitorTee(mv1, mv2);
  }

  public void visitSource(String source, String debug) {
    cv1.visitSource(source, debug);
    cv2.visitSource(source, debug);
  }

  public void visitOuterClass(String owner, String name, String desc) {
    cv1.visitOuterClass(owner, name, desc);
    cv2.visitOuterClass(owner, name, desc);
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return AnnotationVisitorTee.getInstance(
            cv1.visitAnnotation(desc, visible), cv2.visitAnnotation(desc, visible));
  }

  public void visitAttribute(Attribute attrs) {
    cv1.visitAttribute(attrs);
    cv2.visitAttribute(attrs);
  }

  public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
    return AnnotationVisitorTee.getInstance(
            cv1.visitTypeAnnotation(typeRef, typePath, desc, visible),
            cv2.visitTypeAnnotation(typeRef, typePath, desc, visible));
  }
}
