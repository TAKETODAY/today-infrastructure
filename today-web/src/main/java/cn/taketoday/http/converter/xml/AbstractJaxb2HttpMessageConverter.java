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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.taketoday.http.converter.HttpMessageConversionException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * Abstract base class for {@link cn.taketoday.http.converter.HttpMessageConverter HttpMessageConverters}
 * that use JAXB2. Creates {@link JAXBContext} object lazily.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractJaxb2HttpMessageConverter<T> extends AbstractXmlHttpMessageConverter<T> {

  private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);

  /**
   * Create a new {@link Marshaller} for the given class.
   *
   * @param clazz the class to create the marshaller for
   * @return the {@code Marshaller}
   * @throws HttpMessageConversionException in case of JAXB errors
   */
  protected final Marshaller createMarshaller(Class<?> clazz) {
    try {
      JAXBContext jaxbContext = getJaxbContext(clazz);
      Marshaller marshaller = jaxbContext.createMarshaller();
      customizeMarshaller(marshaller);
      return marshaller;
    }
    catch (JAXBException ex) {
      throw new HttpMessageConversionException(
              "Could not create Marshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
    }
  }

  /**
   * Customize the {@link Marshaller} created by this
   * message converter before using it to write the object to the output.
   *
   * @param marshaller the marshaller to customize
   * @see #createMarshaller(Class)
   */
  protected void customizeMarshaller(Marshaller marshaller) {
  }

  /**
   * Create a new {@link Unmarshaller} for the given class.
   *
   * @param clazz the class to create the unmarshaller for
   * @return the {@code Unmarshaller}
   * @throws HttpMessageConversionException in case of JAXB errors
   */
  protected final Unmarshaller createUnmarshaller(Class<?> clazz) {
    try {
      JAXBContext jaxbContext = getJaxbContext(clazz);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      customizeUnmarshaller(unmarshaller);
      return unmarshaller;
    }
    catch (JAXBException ex) {
      throw new HttpMessageConversionException(
              "Could not create Unmarshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
    }
  }

  /**
   * Customize the {@link Unmarshaller} created by this
   * message converter before using it to read the object from the input.
   *
   * @param unmarshaller the unmarshaller to customize
   * @see #createUnmarshaller(Class)
   */
  protected void customizeUnmarshaller(Unmarshaller unmarshaller) {

  }

  /**
   * Return a {@link JAXBContext} for the given class.
   *
   * @param clazz the class to return the context for
   * @return the {@code JAXBContext}
   * @throws HttpMessageConversionException in case of JAXB errors
   */
  protected final JAXBContext getJaxbContext(Class<?> clazz) {
    return this.jaxbContexts.computeIfAbsent(clazz, key -> {
      try {
        return JAXBContext.newInstance(clazz);
      }
      catch (JAXBException ex) {
        throw new HttpMessageConversionException(
                "Could not create JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
      }
    });
  }

}
