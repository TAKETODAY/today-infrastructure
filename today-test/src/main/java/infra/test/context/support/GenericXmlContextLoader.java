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

package infra.test.context.support;

import infra.beans.factory.support.BeanDefinitionReader;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.context.support.GenericApplicationContext;
import infra.test.context.MergedContextConfiguration;
import infra.util.ObjectUtils;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} that reads
 * bean definitions from XML resources.
 *
 * <p>Default resource locations are detected using the suffix
 * {@code "-context.xml"}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see XmlBeanDefinitionReader
 * @see AnnotationConfigContextLoader
 * @since 4.0
 */
public class GenericXmlContextLoader extends AbstractGenericContextLoader {

  /**
   * Create a new {@link XmlBeanDefinitionReader}.
   *
   * @return a new {@code XmlBeanDefinitionReader}
   */
  @Override
  protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
    return new XmlBeanDefinitionReader(context);
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
   * Ensure that the supplied {@link MergedContextConfiguration} does not
   * contain {@link MergedContextConfiguration#getClasses() classes}.
   *
   * @see AbstractGenericContextLoader#validateMergedContextConfiguration
   */
  @Override
  protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    if (mergedConfig.hasClasses()) {
      String msg = String.format(
              "Test class [%s] has been configured with @ContextConfiguration's 'classes' attribute %s, "
                      + "but %s does not support annotated classes.", mergedConfig.getTestClass().getName(),
              ObjectUtils.nullSafeToString(mergedConfig.getClasses()), getClass().getSimpleName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }
  }

}
