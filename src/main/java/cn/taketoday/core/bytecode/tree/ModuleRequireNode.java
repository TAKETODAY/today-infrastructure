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
package cn.taketoday.core.bytecode.tree;

import cn.taketoday.core.bytecode.ModuleVisitor;

/**
 * A node that represents a required module with its name and access of a module descriptor.
 *
 * @author Remi Forax
 */
public class ModuleRequireNode {

  /** The fully qualified name (using dots) of the dependence. */
  public String module;

  /**
   * The access flag of the dependence among {@code ACC_TRANSITIVE}, {@code ACC_STATIC_PHASE},
   * {@code ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   */
  public int access;

  /** The module version at compile time, or {@literal null}. */
  public String version;

  /**
   * Constructs a new {@link ModuleRequireNode}.
   *
   * @param module the fully qualified name (using dots) of the dependence.
   * @param access the access flag of the dependence among {@code ACC_TRANSITIVE}, {@code
   * ACC_STATIC_PHASE}, {@code ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   * @param version the module version at compile time, or {@literal null}.
   */
  public ModuleRequireNode(final String module, final int access, final String version) {
    this.module = module;
    this.access = access;
    this.version = version;
  }

  /**
   * Makes the given module visitor visit this require directive.
   *
   * @param moduleVisitor a module visitor.
   */
  public void accept(final ModuleVisitor moduleVisitor) {
    moduleVisitor.visitRequire(module, access, version);
  }
}
