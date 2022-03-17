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
package cn.taketoday.core.bytecode.proxysample;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.UndeclaredThrowableException;

public final class ProxySample implements ProxySampleInterface_ReturnsObject, ProxySampleInterface_ReturnsBasic {

  private InvocationHandler handler = null;

  protected ProxySample(InvocationHandler handler) {
    this.handler = handler;
  }

  public String getKala(String kalamees) throws Exception {
    String result = null;
    try {
      // invocation is also generated
      result = (String) handler.invoke(this, ProxySampleInterface_ReturnsObject.class.getMethod("getKala",
                      new Class[]
                              { String.class }),
              new Object[]
                      { kalamees });
    }
    catch (ClassCastException e) {
      throw e;
    }
    catch (NoSuchMethodException e) {
      throw new Error(e.getMessage());
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      // generated: catch the exception throwed by interface method and re-throw it
      throw e;
    }
    catch (Error e) {
      throw e;
    }
    catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
    return result;
  }

  public int getKala(float kalamees) {
    Integer result = null;
    try {
      // invocation is also generated
      result = (Integer) handler.invoke(this, ProxySampleInterface_ReturnsBasic.class.getMethod("getKala",
                      Float.TYPE),
              new Object[] { kalamees });
    }
    catch (ClassCastException e) {
      throw e;
    }
    catch (NoSuchMethodException e) {
      // ignore, the method has to be found, as this class is generated
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Error e) {
      throw e;
    }
    catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
    return result.intValue();
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    String result = null;
    try {
      // invocation is also generated
      result = (String) handler.invoke(this, Object.class.getMethod("toString", (Class[]) null), null);
    }
    catch (ClassCastException e) {
      throw e;
    }
    catch (NoSuchMethodException e) {
      // ignore, the method has to be found, as this class is generated
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Error e) {
      throw e;
    }
    catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
    return result;
  }

}
