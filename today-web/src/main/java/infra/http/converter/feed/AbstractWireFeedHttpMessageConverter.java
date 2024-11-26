/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.http.converter.feed;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedInput;
import com.rometools.rome.io.WireFeedOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.converter.AbstractHttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.lang.Constant;
import infra.util.StreamUtils;
import infra.util.StringUtils;

/**
 * Abstract base class for Atom and RSS Feed message converters, using the
 * <a href="https://github.com/rometools/rome">ROME tools</a> project.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AtomFeedHttpMessageConverter
 * @see RssChannelHttpMessageConverter
 * @since 4.0
 */
public abstract class AbstractWireFeedHttpMessageConverter<T extends WireFeed> extends AbstractHttpMessageConverter<T> {

  protected AbstractWireFeedHttpMessageConverter(MediaType supportedMediaType) {
    super(supportedMediaType);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    WireFeedInput feedInput = new WireFeedInput();
    MediaType contentType = inputMessage.getHeaders().getContentType();
    Charset charset = contentType != null && contentType.getCharset() != null
                      ? contentType.getCharset() : Constant.DEFAULT_CHARSET;
    try {
      InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
      Reader reader = new InputStreamReader(inputStream, charset);
      return (T) feedInput.build(reader);
    }
    catch (FeedException ex) {
      throw new HttpMessageNotReadableException("Could not read WireFeed: " + ex.getMessage(), ex, inputMessage);
    }
  }

  @Override
  protected void writeInternal(T wireFeed, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
    Charset charset = StringUtils.isNotEmpty(wireFeed.getEncoding()) ? Charset.forName(wireFeed.getEncoding()) : Constant.DEFAULT_CHARSET;
    MediaType contentType = outputMessage.getHeaders().getContentType();
    if (contentType != null) {
      contentType = contentType.withCharset(charset);
      outputMessage.getHeaders().setContentType(contentType);
    }

    WireFeedOutput feedOutput = new WireFeedOutput();
    try {
      Writer writer = new OutputStreamWriter(outputMessage.getBody(), charset);
      feedOutput.output(wireFeed, writer);
    }
    catch (FeedException ex) {
      throw new HttpMessageNotWritableException("Could not write WireFeed: " + ex.getMessage(), ex);
    }
  }

  @Override
  protected boolean supportsRepeatableWrites(T t) {
    return true;
  }

}
