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

package cn.taketoday.test.context.web;

import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.support.AbstractContextLoader;
import cn.taketoday.test.context.support.AnnotationConfigContextLoaderUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.servlet.support.GenericWebApplicationContext;

/**
 * Concrete implementation of {@link AbstractGenericWebContextLoader} that loads
 * bean definitions from annotated classes.
 *
 * <p>See the Javadoc for
 * {@link ContextConfiguration @ContextConfiguration}
 * for a definition of <em>annotated class</em>.
 *
 * <p>Note: {@code AnnotationConfigWebContextLoader} supports <em>annotated classes</em>
 * rather than the String-based resource locations defined by the legacy
 * {@link ContextLoader ContextLoader} API. Thus,
 * although {@code AnnotationConfigWebContextLoader} extends
 * {@code AbstractGenericWebContextLoader}, {@code AnnotationConfigWebContextLoader}
 * does <em>not</em> support any String-based methods defined by
 * {@link AbstractContextLoader
 * AbstractContextLoader} or {@code AbstractGenericWebContextLoader}.
 * Consequently, {@code AnnotationConfigWebContextLoader} should chiefly be
 * considered a {@link SmartContextLoader SmartContextLoader}
 * rather than a {@link ContextLoader ContextLoader}.
 *
 * @author Sam Brannen
 * @see #processContextConfiguration(ContextConfigurationAttributes)
 * @see #detectDefaultConfigurationClasses(Class)
 * @see #loadBeanDefinitions(GenericWebApplicationContext, WebMergedContextConfiguration)
 * @see GenericXmlWebContextLoader
 * @since 4.0
 */
public class AnnotationConfigWebContextLoader extends AbstractGenericWebContextLoader {

  private static final Logger logger = LoggerFactory.getLogger(AnnotationConfigWebContextLoader.class);

  // SmartContextLoader

  /**
   * Process <em>annotated classes</em> in the supplied {@link ContextConfigurationAttributes}.
   * <p>If the <em>annotated classes</em> are {@code null} or empty and
   * {@link #isGenerateDefaultLocations()} returns {@code true}, this
   * {@code SmartContextLoader} will attempt to {@linkplain
   * #detectDefaultConfigurationClasses detect default configuration classes}.
   * If defaults are detected they will be
   * {@linkplain ContextConfigurationAttributes#setClasses(Class[]) set} in the
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

  /**
   * Detect the default configuration classes for the supplied test class.
   * <p>The default implementation simply delegates to
   * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses(Class)}.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration}
   * @return an array of default configuration classes, potentially empty but never {@code null}
   * @see AnnotationConfigContextLoaderUtils
   */
  protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
    return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
  }

  // AbstractContextLoader

  /**
   * {@code AnnotationConfigWebContextLoader} should be used as a
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
            "AnnotationConfigWebContextLoader does not support the modifyLocations(Class, String...) method");
  }

  /**
   * {@code AnnotationConfigWebContextLoader} should be used as a
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
            "AnnotationConfigWebContextLoader does not support the generateDefaultLocations(Class) method");
  }

  /**
   * {@code AnnotationConfigWebContextLoader} should be used as a
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
            "AnnotationConfigWebContextLoader does not support the getResourceSuffix() method");
  }

  // AbstractGenericWebContextLoader

  /**
   * Register classes in the supplied {@linkplain GenericWebApplicationContext context}
   * from the classes in the supplied {@link WebMergedContextConfiguration}.
   * <p>Each class must represent an <em>annotated class</em>. An
   * {@link AnnotatedBeanDefinitionReader} is used to register the appropriate
   * bean definitions.
   *
   * @param context the context in which the annotated classes should be registered
   * @param webMergedConfig the merged configuration from which the classes should be retrieved
   * @see AbstractGenericWebContextLoader#loadBeanDefinitions
   */
  @Override
  protected void loadBeanDefinitions(
          GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {

    Class<?>[] annotatedClasses = webMergedConfig.getClasses();
    if (logger.isDebugEnabled()) {
      logger.debug("Registering annotated classes: " + ObjectUtils.nullSafeToString(annotatedClasses));
    }
    new AnnotatedBeanDefinitionReader(context).register(annotatedClasses);
  }

  /**
   * Ensure that the supplied {@link WebMergedContextConfiguration} does not
   * contain {@link MergedContextConfiguration#getLocations() locations}.
   *
   * @see AbstractGenericWebContextLoader#validateMergedContextConfiguration
   * @since 4.0
   */
  @Override
  protected void validateMergedContextConfiguration(WebMergedContextConfiguration webMergedConfig) {
    if (webMergedConfig.hasLocations()) {
      String msg = String.format("Test class [%s] has been configured with @ContextConfiguration's 'locations' " +
                      "(or 'value') attribute %s, but %s does not support resource locations.",
              webMergedConfig.getTestClass().getName(),
              ObjectUtils.nullSafeToString(webMergedConfig.getLocations()), getClass().getSimpleName());
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
  }

}
