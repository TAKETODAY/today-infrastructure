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

import java.util.Properties;

import infra.beans.factory.support.BeanDefinitionReader;
import infra.beans.factory.support.PropertiesBeanDefinitionReader;
import infra.context.support.GenericApplicationContext;
import infra.test.context.MergedContextConfiguration;
import infra.util.ObjectUtils;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} that reads
 * bean definitions from Java {@link Properties} resources.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/2 10:53
 */
public class GenericPropertiesContextLoader extends AbstractGenericContextLoader {

  /**
   * Creates a new {@link PropertiesBeanDefinitionReader}.
   *
   * @return a new PropertiesBeanDefinitionReader
   * @see PropertiesBeanDefinitionReader
   */
  @Override
  protected BeanDefinitionReader createBeanDefinitionReader(final GenericApplicationContext context) {
    return new PropertiesBeanDefinitionReader(context);
  }

  /**
   * Returns &quot;{@code -context.properties}&quot;.
   */
  @Override
  protected String getResourceSuffix() {
    return "-context.properties";
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
