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

import infra.test.context.SmartContextLoader;

/**
 * {@code DelegatingSmartContextLoader} is a concrete implementation of
 * {@link AbstractDelegatingSmartContextLoader} that delegates to a
 * {@link GenericXmlContextLoader} {@link AnnotationConfigContextLoader}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SmartContextLoader
 * @see AbstractDelegatingSmartContextLoader
 * @see GenericXmlContextLoader
 * @see AnnotationConfigContextLoader
 * @since 4.0
 */
public class DelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

  private final SmartContextLoader xmlLoader;
  private final SmartContextLoader annotationConfigLoader;

  public DelegatingSmartContextLoader() {
    this.xmlLoader = new GenericXmlContextLoader();
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
