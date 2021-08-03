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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TODAY 2021/8/3 21:18
 */
class AnnotationAttributesTests {

  @Test
  void put() {

    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("name", "TODAY");
    attributes.put("age", 21);
    attributes.put("name", "TODAY1");
    attributes.put("name", "TODAY");

    System.out.println(attributes);

    assertThat(attributes.size()).isEqualTo(2);

    AnnotationAttributes putAttributes = new AnnotationAttributes();
    attributes.put("age", 23);
    attributes.put("name", "TODAY-3");
    attributes.put("gender", "男");

    putAttributes.putAll(attributes);

    System.out.println(attributes);
    System.out.println(putAttributes);

  }

  @Test
  void add() {

    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.add("name", "TODAY");
    attributes.add("age", 21);
    attributes.add("name", "TODAY1");
    attributes.add("name", "TODAY");
    attributes.add("gender", "nv");
    attributes.put("dsds", "dsds");


    System.out.println(attributes);

    Object value = attributes.get("name");
    assertThat(value).isInstanceOf(List.class);

    List<Object> values = (List<Object>) value;
    assertThat(values).hasSize(3);

    assertThat(attributes.size()).isEqualTo(4);

    AnnotationAttributes putAttributes = new AnnotationAttributes();
    putAttributes.put("age", 23);
    putAttributes.put("name", "TODAY-3");
    putAttributes.put("gender", "男");

    System.out.println(putAttributes);

    putAttributes.putAll(attributes);

    System.out.println(attributes);
    System.out.println(putAttributes);

    assertThat(putAttributes.size()).isEqualTo(4);
    // remove
    putAttributes.remove("name");
    // size
    assertThat(putAttributes.size()).isEqualTo(3);

    System.out.println(putAttributes);
  }

}
