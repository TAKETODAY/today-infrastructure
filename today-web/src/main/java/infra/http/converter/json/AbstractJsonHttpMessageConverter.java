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

package infra.http.converter.json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import infra.core.GenericTypeResolver;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.converter.AbstractGenericHttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.lang.Constant;
import infra.lang.Nullable;

/**
 * Common base class for plain JSON converters, e.g. Gson and JSON-B.
 *
 * <p>Note that the Jackson converters have a dedicated class hierarchy
 * due to their multi-format support.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see GsonHttpMessageConverter
 * @see JsonbHttpMessageConverter
 * @see #readInternal(Type, Reader)
 * @see #writeInternal(Object, Type, Writer)
 * @since 4.0
 */
public abstract class AbstractJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

  @Nullable
  private String jsonPrefix;

  public AbstractJsonHttpMessageConverter() {
    super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    setDefaultCharset(Constant.DEFAULT_CHARSET);
  }

  /**
   * Specify a custom prefix to use for JSON output. Default is none.
   *
   * @see #setPrefixJson
   */
  public void setJsonPrefix(String jsonPrefix) {
    this.jsonPrefix = jsonPrefix;
  }

  /**
   * Indicate whether the JSON output by this view should be prefixed with ")]}', ".
   * Default is {@code false}.
   * <p>Prefixing the JSON string in this manner is used to help prevent JSON
   * Hijacking. The prefix renders the string syntactically invalid as a script
   * so that it cannot be hijacked.
   * This prefix should be stripped before parsing the string as JSON.
   *
   * @see #setJsonPrefix
   */
  public void setPrefixJson(boolean prefixJson) {
    this.jsonPrefix = (prefixJson ? ")]}', " : null);
  }

  @Override
  public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    return readResolved(GenericTypeResolver.resolveType(type, contextClass), inputMessage);
  }

  @Override
  protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    return readResolved(clazz, inputMessage);
  }

  private Object readResolved(Type resolvedType, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    Reader reader = getReader(inputMessage);
    try {
      return readInternal(resolvedType, reader);
    }
    catch (Exception ex) {
      throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex, inputMessage);
    }
  }

  @Override
  protected final void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    Writer writer = getWriter(outputMessage);
    if (this.jsonPrefix != null) {
      writer.append(this.jsonPrefix);
    }
    try {
      writeInternal(object, type, writer);
    }
    catch (Exception ex) {
      throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
    }
    writer.flush();
  }

  /**
   * Template method that reads the JSON-bound object from the given {@link Reader}.
   *
   * @param resolvedType the resolved generic type
   * @param reader the {@code} Reader to use
   * @return the JSON-bound object
   * @throws Exception in case of read/parse failures
   */
  protected abstract Object readInternal(Type resolvedType, Reader reader) throws Exception;

  /**
   * Template method that writes the JSON-bound object to the given {@link Writer}.
   *
   * @param object the object to write to the output message
   * @param type the type of object to write (may be {@code null})
   * @param writer the {@code} Writer to use
   * @throws Exception in case of write failures
   */
  protected abstract void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception;

  private static Reader getReader(HttpInputMessage inputMessage) throws IOException {
    return new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders()));
  }

  private static Writer getWriter(HttpOutputMessage outputMessage) throws IOException {
    return new OutputStreamWriter(outputMessage.getBody(), getCharset(outputMessage.getHeaders()));
  }

  private static Charset getCharset(HttpHeaders headers) {
    MediaType contentType = headers.getContentType();
    if (contentType != null) {
      Charset charset = contentType.getCharset();
      if (charset != null) {
        return charset;
      }
    }
    return Constant.DEFAULT_CHARSET;
  }

}
