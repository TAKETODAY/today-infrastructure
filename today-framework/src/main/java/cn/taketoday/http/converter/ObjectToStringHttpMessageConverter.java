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

import java.io.IOException;
import java.nio.charset.Charset;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;

/**
 * An {@code HttpMessageConverter} that uses {@link StringHttpMessageConverter}
 * for reading and writing content and a {@link ConversionService} for converting
 * the String content to and from the target object type.
 *
 * <p>By default, this converter supports the media type {@code text/plain} only.
 * This can be overridden through the {@link #setSupportedMediaTypes supportedMediaTypes}
 * property.
 *
 * <p>A usage example:
 *
 * <pre class="code">
 * &lt;bean class="cn.taketoday.http.converter.ObjectToStringHttpMessageConverter"&gt;
 *   &lt;constructor-arg&gt;
 *     &lt;bean class="cn.taketoday.context.support.ConversionServiceFactoryBean"/&gt;
 *   &lt;/constructor-arg&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

  private final ConversionService conversionService;

  private final StringHttpMessageConverter stringHttpMessageConverter;

  /**
   * A constructor accepting a {@code ConversionService} to use to convert the
   * (String) message body to/from the target class type. This constructor uses
   * {@link Constant#DEFAULT_CHARSET} as the default charset.
   *
   * @param conversionService the conversion service
   */
  public ObjectToStringHttpMessageConverter(ConversionService conversionService) {
    this(conversionService, Constant.DEFAULT_CHARSET);
  }

  /**
   * A constructor accepting a {@code ConversionService} as well as a default charset.
   *
   * @param conversionService the conversion service
   * @param defaultCharset the default charset
   */
  public ObjectToStringHttpMessageConverter(ConversionService conversionService, Charset defaultCharset) {
    super(defaultCharset, MediaType.TEXT_PLAIN);

    Assert.notNull(conversionService, "ConversionService is required");
    this.conversionService = conversionService;
    this.stringHttpMessageConverter = new StringHttpMessageConverter(defaultCharset);
  }

  /**
   * Delegates to {@link StringHttpMessageConverter#setWriteAcceptCharset(boolean)}.
   */
  public void setWriteAcceptCharset(boolean writeAcceptCharset) {
    this.stringHttpMessageConverter.setWriteAcceptCharset(writeAcceptCharset);
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return canRead(mediaType) && this.conversionService.canConvert(String.class, clazz);
  }

  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return canWrite(mediaType) && this.conversionService.canConvert(clazz, String.class);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    // should not be called, since we override canRead/Write
    throw new UnsupportedOperationException();
  }

  @Override
  protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    String value = this.stringHttpMessageConverter.readInternal(String.class, inputMessage);
    Object result = this.conversionService.convert(value, clazz);
    if (result == null) {
      throw new HttpMessageNotReadableException(
              "Unexpected null conversion result for '" + value + "' to " + clazz,
              inputMessage);
    }
    return result;
  }

  @Override
  protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException {
    String value = this.conversionService.convert(obj, String.class);
    if (value != null) {
      this.stringHttpMessageConverter.writeInternal(value, outputMessage);
    }
  }

  @Override
  protected Long getContentLength(Object obj, @Nullable MediaType contentType) {
    String value = this.conversionService.convert(obj, String.class);
    if (value == null) {
      return 0L;
    }
    return this.stringHttpMessageConverter.getContentLength(value, contentType);
  }

}
