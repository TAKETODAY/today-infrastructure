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

package infra.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import infra.util.MappingMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Harry Yang 2021/11/9 15:21
 */
class DefaultMultiValueMapTests {

  @Test
  void addAll() {

    ArrayList<String> list = new ArrayList<>();

    list.add("value1");
    list.add("value2");

    Enumeration<String> enumeration = Collections.enumeration(list);
    MappingMultiValueMap<Object, Object> multiValueMap = new MappingMultiValueMap<>();

    multiValueMap.addAll("key", enumeration);

    assertThat(multiValueMap).hasSize(1);

    List<Object> objectList = multiValueMap.get("key");

    assertThat(objectList).isEqualTo(list);
  }

}
