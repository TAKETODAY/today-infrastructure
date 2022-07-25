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
package cn.taketoday.bytecode;

import java.io.Serial;

/**
 * Exception thrown when the constant pool of a class produced by a {@link ClassWriter} is too
 * large.
 *
 * @author Jason Zaugg
 */
public final class ClassTooLargeException extends IndexOutOfBoundsException {
  @Serial
  private static final long serialVersionUID = 160715609518896765L;

  private final String className;
  private final int constantPoolCount;

  /**
   * Constructs a new {@link ClassTooLargeException}.
   *
   * @param className the internal name of the class.
   * @param constantPoolCount the number of constant pool items of the class.
   */
  public ClassTooLargeException(final String className, final int constantPoolCount) {
    super("Class too large: " + className);
    this.className = className;
    this.constantPoolCount = constantPoolCount;
  }

  /**
   * Returns the internal name of the class.
   *
   * @return the internal name of the class.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Returns the number of constant pool items of the class.
   *
   * @return the number of constant pool items of the class.
   */
  public int getConstantPoolCount() {
    return constantPoolCount;
  }
}
