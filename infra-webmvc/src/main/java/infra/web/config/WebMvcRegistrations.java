/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.config;

import org.jspecify.annotations.Nullable;

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
  @Nullable
  default RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
    return null;
  }

  /**
   * Return the custom {@link RequestMappingHandlerAdapter} that should be used and
   * processed by the MVC configuration.
   *
   * @return the custom {@link RequestMappingHandlerAdapter} instance
   */
  @Nullable
  default RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
    return null;
  }

  /**
   * Return the custom {@link ExceptionHandlerAnnotationExceptionHandler} that should be used and
   * processed by the MVC configuration.
   *
   * @return the custom {@link ExceptionHandlerAnnotationExceptionHandler} instance
   */
  @Nullable
  default ExceptionHandlerAnnotationExceptionHandler createAnnotationExceptionHandler() {
    return null;
  }

}
