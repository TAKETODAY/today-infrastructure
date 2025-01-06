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

package infra.context.annotation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.BeanNameGenerator;
import infra.context.BootstrapContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.ComponentScan.Filter;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import infra.core.type.filter.TypeFilter;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * Parser for the @{@link ComponentScan} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClassPathBeanDefinitionScanner#scan(String...)
 * @since 4.0
 */
class ComponentScanAnnotationParser {

  private final BootstrapContext context;

  public ComponentScanAnnotationParser(BootstrapContext bootstrapContext) {
    this.context = bootstrapContext;
  }

  public Set<BeanDefinitionHolder> parse(MergedAnnotation<ComponentScan> componentScan, String declaringClass) {
    var scanner = new ClassPathBeanDefinitionScanner(
            context.getRegistry(), componentScan.getBoolean("useDefaultFilters"),
            context.getEnvironment(), context.getResourceLoader());

    var generatorClass = componentScan.<BeanNameGenerator>getClass("nameGenerator");
    scanner.setBeanNameGenerator(BeanNameGenerator.class == generatorClass
            ? context.getBeanNameGenerator()
            : context.instantiate(generatorClass));

    var scopedProxyMode = componentScan.getEnum("scopedProxy", ScopedProxyMode.class);
    if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
      scanner.setScopedProxyMode(scopedProxyMode);
    }
    else {
      var resolverClass = componentScan.<ScopeMetadataResolver>getClass("scopeResolver");
      if (resolverClass != ScopeMetadataResolver.class) {
        scanner.setScopeMetadataResolver(context.instantiate(resolverClass));
      }
    }

    scanner.setResourcePattern(componentScan.getString("resourcePattern"));

    for (var includeFilter : componentScan.getAnnotationArray("includeFilters", Filter.class)) {
      List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(includeFilter, context);
      for (TypeFilter typeFilter : typeFilters) {
        scanner.addIncludeFilter(typeFilter);
      }
    }
    for (var excludeFilter : componentScan.getAnnotationArray("excludeFilters", Filter.class)) {
      List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(excludeFilter, context);
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
              context.evaluateExpression(pkg), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
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

    scanner.setMetadataReaderFactory(context.getMetadataReaderFactory());
    return scanner.collectHolders(StringUtils.toStringArray(basePackages));
  }

}
