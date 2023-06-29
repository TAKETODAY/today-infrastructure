/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY
 * 2021/1/6 23:02
 */
class TypeReferenceTests {

  @Test
  void stringTypeReference() {
    TypeReference<String> typeReference = new TypeReference<>() { };
    assertThat(typeReference.getType()).isEqualTo(String.class);
  }

  @Test
  void mapTypeReference() throws Exception {
    Type mapType = getClass().getMethod("mapMethod").getGenericReturnType();
    TypeReference<Map<Object, String>> typeReference = new TypeReference<>() { };
    assertThat(typeReference.getType()).isEqualTo(mapType);
  }

  @Test
  void listTypeReference() throws Exception {
    Type listType = getClass().getMethod("listMethod").getGenericReturnType();
    TypeReference<List<String>> typeReference = new TypeReference<>() { };
    assertThat(typeReference.getType()).isEqualTo(listType);
  }

  @Test
  void reflectiveTypeReferenceWithSpecificDeclaration() throws Exception {
    Type listType = getClass().getMethod("listMethod").getGenericReturnType();
    TypeReference<List<String>> typeReference = TypeReference.fromType(listType);
    assertThat(typeReference.getType()).isEqualTo(listType);
  }

  @Test
  void reflectiveTypeReferenceWithGenericDeclaration() throws Exception {
    Type listType = getClass().getMethod("listMethod").getGenericReturnType();
    TypeReference<?> typeReference = TypeReference.fromType(listType);
    assertThat(typeReference.getType()).isEqualTo(listType);
  }

  public static Map<Object, String> mapMethod() {
    return null;
  }

  public static List<String> listMethod() {
    return null;
  }

}
