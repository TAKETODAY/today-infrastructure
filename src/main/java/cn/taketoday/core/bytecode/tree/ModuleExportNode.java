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

import java.util.List;

import cn.taketoday.core.bytecode.ModuleVisitor;
import cn.taketoday.core.bytecode.Opcodes;

/**
 * A node that represents an exported package with its name and the module that can access to it.
 *
 * @author Remi Forax
 */
public class ModuleExportNode {

  /** The internal name of the exported package. */
  public String packaze;

  /**
   * The access flags (see {@link Opcodes}). Valid values are {@code
   * ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   */
  public int access;

  /**
   * The list of modules that can access this exported package, specified with fully qualified names
   * (using dots). May be {@literal null}.
   */
  public List<String> modules;

  /**
   * Constructs a new {@link ModuleExportNode}.
   *
   * @param packaze the internal name of the exported package.
   * @param access the package access flags, one or more of {@code ACC_SYNTHETIC} and {@code
   * ACC_MANDATED}.
   * @param modules a list of modules that can access this exported package, specified with fully
   * qualified names (using dots).
   */
  public ModuleExportNode(final String packaze, final int access, final List<String> modules) {
    this.packaze = packaze;
    this.access = access;
    this.modules = modules;
  }

  /**
   * Makes the given module visitor visit this export declaration.
   *
   * @param moduleVisitor a module visitor.
   */
  public void accept(final ModuleVisitor moduleVisitor) {
    moduleVisitor.visitExport(
            packaze, access, modules == null ? null : modules.toArray(new String[0]));
  }
}
