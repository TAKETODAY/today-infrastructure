/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.format.support;

import org.junit.jupiter.params.provider.Arguments;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import infra.core.conversion.ConversionFailedException;
import infra.core.conversion.ConversionService;
import infra.core.io.InputStreamSource;
import infra.core.io.Resource;

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
