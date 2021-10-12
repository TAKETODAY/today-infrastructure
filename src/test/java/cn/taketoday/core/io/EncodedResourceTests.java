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

package cn.taketoday.core.io;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/9 20:11
 */
class EncodedResourceTests {

  private static final String UTF8 = "UTF-8";
  private static final String UTF16 = "UTF-16";
  private static final Charset UTF8_CS = Charset.forName(UTF8);
  private static final Charset UTF16_CS = Charset.forName(UTF16);

  private final Resource resource = new DescriptiveResource("test");

  @Test
  void equalsWithNullOtherObject() {
    assertThat(new EncodedResource(resource).equals(null)).isFalse();
  }

  @Test
  void equalsWithSameEncoding() {
    EncodedResource er1 = new EncodedResource(resource, UTF8);
    EncodedResource er2 = new EncodedResource(resource, UTF8);
    assertThat(er2).isEqualTo(er1);
  }

  @Test
  void equalsWithDifferentEncoding() {
    EncodedResource er1 = new EncodedResource(resource, UTF8);
    EncodedResource er2 = new EncodedResource(resource, UTF16);
    assertThat(er2).isNotEqualTo(er1);
  }

  @Test
  void equalsWithSameCharset() {
    EncodedResource er1 = new EncodedResource(resource, UTF8_CS);
    EncodedResource er2 = new EncodedResource(resource, UTF8_CS);
    assertThat(er2).isEqualTo(er1);
  }

  @Test
  void equalsWithDifferentCharset() {
    EncodedResource er1 = new EncodedResource(resource, UTF8_CS);
    EncodedResource er2 = new EncodedResource(resource, UTF16_CS);
    assertThat(er2).isNotEqualTo(er1);
  }

  @Test
  void equalsWithEncodingAndCharset() {
    EncodedResource er1 = new EncodedResource(resource, UTF8);
    EncodedResource er2 = new EncodedResource(resource, UTF8_CS);
    assertThat(er2).isNotEqualTo(er1);
  }

}
