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

package cn.taketoday.orm.jpa.persistenceunit;

import cn.taketoday.context.classloading.LoadTimeWeaver;
import cn.taketoday.context.classloading.SimpleThrowawayClassLoader;
import cn.taketoday.core.DecoratingClassLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.persistence.spi.ClassTransformer;

/**
 * Subclass of {@link MutablePersistenceUnitInfo} that adds instrumentation hooks based on
 * Framework's {@link cn.taketoday.context.classloading.LoadTimeWeaver} abstraction.
 *
 * <p>This class is restricted to package visibility, in contrast to its superclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @see PersistenceUnitManager
 * @since 4.0
 */
class JpaPersistenceUnitInfo extends MutablePersistenceUnitInfo {

  @Nullable
  private LoadTimeWeaver loadTimeWeaver;

  @Nullable
  private ClassLoader classLoader;

  /**
   * Initialize this PersistenceUnitInfo with the LoadTimeWeaver SPI interface
   * used by Framework to add instrumentation to the current class loader.
   */
  public void init(LoadTimeWeaver loadTimeWeaver) {
    Assert.notNull(loadTimeWeaver, "LoadTimeWeaver must not be null");
    this.loadTimeWeaver = loadTimeWeaver;
    this.classLoader = loadTimeWeaver.getInstrumentableClassLoader();
  }

  /**
   * Initialize this PersistenceUnitInfo with the current class loader
   * (instead of with a LoadTimeWeaver).
   */
  public void init(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * This implementation returns the LoadTimeWeaver's instrumentable ClassLoader,
   * if specified.
   */
  @Override
  @Nullable
  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  /**
   * This implementation delegates to the LoadTimeWeaver, if specified.
   */
  @Override
  public void addTransformer(ClassTransformer classTransformer) {
    if (this.loadTimeWeaver == null) {
      throw new IllegalStateException("Cannot apply class transformer without LoadTimeWeaver specified");
    }
    this.loadTimeWeaver.addTransformer(new ClassFileTransformerAdapter(classTransformer));
  }

  /**
   * This implementation delegates to the LoadTimeWeaver, if specified.
   */
  @Override
  public ClassLoader getNewTempClassLoader() {
    ClassLoader tcl = loadTimeWeaver != null
                      ? this.loadTimeWeaver.getThrowawayClassLoader()
                      : new SimpleThrowawayClassLoader(this.classLoader);
    String packageToExclude = getPersistenceProviderPackageName();
    if (packageToExclude != null && tcl instanceof DecoratingClassLoader) {
      ((DecoratingClassLoader) tcl).excludePackage(packageToExclude);
    }
    return tcl;
  }

}
