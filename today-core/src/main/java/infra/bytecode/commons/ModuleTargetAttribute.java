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

package infra.bytecode.commons;

import infra.bytecode.Attribute;
import infra.bytecode.ByteVector;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.bytecode.ClassWriter;
import infra.bytecode.Label;

/**
 * A ModuleTarget attribute. This attribute is specific to the OpenJDK and may change in the future.
 *
 * @author Remi Forax
 */
public final class ModuleTargetAttribute extends Attribute {

  /** The name of the platform on which the module can run. */
  public String platform;

  /**
   * Constructs a new {@link ModuleTargetAttribute}.
   *
   * @param platform the name of the platform on which the module can run.
   */
  public ModuleTargetAttribute(final String platform) {
    super("ModuleTarget");
    this.platform = platform;
  }

  /**
   * Constructs an empty {@link ModuleTargetAttribute}. This object can be passed as a prototype to
   * the {@link ClassReader#accept(ClassVisitor, Attribute[], int)} method.
   */
  public ModuleTargetAttribute() {
    this(null);
  }

  @Override
  protected Attribute read(
          final ClassReader classReader,
          final int offset,
          final int length,
          final char[] charBuffer,
          final int codeOffset,
          final Label[] labels) {
    return new ModuleTargetAttribute(classReader.readUTF8(offset, charBuffer));
  }

  @Override
  protected ByteVector write(
          final ClassWriter classWriter,
          final byte[] code,
          final int codeLength,
          final int maxStack,
          final int maxLocals) {
    ByteVector byteVector = new ByteVector();
    byteVector.putShort(platform == null ? 0 : classWriter.newUTF8(platform));
    return byteVector;
  }
}
