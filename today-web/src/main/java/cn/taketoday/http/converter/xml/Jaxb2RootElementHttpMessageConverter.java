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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConversionException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write XML using JAXB2.
 *
 * <p>This converter can read classes annotated with {@link XmlRootElement} and
 * {@link XmlType}, and write classes annotated with {@link XmlRootElement},
 * or subclasses thereof.
 *
 * <p>Note: When using Infra Marshaller/Unmarshaller abstractions from {@code spring-oxm},
 * you should use the {@link MarshallingHttpMessageConverter} instead.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see MarshallingHttpMessageConverter
 * @since 4.0
 */
public class Jaxb2RootElementHttpMessageConverter extends AbstractJaxb2HttpMessageConverter<Object> {

  private boolean supportDtd = false;

  private boolean processExternalEntities = false;

  /**
   * Indicate whether DTD parsing should be supported.
   * <p>Default is {@code false} meaning that DTD is disabled.
   */
  public void setSupportDtd(boolean supportDtd) {
    this.supportDtd = supportDtd;
  }

  /**
   * Return whether DTD parsing is supported.
   */
  public boolean isSupportDtd() {
    return this.supportDtd;
  }

  /**
   * Indicate whether external XML entities are processed when converting to a Source.
   * <p>Default is {@code false}, meaning that external entities are not resolved.
   * <p><strong>Note:</strong> setting this option to {@code true} also
   * automatically sets {@link #setSupportDtd} to {@code true}.
   */
  public void setProcessExternalEntities(boolean processExternalEntities) {
    this.processExternalEntities = processExternalEntities;
    if (processExternalEntities) {
      this.supportDtd = true;
    }
  }

  /**
   * Return whether XML external entities are allowed.
   */
  public boolean isProcessExternalEntities() {
    return this.processExternalEntities;
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return (clazz.isAnnotationPresent(XmlRootElement.class) || clazz.isAnnotationPresent(XmlType.class)) &&
            canRead(mediaType);
  }

  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return (AnnotationUtils.findAnnotation(clazz, XmlRootElement.class) != null && canWrite(mediaType));
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    // should not be called, since we override canRead/Write
    throw new UnsupportedOperationException();
  }

  @Override
  protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source) throws Exception {
    try {
      source = processSource(source);
      Unmarshaller unmarshaller = createUnmarshaller(clazz);
      if (clazz.isAnnotationPresent(XmlRootElement.class)) {
        return unmarshaller.unmarshal(source);
      }
      else {
        JAXBElement<?> jaxbElement = unmarshaller.unmarshal(source, clazz);
        return jaxbElement.getValue();
      }
    }
    catch (NullPointerException ex) {
      if (!isSupportDtd()) {
        throw new IllegalStateException("NPE while unmarshalling. " +
                "This can happen due to the presence of DTD declarations which are disabled.", ex);
      }
      throw ex;
    }
    catch (UnmarshalException ex) {
      throw ex;
    }
    catch (JAXBException ex) {
      throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
    }
  }

  protected Source processSource(Source source) {
    if (source instanceof StreamSource streamSource) {
      InputSource inputSource = new InputSource(streamSource.getInputStream());
      try {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
        String featureName = "http://xml.org/sax/features/external-general-entities";
        saxParserFactory.setFeature(featureName, isProcessExternalEntities());
        SAXParser saxParser = saxParserFactory.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        if (!isProcessExternalEntities()) {
          xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
        }
        return new SAXSource(xmlReader, inputSource);
      }
      catch (SAXException | ParserConfigurationException ex) {
        logger.warn("Processing of external entities could not be disabled", ex);
        return source;
      }
    }
    else {
      return source;
    }
  }

  @Override
  protected void writeToResult(Object o, HttpHeaders headers, Result result) throws Exception {
    try {
      Class<?> clazz = ClassUtils.getUserClass(o);
      Marshaller marshaller = createMarshaller(clazz);
      setCharset(headers.getContentType(), marshaller);
      marshaller.marshal(o, result);
    }
    catch (MarshalException ex) {
      throw ex;
    }
    catch (JAXBException ex) {
      throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
    }
  }

  private void setCharset(@Nullable MediaType contentType, Marshaller marshaller) throws PropertyException {
    if (contentType != null && contentType.getCharset() != null) {
      marshaller.setProperty(Marshaller.JAXB_ENCODING, contentType.getCharset().name());
    }
  }

  private static final EntityResolver NO_OP_ENTITY_RESOLVER =
          (publicId, systemId) -> new InputSource(new StringReader(""));

}
