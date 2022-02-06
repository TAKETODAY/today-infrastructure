/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.scope;

import java.util.function.Supplier;

import cn.taketoday.beans.factory.Scope;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 21:31
 */
public abstract class AbstractRequestAttributesScope implements Scope {

  @Override
  public Object get(String beanName, Supplier<?> objectFactory) {
    RequestContext context = RequestContextHolder.getRequired();
    Object scopedObject = getAttribute(beanName, context);
    if (scopedObject == null) {
      scopedObject = objectFactory.get();
      setAttribute(context, beanName, scopedObject);
      // Retrieve object again, registering it for implicit session attribute updates.
      // As a bonus, we also allow for potential decoration at the getAttribute level.
      Object retrievedObject = getAttribute(beanName, context);
      if (retrievedObject != null) {
        // Only proceed with retrieved object if still present (the expected case).
        // If it disappeared concurrently, we return our locally created instance.
        scopedObject = retrievedObject;
      }
    }
    return scopedObject;
  }

  protected void setAttribute(RequestContext context, String beanName, Object scopedObject) {
    context.setAttribute(beanName, scopedObject);
  }

  protected Object getAttribute(String beanName, RequestContext context) {
    return context.getAttribute(beanName);
  }

  @Override
  public Object remove(String name) {
    RequestContext context = RequestContextHolder.currentContext();
    Object scopedObject = getAttribute(name, context);
    if (scopedObject != null) {
      removeAttribute(context, name);
      return scopedObject;
    }
    else {
      return null;
    }
  }

  protected void removeAttribute(RequestContext context, String name) {
    context.removeAttribute(name);
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    RequestContext context = RequestContextHolder.currentContext();
    // registerDestructionCallback(name, callback); TODO
  }

  @Override
  @Nullable
  public Object resolveContextualObject(String key) {
    RequestContext context = RequestContextHolder.currentContext();
//    return context.resolveReference(key);
    return null;
  }

}
