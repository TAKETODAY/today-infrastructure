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
package cn.taketoday.bytecode.commons;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;

/**
 * A {@link ClassVisitor} that merges &lt;clinit&gt; methods into a single one. All the existing
 * &lt;clinit&gt; methods are renamed, and a new one is created, which calls all the renamed
 * methods.
 *
 * @author Eric Bruneton
 */
public class StaticInitMerger extends ClassVisitor {

  /** The internal name of the visited class. */
  private String owner;

  /** The prefix to use to rename the existing &lt;clinit&gt; methods. */
  private final String renamedClinitMethodPrefix;

  /** The number of &lt;clinit&gt; methods visited so far. */
  private int numClinitMethods;

  /** The MethodVisitor for the merged &lt;clinit&gt; method. */
  private MethodVisitor mergedClinitVisitor;

  /**
   * Constructs a new {@link StaticInitMerger}.
   *
   * @param prefix the prefix to use to rename the existing &lt;clinit&gt; methods.
   * @param classVisitor the class visitor to which this visitor must delegate method calls. May be
   * null.
   */
  public StaticInitMerger(final String prefix, final ClassVisitor classVisitor) {
    super(classVisitor);
    this.renamedClinitMethodPrefix = prefix;
  }

  @Override
  public void visit(
          final int version,
          final int access,
          final String name,
          final String signature,
          final String superName,
          final String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.owner = name;
  }

  @Override
  public MethodVisitor visitMethod(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final String[] exceptions) {
    MethodVisitor methodVisitor;
    if ("<clinit>".equals(name)) {
      int newAccess = Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC;
      String newName = renamedClinitMethodPrefix + numClinitMethods++;
      methodVisitor = super.visitMethod(newAccess, newName, descriptor, signature, exceptions);

      MethodVisitor mergedClinitVisitor = this.mergedClinitVisitor;
      if (mergedClinitVisitor == null) {
        mergedClinitVisitor = super.visitMethod(newAccess, name, descriptor, null, null);
        this.mergedClinitVisitor = mergedClinitVisitor;
      }
      mergedClinitVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner, newName, descriptor, false);
    }
    else {
      methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
    }
    return methodVisitor;
  }

  @Override
  public void visitEnd() {
    MethodVisitor mergedClinitVisitor = this.mergedClinitVisitor;
    if (mergedClinitVisitor != null) {
      mergedClinitVisitor.visitInsn(Opcodes.RETURN);
      mergedClinitVisitor.visitMaxs(0, 0);
    }
    super.visitEnd();
  }
}
