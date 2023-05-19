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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Class for storing auto-configuration packages for reference later (e.g. by JPA entity
 * scanner).
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Oliver Gierke
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:33
 */
public abstract class AutoConfigurationPackages {
  private static final Logger log = LoggerFactory.getLogger(AutoConfigurationPackages.class);

  private static final String BEAN_NAME = AutoConfigurationPackages.class.getName();

  /**
   * Determine if the auto-configuration base packages for the given bean factory are
   * available.
   *
   * @param beanFactory the source bean factory
   * @return true if there are auto-config packages available
   */
  public static boolean has(BeanFactory beanFactory) {
    return beanFactory.containsBean(BEAN_NAME) && !get(beanFactory).isEmpty();
  }

  /**
   * Return the autoconfiguration base packages for the given bean factory.
   *
   * @param beanFactory the source bean factory
   * @return a list of autoconfiguration packages
   * @throws IllegalStateException if autoconfiguration is not enabled
   */
  public static List<String> get(BeanFactory beanFactory) {
    try {
      return beanFactory.getBean(BEAN_NAME, BasePackages.class).get();
    }
    catch (NoSuchBeanDefinitionException ex) {
      throw new IllegalStateException("Unable to retrieve @EnableAutoConfiguration base packages");
    }
  }

  /**
   * Programmatically registers the auto-configuration package names. Subsequent
   * invocations will add the given package names to those that have already been
   * registered. You can use this method to manually define the base packages that will
   * be used for a given {@link BeanDefinitionRegistry}. Generally it's recommended that
   * you don't call this method directly, but instead rely on the default convention
   * where the package name is set from your {@code @EnableAutoConfiguration}
   * configuration class or classes.
   *
   * @param registry the bean definition registry
   * @param packageNames the package names to set
   */
  public static void register(BeanDefinitionRegistry registry, String... packageNames) {
    if (registry.containsBeanDefinition(BEAN_NAME)
            && registry.getBeanDefinition(BEAN_NAME) instanceof BasePackagesBeanDefinition definition) {
      definition.addBasePackages(packageNames);
    }
    else {
      registry.registerBeanDefinition(BEAN_NAME, new BasePackagesBeanDefinition(packageNames));
    }
  }

  /**
   * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
   * configuration.
   */
  static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BootstrapContext context) {
      String[] packageNames = StringUtils.toStringArray(new PackageImports(metadata).getPackageNames());
      register(context.getRegistry(), packageNames);
    }

    @Override
    public Set<Object> determineImports(AnnotationMetadata metadata) {
      return Collections.singleton(new PackageImports(metadata));
    }

  }

  /**
   * Wrapper for a package import.
   */
  private static final class PackageImports {

    private final List<String> packageNames;

    PackageImports(AnnotationMetadata metadata) {
      var annotation = metadata.getAnnotation(AutoConfigurationPackage.class);
      Assert.state(annotation.isPresent(), "attributes error");
      ArrayList<String> packageNames = CollectionUtils.newArrayList(annotation.getStringArray("basePackages"));
      for (Class<?> basePackageClass : annotation.getClassArray("basePackageClasses")) {
        packageNames.add(basePackageClass.getPackage().getName());
      }
      if (packageNames.isEmpty()) {
        packageNames.add(ClassUtils.getPackageName(metadata.getClassName()));
      }
      this.packageNames = packageNames;
    }

    List<String> getPackageNames() {
      return this.packageNames;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      return this.packageNames.equals(((PackageImports) obj).packageNames);
    }

    @Override
    public int hashCode() {
      return this.packageNames.hashCode();
    }

    @Override
    public String toString() {
      return "Package Imports " + this.packageNames;
    }

  }

  /**
   * Holder for the base package (name may be null to indicate no scanning).
   */
  static final class BasePackages {

    private final List<String> packages;

    private boolean loggedBasePackageInfo;

    BasePackages(LinkedHashSet<String> names) {
      ArrayList<String> packages = new ArrayList<>();
      for (String name : names) {
        if (StringUtils.hasText(name)) {
          packages.add(name);
        }
      }
      this.packages = packages;
    }

    List<String> get() {
      if (!loggedBasePackageInfo) {
        if (packages.isEmpty()) {
          if (log.isWarnEnabled()) {
            log.warn("@EnableAutoConfiguration was declared on a class "
                    + "in the default package. Automatic @Repository and "
                    + "@Entity scanning is not enabled.");
          }
        }
        else {
          if (log.isDebugEnabled()) {
            String packageNames = StringUtils.collectionToCommaDelimitedString(packages);
            log.debug("@EnableAutoConfiguration was declared on a class in the package '{}'." +
                    " Automatic @Repository and @Entity scanning is enabled.", packageNames);
          }
        }
        this.loggedBasePackageInfo = true;
      }
      return packages;
    }

  }

  static final class BasePackagesBeanDefinition extends GenericBeanDefinition {
    private final LinkedHashSet<String> basePackages = new LinkedHashSet<>();

    BasePackagesBeanDefinition(String... basePackages) {
      setBeanClass(BasePackages.class);
      setRole(ROLE_INFRASTRUCTURE);
      addBasePackages(basePackages);
      setInstanceSupplier(() -> new BasePackages(this.basePackages));
    }

    private void addBasePackages(String[] additionalBasePackages) {
      CollectionUtils.addAll(basePackages, additionalBasePackages);
    }

  }

}
