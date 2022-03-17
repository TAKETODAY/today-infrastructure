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

package cn.taketoday.http.codec;

import cn.taketoday.core.codec.Encoder;

/**
 * Extension of {@link CodecConfigurer} for HTTP message reader and writer
 * options relevant on the server side.
 *
 * <p>HTTP message readers for the following are registered by default:
 * <ul>{@code byte[]}
 * <li>{@link java.nio.ByteBuffer}
 * <li>{@link cn.taketoday.core.io.buffer.DataBuffer DataBuffer}
 * <li>{@link cn.taketoday.core.io.Resource Resource}
 * <li>{@link String}
 * <li>{@link cn.taketoday.core.MultiValueMap
 * MultiValueMap&lt;String,String&gt;} for form data
 * <li>{@link cn.taketoday.core.MultiValueMap
 * MultiValueMap&lt;String,Object&gt;} for multipart data
 * <li>JSON and Smile, if Jackson is present
 * <li>XML, if JAXB2 is present
 * </ul>
 *
 * <p>HTTP message writers registered by default:
 * <ul>{@code byte[]}
 * <li>{@link java.nio.ByteBuffer}
 * <li>{@link cn.taketoday.core.io.buffer.DataBuffer DataBuffer}
 * <li>{@link cn.taketoday.core.io.Resource Resource}
 * <li>{@link String}
 * <li>{@link cn.taketoday.core.MultiValueMap
 * MultiValueMap&lt;String,String&gt;} for form data
 * <li>JSON and Smile, if Jackson is present
 * <li>XML, if JAXB2 is present
 * <li>Server-Sent Events
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ServerCodecConfigurer extends CodecConfigurer {

  /**
   * {@inheritDoc}
   * <p>On the server side, built-in default also include customizations
   * related to the encoder for SSE.
   */
  @Override
  ServerDefaultCodecs defaultCodecs();

  /**
   * {@inheritDoc}.
   */
  @Override
  ServerCodecConfigurer clone();

  /**
   * Static factory method for a {@code ServerCodecConfigurer}.
   */
  static ServerCodecConfigurer create() {
    return CodecConfigurerFactory.create(ServerCodecConfigurer.class);
  }

  /**
   * {@link DefaultCodecs} extension with extra client-side options.
   */
  interface ServerDefaultCodecs extends DefaultCodecs {

    /**
     * Configure the {@code HttpMessageReader} to use for multipart requests.
     * <p>Note that {@link #maxInMemorySize(int)} and/or
     * {@link #enableLoggingRequestDetails(boolean)}, if configured, will be
     * applied to the given reader, if applicable.
     *
     * @param reader the message reader to use for multipart requests.
     */
    void multipartReader(HttpMessageReader<?> reader);

    /**
     * Configure the {@code Encoder} to use for Server-Sent Events.
     * <p>By default if this is not set, and Jackson is available, the
     * {@link #jackson2JsonEncoder} override is used instead. Use this method
     * to customize the SSE encoder.
     */
    void serverSentEventEncoder(Encoder<?> encoder);
  }

}
