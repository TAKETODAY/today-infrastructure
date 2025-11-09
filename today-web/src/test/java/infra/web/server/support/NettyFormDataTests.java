/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.server.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import io.netty.handler.codec.http.multipart.Attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 21:57
 */
class NettyFormDataTests {

  @Test
  void shouldReturnCorrectValue() throws IOException {
    // given
    Attribute attribute = mock(Attribute.class);
    when(attribute.getValue()).thenReturn("testValue");

    NettyFormData formData = new NettyFormData(attribute);

    // when & then
    assertThat(formData.getValue()).isEqualTo("testValue");
  }

  @Test
  void shouldReturnCorrectBytes() throws IOException {
    // given
    Attribute attribute = mock(Attribute.class);
    byte[] expectedBytes = "testValue".getBytes();
    when(attribute.get()).thenReturn(expectedBytes);

    NettyFormData formData = new NettyFormData(attribute);

    // when & then
    assertThat(formData.getBytes()).isEqualTo(expectedBytes);
  }

  @Test
  void shouldReturnTrueForFormField() {
    // given
    Attribute attribute = mock(Attribute.class);
    NettyFormData formData = new NettyFormData(attribute);

    // when & then
    assertThat(formData.isFormField()).isTrue();
  }

  @Test
  void shouldReturnCorrectName() {
    // given
    Attribute attribute = mock(Attribute.class);
    when(attribute.getName()).thenReturn("testName");

    NettyFormData formData = new NettyFormData(attribute);

    // when & then
    assertThat(formData.getName()).isEqualTo("testName");
  }

  @Test
  void shouldCleanupAttribute() throws IOException {
    // given
    Attribute attribute = mock(Attribute.class);
    NettyFormData formData = new NettyFormData(attribute);

    // when
    formData.cleanup();

    // then
    verify(attribute).delete();
  }

}