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

package cn.taketoday.context.factory;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.exception.NoSuchPropertyException;
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

    final Object object = nestedBean.getObject();

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

    final Object object = nestedBean.getObject();

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
    catch (UnsupportedOperationException ignored) { }
    try {
      nestedBean.getProperty("nested.doubles[2]");
    }
    catch (ArrayIndexOutOfBoundsException ignored) { }
    try {
      nestedBean.getProperty("nested.doubles[1");
    }
    catch (UnsupportedOperationException ignored) { }

    //NumberFormatException

    try {
      nestedBean.getProperty("nested.doubles[name]");
    }
    catch (UnsupportedOperationException ignored) { }
    try {
      nestedBean.getProperty("nested.list[name]");
    }
    catch (UnsupportedOperationException ignored) { }
    // 
    try {
      nestedBean.getProperty("nested.name[name]");
    }
    catch (UnsupportedOperationException ignored) { }

    System.out.println(object);
  }

}
