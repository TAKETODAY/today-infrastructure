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

package cn.taketoday.context.annotation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanNamePopulator;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.ComponentScan.Filter;
import cn.taketoday.context.loader.ClassPathBeanDefinitionScanner;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.loader.ScopeMetadataResolver;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Parser for the @{@link ComponentScan} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ClassPathBeanDefinitionScanner#scan(String...)
 * @since 4.0
 */
class ComponentScanAnnotationParser {

  private final BootstrapContext loadingContext;

  public ComponentScanAnnotationParser(BootstrapContext loadingContext) {
    this.loadingContext = loadingContext;
  }

  public Set<BeanDefinition> parse(MergedAnnotation<ComponentScan> componentScan, String declaringClass) {
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(
            loadingContext.getRegistry(), componentScan.getBoolean("useDefaultFilters"),
            loadingContext.getEnvironment()
    );

    Class<? extends BeanNamePopulator> generatorClass = componentScan.getClass("namePopulator");

    boolean useInheritedPopulator = BeanNamePopulator.class == generatorClass;
    scanner.setBeanNamePopulator(
            useInheritedPopulator ? loadingContext.getBeanNamePopulator()
                                  : BeanUtils.newInstance(generatorClass));

    Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
    if (resolverClass != ScopeMetadataResolver.class) {
      scanner.setScopeMetadataResolver(BeanUtils.newInstance(resolverClass));
    }

    scanner.setResourcePattern(componentScan.getString("resourcePattern"));

    for (MergedAnnotation<Filter> includeFilter
            : componentScan.getAnnotationArray("includeFilters", Filter.class)) {
      List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(includeFilter, loadingContext);
      for (TypeFilter typeFilter : typeFilters) {
        scanner.addIncludeFilter(typeFilter);
      }
    }
    for (MergedAnnotation<Filter> excludeFilter
            : componentScan.getAnnotationArray("excludeFilters", Filter.class)) {
      List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(excludeFilter, loadingContext);
      for (TypeFilter typeFilter : typeFilters) {
        scanner.addExcludeFilter(typeFilter);
      }
    }

    boolean lazyInit = componentScan.getBoolean("lazyInit");
    if (lazyInit) {
      scanner.getBeanDefinitionDefaults().setLazyInit(true);
    }

    LinkedHashSet<String> basePackages = new LinkedHashSet<>();
    String[] basePackagesArray = componentScan.getStringArray("basePackages");
    for (String pkg : basePackagesArray) {
      String[] tokenized = StringUtils.tokenizeToStringArray(
              loadingContext.evaluateExpression(pkg),
              ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS
      );
      Collections.addAll(basePackages, tokenized);
    }
    for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
      basePackages.add(ClassUtils.getPackageName(clazz));
    }
    if (basePackages.isEmpty()) {
      basePackages.add(ClassUtils.getPackageName(declaringClass));
    }

    scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
      @Override
      protected boolean matchClassName(String className) {
        return declaringClass.equals(className);
      }
    });

    return scanner.scanning(StringUtils.toStringArray(basePackages));
  }

}
