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

package cn.taketoday.aop.testfixture.testfixture.advice;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import cn.taketoday.aop.ThrowsAdvice;

@SuppressWarnings("serial")
public class MyThrowsHandler extends MethodCounter implements ThrowsAdvice {

  // Full method signature
  public void afterThrowing(Method m, Object[] args, Object target, IOException ex) {
    count("ioException");
  }

  public void afterThrowing(RemoteException ex) throws Throwable {
    count("remoteException");
  }

  /** Not valid, wrong number of arguments */
  public void afterThrowing(Method m, Exception ex) throws Throwable {
    throw new UnsupportedOperationException("Shouldn't be called");
  }

}
