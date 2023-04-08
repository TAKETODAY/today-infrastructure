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

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.oxm.Marshaller;
import cn.taketoday.oxm.Unmarshaller;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write XML using Infra {@link Marshaller} and {@link Unmarshaller} abstractions.
 *
 * <p>This converter requires a {@code Marshaller} and {@code Unmarshaller} before it can be used.
 * These can be injected by the {@linkplain #MarshallingHttpMessageConverter(Marshaller) constructor}
 * or {@linkplain #setMarshaller(Marshaller) bean properties}.
 *
 * <p>By default, this converter supports {@code text/xml} and {@code application/xml}. This can be
 * overridden by setting the {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class MarshallingHttpMessageConverter extends AbstractXmlHttpMessageConverter<Object> {

  @Nullable
  private Marshaller marshaller;

  @Nullable
  private Unmarshaller unmarshaller;

  /**
   * Construct a new {@code MarshallingHttpMessageConverter} with no {@link Marshaller} or
   * {@link Unmarshaller} set. The Marshaller and Unmarshaller must be set after construction
   * by invoking {@link #setMarshaller(Marshaller)} and {@link #setUnmarshaller(Unmarshaller)}.
   */
  public MarshallingHttpMessageConverter() {

  }

  /**
   * Construct a new {@code MarshallingMessageConverter} with the given {@link Marshaller} set.
   * <p>If the given {@link Marshaller} also implements the {@link Unmarshaller} interface,
   * it is used for both marshalling and unmarshalling. Otherwise, an exception is thrown.
   * <p>Note that all {@code Marshaller} implementations in Spring also implement the
   * {@code Unmarshaller} interface, so that you can safely use this constructor.
   *
   * @param marshaller object used as marshaller and unmarshaller
   */
  public MarshallingHttpMessageConverter(Marshaller marshaller) {
    Assert.notNull(marshaller, "Marshaller must not be null");
    this.marshaller = marshaller;
    // The following pattern variable cannot be named "unmarshaller" due to lacking
    // support in Checkstyle: https://github.com/checkstyle/checkstyle/issues/10969
    if (marshaller instanceof Unmarshaller _unmarshaller) {
      this.unmarshaller = _unmarshaller;
    }
  }

  /**
   * Construct a new {@code MarshallingMessageConverter} with the given
   * {@code Marshaller} and {@code Unmarshaller}.
   *
   * @param marshaller the Marshaller to use
   * @param unmarshaller the Unmarshaller to use
   */
  public MarshallingHttpMessageConverter(Marshaller marshaller, Unmarshaller unmarshaller) {
    Assert.notNull(marshaller, "Marshaller must not be null");
    Assert.notNull(unmarshaller, "Unmarshaller must not be null");
    this.marshaller = marshaller;
    this.unmarshaller = unmarshaller;
  }

  /**
   * Set the {@link Marshaller} to be used by this message converter.
   */
  public void setMarshaller(Marshaller marshaller) {
    this.marshaller = marshaller;
  }

  /**
   * Set the {@link Unmarshaller} to be used by this message converter.
   */
  public void setUnmarshaller(Unmarshaller unmarshaller) {
    this.unmarshaller = unmarshaller;
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return (canRead(mediaType) && this.unmarshaller != null && this.unmarshaller.supports(clazz));
  }

  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return (canWrite(mediaType) && this.marshaller != null && this.marshaller.supports(clazz));
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    // should not be called, since we override canRead()/canWrite()
    throw new UnsupportedOperationException();
  }

  @Override
  protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source) throws Exception {
    Assert.state(this.unmarshaller != null, "Property 'unmarshaller' is required");
    Object result = this.unmarshaller.unmarshal(source);
    if (!clazz.isInstance(result)) {
      throw new TypeMismatchException(result, clazz);
    }
    return result;
  }

  @Override
  protected void writeToResult(Object o, HttpHeaders headers, Result result) throws Exception {
    Assert.state(this.marshaller != null, "Property 'marshaller' is required");
    this.marshaller.marshal(o, result);
  }

}
