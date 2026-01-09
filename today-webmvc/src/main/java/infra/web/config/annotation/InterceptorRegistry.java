/*
 * Copyright 2002-present the original author or authors.
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

package infra.web.config.annotation;

import java.util.ArrayList;
import java.util.List;

import infra.core.OrderComparator;
import infra.web.HandlerInterceptor;

/**
 * Helps with configuring a list of mapped interceptors.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/8/30 21:43
 */
public class InterceptorRegistry {

  private final ArrayList<InterceptorRegistration> registrations = new ArrayList<>();

  /**
   * Adds the provided {@link HandlerInterceptor}.
   *
   * @param interceptor the interceptor to add
   * @return an {@link InterceptorRegistration} that allows you optionally configure the
   * registered interceptor further for example adding URL patterns it should apply to.
   */
  public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
    InterceptorRegistration registration = new InterceptorRegistration(interceptor);
    registrations.add(registration);
    return registration;
  }

  /**
   * Return all registered interceptors.
   */
  protected List<Object> getInterceptors() {
    OrderComparator.sort(registrations);
    ArrayList<Object> interceptors = new ArrayList<>(registrations.size());
    for (InterceptorRegistration registration : registrations) {
      interceptors.add(registration.getInterceptor());
    }
    return interceptors;
  }

}
