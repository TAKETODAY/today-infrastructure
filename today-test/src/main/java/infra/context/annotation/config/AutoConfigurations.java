/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.annotation.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import infra.core.Ordered;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.util.ClassUtils;

/**
 * {@link Configurations} representing auto-configuration {@code @Configuration} classes.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 12:15
 */
public class AutoConfigurations extends Configurations implements Ordered {

  private static final SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

  private static final int ORDER = AutoConfigurationImportSelector.ORDER;

  static final AutoConfigurationReplacements replacements = AutoConfigurationReplacements
          .load(AutoConfiguration.class, null);

  private final UnaryOperator<String> replacementMapper;

  protected AutoConfigurations(Collection<Class<?>> classes) {
    this(replacements::replace, classes);
  }

  AutoConfigurations(UnaryOperator<String> replacementMapper, Collection<Class<?>> classes) {
    super(sorter(replacementMapper), classes, Class::getName);
    this.replacementMapper = replacementMapper;
  }

  private static UnaryOperator<Collection<Class<?>>> sorter(UnaryOperator<String> replacementMapper) {
    AutoConfigurationSorter sorter = new AutoConfigurationSorter(metadataReaderFactory, null, replacementMapper);
    return classes -> {
      List<String> names = classes.stream().map(Class::getName).map(replacementMapper).toList();
      List<String> sorted = sorter.getInPriorityOrder(names);
      return sorted.stream()
              .map((className) -> ClassUtils.resolveClassName(className, null))
              .collect(Collectors.toCollection(ArrayList::new));
    };
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  protected AutoConfigurations merge(Set<Class<?>> mergedClasses) {
    return new AutoConfigurations(this.replacementMapper, mergedClasses);
  }

  public static AutoConfigurations of(Class<?>... classes) {
    return new AutoConfigurations(Arrays.asList(classes));
  }

}
