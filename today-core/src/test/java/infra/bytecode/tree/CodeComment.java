/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package infra.bytecode.tree;

import infra.bytecode.Attribute;
import infra.bytecode.ByteVector;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassWriter;
import infra.bytecode.Label;

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
}
