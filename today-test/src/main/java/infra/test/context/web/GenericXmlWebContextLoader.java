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

package infra.test.context.web;

import java.util.Arrays;

import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.test.context.MergedContextConfiguration;
import infra.web.mock.support.GenericWebApplicationContext;

/**
 * Concrete implementation of {@link AbstractGenericWebContextLoader} that loads
 * bean definitions from XML resources.
 *
 * <p>Default resource locations are detected using the suffix
 * {@code "-context.xml"}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationConfigWebContextLoader
 * @since 4.0
 */
public class GenericXmlWebContextLoader extends AbstractGenericWebContextLoader {

  /**
   * Load bean definitions into the supplied {@link GenericWebApplicationContext context}
   * from the locations in the supplied {@code WebMergedContextConfiguration}, using an
   * {@link XmlBeanDefinitionReader}.
   *
   * @see AbstractGenericWebContextLoader#loadBeanDefinitions
   */
  @Override
  protected void loadBeanDefinitions(GenericWebApplicationContext context,
          WebMergedContextConfiguration webMergedConfig) {
    new XmlBeanDefinitionReader(context).loadBeanDefinitions(webMergedConfig.getLocations());
  }

  /**
   * Returns {@code "-context.xml"} in order to support detection of a
   * default XML config file.
   */
  @Override
  protected String getResourceSuffix() {
    return "-context.xml";
  }

  /**
   * Ensure that the supplied {@link WebMergedContextConfiguration} does not
   * contain {@link MergedContextConfiguration#getClasses() classes}.
   *
   * @see AbstractGenericWebContextLoader#validateMergedContextConfiguration
   */
  @Override
  protected void validateMergedContextConfiguration(WebMergedContextConfiguration webMergedConfig) {
    if (webMergedConfig.hasClasses()) {
      String msg = """
              Test class [%s] has been configured with @ContextConfiguration's 'classes' \
              attribute %s, but %s does not support annotated classes."""
              .formatted(webMergedConfig.getTestClass().getName(),
                      Arrays.toString(webMergedConfig.getClasses()), getClass().getSimpleName());
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
  }

}
