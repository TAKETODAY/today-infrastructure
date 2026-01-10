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

package infra.beans.factory.support;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.Scope;

/**
 * A subclass of {@link BeanCreationException} which indicates that the target scope
 * is not active, e.g. in case of request or session scope.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactory#getBean
 * @see Scope
 * @see BeanDefinition#setScope
 * @since 4.0 2022/1/9 23:23
 */
public class ScopeNotActiveException extends BeanCreationException {

  /**
   * Create a new ScopeNotActiveException.
   *
   * @param beanName the name of the bean requested
   * @param scopeName the name of the target scope
   * @param cause the root cause, typically from {@link Scope#get}
   */
  public ScopeNotActiveException(String beanName, String scopeName, IllegalStateException cause) {
    super(beanName, "Scope '%s' is not active for the current thread; consider defining a scoped proxy for this bean if you intend to refer to it from a singleton"
            .formatted(scopeName), cause);
  }

}
