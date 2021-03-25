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

package cn.taketoday.context;

import org.junit.Test;

import java.awt.Color;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author TODAY 2021/3/23 11:50
 * @since 3.0
 */
public class GenericDescriptorTests {

  public void handlerMethod(MyList<String> color) { }

  public void handlerMethod1(ColorList<Color> colorList) { }

  // @off
  interface Interface<T> { }
  interface Interface1<T> { }
  interface Interface2<T> { }
  interface NestedGenericInterface extends Interface<String> {}
  static abstract class Abs {}
  static abstract class GenericAbs implements Interface<String> {}
  static class AbsGeneric extends Abs
          implements Interface<String>,
                     Interface1<Integer>,
                     Interface2<Interface<String>> { }
  static class GenericAbsGeneric extends GenericAbs
          implements Interface1<Integer>, Interface2<Interface<String>> { }
  static class NestedGenericInterfaceBean extends GenericAbs
          implements NestedGenericInterface, Interface1<Integer>, Interface2<Interface<String>> { }
  static class NoGeneric { } // @on

  static class MyList<TT> extends ArrayList<NoGeneric> implements Interface<TT> { }

  static class ColorList<T> extends ArrayList<T> { }

  @Test
  public void testGenerics() throws NoSuchMethodException {
    final Method handlerMethod = getClass().getMethod("handlerMethod", MyList.class);
    final Method colorList = getClass().getMethod("handlerMethod1", ColorList.class);

//    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(handlerMethod, 0);
//    final GenericDescriptor descriptor1 = GenericDescriptor.ofParameter(colorList, 0);

//    final Class<?>[] generics = descriptor.getGenerics(Collection.class);
//
//    final Class<?>[] generics1 = descriptor1.getGenerics(Collection.class);
//    final Class<?>[] Interface = descriptor.getGenerics(Interface.class);
//
//    System.out.println(Arrays.toString(generics));
//    System.out.println(Arrays.toString(generics1));
//    System.out.println(Arrays.toString(Interface));

    final Type[] genericInterfaces = MyList.class.getGenericInterfaces();

    System.out.println(Arrays.toString(genericInterfaces));

  }



  @Test
  public void getGenerics() throws NoSuchMethodException {
//    final Method handlerMethod = getClass().getMethod("handlerMethod", List.class);
//    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(handlerMethod, 0);

//    final Class<?>[] generics = descriptor.getGenerics();
//    final Class<?>[] generics1 = descriptor.getGenerics(Set.class);

//    System.out.println(Arrays.toString(generics));

  }
}
