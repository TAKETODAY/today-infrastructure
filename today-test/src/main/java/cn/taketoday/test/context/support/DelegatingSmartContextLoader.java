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

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.util.ClassUtils;

/**
 * {@code DelegatingSmartContextLoader} is a concrete implementation of
 * {@link AbstractDelegatingSmartContextLoader} that delegates to a
 * {@link GenericXmlContextLoader} (or a {@link GenericGroovyXmlContextLoader} if Groovy
 * is present in the classpath) and an {@link AnnotationConfigContextLoader}.
 *
 * @author Sam Brannen
 * @see SmartContextLoader
 * @see AbstractDelegatingSmartContextLoader
 * @see GenericXmlContextLoader
 * @see GenericGroovyXmlContextLoader
 * @see AnnotationConfigContextLoader
 *@since 4.0
 */
public class DelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

  private static final String GROOVY_XML_CONTEXT_LOADER_CLASS_NAME = "cn.taketoday.test.context.support.GenericGroovyXmlContextLoader";

  private static final boolean groovyPresent = ClassUtils.isPresent("groovy.lang.Closure",
          DelegatingSmartContextLoader.class.getClassLoader())
          && ClassUtils.isPresent(GROOVY_XML_CONTEXT_LOADER_CLASS_NAME,
          DelegatingSmartContextLoader.class.getClassLoader());

  private final SmartContextLoader xmlLoader;
  private final SmartContextLoader annotationConfigLoader;

  public DelegatingSmartContextLoader() {
    if (groovyPresent) {
      try {
        Class<?> loaderClass = ClassUtils.forName(GROOVY_XML_CONTEXT_LOADER_CLASS_NAME,
                DelegatingSmartContextLoader.class.getClassLoader());
        this.xmlLoader = (SmartContextLoader) BeanUtils.newInstance(loaderClass);
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Failed to enable support for Groovy scripts; "
                + "could not load class: " + GROOVY_XML_CONTEXT_LOADER_CLASS_NAME, ex);
      }
    }
    else {
      this.xmlLoader = new GenericXmlContextLoader();
    }

    this.annotationConfigLoader = new AnnotationConfigContextLoader();
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
