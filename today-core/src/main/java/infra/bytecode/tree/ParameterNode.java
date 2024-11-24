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

import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;

/**
 * A node that represents a parameter of a method.
 *
 * @author Remi Forax
 */
public class ParameterNode {

  /** The parameter's name. */
  public String name;

  /**
   * The parameter's access flags (see {@link Opcodes}). Valid values are {@code
   * ACC_FINAL}, {@code ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   */
  public int access;

  /**
   * Constructs a new {@link ParameterNode}.
   *
   * @param access The parameter's access flags. Valid values are {@code ACC_FINAL}, {@code
   * ACC_SYNTHETIC} or/and {@code ACC_MANDATED} (see {@link Opcodes}).
   * @param name the parameter's name.
   */
  public ParameterNode(final String name, final int access) {
    this.name = name;
    this.access = access;
  }

  /**
   * Makes the given visitor visit this parameter declaration.
   *
   * @param methodVisitor a method visitor.
   */
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitParameter(name, access);
  }
}
