/*
 * Copyright 2002-present the original author or authors.
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

package infra.http.converter;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.lang.Constant;

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
    Charset charset = getContentTypeCharset(inputMessage.getContentType());
    long length = inputMessage.getContentLength();
    byte[] bytes = (length >= 0 && length <= Integer.MAX_VALUE ?
            inputMessage.getBody().readNBytes((int) length) :
            inputMessage.getBody().readAllBytes());
    return new String(bytes, charset);
  }

  @Override
  protected @Nullable Long getContentLength(String str, HttpOutputMessage message) throws IOException {
    Charset charset = getContentTypeCharset(message.getContentType());
    return (long) str.getBytes(charset).length;
  }

  @Override
  public void addDefaultHeaders(HttpOutputMessage message, String s, @Nullable MediaType type) throws IOException {
    String contentTypeString = message.getContentTypeAsString();
    if (contentTypeString == null) {
      if (type != null && type.isConcrete() && (
              type.isCompatibleWith(MediaType.APPLICATION_JSON)
                      || type.isCompatibleWith(APPLICATION_PLUS_JSON))) {
        // Prevent charset parameter for JSON..
        message.setContentType(type);
      }
    }

    super.addDefaultHeaders(message, s, type);
  }

  @Override
  protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
    HttpHeaders headers = outputMessage.getHeaders();
    if (this.writeAcceptCharset && headers.get(HttpHeaders.ACCEPT_CHARSET) == null) {
      headers.setAcceptCharset(getAcceptedCharsets());
    }
    Charset charset = getContentTypeCharset(headers.getContentType());
    outputMessage.getBody().write(str.getBytes(charset));
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
