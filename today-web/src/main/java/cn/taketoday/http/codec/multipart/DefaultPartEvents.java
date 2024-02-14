/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.http.codec.multipart;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;

/**
 * Default implementations of {@link PartEvent} and subtypes.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/22 9:13
 */
abstract class DefaultPartEvents {

  public static FormPartEvent form(HttpHeaders headers) {
    Assert.notNull(headers, "Headers is required");
    return new DefaultFormFieldPartEvent(headers);
  }

  public static FormPartEvent form(HttpHeaders headers, String value) {
    Assert.notNull(headers, "Headers is required");
    Assert.notNull(value, "Value is required");
    return new DefaultFormFieldPartEvent(headers, value);
  }

  public static FilePartEvent file(HttpHeaders headers, DataBuffer dataBuffer, boolean isLast) {
    Assert.notNull(headers, "Headers is required");
    Assert.notNull(dataBuffer, "DataBuffer is required");
    return new DefaultFilePartEvent(headers, dataBuffer, isLast);
  }

  public static FilePartEvent file(HttpHeaders headers) {
    Assert.notNull(headers, "Headers is required");
    return new DefaultFilePartEvent(headers);
  }

  public static PartEvent create(HttpHeaders headers, DataBuffer dataBuffer, boolean isLast) {
    Assert.notNull(headers, "Headers is required");
    Assert.notNull(dataBuffer, "DataBuffer is required");
    if (headers.getContentDisposition().getFilename() != null) {
      return file(headers, dataBuffer, isLast);
    }
    else {
      return new DefaultPartEvent(headers, dataBuffer, isLast);
    }
  }

  public static PartEvent create(HttpHeaders headers) {
    Assert.notNull(headers, "Headers is required");
    if (headers.getContentDisposition().getFilename() != null) {
      return file(headers);
    }
    else {
      return new DefaultPartEvent(headers);
    }
  }

  private static abstract class AbstractPartEvent implements PartEvent {

    private final HttpHeaders headers;

    protected AbstractPartEvent(HttpHeaders headers) {
      this.headers = headers.asReadOnly();
    }

    @Override
    public HttpHeaders headers() {
      return this.headers;
    }
  }

  /**
   * Default implementation of {@link PartEvent}.
   */
  private static class DefaultPartEvent extends AbstractPartEvent {

    private static final DataBuffer EMPTY = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);

    private final DataBuffer content;

    private final boolean last;

    public DefaultPartEvent(HttpHeaders headers) {
      this(headers, EMPTY, true);
    }

    public DefaultPartEvent(HttpHeaders headers, DataBuffer content, boolean last) {
      super(headers);
      this.content = content;
      this.last = last;
    }

    @Override
    public DataBuffer content() {
      return this.content;
    }

    @Override
    public boolean isLast() {
      return this.last;
    }

  }

  /**
   * Default implementation of {@link FormPartEvent}.
   */
  private static final class DefaultFormFieldPartEvent extends AbstractPartEvent implements FormPartEvent {

    private static final String EMPTY = "";

    private final String value;

    public DefaultFormFieldPartEvent(HttpHeaders headers) {
      this(headers, EMPTY);
    }

    public DefaultFormFieldPartEvent(HttpHeaders headers, String value) {
      super(headers);
      this.value = value;
    }

    @Override
    public String value() {
      return this.value;
    }

    @Override
    public DataBuffer content() {
      byte[] bytes = this.value.getBytes(MultipartUtils.charset(headers()));
      return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    }

    @Override
    public boolean isLast() {
      return true;
    }
  }

  /**
   * Default implementation of {@link FilePartEvent}.
   */
  private static class DefaultFilePartEvent extends DefaultPartEvent implements FilePartEvent {

    public DefaultFilePartEvent(HttpHeaders headers) {
      super(headers);
    }

    public DefaultFilePartEvent(HttpHeaders headers, DataBuffer content, boolean last) {
      super(headers, content, last);
    }
  }
}
