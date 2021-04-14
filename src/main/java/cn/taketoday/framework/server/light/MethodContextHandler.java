/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.framework.server.light;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.taketoday.web.RequestContext;

/**
 * The {@code MethodContextHandler} services a context
 * by invoking a handler method on a specified object.
 * <p>
 * The method must have the same signature and contract as
 * {@link ContextHandler#serve}, but can have an arbitrary name.
 *
 * @author TODAY 2021/4/13 10:49
 * @see VirtualHost#addContexts(Object)
 */
public class MethodContextHandler implements ContextHandler {

  protected final Method m;
  protected final Object obj;

  public MethodContextHandler(Method m, Object obj) throws IllegalArgumentException {
    this.m = m;
    this.obj = obj;
    Class<?>[] params = m.getParameterTypes();
    if (params.length != 2
            || !HttpRequest.class.isAssignableFrom(params[0])
            || !Response.class.isAssignableFrom(params[1])
            || !int.class.isAssignableFrom(m.getReturnType()))
      throw new IllegalArgumentException("invalid method signature: " + m);
  }

  public int serve(HttpRequest req, Response resp) throws IOException {
    try {
      return (Integer) m.invoke(obj, req, resp);
    }
    catch (InvocationTargetException ite) {
      throw new IOException("error: " + ite.getCause().getMessage());
    }
    catch (Exception e) {
      throw new IOException("error: " + e);
    }
  }

  @Override public int serve(RequestContext context) throws IOException {
    return 0;
  }
}
