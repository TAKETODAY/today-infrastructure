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

package cn.taketoday.framework;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.beans.factory.support.AbstractBeanDefinitionReader;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Loads bean definitions from underlying sources, including XML and JavaConfig. Acts as a
 * simple facade over {@link AnnotatedBeanDefinitionReader},
 * {@link XmlBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}. See
 * {@link Application} for the types of sources that are supported.
 *
 * @author Phillip Webb
 * @author Vladislav Kisel
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setBeanNameGenerator(BeanNameGenerator)
 * @since 4.0 2022/3/29 11:47
 */
class ApplicationBeanDefinitionLoader {

  private final Object[] sources;

  private final AnnotatedBeanDefinitionReader annotatedReader;

  private final AbstractBeanDefinitionReader xmlReader;

  private final ClassPathBeanDefinitionScanner scanner;

  @Nullable
  private ResourceLoader resourceLoader;

  /**
   * Create a new {@link ApplicationBeanDefinitionLoader} that will load beans into the specified
   * {@link BeanDefinitionRegistry}.
   *
   * @param registry the bean definition registry that will contain the loaded beans
   * @param sources the bean sources
   */
  ApplicationBeanDefinitionLoader(BeanDefinitionRegistry registry, Object... sources) {
    Assert.notNull(registry, "Registry is required");
    Assert.notEmpty(sources, "Sources must not be empty");
    this.sources = sources;
    this.xmlReader = new XmlBeanDefinitionReader(registry);
    this.scanner = new ClassPathBeanDefinitionScanner(registry);
    this.annotatedReader = new AnnotatedBeanDefinitionReader(registry);
    this.scanner.addExcludeFilter(new ClassExcludeFilter(sources));
  }

