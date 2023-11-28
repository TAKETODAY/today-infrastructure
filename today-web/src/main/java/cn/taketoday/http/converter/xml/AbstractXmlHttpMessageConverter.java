/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.converter.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.AbstractHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConversionException;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.util.StreamUtils;

/**
 * Abstract base class for {@link cn.taketoday.http.converter.HttpMessageConverter HttpMessageConverters}
 * that convert from/to XML.
 *
 * <p>By default, subclasses of this converter support {@code text/xml}, {@code application/xml}, and {@code
 * application/*-xml}. This can be overridden by setting the {@link #setSupportedMediaTypes(java.util.List)
 * supportedMediaTypes} property.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractXmlHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

  private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

  /**
   * Protected constructor that sets the {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes}
   * to {@code text/xml} and {@code application/xml}, and {@code application/*-xml}.
   */
  protected AbstractXmlHttpMessageConverter() {
    super(MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("application", "*+xml"));
  }

  @Override
  public final T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    try {
      InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
      return readFromSource(clazz, inputMessage.getHeaders(), new StreamSource(inputStream));
    }
    catch (IOException | HttpMessageConversionException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new HttpMessageNotReadableException("Could not unmarshal to [" + clazz + "]: " + ex,
              ex, inputMessage);
    }
  }

  @Override
  protected final void writeInternal(T t, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    try {
      writeToResult(t, outputMessage.getHeaders(), new StreamResult(outputMessage.getBody()));
    }
    catch (IOException | HttpMessageConversionException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new HttpMessageNotWritableException("Could not marshal [" + t + "]: " + ex.getMessage(), ex);
    }
  }

  /**
   * Transforms the given {@code Source} to the {@code Result}.
   *
   * @param source the source to transform from
   * @param result the result to transform to
   * @throws TransformerException in case of transformation errors
   */
  protected void transform(Source source, Result result) throws TransformerException {
    this.transformerFactory.newTransformer().transform(source, result);
  }

  /**
   * Abstract template method called from {@link #read(Class, HttpInputMessage)}.
   *
   * @param clazz the type of object to return
   * @param headers the HTTP input headers
   * @param source the HTTP input body
   * @return the converted object
   * @throws Exception in case of I/O or conversion errors
   */
  protected abstract T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source) throws Exception;

  /**
   * Abstract template method called from {@link #writeInternal(Object, HttpOutputMessage)}.
   *
   * @param t the object to write to the output message
   * @param headers the HTTP output headers
   * @param result the HTTP output body
   * @throws Exception in case of I/O or conversion errors
   */
  protected abstract void writeToResult(T t, HttpHeaders headers, Result result) throws Exception;

}
