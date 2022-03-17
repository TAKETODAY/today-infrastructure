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

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.test.context.support.AbstractDelegatingSmartContextLoader;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.util.ClassUtils;

/**
 * {@code WebDelegatingSmartContextLoader} is a concrete implementation of
 * {@link AbstractDelegatingSmartContextLoader} that delegates to a
 * {@link GenericXmlWebContextLoader} (or a {@link GenericGroovyXmlWebContextLoader} if
 * Groovy is present on the classpath) and an {@link AnnotationConfigWebContextLoader}.
 *
 * @author Sam Brannen
 * @see SmartContextLoader
 * @see AbstractDelegatingSmartContextLoader
 * @see GenericXmlWebContextLoader
 * @see AnnotationConfigWebContextLoader
 * @since 4.0
 */
public class WebDelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

  private static final String GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME = "cn.taketoday.test.context.web.GenericGroovyXmlWebContextLoader";

  private static final boolean groovyPresent = ClassUtils.isPresent("groovy.lang.Closure",
          WebDelegatingSmartContextLoader.class.getClassLoader())
          && ClassUtils.isPresent(GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME,
          WebDelegatingSmartContextLoader.class.getClassLoader());

  private final SmartContextLoader xmlLoader;
  private final SmartContextLoader annotationConfigLoader;

  public WebDelegatingSmartContextLoader() {
    if (groovyPresent) {
      try {
        Class<?> loaderClass = ClassUtils.forName(GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME,
                WebDelegatingSmartContextLoader.class.getClassLoader());
        this.xmlLoader = (SmartContextLoader) BeanUtils.newInstance(loaderClass);
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Failed to enable support for Groovy scripts; "
                + "could not load class: " + GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME, ex);
      }
    }
    else {
      this.xmlLoader = new GenericXmlWebContextLoader();
    }

    this.annotationConfigLoader = new AnnotationConfigWebContextLoader();
  }

  @Override
  protected SmartContextLoader getXmlLoader() {
    return this.xmlLoader;
  }

  @Override
  protected SmartContextLoader getAnnotationConfigLoader() {
    return this.annotationConfigLoader;
  }

}
