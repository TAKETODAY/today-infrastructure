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

package infra.annotation.config.web;

import infra.web.config.annotation.WebMvcConfigurationSupport;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import infra.web.handler.method.RequestMappingHandlerAdapter;
import infra.web.handler.method.RequestMappingHandlerMapping;

/**
 * Interface to register key components of the {@link WebMvcConfigurationSupport} in place
 * of the default ones provided by Web MVC.
 * <p>
 * All custom instances are later processed by Web MVC configurations. A
 * single instance of this component should be registered, otherwise making it impossible
 * to choose from redundant MVC components.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 15:16
 */
public interface WebMvcRegistrations {

  /**
   * Return the custom {@link RequestMappingHandlerMapping} that should be used and
   * processed by the MVC configuration.
   *
   * @return the custom {@link RequestMappingHandlerMapping} instance
   */
  default RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
    return null;
  }

  /**
   * Return the custom {@link RequestMappingHandlerAdapter} that should be used and
   * processed by the MVC configuration.
   *
   * @return the custom {@link RequestMappingHandlerAdapter} instance
   */
  default RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
    return null;
  }

  /**
   * Return the custom {@link ExceptionHandlerAnnotationExceptionHandler} that should be used and
   * processed by the MVC configuration.
   *
   * @return the custom {@link ExceptionHandlerAnnotationExceptionHandler} instance
   */
  default ExceptionHandlerAnnotationExceptionHandler createAnnotationExceptionHandler() {
    return null;
  }

}
