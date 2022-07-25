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
package cn.taketoday.bytecode.proxysample;

import java.lang.reflect.Method;

import cn.taketoday.bytecode.proxy.InvocationHandler;

/**
 * @author neeme
 */
public class InvocationHandlerSample implements InvocationHandler {

  private Object o;

  /**
   * Constructor for InvocationHandlerSample.
   */
  public InvocationHandlerSample(Object o) {
    this.o = o;
  }

  public Object invoke(Object proxy, Method method, Object[] args)
          throws Throwable {
    System.out.println("invoke() start");
    System.out.println("    method: " + method.getName());
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        System.out.println("    arg: " + args[i]);
      }
    }
    Object r = method.invoke(o, args);
    System.out.println("    return: " + r);
    System.out.println("invoke() end");
    return r;
  }

}
