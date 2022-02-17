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

package cn.taketoday.core.conversion.support.annotation;

import org.junit.jupiter.params.provider.Arguments;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumingThat;

/**
 * Tests for {@link InetAddressFormatter}.
 *
 * @author Phillip Webb
 */
class InetAddressFormatterTests {

  @ConversionServiceTest
  void convertFromInetAddressToStringShouldConvert(ConversionService conversionService) {
    assumingThat(isResolvable("example.com"), () -> {
      InetAddress address = InetAddress.getByName("example.com");
      String converted = conversionService.convert(address, String.class);
      assertThat(converted).isEqualTo(address.getHostAddress());
    });
  }

  @ConversionServiceTest
  void convertFromStringToInetAddressShouldConvert(ConversionService conversionService) {
    assumingThat(isResolvable("example.com"), () -> {
      InetAddress converted = conversionService.convert("example.com", InetAddress.class);
      assertThat(converted.toString()).startsWith("example.com");
    });
  }

  @ConversionServiceTest
  void convertFromStringToInetAddressWhenHostDoesNotExistShouldThrowException(ConversionService conversionService) {
    String missingDomain = "ireallydontexist.example.com";
    assumingThat(!isResolvable("ireallydontexist.example.com"),
            () -> assertThatExceptionOfType(ConversionFailedException.class)
                    .isThrownBy(() -> conversionService.convert(missingDomain, InetAddress.class)));
  }

  private boolean isResolvable(String host) {
    try {
      InetAddress.getByName(host);
      return true;
    }
    catch (UnknownHostException ex) {
      return false;
    }
  }

  static Stream<? extends Arguments> conversionServices() {
    return ConversionServiceArguments.with(new InetAddressFormatter());
  }

}
