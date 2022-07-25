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

import org.junit.jupiter.params.provider.Arguments;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InputStreamSourceToByteArrayConverter}.
 *
 * @author Phillip Webb
 */
class InputStreamSourceToByteArrayConverterTests {

  @ConversionServiceTest
  void convertConvertsSource(ConversionService conversionService) {
    InputStreamSource source = () -> new ByteArrayInputStream(new byte[] { 0, 1, 2 });
    assertThat(conversionService.convert(source, byte[].class)).containsExactly(0, 1, 2);
  }

  @ConversionServiceTest
  void convertWhenFailsWithIOExceptionThrowsException(ConversionService conversionService) throws Exception {
    InputStreamSource source = mock(InputStreamSource.class);
    given(source.getInputStream()).willThrow(IOException.class);
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> conversionService.convert(source, byte[].class))
            .havingCause()
            .isInstanceOf(IllegalStateException.class)
            .withMessageContaining("Unable to read from input stream source");
  }

//  @ConversionServiceTest
//  void convertWhenFailsWithIOExceptionFromOriginProviderThrowsException(ConversionService conversionService)
//          throws Exception {
//    Origin origin = new TestOrigin("mylocation");
//    InputStreamSource source = mock(InputStreamSource.class, withSettings().extraInterfaces(OriginProvider.class));
//    given(source.getInputStream()).willThrow(IOException.class);
//    given(((OriginProvider) source).getOrigin()).willReturn(origin);
//    assertThatExceptionOfType(ConversionFailedException.class)
//            .isThrownBy(() -> conversionService.convert(source, byte[].class))
//            .withCauseExactlyInstanceOf(IllegalStateException.class)
//            .withMessageContaining("Unable to read from mylocation");
//  }

  @ConversionServiceTest
  void convertWhenFailsWithIOExceptionFromResourceThrowsException(ConversionService conversionService)
          throws Exception {
    Resource source = mock(Resource.class);
    given(source.getInputStream()).willThrow(IOException.class);
    given(source.toString()).willReturn("myresource");
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> conversionService.convert(source, byte[].class))
            .havingCause()
            .isInstanceOf(IllegalStateException.class)
            .withMessageContaining("Unable to read from myresource");
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments
            .with((service) -> service.addConverter(new InputStreamSourceToByteArrayConverter()));
  }
//
//  private static class TestOrigin implements Origin {
//
//    private final String string;
//
//    TestOrigin(String string) {
//      this.string = string;
//    }
//
//    @Override
//    public String toString() {
//      return this.string;
//    }
//
//  }

}
