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
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.FieldVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;

/**
 * @author TODAY <br>
 * 2019-09-01 21:46
 */
abstract public class AbstractClassFilterTransformer extends AbstractClassTransformer {

  private final ClassTransformer pass;
  private ClassVisitor target;

  public void setTarget(ClassVisitor target) {
    super.setTarget(target);
    pass.setTarget(target);
  }

  protected AbstractClassFilterTransformer(ClassTransformer pass) {
    this.pass = pass;
  }

  abstract protected boolean accept(int version, int access, //
                                    String name, String signature, String superName, String[] interfaces);

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    target = accept(version, access, name, signature, superName, interfaces) ? pass : cv;
    target.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitSource(String source, String debug) {
    target.visitSource(source, debug);
  }

  public void visitOuterClass(String owner, String name, String desc) {
    target.visitOuterClass(owner, name, desc);
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return target.visitAnnotation(desc, visible);
  }

  public void visitAttribute(Attribute attr) {
    target.visitAttribute(attr);
  }

  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    target.visitInnerClass(name, outerName, innerName, access);
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    return target.visitField(access, name, desc, signature, value);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    return target.visitMethod(access, name, desc, signature, exceptions);
  }

  public void visitEnd() {
    target.visitEnd();
    target = null; // just to be safe
  }
}
