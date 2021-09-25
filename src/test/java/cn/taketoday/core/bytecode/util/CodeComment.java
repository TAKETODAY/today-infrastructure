/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.core.bytecode.Attribute;
import cn.taketoday.core.bytecode.ByteVector;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.ClassWriter;
import cn.taketoday.core.bytecode.Label;

import java.util.Map;

/**
 * A non standard code attribute used for testing purposes.
 *
 * @author Eric Bruneton
 */
public class CodeComment extends Attribute implements ASMifierSupport, TextifierSupport {

  public CodeComment() {
    super("CodeComment");
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public boolean isCodeAttribute() {
    return true;
  }

  @Override
  protected Attribute read(
          final ClassReader classReader,
          final int offset,
          final int length,
          final char[] charBuffer,
          final int codeAttributeOffset,
          final Label[] labels) {
    return new CodeComment();
  }

  @Override
  protected ByteVector write(
          final ClassWriter classWriter,
          final byte[] code,
          final int codeLength,
          final int maxStack,
          final int maxLocals) {
    return new ByteVector();
  }

  @Override
  public void asmify(
          final StringBuilder stringBuilder,
          final String varName,
          final Map<Label, String> labelNames) {
    stringBuilder
            .append("Attribute ")
            .append(varName)
            .append(" = new cn.taketoday.core.bytecode.util.CodeComment();");
  }

  @Override
  public void textify(final StringBuilder stringBuilder, final Map<Label, String> labelNames) { }
}
