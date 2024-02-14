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

import cn.taketoday.core.codec.Decoder;
import cn.taketoday.util.MultiValueMap;

/**
 * Extension of {@link CodecConfigurer} for HTTP message reader and writer
 * options relevant on the client side.
 *
 * <p>HTTP message readers for the following are registered by default:
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
 * <p>HTTP message writers registered by default:
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
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ClientCodecConfigurer extends CodecConfigurer {

  /**
   * {@inheritDoc}
   * <p>On the client side, built-in default also include customizations related
   * to multipart readers and writers, as well as the decoder for SSE.
   */
  @Override
  ClientDefaultCodecs defaultCodecs();

  /**
   * {@inheritDoc}.
   */
  @Override
  ClientCodecConfigurer clone();

  /**
   * Static factory method for a {@code ClientCodecConfigurer}.
   */
  static ClientCodecConfigurer create() {
    return CodecConfigurerFactory.create(ClientCodecConfigurer.class);
  }

  /**
   * {@link DefaultCodecs} extension with extra client-side options.
   */
  interface ClientDefaultCodecs extends DefaultCodecs {

    /**
     * Configure the {@code Decoder} to use for Server-Sent Events.
     * <p>By default if this is not set, and Jackson is available,
     * the {@link #jackson2JsonDecoder} override is used instead.
     * Use this method to customize the SSE decoder.
     * <p>Note that {@link #maxInMemorySize(int)}, if configured,
     * will be applied to the given decoder.
     *
     * @param decoder the decoder to use
     */
    void serverSentEventDecoder(Decoder<?> decoder);
  }

}
