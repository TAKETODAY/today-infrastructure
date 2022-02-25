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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.InvalidPropertyException;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanPropertyAccessor;
import lombok.Getter;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY
 * 2021/1/27 23:18
 * @since 3.0
 */
public class BeanPropertyAccessorTest {

  @Setter
  @Getter
  static class NestedType {

    String name;
    Double aDouble;

    NestedType nested;

    @Override
    public String toString() {
      return "NestedType{" +
              "name='" + name + '\'' +
              ", aDouble=" + aDouble +
              ", \nnested=" + nested +
              '}';
    }

  }

  @Test
  public void testSimple() {
    final BeanPropertyAccessor nestedBean = BeanPropertyAccessor.ofClass(NestedType.class);

    nestedBean.setProperty("name", "TODAY");
    nestedBean.setProperty("aDouble", 1);
    nestedBean.setProperty("nested.aDouble", 100);
    nestedBean.setProperty("nested.name", "nested-TODAY");

    nestedBean.setProperty("nested.nested.name", "nested-nested-TODAY");

    final Object object = nestedBean.getRootObject();

    assertThat(object).isInstanceOf(NestedType.class);
    NestedType base = (NestedType) object;

    assertThat(base.name).isEqualTo("TODAY");
    assertThat(base.nested.name).isEqualTo("nested-TODAY");
    assertThat(base.nested.nested.name).isEqualTo("nested-nested-TODAY");

    assertThat(base.aDouble).isEqualTo(1d);
    assertThat(base.nested.aDouble).isEqualTo(100d);
    assertThat(base.nested.nested.aDouble).isNull();

    //

    assertThat(base.name).isEqualTo(nestedBean.getProperty("name"));
    assertThat(base.nested.name).isEqualTo(nestedBean.getProperty("nested.name"));
    assertThat(base.nested.nested.name).isEqualTo(nestedBean.getProperty("nested.nested.name"));

    assertThat(base.aDouble).isEqualTo(nestedBean.getProperty("aDouble"));
    assertThat(base.nested.aDouble).isEqualTo(nestedBean.getProperty("nested.aDouble"));
    assertThat(base.nested.nested.aDouble).isEqualTo(nestedBean.getProperty("nested.nested.aDouble"));

    System.out.println(object);
  }

  @Setter
  @Getter
  static class Nested {

    String name;
    Double[] doubles;

    List<String> list;
    Map<String, String> map;

    Nested nested;

    @Override
    public String toString() {
      return "Nested{" +
              "name='" + name + '\'' +
              ", doubles=" + Arrays.toString(doubles) +
              ", list=" + list +
              ", map=" + map +
              ", \nnested=" + nested +
              '}';
    }
  }

