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

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.support.DefaultTestContextBootstrapper;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.test.context.TestContextBootstrapper;

/**
 * Web-specific implementation of the {@link TestContextBootstrapper} SPI.
 *
 * <ul>
 * <li>Uses {@link WebDelegatingSmartContextLoader} as the default {@link ContextLoader}
 * if the test class is annotated with {@link WebAppConfiguration @WebAppConfiguration}
 * and otherwise delegates to the superclass.
 * <li>Builds a {@link WebMergedContextConfiguration} if the test class is annotated
 * with {@link WebAppConfiguration @WebAppConfiguration}.
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class WebTestContextBootstrapper extends DefaultTestContextBootstrapper {

  /**
   * Returns {@link WebDelegatingSmartContextLoader} if the supplied class is
   * annotated with {@link WebAppConfiguration @WebAppConfiguration} and
   * otherwise delegates to the superclass.
   */
  @Override
  protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
    if (getWebAppConfiguration(testClass) != null) {
      return WebDelegatingSmartContextLoader.class;
    }
    else {
      return super.getDefaultContextLoaderClass(testClass);
    }
  }

  /**
   * Returns a {@link WebMergedContextConfiguration} if the test class in the
   * supplied {@code MergedContextConfiguration} is annotated with
   * {@link WebAppConfiguration @WebAppConfiguration} and otherwise returns
   * the supplied instance unmodified.
   */
  @Override
  protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    WebAppConfiguration webAppConfiguration = getWebAppConfiguration(mergedConfig.getTestClass());
    if (webAppConfiguration != null) {
      return new WebMergedContextConfiguration(mergedConfig, webAppConfiguration.value());
    }
    else {
      return mergedConfig;
    }
  }

  @Nullable
  private static WebAppConfiguration getWebAppConfiguration(Class<?> testClass) {
    return TestContextAnnotationUtils.findMergedAnnotation(testClass, WebAppConfiguration.class);
  }

}
