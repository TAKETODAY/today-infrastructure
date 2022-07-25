/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.bytecode.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Juozas Baliuka <a href="mailto:baliuka@mwm.lt">baliuka@mwm.lt</a>
 * @version $Id: TestInterceptor.java,v 1.3 2004/06/24 21:15:16 herbyderby Exp $
 */
public class TestInterceptor implements MethodInterceptor, Serializable {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  String value;

  public String getValue() {
    return value;
  }

  public TestInterceptor(String ser) {
    value = ser;
  }

  public TestInterceptor() { }

  public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
    System.out.println(method);
    Throwable e = null;
    boolean invokedSuper = false;
    Object retValFromSuper = null;
    if (!Modifier.isAbstract(method.getModifiers()) && invokeSuper(obj, method, args)) {
      invokedSuper = true;
      try {
        retValFromSuper = proxy.invokeSuper(obj, args);
      }
      catch (Throwable t) {
        e = t;
      }
    }
    return afterReturn(obj, method, args, invokedSuper, retValFromSuper, e);
  }

  public boolean invokeSuper(Object obj, Method method, Object[] args) throws Throwable {
    return true;
  }

  public Object afterReturn(Object obj, Method method, Object[] args,
          boolean invokedSuper, Object retValFromSuper,
          Throwable e) throws Throwable {
    if (e != null)
      throw e.fillInStackTrace();
    return retValFromSuper;
  }
}