  @Test
  public void testArrayListMap() {
    final BeanPropertyAccessor nestedBean = BeanPropertyAccessor.ofClass(Nested.class);
    nestedBean.setProperty("name", "TODAY");
    nestedBean.setProperty("doubles", new double[] { 10d, 20d });

    nestedBean.setProperty("nested.name", "nested-TODAY");
    nestedBean.setProperty("nested.doubles", new double[] { 30d, 40d });
    nestedBean.setProperty("nested.nested.name", "nested-nested-TODAY");

    final HashMap<String, String> map = new HashMap<>();
    map.put("age", "23");
    map.put("key", "value");
    map.put("gender", "男");
    map.put("name", "TODAY");
    nestedBean.setProperty("nested.map", map);

    final ArrayList<String> list = new ArrayList<>();
    list.add("TODAY");
    list.add("TODAY1");
    list.add("TODAY2");
    nestedBean.setProperty("nested.list", list);

    final Object object = nestedBean.getRootObject();

    assertThat(object).isInstanceOf(Nested.class);
    Nested base = (Nested) object;

    // array
    assertThat(base.doubles[0]).isEqualTo(10d).isEqualTo(nestedBean.getProperty("doubles[0]"));
    assertThat(base.doubles[1]).isEqualTo(20d).isEqualTo(nestedBean.getProperty("doubles[1]"));
    assertThat(base.nested.doubles[0]).isEqualTo(30d).isEqualTo(nestedBean.getProperty("nested.doubles[0]"));
    assertThat(base.nested.doubles[1]).isEqualTo(40d).isEqualTo(nestedBean.getProperty("nested.doubles[1]"));

    // list
    assertThat(base.nested.list.get(0)).isEqualTo("TODAY").isEqualTo(nestedBean.getProperty("nested.list[0]"));
    assertThat(base.nested.list.get(1)).isEqualTo("TODAY1").isEqualTo(nestedBean.getProperty("nested.list[1]"));
    assertThat(base.nested.list.get(2)).isEqualTo("TODAY2").isEqualTo(nestedBean.getProperty("nested.list[2]"));

    // map
    assertThat(base.nested.map.get("age")).isEqualTo("23").isEqualTo(nestedBean.getProperty("nested.map[age]"));
    assertThat(base.nested.map.get("key")).isEqualTo("value").isEqualTo(nestedBean.getProperty("nested.map[key]"));
    assertThat(base.nested.map.get("name")).isEqualTo("TODAY").isEqualTo(nestedBean.getProperty("nested.map[name]"));
    assertThat(base.nested.map.get("gender")).isEqualTo("男").isEqualTo(nestedBean.getProperty("nested.map[gender]"));

    // required type

    assertThat(nestedBean.getProperty("nested.map[age]", int.class)).isEqualTo(23);
    assertThat(nestedBean.getProperty("nested.map[age]", Integer.class)).isEqualTo(23);

    // null
    nestedBean.getProperty("nested.nested.nested.name[1]");

    // ex
    try {
      nestedBean.getProperty("nested.xxx[1]");
    }
    catch (NoSuchPropertyException ignored) { }

    try {
      nestedBean.getProperty("nested.doubles[]");
    }
    catch (IllegalArgumentException ignored) { }
    try {
      nestedBean.getProperty("nested.doubles[2]");
    }
    catch (ArrayIndexOutOfBoundsException ignored) { }
    try {
      nestedBean.getProperty("nested.doubles[1");
    }
    catch (IllegalArgumentException ignored) { }

    //NumberFormatException

    try {
      nestedBean.getProperty("nested.doubles[name]");
    }
    catch (IllegalArgumentException ignored) { }
    try {
      nestedBean.getProperty("nested.list[name]");
    }
    catch (IllegalArgumentException ignored) { }
    // 
    try {
      nestedBean.getProperty("nested.name[name]");
    }
    catch (IllegalArgumentException ignored) { }

    System.out.println(object);
  }

  @Setter
  @Getter
  static class NestedArrayDot {

    String name;

    List<NestedArrayDot> list;
    Map<String, NestedArrayDot> map;

    NestedArrayDot[] nested;

    int[][] ints = new int[][] {
            { 1, 2, 3 },
            { 4, 5, 6 },
    };

    @Override
    public String toString() {
      return "NestedArrayDot{" +
              "name='" + name + '\'' +
              ", list=" + list +
              ", map=" + map +
              ", \nnested=" + Arrays.toString(nested) +
              '}';
    }
  }

  @Test
  public void testArrayDotProperty() {
    final BeanPropertyAccessor nestedBean = BeanPropertyAccessor.ofClass(NestedArrayDot.class);
    nestedBean.setProperty("name", "TODAY");

    final NestedArrayDot nestedArrayDot = new NestedArrayDot();
    nestedArrayDot.name = "TODAY-array";
    final NestedArrayDot[] arrayDots = { nestedArrayDot };

    nestedBean.setProperty("nested", arrayDots);

    final HashMap<String, NestedArrayDot> map = new HashMap<>();
    map.put("nested", nestedArrayDot);
    map.put("key", nestedArrayDot);
    nestedBean.setProperty("map", map);

    final ArrayList<NestedArrayDot> list = new ArrayList<>();
    list.add(nestedArrayDot);
    nestedBean.setProperty("list", list);

    final Object object = nestedBean.getRootObject();

    assertThat(object).isInstanceOf(NestedArrayDot.class);
    NestedArrayDot base = (NestedArrayDot) object;

    assertThat(base.nested[0].name).isEqualTo("TODAY-array")
            .isEqualTo(nestedBean.getProperty("nested[0].name"))
            .isEqualTo(base.list.get(0).name).isEqualTo(nestedBean.getProperty("list[0].name"))
            .isEqualTo(base.map.get("key").name).isEqualTo(nestedBean.getProperty("map[key].name"))
            .isEqualTo(base.map.get("nested").name).isEqualTo(nestedBean.getProperty("map[nested].name"));

    assertThat(base.ints[0]).isInstanceOf(int[].class).isEqualTo(nestedBean.getProperty("ints[0]"));

    assertThat(base.ints[0][0]).isInstanceOf(Integer.class).isEqualTo(1)
            .isEqualTo(nestedBean.getProperty("ints[0][0]"))
            .isEqualTo(nestedBean.getProperty("ints[0][0]xxx"))
            .isEqualTo(nestedBean.getProperty("ints[0][0].value"));

    try {
      nestedBean.getProperty("ints[0][xxx");
    }
    catch (IllegalArgumentException e) { }

    System.out.println(object);
  }

