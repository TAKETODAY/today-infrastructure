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

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.MethodVisitor;

public class ClassTransformerChain extends AbstractClassTransformer {

  private final ClassTransformer[] chain;

  public ClassTransformerChain(ClassTransformer[] chain) {
    this.chain = chain.clone();
  }

  public void setTarget(ClassVisitor v) {
    final ClassTransformer[] chain = this.chain;
    super.setTarget(chain[0]);
    ClassVisitor next = v;
    for (int i = chain.length - 1; i >= 0; i--) {
      chain[i].setTarget(next);
      next = chain[i];
    }
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    return cv.visitMethod(access, name, desc, signature, exceptions);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ClassTransformerChain{");
    final ClassTransformer[] chain = this.chain;
    for (int i = 0; i < chain.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(chain[i].toString());
    }
    sb.append("}");
    return sb.toString();
  }
}
