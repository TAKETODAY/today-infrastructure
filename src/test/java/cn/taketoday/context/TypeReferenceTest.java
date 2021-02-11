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

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.lang.reflect.Type;

import cn.taketoday.context.utils.ClassUtils;

/**
 * @author TODAY
 * 2021/1/6 23:02
 */
public class TypeReferenceTest {

  @Test
  public void testTypeReference() {
    TypeReference<Integer> reference = new TypeReference<Integer>() { };

    final Type[] generics = ClassUtils.getGenerics(reference.getClass(), TypeReference.class);

    Assertions.assertThat(generics)
            .hasSize(1);
    Assertions.assertThat(generics[0])
            .isEqualTo(Integer.class)
            .isEqualTo(new IntegerTypeReference().getRawType())
            .isEqualTo(new IntegerTypeReference1().getRawType())
            .isEqualTo(new IntegerTypeReference2().getRawType())
            .isEqualTo(new IntegerTypeReference().getTypeParameter(reference.getClass()))
            .isEqualTo(new IntegerTypeReference1().getTypeParameter(reference.getClass()))
            .isEqualTo(new IntegerTypeReference2().getTypeParameter(reference.getClass()));
  }

  static class IntegerTypeReference extends TypeReference<Integer> {

  }

  static class IntegerTypeReference1 extends IntegerTypeReference {

  }

  static class IntegerTypeReference2 extends IntegerTypeReference1 {

  }
}
