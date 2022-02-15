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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write byte arrays.
 *
 * <p>By default, this converter supports all media types (<code>&#42;/&#42;</code>), and
 * writes with a {@code Content-Type} of {@code application/octet-stream}. This can be
 * overridden by setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

  /**
   * Create a new instance of the {@code ByteArrayHttpMessageConverter}.
   */
  public ByteArrayHttpMessageConverter() {
    super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return byte[].class == clazz;
  }

  @Override
  public byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage inputMessage) throws IOException {
    long contentLength = inputMessage.getHeaders().getContentLength();
    ByteArrayOutputStream bos =
            new ByteArrayOutputStream(contentLength >= 0 ? (int) contentLength : StreamUtils.BUFFER_SIZE);
    StreamUtils.copy(inputMessage.getBody(), bos);
    return bos.toByteArray();
  }

  @Override
  protected Long getContentLength(byte[] bytes, @Nullable MediaType contentType) {
    return (long) bytes.length;
  }

  @Override
  protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
    StreamUtils.copy(bytes, outputMessage.getBody());
  }

}
