/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.context.support;

import java.util.function.Supplier;

import infra.beans.factory.config.Scope;
import infra.core.AttributeAccessor;
import infra.lang.Nullable;

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
public abstract class AbstractRequestContextScope<T> implements Scope {

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
