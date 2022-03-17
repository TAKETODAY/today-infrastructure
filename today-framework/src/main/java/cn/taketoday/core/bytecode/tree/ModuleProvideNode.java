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
 * A node that represents a service and its implementation provided by the current module.
 *
 * @author Remi Forax
 */
public class ModuleProvideNode {

  /** The internal name of the service. */
  public String service;

  /** The internal names of the implementations of the service (there is at least one provider). */
  public List<String> providers;

  /**
   * Constructs a new {@link ModuleProvideNode}.
   *
   * @param service the internal name of the service.
   * @param providers the internal names of the implementations of the service (there is at least
   * one provider).
   */
  public ModuleProvideNode(final String service, final List<String> providers) {
    this.service = service;
    this.providers = providers;
  }

  /**
   * Makes the given module visitor visit this require declaration.
   *
   * @param moduleVisitor a module visitor.
   */
  public void accept(final ModuleVisitor moduleVisitor) {
    moduleVisitor.visitProvide(service, providers.toArray(new String[0]));
  }
}
