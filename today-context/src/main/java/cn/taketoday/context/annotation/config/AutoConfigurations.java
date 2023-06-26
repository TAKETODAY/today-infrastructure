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

package cn.taketoday.context.annotation.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import cn.taketoday.util.ClassUtils;

/**
 * {@link Configurations} representing auto-configuration {@code @Configuration} classes.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 12:15
 */
public class AutoConfigurations extends Configurations implements Ordered {

  private static final AutoConfigurationSorter SORTER =
          new AutoConfigurationSorter(new SimpleMetadataReaderFactory(), null);

  private static final Ordered ORDER = new AutoConfigurationImportSelector(null);

  protected AutoConfigurations(Collection<Class<?>> classes) {
    super(classes);
  }

  @Override
  protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
    var names = new ArrayList<String>();
    for (Class<?> c : classes) {
      names.add(c.getName());
    }

    var sorted = SORTER.getInPriorityOrder(names);
    var configClasses = new ArrayList<Class<?>>();
    for (String className : sorted) {
      Class<?> configClass = ClassUtils.resolveClassName(className, null);
      configClasses.add(configClass);
    }
    return configClasses;
  }

  @Override
  public int getOrder() {
    return ORDER.getOrder();
  }

  @Override
  protected AutoConfigurations merge(Set<Class<?>> mergedClasses) {
    return new AutoConfigurations(mergedClasses);
  }

  public static AutoConfigurations of(Class<?>... classes) {
    return new AutoConfigurations(Arrays.asList(classes));
  }

}
