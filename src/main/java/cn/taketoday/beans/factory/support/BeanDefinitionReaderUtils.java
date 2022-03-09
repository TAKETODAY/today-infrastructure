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

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Utility methods that are useful for bean definition reader implementations.
 * Mainly intended for internal use.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultBeanDefinitionDocumentReader
 * @since 4.0 2022/3/6 22:20
 */
public abstract class BeanDefinitionReaderUtils {

  /**
   * Separator for generated bean names. If a class name or parent name is not
   * unique, "#1", "#2" etc will be appended, until the name becomes unique.
   */
  public static final String GENERATED_BEAN_NAME_SEPARATOR = BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;

  /**
   * Create a new GenericBeanDefinition for the given parent name and class name,
   * eagerly loading the bean class if a ClassLoader has been specified.
   *
   * @param parentName the name of the parent bean, if any
   * @param className the name of the bean class, if any
   * @param classLoader the ClassLoader to use for loading bean classes
   * (can be {@code null} to just register bean classes by name)
   * @return the bean definition
   * @throws ClassNotFoundException if the bean class could not be loaded
   */
  public static AbstractBeanDefinition createBeanDefinition(
          @Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {

    GenericBeanDefinition bd = new GenericBeanDefinition();
    bd.setParentName(parentName);
    if (className != null) {
      if (classLoader != null) {
        bd.setBeanClass(ClassUtils.forName(className, classLoader));
      }
      else {
        bd.setBeanClassName(className);
      }
    }
    return bd;
  }

  /**
   * Generate a bean name for the given top-level bean definition,
   * unique within the given bean factory.
   *
   * @param beanDefinition the bean definition to generate a bean name for
   * @param registry the bean factory that the definition is going to be
   * registered with (to check for existing bean names)
   * @return the generated bean name
   * @throws BeanDefinitionStoreException if no unique name can be generated
   * for the given bean definition
   * @see #generateBeanName(BeanDefinition, BeanDefinitionRegistry, boolean)
   */
  public static String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry)
          throws BeanDefinitionStoreException {

    return generateBeanName(beanDefinition, registry, false);
  }

  /**
   * Generate a bean name for the given bean definition, unique within the
   * given bean factory.
   *
   * @param definition the bean definition to generate a bean name for
   * @param registry the bean factory that the definition is going to be
   * registered with (to check for existing bean names)
   * @param isInnerBean whether the given bean definition will be registered
   * as inner bean or as top-level bean (allowing for special name generation
   * for inner beans versus top-level beans)
   * @return the generated bean name
   * @throws BeanDefinitionStoreException if no unique name can be generated
   * for the given bean definition
   */
  public static String generateBeanName(
          BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
          throws BeanDefinitionStoreException {

    String generatedBeanName = definition.getBeanClassName();
    if (generatedBeanName == null) {
      if (definition.getParentName() != null) {
        generatedBeanName = definition.getParentName() + "$child";
      }
      else if (definition.getFactoryBeanName() != null) {
        generatedBeanName = definition.getFactoryBeanName() + "$created";
      }
    }
    if (!StringUtils.hasText(generatedBeanName)) {
      throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " +
              "'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
    }

    if (isInnerBean) {
      // Inner bean: generate identity hashcode suffix.
      return generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
    }

    // Top-level bean: use plain class name with unique suffix if necessary.
    return uniqueBeanName(generatedBeanName, registry);
  }

  /**
   * Turn the given bean name into a unique bean name for the given bean factory,
   * appending a unique counter as suffix if necessary.
   *
   * @param beanName the original bean name
   * @param registry the bean factory that the definition is going to be
   * registered with (to check for existing bean names)
   * @return the unique bean name to use
   */
  public static String uniqueBeanName(String beanName, BeanDefinitionRegistry registry) {
    String id = beanName;
    int counter = -1;

    // Increase counter until the id is unique.
    String prefix = beanName + GENERATED_BEAN_NAME_SEPARATOR;
    while (counter == -1 || registry.containsBeanDefinition(id)) {
      counter++;
      id = prefix + counter;
    }
    return id;
  }

  /**
   * Register the given bean definition with the given bean factory.
   *
   * @param definitionHolder the bean definition including name and aliases
   * @param registry the bean factory to register with
   * @throws BeanDefinitionStoreException if registration failed
   */
  public static void registerBeanDefinition(
          BeanDefinition definitionHolder, BeanDefinitionRegistry registry)
          throws BeanDefinitionStoreException {

    // Register bean definition under primary name.
    registry.registerBeanDefinition(definitionHolder);
  }

  /**
   * Register the given bean definition with the given bean factory.
   *
   * @param definitionHolder the bean definition including name and aliases
   * @param registry the bean factory to register with
   * @throws BeanDefinitionStoreException if registration failed
   */
  public static void registerBeanDefinition(
          BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) throws BeanDefinitionStoreException {

    // Register bean definition under primary name.
    String beanName = definitionHolder.getBeanName();
    registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

    // Register aliases for bean name, if any.
    String[] aliases = definitionHolder.getAliases();
    if (aliases != null) {
      for (String alias : aliases) {
        registry.registerAlias(beanName, alias);
      }
    }
  }

  /**
   * Register the given bean definition with a generated name,
   * unique within the given bean factory.
   *
   * @param definition the bean definition to generate a bean name for
   * @param registry the bean factory to register with
   * @return the generated bean name
   * @throws BeanDefinitionStoreException if no unique name can be generated
   * for the given bean definition or the definition cannot be registered
   */
  public static String registerWithGeneratedName(
          BeanDefinition definition, BeanDefinitionRegistry registry)
          throws BeanDefinitionStoreException {

    String generatedName = generateBeanName(definition, registry, false);
    registry.registerBeanDefinition(generatedName, definition);
    return generatedName;
  }

}
