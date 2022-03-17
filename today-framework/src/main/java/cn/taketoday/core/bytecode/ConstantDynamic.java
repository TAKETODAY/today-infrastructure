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

import java.util.Arrays;

/**
 * A constant whose value is computed at runtime, with a bootstrap method.
 *
 * @author Remi Forax
 */
public final class ConstantDynamic {

  /** The constant name (can be arbitrary). */
  private final String name;

  /** The constant type (must be a field descriptor). */
  private final String descriptor;

  /** The bootstrap method to use to compute the constant value at runtime. */
  private final Handle bootstrapMethod;

  /**
   * The arguments to pass to the bootstrap method, in order to compute the constant value at
   * runtime.
   */
  private final Object[] bootstrapMethodArguments;

  /**
   * Constructs a new {@link ConstantDynamic}.
   *
   * @param name the constant name (can be arbitrary).
   * @param descriptor the constant type (must be a field descriptor).
   * @param bootstrapMethod the bootstrap method to use to compute the constant value at runtime.
   * @param bootstrapMethodArguments the arguments to pass to the bootstrap method, in order to
   * compute the constant value at runtime.
   */
  public ConstantDynamic(
          final String name,
          final String descriptor,
          final Handle bootstrapMethod,
          final Object... bootstrapMethodArguments) {
    this.name = name;
    this.descriptor = descriptor;
    this.bootstrapMethod = bootstrapMethod;
    this.bootstrapMethodArguments = bootstrapMethodArguments;
  }

  /**
   * Returns the name of this constant.
   *
   * @return the name of this constant.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the type of this constant.
   *
   * @return the type of this constant, as a field descriptor.
   */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * Returns the bootstrap method used to compute the value of this constant.
   *
   * @return the bootstrap method used to compute the value of this constant.
   */
  public Handle getBootstrapMethod() {
    return bootstrapMethod;
  }

  /**
   * Returns the number of arguments passed to the bootstrap method, in order to compute the value
   * of this constant.
   *
   * @return the number of arguments passed to the bootstrap method, in order to compute the value
   * of this constant.
   */
  public int getBootstrapMethodArgumentCount() {
    return bootstrapMethodArguments.length;
  }

  /**
   * Returns an argument passed to the bootstrap method, in order to compute the value of this
   * constant.
   *
   * @param index an argument index, between 0 and {@link #getBootstrapMethodArgumentCount()}
   * (exclusive).
   * @return the argument passed to the bootstrap method, with the given index.
   */
  public Object getBootstrapMethodArgument(final int index) {
    return bootstrapMethodArguments[index];
  }

  /**
   * Returns the arguments to pass to the bootstrap method, in order to compute the value of this
   * constant. WARNING: this array must not be modified, and must not be returned to the user.
   *
   * @return the arguments to pass to the bootstrap method, in order to compute the value of this
   * constant.
   */
  Object[] getBootstrapMethodArgumentsUnsafe() {
    return bootstrapMethodArguments;
  }

  /**
   * Returns the size of this constant.
   *
   * @return the size of this constant, i.e., 2 for {@code long} and {@code double}, 1 otherwise.
   */
  public int getSize() {
    char firstCharOfDescriptor = descriptor.charAt(0);
    return (firstCharOfDescriptor == 'J' || firstCharOfDescriptor == 'D') ? 2 : 1;
  }

  @Override
  public boolean equals(final Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof ConstantDynamic constantDynamic)) {
      return false;
    }
    return name.equals(constantDynamic.name)
            && descriptor.equals(constantDynamic.descriptor)
            && bootstrapMethod.equals(constantDynamic.bootstrapMethod)
            && Arrays.equals(bootstrapMethodArguments, constantDynamic.bootstrapMethodArguments);
  }

  @Override
  public int hashCode() {
    return name.hashCode()
            ^ Integer.rotateLeft(descriptor.hashCode(), 8)
            ^ Integer.rotateLeft(bootstrapMethod.hashCode(), 16)
            ^ Integer.rotateLeft(Arrays.hashCode(bootstrapMethodArguments), 24);
  }

  @Override
  public String toString() {
    return name
            + " : "
            + descriptor
            + ' '
            + bootstrapMethod
            + ' '
            + Arrays.toString(bootstrapMethodArguments);
  }
}
