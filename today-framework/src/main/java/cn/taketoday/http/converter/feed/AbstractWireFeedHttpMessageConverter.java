/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.converter.feed;

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
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.converter.AbstractHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for Atom and RSS Feed message converters, using the
 * <a href="https://github.com/rometools/rome">ROME tools</a> project.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @see AtomFeedHttpMessageConverter
 * @see RssChannelHttpMessageConverter
 * @since 4.0
 */
public abstract class AbstractWireFeedHttpMessageConverter<T extends WireFeed>
        extends AbstractHttpMessageConverter<T> {

  /**
   * The default charset used by the converter.
   */
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

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
                      ? contentType.getCharset() : DEFAULT_CHARSET;
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
  protected void writeInternal(T wireFeed, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {
    Charset charset = (StringUtils.isNotEmpty(wireFeed.getEncoding())
                       ? Charset.forName(wireFeed.getEncoding()) : DEFAULT_CHARSET);
    MediaType contentType = outputMessage.getHeaders().getContentType();
    if (contentType != null) {
      contentType = new MediaType(contentType, charset);
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

}
