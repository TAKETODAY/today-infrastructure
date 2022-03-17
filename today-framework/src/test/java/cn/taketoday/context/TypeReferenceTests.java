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

package cn.taketoday.context;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeReference;
import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY
 * 2021/1/6 23:02
 */
class TypeReferenceTests {

  @Test
  public void testTypeReference() {
    TypeReference<Integer> reference = new TypeReference<Integer>() { };
    final Type[] generics = ClassUtils.getGenerics(reference.getClass(), TypeReference.class);

    Assertions.assertThat(generics[0])
            .isEqualTo(Integer.class);

    Assertions.assertThat(generics)
            .hasSize(1);

    Assertions.assertThat(ResolvableType.fromClass(Integer.class))
            .isEqualTo(new IntegerTypeReference().getResolvableType())
            .isEqualTo(new IntegerTypeReference1().getResolvableType())
            .isEqualTo(new IntegerTypeReference2().getResolvableType())
    ;
  }

  static class IntegerTypeReference extends TypeReference<Integer> {

  }

  static class IntegerTypeReference1 extends IntegerTypeReference {

  }

  static class IntegerTypeReference2 extends IntegerTypeReference1 {

  }
}
