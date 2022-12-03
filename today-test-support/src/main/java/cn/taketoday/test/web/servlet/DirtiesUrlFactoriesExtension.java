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

package cn.taketoday.test.web.servlet;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.InaccessibleObjectException;
import java.net.URL;

import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.util.ClassUtils;

/**
 * JUnit extension used by {@link DirtiesUrlFactories @DirtiesUrlFactories}.
 *
 * @author Phillip Webb
 */
class DirtiesUrlFactoriesExtension implements BeforeEachCallback, AfterEachCallback {

  private static final String TOMCAT_URL_STREAM_HANDLER_FACTORY = "org.apache.catalina.webresources.TomcatURLStreamHandlerFactory";

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    reset();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    reset();
  }

  private void reset() {
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      if (ClassUtils.isPresent(TOMCAT_URL_STREAM_HANDLER_FACTORY, classLoader)) {
        Class<?> factoryClass = ClassUtils.resolveClassName(TOMCAT_URL_STREAM_HANDLER_FACTORY, classLoader);
        ReflectionTestUtils.setField(factoryClass, "instance", null);
      }
      ReflectionTestUtils.setField(URL.class, "factory", null);
    }
    catch (InaccessibleObjectException ex) {
      throw new IllegalStateException(
              "Unable to reset field. Please run with '--add-opens=java.base/java.net=ALL-UNNAMED'", ex);
    }
  }

}