  @Test
  public void testArray() {
    final NestedArrayDot nested = new NestedArrayDot();
    nested.name = "TODAY";
    final NestedArrayDot[] arrayDots = { null, nested };
    assertThat(BeanPropertyAccessor.getProperty(arrayDots, "[1].name")).isEqualTo("TODAY");
  }

  @Setter
  @Getter
  static class BeanPropertyTest {
    String name;
    List<String> list;
    List<List<String>> listList;
  }

  @Test
  public void testBeanProperty() throws NoSuchFieldException {
    BeanProperty beanProperty = BeanProperty.valueOf(BeanPropertyTest.class, "listList");
    BeanProperty listBeanProperty = BeanProperty.valueOf(BeanPropertyTest.class, "list");

    assertThat(beanProperty.getResolvableType().resolveGeneric(0)).isInstanceOf(ParameterizedType.class);
    ParameterizedType generic = (ParameterizedType) beanProperty.getResolvableType().getGeneric(0).getType();
    assertThat(listBeanProperty.getResolvableType().resolveGeneric(0)).isInstanceOf(Class.class).isEqualTo(generic.getActualTypeArguments()[0]);
  }

  // set

  @Setter
  @Getter
  static class SetNested {
    String name;

    List<SetNested> list;
    Map<String, SetNested> map;

    SetNested[] nested;

    @Override
    public String toString() {
      return "SetNested{" +
              "name='" + name + '\'' +
              ", \nnested=" + Arrays.toString(nested) +
              '}';
    }
  }

  @Test
  public void testSetArrayDotProperty() {
    final BeanPropertyAccessor nestedBean = BeanPropertyAccessor.ofClass(SetNested.class);
    nestedBean.setProperty("name", "TODAY");

    final SetNested nested = new SetNested();
    nested.name = "TODAY";
    final SetNested[] arrayDots = { nested };

    nestedBean.setProperty("nested", arrayDots);

    final HashMap<String, SetNested> map = new HashMap<>();
    map.put("nested", nested);
    map.put("key", nested);
    nestedBean.setProperty("map", map);
    nestedBean.setProperty("map[newKey]", nested);

    final ArrayList<SetNested> list = new ArrayList<>();
    list.add(nested);
    nestedBean.setProperty("list", list);
    nestedBean.setProperty("list[1]", nested);
    nestedBean.setProperty("nested[0].list[1]", nested);
    nestedBean.setProperty("nested[0].list[1].name", "set");
    nestedBean.setProperty("nested[1].list[1].name", "set2");
    nestedBean.setProperty("nested[1].name", "set2");
    nestedBean.setProperty("nested[1].map[new].name", "set2");

    final Object object = nestedBean.getRootObject();

    assertThat(object).isInstanceOf(SetNested.class);
    SetNested base = (SetNested) object;

    assertThat(base.nested[0].name)
            .isEqualTo("set")
            .isEqualTo(nested.name)
            .isEqualTo(base.list.get(1).name)
            .isEqualTo(nestedBean.getProperty("list[1].name"))
            .isEqualTo(nestedBean.getProperty("nested[0].name"))
            .isEqualTo(nestedBean.getProperty("nested[0].list[1].name"))
            .isEqualTo(base.map.get("newKey").name).isEqualTo(nestedBean.getProperty("map[key].name"));

    assertThat(base.nested[0])
            .isEqualTo(nested)
            .isEqualTo(base.list.get(1))
            .isEqualTo(nestedBean.getProperty("list[1]"))
            .isEqualTo(nestedBean.getProperty("nested[0]"))
            .isEqualTo(nestedBean.getProperty("nested[0].list[1]"))
            .isEqualTo(base.map.get("newKey")).isEqualTo(nestedBean.getProperty("map[key]"));

    //
    assertThat(base.nested[1].list.get(1).name)
            .isEqualTo(base.nested[1].map.get("new").name)
            .isEqualTo(nestedBean.getProperty("nested[1].list[1].name"))
            .isEqualTo(nestedBean.getProperty("nested[1].name"));

    try {
      nestedBean.setProperty("nested[1].map[new].name[m]", "set2"); // type error
    }
    catch (InvalidPropertyException ignored) { }
    try {
      nestedBean.setProperty("nested[1].list[-1].name", "set2"); // -1
    }
    catch (InvalidPropertyException ignored) { }

    System.out.println(object);
  }

}
