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

import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ByteVector;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Label;

/**
 * A non standard code attribute used for testing purposes.
 *
 * @author Eric Bruneton
 */
public class CodeComment extends Attribute {

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
          final int codeOffset,
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
  protected Label[] getLabels() {
    super.getLabels();
    return new Label[] { new Label() };
  }
}
