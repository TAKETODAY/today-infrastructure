/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.converter.xml;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.converter.AbstractHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.lang.Nullable;
import infra.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter}
 * that can read and write {@link Source} objects.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 15:43
 */
public class SourceHttpMessageConverter<T extends Source> extends AbstractHttpMessageConverter<T> {

  private static final EntityResolver NO_OP_ENTITY_RESOLVER =
          (publicId, systemId) -> new InputSource(new StringReader(""));

  private static final XMLResolver NO_OP_XML_RESOLVER =
          (publicID, systemID, base, ns) -> InputStream.nullInputStream();

  private static final Set<Class<?>> SUPPORTED_CLASSES = Set.of(
          DOMSource.class, SAXSource.class, StAXSource.class, StreamSource.class, Source.class);

  private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

  private boolean supportDtd = false;

  private boolean processExternalEntities = false;

  @Nullable
  private volatile DocumentBuilderFactory documentBuilderFactory;

  @Nullable
  private volatile SAXParserFactory saxParserFactory;

  @Nullable
  private volatile XMLInputFactory xmlInputFactory;

  /**
   * Sets the {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes}
   * to {@code text/xml} and {@code application/xml}, and {@code application/*+xml}.
   */
  public SourceHttpMessageConverter() {
    super(MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("application", "*+xml"));
  }

  /**
   * Indicate whether DTD parsing should be supported.
   * <p>Default is {@code false} meaning that DTD is disabled.
   */
  public void setSupportDtd(boolean supportDtd) {
    this.supportDtd = supportDtd;
    this.documentBuilderFactory = null;
    this.saxParserFactory = null;
    this.xmlInputFactory = null;
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
    this.documentBuilderFactory = null;
    this.saxParserFactory = null;
    this.xmlInputFactory = null;
  }

  /**
   * Return whether XML external entities are allowed.
   */
  public boolean isProcessExternalEntities() {
    return this.processExternalEntities;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return SUPPORTED_CLASSES.contains(clazz);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    InputStream body = StreamUtils.nonClosing(inputMessage.getBody());
    if (DOMSource.class == clazz) {
      return (T) readDOMSource(body, inputMessage);
    }
    else if (SAXSource.class == clazz) {
      return (T) readSAXSource(body, inputMessage);
    }
    else if (StAXSource.class == clazz) {
      return (T) readStAXSource(body, inputMessage);
    }
    else if (StreamSource.class == clazz || Source.class == clazz) {
      return (T) readStreamSource(body);
    }
    else {
      throw new HttpMessageNotReadableException("Could not read class [" + clazz +
              "]. Only DOMSource, SAXSource, StAXSource, and StreamSource are supported.", inputMessage);
    }
  }

  private DOMSource readDOMSource(InputStream body, HttpInputMessage inputMessage) throws IOException {
    try {
      DocumentBuilderFactory builderFactory = this.documentBuilderFactory;
      if (builderFactory == null) {
        builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
        builderFactory.setFeature("http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
        this.documentBuilderFactory = builderFactory;
      }
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      if (!isProcessExternalEntities()) {
        builder.setEntityResolver(NO_OP_ENTITY_RESOLVER);
      }
      Document document = builder.parse(body);
      return new DOMSource(document);
    }
    catch (NullPointerException ex) {
      if (!isSupportDtd()) {
        throw new HttpMessageNotReadableException("NPE while unmarshalling: This can happen " +
                "due to the presence of DTD declarations which are disabled.", ex, inputMessage);
      }
      throw ex;
    }
    catch (ParserConfigurationException ex) {
      throw new HttpMessageNotReadableException(
              "Could not set feature: " + ex.getMessage(), ex, inputMessage);
    }
    catch (SAXException ex) {
      throw new HttpMessageNotReadableException(
              "Could not parse document: " + ex.getMessage(), ex, inputMessage);
    }
  }

  private SAXSource readSAXSource(InputStream body, HttpInputMessage inputMessage) throws IOException {
    try {
      SAXParserFactory parserFactory = this.saxParserFactory;
      if (parserFactory == null) {
        parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
        parserFactory.setFeature("http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
        this.saxParserFactory = parserFactory;
      }
      SAXParser saxParser = parserFactory.newSAXParser();
      XMLReader xmlReader = saxParser.getXMLReader();
      if (!isProcessExternalEntities()) {
        xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
      }
      byte[] bytes = StreamUtils.copyToByteArray(body);
      return new SAXSource(xmlReader, new InputSource(new ByteArrayInputStream(bytes)));
    }
    catch (SAXException | ParserConfigurationException ex) {
      throw new HttpMessageNotReadableException(
              "Could not parse document: " + ex.getMessage(), ex, inputMessage);
    }
  }

  private Source readStAXSource(InputStream body, HttpInputMessage inputMessage) {
    try {
      XMLInputFactory inputFactory = this.xmlInputFactory;
      if (inputFactory == null) {
        inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, isSupportDtd());
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, isProcessExternalEntities());
        if (!isProcessExternalEntities()) {
          inputFactory.setXMLResolver(NO_OP_XML_RESOLVER);
        }
        this.xmlInputFactory = inputFactory;
      }
      XMLStreamReader streamReader = inputFactory.createXMLStreamReader(body);
      return new StAXSource(streamReader);
    }
    catch (XMLStreamException ex) {
      throw new HttpMessageNotReadableException(
              "Could not parse document: " + ex.getMessage(), ex, inputMessage);
    }
  }

  private StreamSource readStreamSource(InputStream body) throws IOException {
    byte[] bytes = StreamUtils.copyToByteArray(body);
    return new StreamSource(new ByteArrayInputStream(bytes));
  }

  @Override
  @Nullable
  protected Long getContentLength(T t, @Nullable MediaType contentType) {
    if (t instanceof DOMSource) {
      try {
        CountingOutputStream os = new CountingOutputStream();
        transform(t, new StreamResult(os));
        return os.count;
      }
      catch (TransformerException ex) {
        // ignore
      }
    }
    return null;
  }

  @Override
  protected void writeInternal(T t, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {
    try {
      Result result = new StreamResult(outputMessage.getBody());
      transform(t, result);
    }
    catch (TransformerException ex) {
      throw new HttpMessageNotWritableException("Could not transform [" + t + "] to output message", ex);
    }
  }

  private void transform(Source source, Result result) throws TransformerException {
    this.transformerFactory.newTransformer().transform(source, result);
  }

  @Override
  protected boolean supportsRepeatableWrites(T t) {
    return t instanceof DOMSource;
  }

  private static final class CountingOutputStream extends OutputStream {

    long count = 0;

    @Override
    public void write(int b) throws IOException {
      this.count++;
    }

    @Override
    public void write(byte[] b) throws IOException {
      this.count += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      this.count += len;
    }
  }

}
