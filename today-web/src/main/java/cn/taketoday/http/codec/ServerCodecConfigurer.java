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

package cn.taketoday.http.codec;

import cn.taketoday.core.codec.Encoder;
import cn.taketoday.util.MultiValueMap;

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
 * <li>{@link MultiValueMap
 * MultiValueMap&lt;String,String&gt;} for form data
 * <li>{@link MultiValueMap
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
 * <li>{@link MultiValueMap
 * MultiValueMap&lt;String,String&gt;} for form data
 * <li>JSON and Smile, if Jackson is present
 * <li>XML, if JAXB2 is present
 * <li>Server-Sent Events
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
   * {@link CodecConfigurer.DefaultCodecs} extension with extra server-side options.
   */
  interface ServerDefaultCodecs extends DefaultCodecs {

    /**
     * Configure the {@code Encoder} to use for Server-Sent Events.
     * <p>By default if this is not set, and Jackson is available,
     * the {@link #jackson2JsonEncoder} override is used instead.
     * Use this method to customize the SSE encoder.
     */
    void serverSentEventEncoder(Encoder<?> encoder);
  }

}
