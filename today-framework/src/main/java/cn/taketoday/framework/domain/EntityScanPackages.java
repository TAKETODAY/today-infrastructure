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

package cn.taketoday.framework.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Class for storing {@link EntityScan @EntityScan} specified packages for reference later
 * (e.g. by JPA auto-configuration).
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityScan
 * @see EntityScanner
 * @since 4.0
 */
public class EntityScanPackages {

  private static final String BEAN = EntityScanPackages.class.getName();

  private static final EntityScanPackages NONE = new EntityScanPackages();

  private final List<String> packageNames;

  EntityScanPackages(String... packageNames) {
    List<String> packages = new ArrayList<>();
    for (String name : packageNames) {
      if (StringUtils.hasText(name)) {
        packages.add(name);
      }
    }
    this.packageNames = Collections.unmodifiableList(packages);
  }

  /**
   * Return the package names specified from all {@link EntityScan @EntityScan}
   * annotations.
   *
   * @return the entity scan package names
   */
  public List<String> getPackageNames() {
    return this.packageNames;
  }

  /**
   * Return the {@link EntityScanPackages} for the given bean factory.
   *
   * @param beanFactory the source bean factory
   * @return the {@link EntityScanPackages} for the bean factory (never {@code null})
   */
  public static EntityScanPackages get(BeanFactory beanFactory) {
    // Currently we only store a single base package, but we return a list to
    // allow this to change in the future if needed
    try {
      return beanFactory.getBean(BEAN, EntityScanPackages.class);
    }
    catch (NoSuchBeanDefinitionException ex) {
      return NONE;
    }
  }

  /**
   * Register the specified entity scan packages with the system.
   *
   * @param registry the source registry
   * @param packageNames the package names to register
   */
  public static void register(BeanDefinitionRegistry registry, String... packageNames) {
    Assert.notNull(registry, "Registry must not be null");
    Assert.notNull(packageNames, "PackageNames must not be null");
    register(registry, Arrays.asList(packageNames));
  }

  /**
   * Register the specified entity scan packages with the system.
   *
   * @param registry the source registry
   * @param packageNames the package names to register
   */
  public static void register(BeanDefinitionRegistry registry, Collection<String> packageNames) {
    Assert.notNull(registry, "Registry must not be null");
    Assert.notNull(packageNames, "PackageNames must not be null");
    if (registry.containsBeanDefinition(BEAN)
            && registry.getBeanDefinition(BEAN) instanceof EntityScanPackagesBeanDefinition definition) {
      definition.addPackageNames(packageNames);
    }
    else {
      registry.registerBeanDefinition(BEAN, new EntityScanPackagesBeanDefinition(packageNames));
    }
  }

  /**
   * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
   * configuration.
   */
  static class Registrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      register(context.getRegistry(), getPackagesToScan(importMetadata, context.getEnvironment()));
    }

    private Set<String> getPackagesToScan(AnnotationMetadata metadata, Environment environment) {
      AnnotationAttributes attributes = AnnotationAttributes.fromMap(
              metadata.getAnnotationAttributes(EntityScan.class.getName()));
      Set<String> packagesToScan = new LinkedHashSet<>();
      Assert.state(attributes != null, "EntityScan Error");
      for (String basePackage : attributes.getStringArray("basePackages")) {
        String[] tokenized = StringUtils.tokenizeToStringArray(
                environment.resolvePlaceholders(basePackage),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        Collections.addAll(packagesToScan, tokenized);
      }
      for (Class<?> basePackageClass : attributes.getClassArray("basePackageClasses")) {
        packagesToScan.add(environment.resolvePlaceholders(ClassUtils.getPackageName(basePackageClass)));
      }
      if (packagesToScan.isEmpty()) {
        String packageName = ClassUtils.getPackageName(metadata.getClassName());
        Assert.state(StringUtils.isNotEmpty(packageName), "@EntityScan cannot be used with the default package");
        return Collections.singleton(packageName);
      }
      return packagesToScan;
    }

  }

  static class EntityScanPackagesBeanDefinition extends GenericBeanDefinition {

    private final Set<String> packageNames = new LinkedHashSet<>();

    EntityScanPackagesBeanDefinition(Collection<String> packageNames) {
      setBeanClass(EntityScanPackages.class);
      setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      addPackageNames(packageNames);

      setInstanceSupplier(() -> new EntityScanPackages(StringUtils.toStringArray(this.packageNames)));
    }

    private void addPackageNames(Collection<String> additionalPackageNames) {
      this.packageNames.addAll(additionalPackageNames);
    }

  }

}
