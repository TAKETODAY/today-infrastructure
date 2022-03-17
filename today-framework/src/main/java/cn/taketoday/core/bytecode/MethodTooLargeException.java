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
package cn.taketoday.core.bytecode;

/**
 * Exception thrown when the Code attribute of a method produced by a {@link ClassWriter} is too
 * large.
 *
 * @author Jason Zaugg
 */
public final class MethodTooLargeException extends IndexOutOfBoundsException {
  private static final long serialVersionUID = 6807380416709738314L;

  private final String className;
  private final String methodName;
  private final String descriptor;
  private final int codeSize;

  /**
   * Constructs a new {@link MethodTooLargeException}.
   *
   * @param className the internal name of the owner class.
   * @param methodName the name of the method.
   * @param descriptor the descriptor of the method.
   * @param codeSize the size of the method's Code attribute, in bytes.
   */
  public MethodTooLargeException(
          final String className,
          final String methodName,
          final String descriptor,
          final int codeSize) {
    super("Method too large: " + className + "." + methodName + " " + descriptor);
    this.className = className;
    this.methodName = methodName;
    this.descriptor = descriptor;
    this.codeSize = codeSize;
  }

  /**
   * Returns the internal name of the owner class.
   *
   * @return the internal name of the owner class.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Returns the name of the method.
   *
   * @return the name of the method.
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * Returns the descriptor of the method.
   *
   * @return the descriptor of the method.
   */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * Returns the size of the method's Code attribute, in bytes.
   *
   * @return the size of the method's Code attribute, in bytes.
   */
  public int getCodeSize() {
    return codeSize;
  }
}
