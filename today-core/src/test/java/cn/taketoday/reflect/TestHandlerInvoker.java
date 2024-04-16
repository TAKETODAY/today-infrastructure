/*
 * Copyright 2017 - 2024 the original author or authors.
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
package cn.taketoday.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * @author TODAY <br>
 * 2019-10-18 23:34
 */
public class TestHandlerInvoker {

  @Test
  public void testAll() throws Exception {
    main();
  }

//  public class MethodInvoker0 extends MethodInvoker {
//
//    public MethodInvoker0(Method method) {
//      super(method);
//    }
//
//    @Override
//    public Object invoke(Object obj, Object[] args) {
//      return ((Bean) obj).returnString((String) args[0]);
//    }
//
//  }

  public static void main(String... args) throws Exception {

//    System.setProperty("bytecode.debugLocation", "D:/dev/temp/debug");
    {
      final Method main = Bean.class.getDeclaredMethod("main");
      final MethodInvoker mainInvoker = MethodInvoker.forMethod(main);
      mainInvoker.invoke(null, null);
    }
    {
      final Method test = Bean.class.getDeclaredMethod("test", short.class);
      final MethodInvoker mainInvoker = MethodInvoker.forMethod(test);
      mainInvoker.invoke(null, new Object[] { (short) 1 });
      mainInvoker.invoke(null, new Object[] { null });
    }

    final MethodInvoker create = MethodInvoker.forMethod(Bean.class, "test");
    create.invoke(new Bean(), null);

    final MethodInvoker itself = MethodInvoker.forMethod(Bean.class, "test", Bean.class);
    itself.invoke(new Bean(), new Object[] { new Bean() });

    final MethodInvoker returnString = MethodInvoker.forMethod(Bean.class, "returnString", String.class);
    final Object invoke1 = returnString.invoke(new Bean(), new Object[] { "TODAY" });
    System.out.println(invoke1);
  }

  private static class Bean1 {

    public Bean1(String name) {
      System.out.println("构造参数: " + name);
    }
  }

  public static class Bean {

    public String returnString(String input) {
      return "input->" + input;
    }

    public static void test(short i) throws Throwable {
      System.err.println("static main " + i);
    }

    protected static void main() throws Throwable {
      System.err.println("static main");
    }

    public void test() throws Throwable {
      System.err.println("instance test");
    }

    void test(Bean itself) {
      System.err.println("instance test :" + itself);
    }

  }

//  private void privateMethod() {
//    System.out.println("privateMethod");
//  }
//
//  public static final class Access {
//
//    void invoke() {
//      TestHandlerInvoker invoker = new TestHandlerInvoker();
//      invoker.privateMethod();
//    }
//
//  }

}
