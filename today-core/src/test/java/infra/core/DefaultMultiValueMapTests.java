/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
