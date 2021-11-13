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

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;

public class MethodFilterTransformer extends AbstractClassTransformer {
  private final MethodFilter filter;
  private final ClassTransformer pass;
  private ClassVisitor direct;

  public MethodFilterTransformer(MethodFilter filter, ClassTransformer pass) {
    this.filter = filter;
    this.pass = pass;
    super.setTarget(pass);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    return (filter.accept(access, name, desc, signature, exceptions) ? pass : direct).visitMethod(access, name,
                                                                                                  desc, signature, exceptions);
  }

  public void setTarget(ClassVisitor target) {
    pass.setTarget(target);
    direct = target;
  }
}
