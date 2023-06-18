/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.bytecode.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author Chris Nokleberg
 * <a href="mailto:chris@nokleberg.com">chris@nokleberg.com</a>
 * @version $Id: BeanMapProxy.java,v 1.2 2004/06/24 21:15:17 herbyderby Exp $
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BeanMapProxy implements InvocationHandler {
  private Map map;

  public static Object newInstance(Map map, Class[] interfaces) {
    return Proxy.newProxyInstance(map.getClass().getClassLoader(),
            interfaces,
            new BeanMapProxy(map));
  }

  public BeanMapProxy(Map map) {
    this.map = map;
  }

  public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
    String name = m.getName();
    if (name.startsWith("get")) {
      return map.get(name.substring(3));
    }
    else if (name.startsWith("set")) {
      map.put(name.substring(3), args[0]);
      return null;
    }
    return null;
  }
}
