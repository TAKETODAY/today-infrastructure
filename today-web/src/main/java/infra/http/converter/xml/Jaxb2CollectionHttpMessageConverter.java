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

package infra.http.converter.xml;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.converter.GenericHttpMessageConverter;
import infra.http.converter.HttpMessageConversionException;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.lang.Nullable;
import infra.util.ReflectionUtils;
import infra.util.xml.StaxUtils;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * An {@code HttpMessageConverter} that can read XML collections using JAXB2.
 *
 * <p>This converter can read {@linkplain Collection collections} that contain classes
 * annotated with {@link XmlRootElement} and {@link XmlType}. Note that this converter
 * does not support writing.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
public class Jaxb2CollectionHttpMessageConverter<T extends Collection>
        extends AbstractJaxb2HttpMessageConverter<T> implements GenericHttpMessageConverter<T> {

  private final XMLInputFactory inputFactory = createXmlInputFactory();

  /**
   * Always returns {@code false} since Jaxb2CollectionHttpMessageConverter
   * required generic type information in order to read a Collection.
   */
  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return false;
  }

  /**
   * {@inheritDoc}
   * <p>Jaxb2CollectionHttpMessageConverter can read a generic
   * {@link Collection} where the generic type is a JAXB type annotated with
   * {@link XmlRootElement} or {@link XmlType}.
   */
  @Override
  public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
    if (!(type instanceof ParameterizedType parameterizedType)) {
      return false;
    }
    if (!(parameterizedType.getRawType() instanceof Class<?> rawType)) {
      return false;
    }
    if (!(Collection.class.isAssignableFrom(rawType))) {
      return false;
    }
    if (parameterizedType.getActualTypeArguments().length != 1) {
      return false;
    }
    Type typeArgument = parameterizedType.getActualTypeArguments()[0];
    if (!(typeArgument instanceof Class<?> typeArgumentClass)) {
      return false;
    }
    return (typeArgumentClass.isAnnotationPresent(XmlRootElement.class) ||
            typeArgumentClass.isAnnotationPresent(XmlType.class)) && canRead(mediaType);
  }

  /**
   * Always returns {@code false} since Jaxb2CollectionHttpMessageConverter
   * does not convert collections to XML.
   */
  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return false;
  }

  /**
   * Always returns {@code false} since Jaxb2CollectionHttpMessageConverter
   * does not convert collections to XML.
   */
  @Override
  public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
    return false;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    // should not be called, since we override canRead/Write
    throw new UnsupportedOperationException();
  }

  @Override
  protected T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source) throws Exception {
    // should not be called, since we return false for canRead(Class)
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    ParameterizedType parameterizedType = (ParameterizedType) type;
    T result = createCollection((Class<?>) parameterizedType.getRawType());
    Class<?> elementClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];

    try {
      Unmarshaller unmarshaller = createUnmarshaller(elementClass);
      XMLStreamReader streamReader = this.inputFactory.createXMLStreamReader(inputMessage.getBody());
      int event = moveToFirstChildOfRootElement(streamReader);

      while (event != XMLStreamReader.END_DOCUMENT) {
        if (elementClass.isAnnotationPresent(XmlRootElement.class)) {
          result.add(unmarshaller.unmarshal(streamReader));
        }
        else if (elementClass.isAnnotationPresent(XmlType.class)) {
          result.add(unmarshaller.unmarshal(streamReader, elementClass).getValue());
        }
        else {
          // should not happen, since we check in canRead(Type)
          throw new HttpMessageNotReadableException(
                  "Cannot unmarshal to [" + elementClass + "]", inputMessage);
        }
        event = moveToNextElement(streamReader);
      }
      return result;
    }
    catch (XMLStreamException ex) {
      throw new HttpMessageNotReadableException(
              "Failed to read XML stream: " + ex.getMessage(), ex, inputMessage);
    }
    catch (UnmarshalException ex) {
      throw new HttpMessageNotReadableException(
              "Could not unmarshal to [" + elementClass + "]: " + ex, ex, inputMessage);
    }
    catch (JAXBException ex) {
      throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
    }
  }

  /**
   * Create a Collection of the given type, with the given initial capacity
   * (if supported by the Collection type).
   *
   * @param collectionClass the type of Collection to instantiate
   * @return the created Collection instance
   */
  @SuppressWarnings("unchecked")
  protected T createCollection(Class<?> collectionClass) {
    if (!collectionClass.isInterface()) {
      try {
        return (T) ReflectionUtils.accessibleConstructor(collectionClass).newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException(
                "Could not instantiate collection class: " + collectionClass.getName(), ex);
      }
    }
    else if (List.class == collectionClass) {
      return (T) new ArrayList();
    }
    else if (SortedSet.class == collectionClass) {
      return (T) new TreeSet();
    }
    else {
      return (T) new LinkedHashSet();
    }
  }

  private int moveToFirstChildOfRootElement(XMLStreamReader streamReader) throws XMLStreamException {
    // root
    int event = streamReader.next();
    while (event != XMLStreamReader.START_ELEMENT) {
      event = streamReader.next();
    }

    // first child
    event = streamReader.next();
    while ((event != XMLStreamReader.START_ELEMENT) && (event != XMLStreamReader.END_DOCUMENT)) {
      event = streamReader.next();
    }
    return event;
  }

  private int moveToNextElement(XMLStreamReader streamReader) throws XMLStreamException {
    int event = streamReader.getEventType();
    while (event != XMLStreamReader.START_ELEMENT && event != XMLStreamReader.END_DOCUMENT) {
      event = streamReader.next();
    }
    return event;
  }

  @Override
  public void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    throw new UnsupportedOperationException();
  }

  @Override
  protected void writeToResult(T t, HttpHeaders headers, Result result) throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Create an {@code XMLInputFactory} that this converter will use to create
   * {@link javax.xml.stream.XMLStreamReader} and {@link javax.xml.stream.XMLEventReader}
   * objects.
   * <p>Can be overridden in subclasses, adding further initialization of the factory.
   * The resulting factory is cached, so this method will only be called once.
   *
   * @see StaxUtils#createDefensiveInputFactory()
   */
  protected XMLInputFactory createXmlInputFactory() {
    return StaxUtils.createDefensiveInputFactory();
  }

}
