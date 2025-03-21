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

package infra.oxm.support;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.oxm.Marshaller;

/**
 * {@link Source} implementation that uses a {@link Marshaller}.Can be constructed with a
 * {@code Marshaller} and an object to be marshalled.
 *
 * <p>Even though {@code MarshallingSource} extends from {@code SAXSource},
 * calling the methods of {@code SAXSource} is <strong>not supported</strong>.
 * In general, the only supported operation on this class is to use the
 * {@code XMLReader} obtained via {@link #getXMLReader()} to parse the
 * input source obtained via {@link #getInputSource()}. Calling
 * {@link #setXMLReader(XMLReader)} or {@link #setInputSource(InputSource)}
 * will result in {@code UnsupportedOperationException}s.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see javax.xml.transform.Transformer
 * @since 4.0
 */
public class MarshallingSource extends SAXSource {

  private final Marshaller marshaller;

  private final Object content;

  /**
   * Create a new {@code MarshallingSource} with the given marshaller and content.
   *
   * @param marshaller the marshaller to use
   * @param content the object to be marshalled
   */
  public MarshallingSource(Marshaller marshaller, Object content) {
    super(new MarshallingXMLReader(marshaller, content), new InputSource());
    Assert.notNull(marshaller, "'marshaller' is required");
    Assert.notNull(content, "'content' is required");
    this.marshaller = marshaller;
    this.content = content;
  }

  /**
   * Return the {@code Marshaller} used by this {@code MarshallingSource}.
   */
  public Marshaller getMarshaller() {
    return this.marshaller;
  }

  /**
   * Return the object to be marshalled.
   */
  public Object getContent() {
    return this.content;
  }

  /**
   * Throws a {@code UnsupportedOperationException}.
   */
  @Override
  public void setInputSource(InputSource inputSource) {
    throw new UnsupportedOperationException("setInputSource is not supported");
  }

  /**
   * Throws a {@code UnsupportedOperationException}.
   */
  @Override
  public void setXMLReader(XMLReader reader) {
    throw new UnsupportedOperationException("setXMLReader is not supported");
  }

  static final class MarshallingXMLReader implements XMLReader {

    private final Marshaller marshaller;

    private final Object content;

    @Nullable
    private DTDHandler dtdHandler;

    @Nullable
    private ContentHandler contentHandler;

    @Nullable
    private EntityResolver entityResolver;

    @Nullable
    private ErrorHandler errorHandler;

    @Nullable
    private LexicalHandler lexicalHandler;

    private MarshallingXMLReader(Marshaller marshaller, Object content) {
      Assert.notNull(marshaller, "'marshaller' is required");
      Assert.notNull(content, "'content' is required");
      this.marshaller = marshaller;
      this.content = content;
    }

    @Override
    public void setContentHandler(@Nullable ContentHandler contentHandler) {
      this.contentHandler = contentHandler;
    }

    @Override
    @Nullable
    public ContentHandler getContentHandler() {
      return this.contentHandler;
    }

    @Override
    public void setDTDHandler(@Nullable DTDHandler dtdHandler) {
      this.dtdHandler = dtdHandler;
    }

    @Override
    @Nullable
    public DTDHandler getDTDHandler() {
      return this.dtdHandler;
    }

    @Override
    public void setEntityResolver(@Nullable EntityResolver entityResolver) {
      this.entityResolver = entityResolver;
    }

    @Override
    @Nullable
    public EntityResolver getEntityResolver() {
      return this.entityResolver;
    }

    @Override
    public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
    }

    @Override
    @Nullable
    public ErrorHandler getErrorHandler() {
      return this.errorHandler;
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException {
      throw new SAXNotRecognizedException(name);
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
      throw new SAXNotRecognizedException(name);
    }

    @Override
    @Nullable
    public Object getProperty(String name) throws SAXNotRecognizedException {
      if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
        return this.lexicalHandler;
      }
      else {
        throw new SAXNotRecognizedException(name);
      }
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException {
      if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
        this.lexicalHandler = (LexicalHandler) value;
      }
      else {
        throw new SAXNotRecognizedException(name);
      }
    }

    @Override
    public void parse(InputSource input) throws SAXException {
      parse();
    }

    @Override
    public void parse(String systemId) throws SAXException {
      parse();
    }

    private void parse() throws SAXException {
      SAXResult result = new SAXResult(getContentHandler());
      result.setLexicalHandler(lexicalHandler);
      try {
        this.marshaller.marshal(this.content, result);
      }
      catch (IOException ex) {
        SAXParseException saxException = new SAXParseException(ex.getMessage(), null, null, -1, -1, ex);
        ErrorHandler errorHandler = getErrorHandler();
        if (errorHandler != null) {
          errorHandler.fatalError(saxException);
        }
        else {
          throw saxException;
        }
      }
    }
  }

}
