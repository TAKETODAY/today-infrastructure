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

package cn.taketoday.bytecode.commons;

import cn.taketoday.bytecode.ModuleVisitor;

/**
 * A {@link ModuleVisitor} that remaps types with a {@link Remapper}.
 *
 * @author Remi Forax
 */
public class ModuleRemapper extends ModuleVisitor {

  /** The remapper used to remap the types in the visited module. */
  protected final Remapper remapper;

  /**
   * Constructs a new {@link ModuleRemapper}.
   *
   * @param moduleVisitor the module visitor this remapper must delegate to.
   * @param remapper the remapper to use to remap the types in the visited module.
   */
  public ModuleRemapper(final ModuleVisitor moduleVisitor, final Remapper remapper) {
    super(moduleVisitor);
    this.remapper = remapper;
  }

  @Override
  public void visitMainClass(final String mainClass) {
    super.visitMainClass(remapper.mapType(mainClass));
  }

  @Override
  public void visitPackage(final String packaze) {
    super.visitPackage(remapper.mapPackageName(packaze));
  }

  @Override
  public void visitRequire(final String module, final int access, final String version) {
    super.visitRequire(remapper.mapModuleName(module), access, version);
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    String[] remappedModules = null;
    final Remapper remapper = this.remapper;
    if (modules != null) {
      remappedModules = new String[modules.length];
      for (int i = 0; i < modules.length; ++i) {
        remappedModules[i] = remapper.mapModuleName(modules[i]);
      }
    }
    super.visitExport(remapper.mapPackageName(packaze), access, remappedModules);
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    String[] remappedModules = null;
    final Remapper remapper = this.remapper;
    if (modules != null) {
      remappedModules = new String[modules.length];
      for (int i = 0; i < modules.length; ++i) {
        remappedModules[i] = remapper.mapModuleName(modules[i]);
      }
    }
    super.visitOpen(remapper.mapPackageName(packaze), access, remappedModules);
  }

  @Override
  public void visitUse(final String service) {
    super.visitUse(remapper.mapType(service));
  }

  @Override
  public void visitProvide(final String service, final String... providers) {
    final Remapper remapper = this.remapper;

    String[] remappedProviders = new String[providers.length];
    for (int i = 0; i < providers.length; ++i) {
      remappedProviders[i] = remapper.mapType(providers[i]);
    }
    super.visitProvide(remapper.mapType(service), remappedProviders);
  }
}
