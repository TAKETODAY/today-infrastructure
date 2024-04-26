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

package cn.taketoday.web.server.context;

import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.web.server.WebServerFactory;

/**
 * Exception thrown when there is no {@link WebServerFactory} bean of the required type
 * defined in a {@link WebServerApplicationContext}.
 *
 * @author Guirong Hu
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 21:22
 */
public class MissingWebServerFactoryBeanException extends NoSuchBeanDefinitionException {

  private final Class<? extends WebServerFactory> webServerFactoryClass;

  /**
   * Create a new {@code MissingWebServerFactoryBeanException}.
   *
   * @param contextClass the class of the WebServerApplicationContext that required the WebServerFactory
   * @param webServerFactoryClass the class of the WebServerFactory that was missing
   */
  public MissingWebServerFactoryBeanException(Class<? extends WebServerApplicationContext> contextClass,
          Class<? extends WebServerFactory> webServerFactoryClass) {
    super(webServerFactoryClass, String.format("Unable to start %s due to missing %s bean",
            contextClass.getSimpleName(), webServerFactoryClass.getSimpleName()));
    this.webServerFactoryClass = webServerFactoryClass;
  }

  /**
   * Returns the type of {@link WebServerFactory} bean was missing.
   *
   * @return the type of WebServerFactory class
   */
  public Class<? extends WebServerFactory> getWebServerFactoryClass() {
    return webServerFactoryClass;
  }
}
