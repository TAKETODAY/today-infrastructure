/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import java.util.function.Predicate;

import cn.taketoday.context.BootstrapContextAware;
import cn.taketoday.context.EnvironmentAware;
import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * Interface to be implemented by types that determine which @{@link Configuration}
 * class(es) should be imported based on a given selection criteria, usually one or
 * more annotation attributes.
 *
 * <p>An {@link ImportSelector} may implement any of the following
 * {@link cn.taketoday.beans.factory.Aware Aware} interfaces,
 * and their respective methods will be called prior to {@link #selectImports}:
 * <ul>
 * <li>{@link EnvironmentAware EnvironmentAware}</li>
 * <li>{@link cn.taketoday.beans.factory.BeanFactoryAware BeanFactoryAware}</li>
 * <li>{@link cn.taketoday.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}</li>
 * <li>{@link ResourceLoaderAware ResourceLoaderAware}</li>
 * <li>{@link BootstrapContextAware BootstrapContextAware}</li>
 * </ul>
 *
 * <p>Alternatively, the class may provide a single constructor with one or more of
 * the following supported parameter types:
 * <ul>
 * <li>{@link cn.taketoday.core.env.Environment Environment}</li>
 * <li>{@link cn.taketoday.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link cn.taketoday.core.io.ResourceLoader ResourceLoader}</li>
 * <li>{@link cn.taketoday.context.BootstrapContext BootstrapContext}</li>
 * </ul>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author TODAY 2019-10-01 20:20
 * @see Import
 * @see Configuration
 */
@FunctionalInterface
public interface ImportSelector {

  String[] NO_IMPORTS = Constant.EMPTY_STRING_ARRAY;

  /**
   * Select and return the names of which class(es) should be imported based on
   * the {@link AnnotationMetadata} of the importing @{@link Configuration} class.
   *
   * @return the class names, or an empty array if none
   */
  String[] selectImports(AnnotationMetadata importingClassMetadata);

  /**
   * Return a predicate for excluding classes from the import candidates, to be
   * transitively applied to all classes found through this selector's imports.
   * <p>If this predicate returns {@code true} for a given fully-qualified
   * class name, said class will not be considered as an imported configuration
   * class, bypassing class file loading as well as metadata introspection.
   *
   * @return the filter predicate for fully-qualified candidate class names
   * of transitively imported configuration classes, or {@code null} if none
   * @since 4.0
   */
  @Nullable
  default Predicate<String> getExclusionFilter() {
    return null;
  }

}
