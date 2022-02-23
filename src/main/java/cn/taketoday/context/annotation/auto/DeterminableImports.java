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

package cn.taketoday.context.annotation.auto;

import java.util.Set;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.core.type.AnnotationMetadata;

/**
 * Interface that can be implemented by {@link ImportSelector} and
 * {@link ImportBeanDefinitionRegistrar} implementations when they can determine imports
 * early. The {@link ImportSelector} and {@link ImportBeanDefinitionRegistrar} interfaces
 * are quite flexible which can make it hard to tell exactly what bean definitions they
 * will add. This interface should be used when an implementation consistently results in
 * the same imports, given the same source.
 * <p>
 * Using {@link DeterminableImports} is particularly useful when working with Framework's
 * testing support. It allows for better generation of {@link ApplicationContext} cache
 * keys.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:32
 */
@FunctionalInterface
public interface DeterminableImports {

  /**
   * Return a set of objects that represent the imports. Objects within the returned
   * {@code Set} must implement a valid {@link Object#hashCode() hashCode} and
   * {@link Object#equals(Object) equals}.
   * <p>
   * Imports from multiple {@link DeterminableImports} instances may be combined by the
   * caller to create a complete set.
   * <p>
   * Unlike {@link ImportSelector} and {@link ImportBeanDefinitionRegistrar} any
   * {@link Aware} callbacks will not be invoked before this method is called.
   *
   * @param metadata the source meta-data
   * @return a key representing the annotations that actually drive the import
   */
  Set<Object> determineImports(AnnotationMetadata metadata);

}

