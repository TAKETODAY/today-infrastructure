/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

package cn.taketoday.context.conversion;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;

/**
 * @author TODAY
 * @date 2021/1/6 23:07
 */
public class ConverterTypeConverterTest {

  Converter<Object, Integer> integerConverter = new Converter<Object, Integer>() {
    @Override
    public Integer convert(Object source) {
      return Integer.valueOf((String) source);
    }
  };
  Converter<Object, Long> lambdaConverter = source -> Long.valueOf((String) source);

  @Test
  public void testConverterTypeConverter() {
    final ConverterTypeConverter converter = new ConverterTypeConverter();
    converter.addConverter(integerConverter);
    try {
      converter.addConverter(lambdaConverter);
    }
    catch (ConfigurationException e) {
      assert true;
    }

    converter.addConverter(Long.class, lambdaConverter);


    final Object convert = converter.convert(Integer.class, "1234234");
    Assertions.assertThat(convert)
            .isEqualTo(1234234);

    Assertions.assertThat(converter.convert(Long.class,"1234234"))
            .isEqualTo(1234234L);


    Assertions.assertThat(converter.getConverterMap())
            .hasSize(13);
  }
}
