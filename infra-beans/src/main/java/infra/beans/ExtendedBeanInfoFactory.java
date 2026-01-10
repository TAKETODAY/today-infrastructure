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

package infra.beans;

import org.jspecify.annotations.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;

import infra.core.Ordered;

/**
 * {@link BeanInfoFactory} implementation that evaluates whether bean classes have
 * "non-standard" JavaBeans setter methods and are thus candidates for introspection
 * by Framework's (package-visible) {@code ExtendedBeanInfo} implementation.
 *
 * <p>Ordered at {@link Ordered#LOWEST_PRECEDENCE} to allow other user-defined
 * {@link BeanInfoFactory} types to take precedence.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanInfoFactory
 * @see CachedIntrospectionResults
 * @since 4.0 2022/2/23 11:27
 */
public class ExtendedBeanInfoFactory implements BeanInfoFactory, Ordered {

  /**
   * Return an {@link ExtendedBeanInfo} for the given bean class, if applicable.
   */
  @Override
  @Nullable
  public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
    return supports(beanClass) ? new ExtendedBeanInfo(Introspector.getBeanInfo(beanClass)) : null;
  }

  /**
   * Return whether the given bean class declares or inherits any non-void
   * returning bean property or indexed property setter methods.
   */
  private boolean supports(Class<?> beanClass) {
    for (Method method : beanClass.getMethods()) {
      if (ExtendedBeanInfo.isCandidateWriteMethod(method)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

}