  /**
   * Set the bean name generator to be used by the underlying readers and scanner.
   *
   * @param beanNameGenerator the bean name generator
   */
  void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
    this.scanner.setBeanNameGenerator(beanNameGenerator);
    this.xmlReader.setBeanNameGenerator(beanNameGenerator);
    this.annotatedReader.setBeanNameGenerator(beanNameGenerator);
  }

  /**
   * Set the resource loader to be used by the underlying readers and scanner.
   *
   * @param resourceLoader the resource loader
   */
  void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
    this.scanner.setResourceLoader(resourceLoader);
    this.xmlReader.setResourceLoader(resourceLoader);
  }

  /**
   * Set the environment to be used by the underlying readers and scanner.
   *
   * @param environment the environment
   */
  void setEnvironment(ConfigurableEnvironment environment) {
    this.scanner.setEnvironment(environment);
    this.xmlReader.setEnvironment(environment);
    this.annotatedReader.setEnvironment(environment);
  }

  /**
   * Load the sources into the reader.
   */
  void load() {
    for (Object source : this.sources) {
      load(source);
    }
  }

  private void load(Object source) {
    Assert.notNull(source, "Source must not be null");
    if (source instanceof Class<?>) {
      load((Class<?>) source);
      return;
    }
    if (source instanceof Resource) {
      load((Resource) source);
      return;
    }
    if (source instanceof Package) {
      load((Package) source);
      return;
    }
    if (source instanceof CharSequence) {
      load((CharSequence) source);
      return;
    }
    throw new IllegalArgumentException("Invalid source type " + source.getClass());
  }

  private void load(Class<?> source) {
    if (isEligible(source)) {
      this.annotatedReader.register(source);
    }
  }

  private void load(Resource source) {
    String name = source.getName();
    if (name != null && name.endsWith(".xml")) {
      this.xmlReader.loadBeanDefinitions(source);
    }
  }

  private void load(Package source) {
    this.scanner.scan(source.getName());
  }

  private void load(CharSequence source) {
    String resolvedSource = this.scanner.getEnvironment().resolvePlaceholders(source.toString());
    // Attempt as a Class
    try {
      load(ClassUtils.forName(resolvedSource, null));
      return;
    }
    catch (IllegalArgumentException | ClassNotFoundException ex) {
      // swallow exception and continue
    }
    // Attempt as Resources
    if (loadAsResources(resolvedSource)) {
      return;
    }
    // Attempt as package
    Package packageResource = findPackage(resolvedSource);
    if (packageResource != null) {
      load(packageResource);
      return;
    }
    throw new IllegalArgumentException("Invalid source '" + resolvedSource + "'");
  }

  private boolean loadAsResources(String resolvedSource) {
    boolean foundCandidate = false;
    Set<Resource> resources = findResources(resolvedSource);
    for (Resource resource : resources) {
      if (isLoadCandidate(resource)) {
        foundCandidate = true;
        load(resource);
      }
    }
    return foundCandidate;
  }

  private Set<Resource> findResources(String source) {
    ResourceLoader loader = resourceLoader;
    if (loader == null) {
      loader = new PathMatchingPatternResourceLoader();
    }

    try {
      if (loader instanceof PatternResourceLoader patternLoader) {
        return patternLoader.getResources(source);
      }
      return Collections.singleton(loader.getResource(source));
    }
    catch (IOException ex) {
      throw new IllegalStateException("Error reading source '" + source + "'");
    }
  }

  private boolean isLoadCandidate(@Nullable Resource resource) {
    if (resource == null || !resource.exists()) {
      return false;
    }
    if (resource instanceof ClassPathResource classPathResource) {
      // A simple package without a '.' may accidentally get loaded as an XML
      // document if we're not careful. The result of getInputStream() will be
      // a file list of the package content. We double check here that it's not
      // actually a package.
      String path = classPathResource.getPath();
      if (path.indexOf('.') == -1) {
        try {
          return getClass().getClassLoader().getDefinedPackage(path) == null;
        }
        catch (Exception ex) {
          // Ignore
        }
      }
    }
    return true;
  }

  @Nullable
  private Package findPackage(CharSequence source) {
    Package pkg = getClass().getClassLoader().getDefinedPackage(source.toString());
    if (pkg != null) {
      return pkg;
    }
    try {
      // Attempt to find a class in this package
      var resolver = new PathMatchingPatternResourceLoader(getClass().getClassLoader());
      Set<Resource> resources = resolver.getResources(
              ClassUtils.convertClassNameToResourcePath(source.toString()) + "/*.class");
      for (Resource resource : resources) {
        String name = resource.getName();
        Assert.state(name != null, "No name");
        String className = StringUtils.stripFilenameExtension(name);
        load(Class.forName(source + "." + className));
        break;
      }
    }
    catch (Exception ex) {
      // swallow exception and continue
    }
    return getClass().getClassLoader().getDefinedPackage(source.toString());
  }

  /**
   * Check whether the bean is eligible for registration.
   *
   * @param type candidate bean type
   * @return true if the given bean type is eligible for registration, i.e. not a groovy
   * closure nor an anonymous class
   */
  private boolean isEligible(Class<?> type) {
    return !(type.isAnonymousClass() || hasNoConstructors(type));
  }

  private boolean hasNoConstructors(Class<?> type) {
    Constructor<?>[] constructors = type.getDeclaredConstructors();
    return ObjectUtils.isEmpty(constructors);
  }

  /**
   * Simple {@link TypeFilter} used to ensure that specified {@link Class} sources are
   * not accidentally re-added during scanning.
   */
  private static class ClassExcludeFilter extends AbstractTypeHierarchyTraversingFilter {

    private final HashSet<String> classNames = new HashSet<>();

    ClassExcludeFilter(Object... sources) {
      super(false, false);
      for (Object source : sources) {
        if (source instanceof Class<?> sourceClass) {
          classNames.add(sourceClass.getName());
        }
      }
    }

    @Override
    protected boolean matchClassName(String className) {
      return this.classNames.contains(className);
    }

  }

}
