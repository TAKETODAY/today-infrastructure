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
package cn.taketoday.core.bytecode.util;

import java.util.HashSet;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.ModuleVisitor;
import cn.taketoday.core.bytecode.Opcodes;

/**
 * A {@link ModuleVisitor} that checks that its methods are properly used.
 *
 * @author Remi Forax
 */
public class CheckModuleAdapter extends ModuleVisitor {
  /** Whether the visited module is open. */
  private final boolean isOpen;

  /** The fully qualified names of the dependencies of the visited module. */
  private final NameSet requiredModules = new NameSet("Modules requires");

  /** The internal names of the packages exported by the visited module. */
  private final NameSet exportedPackages = new NameSet("Module exports");

  /** The internal names of the packages opened by the visited module. */
  private final NameSet openedPackages = new NameSet("Module opens");

  /** The internal names of the services used by the visited module. */
  private final NameSet usedServices = new NameSet("Module uses");

  /** The internal names of the services provided by the visited module. */
  private final NameSet providedServices = new NameSet("Module provides");

  /** The class version number. */
  int classVersion;

  /** Whether the {@link #visitEnd} method has been called. */
  private boolean visitEndCalled;

  /**
   * Constructs a new {@link CheckModuleAdapter}.
   *
   * @param moduleVisitor the module visitor to which this adapter must delegate calls.
   * @param isOpen whether the visited module is open. Open modules have their {@link
   * Opcodes#ACC_OPEN} access flag set in {@link ClassVisitor#visitModule}.
   * @throws IllegalStateException If a subclass calls this constructor.
   */
  public CheckModuleAdapter(final ModuleVisitor moduleVisitor, final boolean isOpen) {
    super(moduleVisitor);
    this.isOpen = isOpen;
  }

  @Override
  public void visitMainClass(final String mainClass) {
    // Modules can only appear in V9 or more classes.
    CheckMethodAdapter.checkInternalName(Opcodes.V9, mainClass, "module main class");
    super.visitMainClass(mainClass);
  }

  @Override
  public void visitPackage(final String packaze) {
    CheckMethodAdapter.checkInternalName(Opcodes.V9, packaze, "module package");
    super.visitPackage(packaze);
  }

  @Override
  public void visitRequire(final String module, final int access, final String version) {
    checkVisitEndNotCalled();
    CheckClassAdapter.checkFullyQualifiedName(Opcodes.V9, module, "required module");
    requiredModules.checkNameNotAlreadyDeclared(module);
    CheckClassAdapter.checkAccess(
            access,
            Opcodes.ACC_STATIC_PHASE
                    | Opcodes.ACC_TRANSITIVE
                    | Opcodes.ACC_SYNTHETIC
                    | Opcodes.ACC_MANDATED);
    if (classVersion >= Opcodes.V10
            && module.equals("java.base")
            && (access & (Opcodes.ACC_STATIC_PHASE | Opcodes.ACC_TRANSITIVE)) != 0) {
      throw new IllegalArgumentException(
              "Invalid access flags: "
                      + access
                      + " java.base can not be declared ACC_TRANSITIVE or ACC_STATIC_PHASE");
    }
    super.visitRequire(module, access, version);
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    checkVisitEndNotCalled();
    CheckMethodAdapter.checkInternalName(Opcodes.V9, packaze, "package name");
    exportedPackages.checkNameNotAlreadyDeclared(packaze);
    CheckClassAdapter.checkAccess(access, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_MANDATED);
    if (modules != null) {
      for (String module : modules) {
        CheckClassAdapter.checkFullyQualifiedName(Opcodes.V9, module, "module export to");
      }
    }
    super.visitExport(packaze, access, modules);
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    checkVisitEndNotCalled();
    if (isOpen) {
      throw new UnsupportedOperationException("An open module can not use open directive");
    }
    CheckMethodAdapter.checkInternalName(Opcodes.V9, packaze, "package name");
    openedPackages.checkNameNotAlreadyDeclared(packaze);
    CheckClassAdapter.checkAccess(access, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_MANDATED);
    if (modules != null) {
      for (String module : modules) {
        CheckClassAdapter.checkFullyQualifiedName(Opcodes.V9, module, "module open to");
      }
    }
    super.visitOpen(packaze, access, modules);
  }

  @Override
  public void visitUse(final String service) {
    checkVisitEndNotCalled();
    CheckMethodAdapter.checkInternalName(Opcodes.V9, service, "service");
    usedServices.checkNameNotAlreadyDeclared(service);
    super.visitUse(service);
  }

  @Override
  public void visitProvide(final String service, final String... providers) {
    checkVisitEndNotCalled();
    CheckMethodAdapter.checkInternalName(Opcodes.V9, service, "service");
    providedServices.checkNameNotAlreadyDeclared(service);
    if (providers == null || providers.length == 0) {
      throw new IllegalArgumentException("Providers cannot be null or empty");
    }
    for (String provider : providers) {
      CheckMethodAdapter.checkInternalName(Opcodes.V9, provider, "provider");
    }
    super.visitProvide(service, providers);
  }

  @Override
  public void visitEnd() {
    checkVisitEndNotCalled();
    visitEndCalled = true;
    super.visitEnd();
  }

  private void checkVisitEndNotCalled() {
    if (visitEndCalled) {
      throw new IllegalStateException("Cannot call a visit method after visitEnd has been called");
    }
  }

  private static class NameSet {

    private final String type;
    private final HashSet<String> names;

    NameSet(final String type) {
      this.type = type;
      this.names = new HashSet<>();
    }

    void checkNameNotAlreadyDeclared(final String name) {
      if (!names.add(name)) {
        throw new IllegalArgumentException(type + " '" + name + "' already declared");
      }
    }
  }
}
