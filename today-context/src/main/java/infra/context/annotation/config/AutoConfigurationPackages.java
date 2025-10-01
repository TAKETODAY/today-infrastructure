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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConstructorArgumentValues;
import infra.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.BootstrapContext;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.core.type.AnnotationMetadata;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

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
   * Return the auto-configuration base packages for the given bean factory.
   *
   * @param beanFactory the source bean factory
   * @return a list of auto-configuration packages
   * @throws IllegalStateException if auto-configuration is not enabled
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
    if (registry.containsBeanDefinition(BEAN_NAME)) {
      addBasePackages(registry.getBeanDefinition(BEAN_NAME), packageNames);
    }
    else {
      RootBeanDefinition beanDefinition = new RootBeanDefinition(BasePackages.class);
      beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      addBasePackages(beanDefinition, packageNames);
      registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
    }
  }

  private static void addBasePackages(BeanDefinition beanDefinition, String[] additionalBasePackages) {
    ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
    if (constructorArgumentValues.hasIndexedArgumentValue(0)) {
      ValueHolder indexedArgumentValue = constructorArgumentValues.getIndexedArgumentValue(0, String[].class);
      Assert.state(indexedArgumentValue != null, "'indexedArgumentValue' is required");
      String[] existingPackages = (String[]) indexedArgumentValue.getValue();
      Stream<String> existingPackagesStream = existingPackages != null ? Stream.of(existingPackages) : Stream.empty();
      constructorArgumentValues.addIndexedArgumentValue(0,
              Stream.concat(existingPackagesStream, Stream.of(additionalBasePackages))
                      .distinct()
                      .toArray(String[]::new));
    }
    else {
      constructorArgumentValues.addIndexedArgumentValue(0, additionalBasePackages);
    }
  }

  /**
   * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
   * configuration.
   */
  static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BootstrapContext context) {
      String[] packageNames = StringUtils.toStringArray(new PackageImports(metadata).packageNames);
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

    public final List<String> packageNames;

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

    BasePackages(String... names) {
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

}
