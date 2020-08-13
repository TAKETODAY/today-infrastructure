/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.context.invoker;

import java.lang.reflect.Method;

import org.junit.Test;

import cn.taketoday.context.invoker.MethodInvoker;

/**
 * @author TODAY <br>
 *         2019-10-18 23:34
 */
public class TestHandlerInvoker {

    @Test
    public void testAll() throws Exception {
        main();
    }

    public static void main(String... args) throws Exception {

        System.setProperty("cglib.debugLocation", "D:/debug");
        {
            final Method main = Bean.class.getDeclaredMethod("main");
            final MethodInvoker mainInvoker = MethodInvoker.create(main);
            mainInvoker.invoke(null, null);
        }
        {
            final Method test = Bean.class.getDeclaredMethod("test", short.class);
            final MethodInvoker mainInvoker = MethodInvoker.create(test);
            mainInvoker.invoke(null, new Object[] { (short) 1 });
        }

        final MethodInvoker create = MethodInvoker.create(Bean.class, "test");

        create.invoke(new Bean(), null);

        final MethodInvoker itself = MethodInvoker.create(Bean.class, "test", Bean.class);

        itself.invoke(new Bean(), new Object[] { new Bean() });
    }

    public static class Bean {

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
}
