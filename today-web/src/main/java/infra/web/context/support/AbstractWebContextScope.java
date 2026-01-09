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

package infra.web.context.support;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.beans.factory.config.Scope;
import infra.core.AttributeAccessor;

/**
 * Abstract {@link Scope} implementation that reads from a particular scope
 * in the current thread-bound {@link AttributeAccessor} object.
 *
 * <p>Subclasses may wish to override the {@link #doGetBean} and {@link #remove}
 * methods to add synchronization around the call back into this super class.
 *
 * @param <T> context type
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 21:31
 */
public abstract class AbstractWebContextScope<T> implements Scope {

  protected final Object doGetBean(T context, String beanName, Supplier<?> objectFactory) {
    Object scopedObject = getAttribute(context, beanName);
    if (scopedObject == null) {
      scopedObject = objectFactory.get();
      setAttribute(context, beanName, scopedObject);
      // Retrieve object again, registering it for implicit session attribute updates.
      // As a bonus, we also allow for potential decoration at the getAttribute level.
      Object retrievedObject = getAttribute(context, beanName);
      if (retrievedObject != null) {
        // Only proceed with retrieved object if still present (the expected case).
        // If it disappeared concurrently, we return our locally created instance.
        scopedObject = retrievedObject;
      }
    }
    return scopedObject;
  }

  @Nullable
  protected Object remove(T context, String name) {
    Object scopedObject = getAttribute(context, name);
    if (scopedObject != null) {
      removeAttribute(context, name);
      return scopedObject;
    }
    else {
      return null;
    }
  }

  // Abstract stuff

  protected abstract void setAttribute(T context, String beanName, Object scopedObject);

  @Nullable
  protected abstract Object getAttribute(T context, String beanName);

  protected abstract void removeAttribute(T context, String name);

}
