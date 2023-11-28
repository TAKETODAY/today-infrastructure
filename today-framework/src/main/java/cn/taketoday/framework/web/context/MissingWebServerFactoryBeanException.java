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

package cn.taketoday.framework.web.context;

import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.web.server.WebServerFactory;

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

  private final ApplicationType webApplicationType;

  /**
   * Create a new {@code MissingWebServerFactoryBeanException}.
   *
   * @param webServerApplicationContextClass the class of the
   * WebServerApplicationContext that required the WebServerFactory
   * @param webServerFactoryClass the class of the WebServerFactory that was missing
   * @param webApplicationType the type of the web application
   */
  public MissingWebServerFactoryBeanException(Class<? extends WebServerApplicationContext> webServerApplicationContextClass,
          Class<? extends WebServerFactory> webServerFactoryClass, ApplicationType webApplicationType) {
    super(webServerFactoryClass, String.format("Unable to start %s due to missing %s bean",
            webServerApplicationContextClass.getSimpleName(), webServerFactoryClass.getSimpleName()));
    this.webApplicationType = webApplicationType;
  }

  /**
   * Returns the type of web application for which a {@link WebServerFactory} bean was
   * missing.
   *
   * @return the type of web application
   */
  public ApplicationType getWebApplicationType() {
    return this.webApplicationType;
  }

}
