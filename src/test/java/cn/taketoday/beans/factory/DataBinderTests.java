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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.support.PropertyValuesBinder;
import lombok.Getter;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author TODAY 2021/3/21 20:04
 * @since 3.0
 */
public class DataBinderTests {

  @Setter
  @Getter
  static class Nested {

    String name;
    Double[] doubles;

    List<String> list;
    Map<String, String> map;

    BeanPropertyAccessorTest.Nested nested;

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
  public void bind() {
    final PropertyValuesBinder dataBinder = new PropertyValuesBinder(Nested.class);

    final HashMap<String, String> map = new HashMap<>();
    map.put("age", "23");
    map.put("key", "value");
    map.put("gender", "男");
    map.put("name", "TODAY");

    final ArrayList<String> list = new ArrayList<>();
    list.add("TODAY");
    list.add("TODAY1");
    list.add("TODAY2");

    dataBinder.addPropertyValue("name", "TODAY");
    dataBinder.addPropertyValue("doubles", new double[] { 10.0D, 20.0D });
    dataBinder.addPropertyValue("nested.map", map);

    dataBinder.addPropertyValue("nested.name", "nested-TODAY");
    dataBinder.addPropertyValue("nested.doubles", new double[] { 30d, 40d });
    dataBinder.addPropertyValue("nested.nested.name", "nested-nested-TODAY");
    dataBinder.addPropertyValue("nested.list", list);

    final Object object = dataBinder.bind();

    assertThat(object).isInstanceOf(Nested.class);
    Nested base = (Nested) object;

    assertNested(dataBinder, base);

    HashMap<String, Object> propertyValues = new HashMap<>();
    propertyValues.put("name", "TODAY");
    propertyValues.put("doubles", new double[] { 10.0D, 20.0D });
    propertyValues.put("nested.map", map);

    propertyValues.put("nested.name", "nested-TODAY");
    propertyValues.put("nested.doubles", new double[] { 30d, 40d });
    propertyValues.put("nested.nested.name", "nested-nested-TODAY");
    propertyValues.put("nested.list", list);

    final PropertyValuesBinder binder = new PropertyValuesBinder(Nested.class);

    binder.addPropertyValues(propertyValues);
    assertNested(binder, (Nested) binder.bind());
  }

  private void assertNested(PropertyValuesBinder dataBinder, Nested base) {
    // array
    assertThat(base.doubles[0]).isEqualTo(10d).isEqualTo(dataBinder.getProperty("doubles[0]"));
    assertThat(base.doubles[1]).isEqualTo(20d).isEqualTo(dataBinder.getProperty("doubles[1]"));
    assertThat(base.nested.doubles[0]).isEqualTo(30d).isEqualTo(dataBinder.getProperty("nested.doubles[0]"));
    assertThat(base.nested.doubles[1]).isEqualTo(40d).isEqualTo(dataBinder.getProperty("nested.doubles[1]"));

    // list
    assertThat(base.nested.list.get(0)).isEqualTo("TODAY").isEqualTo(dataBinder.getProperty("nested.list[0]"));
    assertThat(base.nested.list.get(1)).isEqualTo("TODAY1").isEqualTo(dataBinder.getProperty("nested.list[1]"));
    assertThat(base.nested.list.get(2)).isEqualTo("TODAY2").isEqualTo(dataBinder.getProperty("nested.list[2]"));

    // map
    assertThat(base.nested.map.get("age")).isEqualTo("23").isEqualTo(dataBinder.getProperty("nested.map[age]"));
    assertThat(base.nested.map.get("key")).isEqualTo("value").isEqualTo(dataBinder.getProperty("nested.map[key]"));
    assertThat(base.nested.map.get("name")).isEqualTo("TODAY").isEqualTo(dataBinder.getProperty("nested.map[name]"));
    assertThat(base.nested.map.get("gender")).isEqualTo("男").isEqualTo(dataBinder.getProperty("nested.map[gender]"));

    // required type

    assertThat(dataBinder.getProperty("nested.map[age]", int.class)).isEqualTo(23);
    assertThat(dataBinder.getProperty("nested.map[age]", Integer.class)).isEqualTo(23);
  }

  static class UnknownProperty {

  }

  @Test
  public void ignoreUnknownProperty() {
    final PropertyValuesBinder ignore = new PropertyValuesBinder(UnknownProperty.class);
    ignore.setProperty("name", "TODAY");

    final PropertyValuesBinder throwsDataBinder = new PropertyValuesBinder(UnknownProperty.class);
    throwsDataBinder.setIgnoreUnknownProperty(false);

    assertThatThrownBy(() -> {
      throwsDataBinder.setProperty("name", "TODAY");
    }).withFailMessage("No such property: '%s' in class: %s", "name", UnknownProperty.class);

  }

}
