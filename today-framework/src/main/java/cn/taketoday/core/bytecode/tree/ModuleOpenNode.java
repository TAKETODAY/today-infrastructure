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

/**
 * A node that represents an opened package with its name and the module that can access it.
 *
 * @author Remi Forax
 */
public class ModuleOpenNode {

  /** The internal name of the opened package. */
  public String packaze;

  /**
   * The access flag of the opened package, valid values are among {@code ACC_SYNTHETIC} and {@code
   * ACC_MANDATED}.
   */
  public int access;

  /**
   * The fully qualified names (using dots) of the modules that can use deep reflection to the
   * classes of the open package, or {@literal null}.
   */
  public List<String> modules;

  /**
   * Constructs a new {@link ModuleOpenNode}.
   *
   * @param packaze the internal name of the opened package.
   * @param access the access flag of the opened package, valid values are among {@code
   * ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   * @param modules the fully qualified names (using dots) of the modules that can use deep
   * reflection to the classes of the open package, or {@literal null}.
   */
  public ModuleOpenNode(final String packaze, final int access, final List<String> modules) {
    this.packaze = packaze;
    this.access = access;
    this.modules = modules;
  }

  /**
   * Makes the given module visitor visit this opened package.
   *
   * @param moduleVisitor a module visitor.
   */
  public void accept(final ModuleVisitor moduleVisitor) {
    moduleVisitor.visitOpen(
            packaze, access, modules == null ? null : modules.toArray(new String[0]));
  }
}
