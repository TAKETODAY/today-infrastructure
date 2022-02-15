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

package cn.taketoday.http.converter;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-case for AbstractHttpMessageConverter.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class HttpMessageConverterTests {

  @Test
  public void canRead() {
    MediaType mediaType = new MediaType("foo", "bar");
    HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

    assertThat(converter.canRead(MyType.class, mediaType)).isTrue();
    assertThat(converter.canRead(MyType.class, new MediaType("foo", "*"))).isFalse();
    assertThat(converter.canRead(MyType.class, MediaType.ALL)).isFalse();
  }

  @Test
  public void canReadWithWildcardSubtype() {
    MediaType mediaType = new MediaType("foo");
    HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

    assertThat(converter.canRead(MyType.class, new MediaType("foo", "bar"))).isTrue();
    assertThat(converter.canRead(MyType.class, new MediaType("foo", "*"))).isTrue();
    assertThat(converter.canRead(MyType.class, MediaType.ALL)).isFalse();
  }

  @Test
  public void canWrite() {
    MediaType mediaType = new MediaType("foo", "bar");
    HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

    assertThat(converter.canWrite(MyType.class, mediaType)).isTrue();
    assertThat(converter.canWrite(MyType.class, new MediaType("foo", "*"))).isTrue();
    assertThat(converter.canWrite(MyType.class, MediaType.ALL)).isTrue();
  }

  @Test
  public void canWriteWithWildcardInSupportedSubtype() {
    MediaType mediaType = new MediaType("foo");
    HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

    assertThat(converter.canWrite(MyType.class, new MediaType("foo", "bar"))).isTrue();
    assertThat(converter.canWrite(MyType.class, new MediaType("foo", "*"))).isTrue();
    assertThat(converter.canWrite(MyType.class, MediaType.ALL)).isTrue();
  }

  private static class MyHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

    private MyHttpMessageConverter(MediaType supportedMediaType) {
      super(supportedMediaType);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
      return MyType.class.equals(clazz);
    }

    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
      throw new AssertionError("Not expected");
    }

    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
      throw new AssertionError("Not expected");
    }
  }

  private static class MyType {

  }

}
