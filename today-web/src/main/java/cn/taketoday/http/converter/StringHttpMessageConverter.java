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

package cn.taketoday.http.converter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write strings.
 *
 * <p>By default, this converter supports all media types (<code>&#42;/&#42;</code>),
 * and writes with a {@code Content-Type} of {@code text/plain}. This can be overridden
 * by setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {

  private static final MediaType APPLICATION_PLUS_JSON = new MediaType("application", "*+json");

  @Nullable
  private volatile List<Charset> availableCharsets;

  private boolean writeAcceptCharset = false;

  /**
   * A default constructor that uses {@code "UTF-8"} as the default charset.
   *
   * @see #StringHttpMessageConverter(Charset)
   */
  public StringHttpMessageConverter() {
    this(Constant.DEFAULT_CHARSET);
  }

  /**
   * A constructor accepting a default charset to use if the requested content
   * type does not specify one.
   */
  public StringHttpMessageConverter(Charset defaultCharset) {
    super(defaultCharset, MediaType.TEXT_PLAIN, MediaType.ALL);
  }

  /**
   * Whether the {@code Accept-Charset} header should be written to any outgoing
   * request sourced from the value of {@link Charset#availableCharsets()}.
   * The behavior is suppressed if the header has already been set.
   */
  public void setWriteAcceptCharset(boolean writeAcceptCharset) {
    this.writeAcceptCharset = writeAcceptCharset;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return String.class == clazz;
  }

  @Override
  protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
    Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
    long length = inputMessage.getHeaders().getContentLength();
    byte[] bytes = (length >= 0 && length <= Integer.MAX_VALUE ?
                    inputMessage.getBody().readNBytes((int) length) :
                    inputMessage.getBody().readAllBytes());
    return new String(bytes, charset);
  }

  @Override
  protected Long getContentLength(String str, @Nullable MediaType contentType) {
    Charset charset = getContentTypeCharset(contentType);
    return (long) str.getBytes(charset).length;
  }

  @Override
  public void addDefaultHeaders(HttpHeaders headers, String s, @Nullable MediaType type) throws IOException {
    String contentTypeString = headers.getFirst(HttpHeaders.CONTENT_TYPE);
    if (contentTypeString == null) {
      if (type != null && type.isConcrete() && (
              type.isCompatibleWith(MediaType.APPLICATION_JSON)
                      || type.isCompatibleWith(APPLICATION_PLUS_JSON))) {
        // Prevent charset parameter for JSON..
        headers.setContentType(type);
      }
    }
    super.addDefaultHeaders(headers, s, type);
  }

  @Override
  protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
    HttpHeaders headers = outputMessage.getHeaders();
    if (this.writeAcceptCharset && headers.get(HttpHeaders.ACCEPT_CHARSET) == null) {
      headers.setAcceptCharset(getAcceptedCharsets());
    }
    Charset charset = getContentTypeCharset(headers.getContentType());
    StreamUtils.copy(str, charset, outputMessage.getBody());
  }

  /**
   * Return the list of supported {@link Charset Charsets}.
   * <p>By default, returns {@link Charset#availableCharsets()}.
   * Can be overridden in subclasses.
   *
   * @return the list of accepted charsets
   */
  protected List<Charset> getAcceptedCharsets() {
    List<Charset> charsets = this.availableCharsets;
    if (charsets == null) {
      charsets = new ArrayList<>(Charset.availableCharsets().values());
      this.availableCharsets = charsets;
    }
    return charsets;
  }

  private Charset getContentTypeCharset(@Nullable MediaType contentType) {
    if (contentType != null) {
      Charset charset = contentType.getCharset();
      if (charset != null) {
        return charset;
      }
      else if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
              || contentType.isCompatibleWith(APPLICATION_PLUS_JSON)) {
        // Matching to AbstractJackson2HttpMessageConverter#DEFAULT_CHARSET
        return StandardCharsets.UTF_8;
      }
    }
    Charset charset = getDefaultCharset();
    Assert.state(charset != null, "No default charset");
    return charset;
  }

  @Override
  protected boolean supportsRepeatableWrites(String s) {
    return true;
  }

}
