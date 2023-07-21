/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.support.AbstractDelegatingSmartContextLoader;

/**
 * {@code WebDelegatingSmartContextLoader} is a concrete implementation of
 * {@link AbstractDelegatingSmartContextLoader} that delegates to a
 * {@link GenericXmlWebContextLoader} and an {@link AnnotationConfigWebContextLoader}.
 *
 * @author Sam Brannen
 * @see SmartContextLoader
 * @see AbstractDelegatingSmartContextLoader
 * @see GenericXmlWebContextLoader
 * @see AnnotationConfigWebContextLoader
 * @since 4.0
 */
public class WebDelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

  private final SmartContextLoader xmlLoader;
  private final SmartContextLoader annotationConfigLoader;

  public WebDelegatingSmartContextLoader() {
    this.xmlLoader = new GenericXmlWebContextLoader();
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
