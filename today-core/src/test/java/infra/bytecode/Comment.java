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
package infra.bytecode;

/**
 * A non standard attribute used for testing purposes.
 *
 * @author Eric Bruneton
 */
public class Comment extends Attribute {

  public Comment() {
    super("Comment");
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  protected Attribute read(
          final ClassReader classReader,
          final int offset,
          final int length,
          final char[] charBuffer,
          final int codeAttributeOffset,
          final Label[] labels) {
    return new Comment();
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
