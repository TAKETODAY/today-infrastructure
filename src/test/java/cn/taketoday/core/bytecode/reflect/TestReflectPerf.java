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
package cn.taketoday.core.bytecode.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.core.reflect.MethodInvoker;

public class TestReflectPerf {
  public interface IndexOf {
    int indexOf(String s, int start);
  }

  @Test
  public void testReflectPerf() throws Throwable {
    int iterations = 1000000/*_0*/;
    System.out.println();
    System.out.println("iteration count: " + iterations);

    String test = "abcabcabc";

    Class<?>[] types = new Class[] { String.class, Integer.TYPE };
    Method indexOf = String.class.getDeclaredMethod("indexOf", types);
    MethodAccess fc = MethodAccess.from(String.class);
    FastMethodAccessor fm = fc.getMethod("indexOf", types);
    int fidx = fm.getIndex();
    Object[] args = new Object[] { "ab", 1 };

    IndexOf fast = MethodDelegate.create(test, "indexOf", IndexOf.class);

    MethodInvoker methodInvoker = MethodInvoker.fromMethod(indexOf);

    int result;
    long t0 = System.currentTimeMillis();

    for (int i = 0; i < iterations; i++) {
      result = ((Integer) methodInvoker.invoke("ab", args)).intValue();
    }

    long t1 = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      result = ((Integer) fc.invoke("indexOf", types, test, args)).intValue();
    }
    long t2 = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      args = new Object[] { "ab", 1 };
      result = ((Integer) indexOf.invoke(test, args)).intValue();
    }
    long t3 = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      result = ((Integer) indexOf.invoke(test, args)).intValue();
    }
    long t4 = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      args = new Object[] { "ab", 1 };
      result = ((Integer) fm.invoke(test, args)).intValue();
    }
    long t5 = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      result = ((Integer) fm.invoke(test, args)).intValue();
    }
    long t6 = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      result = ((Integer) fc.invoke(fidx, test, args)).intValue();
    }
    long t7 = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      result = fast.indexOf("ab", 1);
    }
    long t8 = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      result = test.indexOf("ab", 1);
    }
    long t9 = System.currentTimeMillis();

    System.out.println("fc           = " + (t2 - t1) + "\n" + "reflect+args = " + (t3 - t2) + "\n" + "reflect      = " + (t4 - t3)
            + "\n" + "fm+args      = " + (t5 - t4) + "\n" + "fm           = " + (t6 - t5) + "\n" + "fc w/idx     = "
            + (t7 - t6) + "\n" + "delegate     = " + (t8 - t7) + "\n" + "raw          = " + (t9 - t8)
            + "  \n methodInvoker    = " + (t1 - t0));
  }

}
