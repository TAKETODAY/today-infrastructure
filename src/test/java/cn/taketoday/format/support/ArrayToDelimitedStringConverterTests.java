/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.format.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.Delimiter;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArrayToDelimitedStringConverter}.
 *
 * @author Phillip Webb
 */
class ArrayToDelimitedStringConverterTests {

  @ConversionServiceTest
  void convertListToStringShouldConvert(ConversionService conversionService) {
    String[] list = { "a", "b", "c" };
    String converted = conversionService.convert(list, String.class);
    assertThat(converted).isEqualTo("a,b,c");
  }

  @ConversionServiceTest
  void convertWhenHasDelimiterNoneShouldConvert(ConversionService conversionService) {
    Data data = new Data();
    data.none = new String[] { "1", "2", "3" };
    String converted = (String) conversionService.convert(data.none,
            TypeDescriptor.nested(ReflectionUtils.findField(Data.class, "none"), 0),
            TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("123");
  }

  @ConversionServiceTest
  void convertWhenHasDelimiterDashShouldConvert(ConversionService conversionService) {
    Data data = new Data();
    data.dash = new String[] { "1", "2", "3" };
    String converted = (String) conversionService.convert(data.dash,
            TypeDescriptor.nested(ReflectionUtils.findField(Data.class, "dash"), 0),
            TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("1-2-3");
  }

  @ConversionServiceTest
  void convertShouldConvertNull(ConversionService conversionService) {
    String[] list = null;
    String converted = conversionService.convert(list, String.class);
    assertThat(converted).isNull();
  }

  @Test
  void convertShouldConvertElements() {
    Data data = new Data();
    data.type = new int[] { 1, 2, 3 };
    String converted = (String) new ApplicationConversionService().convert(data.type,
            TypeDescriptor.nested(ReflectionUtils.findField(Data.class, "type"), 0),
            TypeDescriptor.valueOf(String.class));
    assertThat(converted).isEqualTo("1.2.3");
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments
            .with((service) -> service.addConverter(new ArrayToDelimitedStringConverter(service)));
  }

  static class Data {

    @Delimiter(Delimiter.NONE)
    String[] none;

    @Delimiter("-")
    String[] dash;

    @Delimiter(".")
    int[] type;

  }

}
