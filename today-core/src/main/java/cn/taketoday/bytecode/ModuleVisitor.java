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

/**
 * A visitor to visit a Java module. The methods of this class must be called in the following
 * order: ( {@code visitMainClass} | ( {@code visitPackage} | {@code visitRequire} | {@code
 * visitExport} | {@code visitOpen} | {@code visitUse} | {@code visitProvide} )* ) {@code visitEnd}.
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public abstract class ModuleVisitor {

  /**
   * The module visitor to which this visitor must delegate method calls. May be
   * null.
   */
  protected ModuleVisitor mv;

  /**
   * Constructs a new {@link ModuleVisitor}.
   */
  public ModuleVisitor() {
    this(null);
  }

  /**
   * Constructs a new {@link ModuleVisitor}.
   *
   * @param moduleVisitor the module visitor to which this visitor must delegate method
   * calls. May be null.
   */
  public ModuleVisitor(final ModuleVisitor moduleVisitor) {
    this.mv = moduleVisitor;
  }

  /**
   * Visit the main class of the current module.
   *
   * @param mainClass the internal name of the main class of the current module.
   */
  public void visitMainClass(final String mainClass) {
    if (mv != null) {
      mv.visitMainClass(mainClass);
    }
  }

  /**
   * Visit a package of the current module.
   *
   * @param packaze the internal name of a package.
   */
  public void visitPackage(final String packaze) {
    if (mv != null) {
      mv.visitPackage(packaze);
    }
  }

  /**
   * Visits a dependence of the current module.
   *
   * @param module the fully qualified name (using dots) of the dependence.
   * @param access the access flag of the dependence among {@code ACC_TRANSITIVE}, {@code
   * ACC_STATIC_PHASE}, {@code ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   * @param version the module version at compile time, or {@literal null}.
   */
  public void visitRequire(final String module, final int access, final String version) {
    if (mv != null) {
      mv.visitRequire(module, access, version);
    }
  }

  /**
   * Visit an exported package of the current module.
   *
   * @param packaze the internal name of the exported package.
   * @param access the access flag of the exported package, valid values are among {@code
   * ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   * @param modules the fully qualified names (using dots) of the modules that can access the public
   * classes of the exported package, or {@literal null}.
   */
  public void visitExport(final String packaze, final int access, final String... modules) {
    if (mv != null) {
      mv.visitExport(packaze, access, modules);
    }
  }

  /**
   * Visit an open package of the current module.
   *
   * @param packaze the internal name of the opened package.
   * @param access the access flag of the opened package, valid values are among {@code
   * ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   * @param modules the fully qualified names (using dots) of the modules that can use deep
   * reflection to the classes of the open package, or {@literal null}.
   */
  public void visitOpen(final String packaze, final int access, final String... modules) {
    if (mv != null) {
      mv.visitOpen(packaze, access, modules);
    }
  }

  /**
   * Visit a service used by the current module. The name must be the internal name of an interface
   * or a class.
   *
   * @param service the internal name of the service.
   */
  public void visitUse(final String service) {
    if (mv != null) {
      mv.visitUse(service);
    }
  }

  /**
   * Visit an implementation of a service.
   *
   * @param service the internal name of the service.
   * @param providers the internal names of the implementations of the service (there is at least
   * one provider).
   */
  public void visitProvide(final String service, final String... providers) {
    if (mv != null) {
      mv.visitProvide(service, providers);
    }
  }

  /**
   * Visits the end of the module. This method, which is the last one to be called, is used to
   * inform the visitor that everything have been visited.
   */
  public void visitEnd() {
    if (mv != null) {
      mv.visitEnd();
    }
  }
}
