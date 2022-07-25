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
package cn.taketoday.bytecode.util;

import cn.taketoday.bytecode.ModuleVisitor;

/**
 * A {@link ModuleVisitor} that prints the fields it visits with a {@link Printer}.
 *
 * @author Remi Forax
 */
public final class TraceModuleVisitor extends ModuleVisitor {

  /** The printer to convert the visited module into text. */
  // DontCheck(MemberName): can't be renamed (for backward binary compatibility).
  public final Printer p;

  /**
   * Constructs a new {@link TraceModuleVisitor}.
   *
   * @param printer the printer to convert the visited module into text.
   */
  public TraceModuleVisitor(final Printer printer) {
    this(null, printer);
  }

  /**
   * Constructs a new {@link TraceModuleVisitor}.
   *
   * @param moduleVisitor the module visitor to which to delegate calls. May be {@literal null}.
   * @param printer the printer to convert the visited module into text.
   */
  public TraceModuleVisitor(final ModuleVisitor moduleVisitor, final Printer printer) {
    super(moduleVisitor);
    this.p = printer;
  }

  @Override
  public void visitMainClass(final String mainClass) {
    p.visitMainClass(mainClass);
    super.visitMainClass(mainClass);
  }

  @Override
  public void visitPackage(final String packaze) {
    p.visitPackage(packaze);
    super.visitPackage(packaze);
  }

  @Override
  public void visitRequire(final String module, final int access, final String version) {
    p.visitRequire(module, access, version);
    super.visitRequire(module, access, version);
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    p.visitExport(packaze, access, modules);
    super.visitExport(packaze, access, modules);
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    p.visitOpen(packaze, access, modules);
    super.visitOpen(packaze, access, modules);
  }

  @Override
  public void visitUse(final String use) {
    p.visitUse(use);
    super.visitUse(use);
  }

  @Override
  public void visitProvide(final String service, final String... providers) {
    p.visitProvide(service, providers);
    super.visitProvide(service, providers);
  }

  @Override
  public void visitEnd() {
    p.visitModuleEnd();
    super.visitEnd();
  }
}
