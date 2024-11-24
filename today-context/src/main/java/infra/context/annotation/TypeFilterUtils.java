/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.context.BootstrapContext;
import infra.context.EnvironmentAware;
import infra.context.ResourceLoaderAware;
import infra.context.annotation.ComponentScan.Filter;
import infra.core.annotation.AnnotationAttributes;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.filter.AnnotationTypeFilter;
import infra.core.type.filter.AspectJTypeFilter;
import infra.core.type.filter.AssignableTypeFilter;
import infra.core.type.filter.RegexPatternTypeFilter;
import infra.core.type.filter.TypeFilter;
import infra.lang.Assert;

/**
 * Collection of utilities for working with {@link ComponentScan @ComponentScan}
 * {@linkplain Filter type filters}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Filter
 * @see TypeFilter
 * @since 4.0
 */
public abstract class TypeFilterUtils {

  /**
   * Create {@linkplain TypeFilter type filters} from the supplied
   * {@link AnnotationAttributes}, such as those sourced from
   * {@link ComponentScan#includeFilters()} or {@link ComponentScan#excludeFilters()}.
   * <p>Each {@link TypeFilter} will be instantiated using an appropriate
   * constructor, with {@code BeanClassLoaderAware}, {@code BeanFactoryAware},
   * {@code EnvironmentAware}, and {@code ResourceLoaderAware} contracts
   * invoked if they are implemented by the type filter.
   *
   * @param filterAnnotation {@code AnnotationAttributes} for a
   * {@link Filter @Filter} declaration
   * as a {@link BeanFactory} if applicable
   * @return a list of instantiated and configured type filters
   * @see TypeFilter
   * @see AnnotationTypeFilter
   * @see AssignableTypeFilter
   * @see RegexPatternTypeFilter
   * @see BeanClassLoaderAware
   * @see BeanFactoryAware
   * @see EnvironmentAware
   * @see ResourceLoaderAware
   */
  @SuppressWarnings("unchecked")
  public static List<TypeFilter> createTypeFiltersFor(
          MergedAnnotation<Filter> filterAnnotation, BootstrapContext context) {

    ArrayList<TypeFilter> typeFilters = new ArrayList<>();
    FilterType filterType = filterAnnotation.getEnum("type", FilterType.class);

    // type
    for (Class<?> filterClass : filterAnnotation.getClassValueArray()) {
      switch (filterType) {
        case ANNOTATION -> {
          Assert.isAssignable(Annotation.class, filterClass,
                  "@ComponentScan ANNOTATION type filter requires an annotation type");
          typeFilters.add(new AnnotationTypeFilter((Class<Annotation>) filterClass));
        }
        case ASSIGNABLE_TYPE -> typeFilters.add(new AssignableTypeFilter(filterClass));
        case CUSTOM -> {
          Assert.isAssignable(TypeFilter.class, filterClass,
                  "@ComponentScan CUSTOM type filter requires a TypeFilter implementation");
          TypeFilter filter = context.instantiate(filterClass, TypeFilter.class);
          typeFilters.add(filter);
        }
        default -> throw new IllegalArgumentException("Filter type not supported with Class value: " + filterType);
      }
    }

    // string

    for (String expression : filterAnnotation.getStringArray("pattern")) {
      switch (filterType) {
        case ASPECTJ -> typeFilters.add(new AspectJTypeFilter(expression, context.getClassLoader()));
        case REGEX -> typeFilters.add(new RegexPatternTypeFilter(Pattern.compile(expression)));
        default -> throw new IllegalArgumentException(
                "Filter type not supported with String pattern: " + filterType);
      }
    }

    return typeFilters;
  }

}
