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

package cn.taketoday.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.taketoday.beans.factory.support.BeanDefinitionReader;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.util.ObjectUtils;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} that loads
 * bean definitions from component classes.
 *
 * <p>See the Javadoc for
 * {@link ContextConfiguration @ContextConfiguration}
 * for a definition of <em>component class</em>.
 *
 * <p>Note: {@code AnnotationConfigContextLoader} supports <em>component classes</em>
 * rather than the String-based resource locations defined by the legacy
 * {@link ContextLoader ContextLoader} API. Thus,
 * although {@code AnnotationConfigContextLoader} extends
 * {@code AbstractGenericContextLoader}, {@code AnnotationConfigContextLoader}
 * does <em>not</em> support any String-based methods defined by
 * {@code AbstractContextLoader} or {@code AbstractGenericContextLoader}.
 * Consequently, {@code AnnotationConfigContextLoader} should chiefly be
 * considered a {@link SmartContextLoader SmartContextLoader}
 * rather than a {@link ContextLoader ContextLoader}.
 *
 * @author Sam Brannen
 * @see #processContextConfiguration(ContextConfigurationAttributes)
 * @see #detectDefaultConfigurationClasses(Class)
 * @see #loadBeanDefinitions(GenericApplicationContext, MergedContextConfiguration)
 * @see GenericXmlContextLoader
 * @see GenericGroovyXmlContextLoader
 *@since 4.0
 */
public class AnnotationConfigContextLoader extends AbstractGenericContextLoader {

  private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoader.class);

  // SmartContextLoader

  /**
   * Process <em>component classes</em> in the supplied {@link ContextConfigurationAttributes}.
   * <p>If the <em>component classes</em> are {@code null} or empty and
   * {@link #isGenerateDefaultLocations()} returns {@code true}, this
   * {@code SmartContextLoader} will attempt to {@link
   * #detectDefaultConfigurationClasses detect default configuration classes}.
   * If defaults are detected they will be
   * {@link ContextConfigurationAttributes#setClasses(Class[]) set} in the
   * supplied configuration attributes. Otherwise, properties in the supplied
   * configuration attributes will not be modified.
   *
   * @param configAttributes the context configuration attributes to process
   * @see SmartContextLoader#processContextConfiguration(ContextConfigurationAttributes)
   * @see #isGenerateDefaultLocations()
   * @see #detectDefaultConfigurationClasses(Class)
   */
  @Override
  public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
    if (!configAttributes.hasClasses() && isGenerateDefaultLocations()) {
      configAttributes.setClasses(detectDefaultConfigurationClasses(configAttributes.getDeclaringClass()));
    }
  }

  // AnnotationConfigContextLoader

  /**
   * Detect the default configuration classes for the supplied test class.
   * <p>The default implementation simply delegates to
   * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses(Class)}.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration}
   * @return an array of default configuration classes, potentially empty but
   * never {@code null}
   * @see AnnotationConfigContextLoaderUtils
   */
  protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
    return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
  }

  // AbstractContextLoader

  /**
   * {@code AnnotationConfigContextLoader} should be used as a
   * {@link SmartContextLoader SmartContextLoader},
   * not as a legacy {@link ContextLoader ContextLoader}.
   * Consequently, this method is not supported.
   *
   * @throws UnsupportedOperationException in this implementation
   * @see AbstractContextLoader#modifyLocations
   */
  @Override
  protected String[] modifyLocations(Class<?> clazz, String... locations) {
    throw new UnsupportedOperationException(
            "AnnotationConfigContextLoader does not support the modifyLocations(Class, String...) method");
  }

  /**
   * {@code AnnotationConfigContextLoader} should be used as a
   * {@link SmartContextLoader SmartContextLoader},
   * not as a legacy {@link ContextLoader ContextLoader}.
   * Consequently, this method is not supported.
   *
   * @throws UnsupportedOperationException in this implementation
   * @see AbstractContextLoader#generateDefaultLocations
   */
  @Override
  protected String[] generateDefaultLocations(Class<?> clazz) {
    throw new UnsupportedOperationException(
            "AnnotationConfigContextLoader does not support the generateDefaultLocations(Class) method");
  }

  /**
   * {@code AnnotationConfigContextLoader} should be used as a
   * {@link SmartContextLoader SmartContextLoader},
   * not as a legacy {@link ContextLoader ContextLoader}.
   * Consequently, this method is not supported.
   *
   * @throws UnsupportedOperationException in this implementation
   * @see AbstractContextLoader#getResourceSuffix
   */
  @Override
  protected String getResourceSuffix() {
    throw new UnsupportedOperationException(
            "AnnotationConfigContextLoader does not support the getResourceSuffix() method");
  }

  // AbstractGenericContextLoader

  /**
   * Ensure that the supplied {@link MergedContextConfiguration} does not
   * contain {@link MergedContextConfiguration#getLocations() locations}.
   *
   * @see AbstractGenericContextLoader#validateMergedContextConfiguration
   * @since 4.0
   */
  @Override
  protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    if (mergedConfig.hasLocations()) {
      String msg = String.format("Test class [%s] has been configured with @ContextConfiguration's 'locations' " +
                      "(or 'value') attribute %s, but %s does not support resource locations.",
              mergedConfig.getTestClass().getName(), ObjectUtils.nullSafeToString(mergedConfig.getLocations()),
              getClass().getSimpleName());
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
  }

  /**
   * Register classes in the supplied {@link GenericApplicationContext context}
   * from the classes in the supplied {@link MergedContextConfiguration}.
   * <p>Each class must represent a <em>component class</em>. An
   * {@link AnnotatedBeanDefinitionReader} is used to register the appropriate
   * bean definitions.
   * <p>Note that this method does not call {@link #createBeanDefinitionReader}
   * since {@code AnnotatedBeanDefinitionReader} is not an instance of
   * {@link BeanDefinitionReader}.
   *
   * @param context the context in which the component classes should be registered
   * @param mergedConfig the merged configuration from which the classes should be retrieved
   * @see AbstractGenericContextLoader#loadBeanDefinitions
   */
  @Override
  protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
    Class<?>[] componentClasses = mergedConfig.getClasses();
    if (logger.isDebugEnabled()) {
      logger.debug("Registering component classes: " + ObjectUtils.nullSafeToString(componentClasses));
    }
    new AnnotatedBeanDefinitionReader(context).register(componentClasses);
  }

  /**
   * {@code AnnotationConfigContextLoader} should be used as a
   * {@link SmartContextLoader SmartContextLoader},
   * not as a legacy {@link ContextLoader ContextLoader}.
   * Consequently, this method is not supported.
   *
   * @throws UnsupportedOperationException in this implementation
   * @see #loadBeanDefinitions
   * @see AbstractGenericContextLoader#createBeanDefinitionReader
   */
  @Override
  protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
    throw new UnsupportedOperationException(
            "AnnotationConfigContextLoader does not support the createBeanDefinitionReader(GenericApplicationContext) method");
  }

}
