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

import java.util.Properties;

import cn.taketoday.beans.factory.support.BeanDefinitionReader;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.util.ObjectUtils;

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
   * Creates a new {@link cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader}.
   *
   * @return a new PropertiesBeanDefinitionReader
   * @see cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader
   */
  @Override
  protected BeanDefinitionReader createBeanDefinitionReader(final GenericApplicationContext context) {
    return new cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader(context);
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
