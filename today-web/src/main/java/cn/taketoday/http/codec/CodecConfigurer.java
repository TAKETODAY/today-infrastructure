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

import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageWriter;
import cn.taketoday.lang.Nullable;

/**
 * Defines a common interface for configuring either client or server HTTP
 * message readers and writers. This is used as follows:
 * <ul>
 * <li>Use {@link ClientCodecConfigurer#create()} or
 * {@link ServerCodecConfigurer#create()} to create an instance.
 * <li>Use {@link #defaultCodecs()} to customize HTTP message readers or writers
 * registered by default.
 * <li>Use {@link #customCodecs()} to add custom HTTP message readers or writers.
 * <li>Use {@link #getReaders()} and {@link #getWriters()} to obtain the list of
 * configured HTTP message readers and writers.
 * </ul>
 *
 * <p>HTTP message readers and writers are divided into 3 categories that are
 * ordered as follows:
 * <ol>
 * <li>Typed readers and writers that support specific types, e.g. byte[], String.
 * <li>Object readers and writers, e.g. JSON, XML.
 * <li>Catch-all readers or writers, e.g. String with any media type.
 * </ol>
 *
 * <p>Typed and object readers are further sub-divided and ordered as follows:
 * <ol>
 * <li>Default HTTP reader and writer registrations.
 * <li>Custom readers and writers.
 * </ol>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface CodecConfigurer {

  /**
   * Provides a way to customize or replace HTTP message readers and writers
   * registered by default.
   *
   * @see #registerDefaults(boolean)
   */
  DefaultCodecs defaultCodecs();

  /**
   * Register custom HTTP message readers or writers in addition to the ones
   * registered by default.
   */
  CustomCodecs customCodecs();

  /**
   * Provides a way to completely turn off registration of default HTTP message
   * readers and writers, and instead rely only on the ones provided via
   * {@link #customCodecs()}.
   * <p>By default this is set to {@code "true"} in which case default
   * registrations are made; setting this to {@code false} disables default
   * registrations.
   */
  void registerDefaults(boolean registerDefaults);

  /**
   * Obtain the configured HTTP message readers.
   */
  List<HttpMessageReader<?>> getReaders();

  /**
   * Obtain the configured HTTP message writers.
   */
  List<HttpMessageWriter<?>> getWriters();

  /**
   * Create a copy of this {@link CodecConfigurer}. The returned clone has its
   * own lists of default and custom codecs and generally can be configured
   * independently. Keep in mind however that codec instances (if any are
   * configured) are themselves not cloned.
   */
  CodecConfigurer clone();

  /**
   * Customize or replace the HTTP message readers and writers registered by
   * default. The options are further extended by
   * {@link ClientCodecConfigurer.ClientDefaultCodecs ClientDefaultCodecs} and
   * {@link ServerCodecConfigurer.ServerDefaultCodecs ServerDefaultCodecs}.
   */
  interface DefaultCodecs {

    /**
     * Override the default Jackson JSON {@code Decoder}.
     * <p>Note that {@link #maxInMemorySize(int)}, if configured, will be
     * applied to the given decoder.
     *
     * @param decoder the decoder instance to use
     * @see cn.taketoday.http.codec.json.Jackson2JsonDecoder
     */
    void jackson2JsonDecoder(Decoder<?> decoder);

    /**
     * Override the default Jackson JSON {@code Encoder}.
     *
     * @param encoder the encoder instance to use
     * @see cn.taketoday.http.codec.json.Jackson2JsonEncoder
     */
    void jackson2JsonEncoder(Encoder<?> encoder);

    /**
     * Override the default Jackson Smile {@code Decoder}.
     * <p>Note that {@link #maxInMemorySize(int)}, if configured, will be
     * applied to the given decoder.
     *
     * @param decoder the decoder instance to use
     * @see cn.taketoday.http.codec.json.Jackson2SmileDecoder
     */
    void jackson2SmileDecoder(Decoder<?> decoder);

    /**
     * Override the default Jackson Smile {@code Encoder}.
     *
     * @param encoder the encoder instance to use
     * @see cn.taketoday.http.codec.json.Jackson2SmileEncoder
     */
    void jackson2SmileEncoder(Encoder<?> encoder);

    /**
     * Override the default Protobuf {@code Decoder}.
     * <p>Note that {@link #maxInMemorySize(int)}, if configured, will be
     * applied to the given decoder.
     *
     * @param decoder the decoder instance to use
     * @see cn.taketoday.http.codec.protobuf.ProtobufDecoder
     */
    void protobufDecoder(Decoder<?> decoder);

    /**
     * Override the default Protobuf {@code Encoder}.
     *
     * @param encoder the encoder instance to use
     * @see cn.taketoday.http.codec.protobuf.ProtobufEncoder
     * @see cn.taketoday.http.codec.protobuf.ProtobufHttpMessageWriter
     */
    void protobufEncoder(Encoder<?> encoder);

    /**
     * Register a consumer to apply to default config instances. This can be
     * used to configure rather than replace a specific codec or multiple
     * codecs. The consumer is applied to every default {@link Encoder},
     * {@link Decoder}, {@link HttpMessageReader} and {@link HttpMessageWriter}
     * instance.
     *
     * @param codecConsumer the consumer to apply
     */
    void configureDefaultCodec(Consumer<Object> codecConsumer);

    /**
     * Configure a limit on the number of bytes that can be buffered whenever
     * the input stream needs to be aggregated. This can be a result of
     * decoding to a single {@code DataBuffer},
     * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
     * {@link cn.taketoday.core.io.Resource Resource}, {@code String}, etc.
     * It can also occur when splitting the input stream, e.g. delimited text,
     * in which case the limit applies to data buffered between delimiters.
     * <p>By default this is not set, in which case individual codec defaults
     * apply. All codecs are limited to 256K by default.
     *
     * @param byteCount the max number of bytes to buffer, or -1 for unlimited
     */
    void maxInMemorySize(int byteCount);

    /**
     * Whether to log form data at DEBUG level, and headers at TRACE level.
     * Both may contain sensitive information.
     * <p>By default set to {@code false} so that request details are not shown.
     *
     * @param enable whether to enable or not
     */
    void enableLoggingRequestDetails(boolean enable);

    /**
     * Configure encoders or writers for use with
     * {@link MultipartHttpMessageWriter
     * MultipartHttpMessageWriter}.
     */
    MultipartCodecs multipartCodecs();

    /**
     * Configure the {@code HttpMessageReader} to use for multipart requests.
     * <p>Note that {@link #maxInMemorySize(int)} and/or
     * {@link #enableLoggingRequestDetails(boolean)}, if configured, will be
     * applied to the given reader, if applicable.
     *
     * @param reader the message reader to use for multipart requests.
     */
    void multipartReader(HttpMessageReader<?> reader);
  }

  /**
   * Registry for custom HTTP message readers and writers.
   */
  interface CustomCodecs {

    /**
     * Register a custom codec. This is expected to be one of the following:
     * <ul>
     * <li>{@link HttpMessageReader}
     * <li>{@link HttpMessageWriter}
     * <li>{@link Encoder} (wrapped internally with {@link EncoderHttpMessageWriter})
     * <li>{@link Decoder} (wrapped internally with {@link DecoderHttpMessageReader})
     * </ul>
     *
     * @param codec the codec to register
     * @since 4.0
     */
    void register(Object codec);

    /**
     * Variant of {@link #register(Object)} that also applies the below
     * properties, if configured, via {@link #defaultCodecs()}:
     * <ul>
     * <li>{@link DefaultCodecs#maxInMemorySize(int) maxInMemorySize}
     * <li>{@link DefaultCodecs#enableLoggingRequestDetails(boolean) enableLoggingRequestDetails}
     * </ul>
     * <p>The properties are applied every time {@link #getReaders()} or
     * {@link #getWriters()} are used to obtain the list of configured
     * readers or writers.
     *
     * @param codec the codec to register and apply default config to
     */
    void registerWithDefaultConfig(Object codec);

    /**
     * Variant of {@link #register(Object)} that also allows the caller to
     * apply the properties from {@link DefaultCodecConfig} to the given
     * codec. If you want to apply all the properties, prefer using
     * {@link #registerWithDefaultConfig(Object)}.
     * <p>The consumer is called every time {@link #getReaders()} or
     * {@link #getWriters()} are used to obtain the list of configured
     * readers or writers.
     *
     * @param codec the codec to register
     * @param configConsumer consumer of the default config
     */
    void registerWithDefaultConfig(Object codec, Consumer<DefaultCodecConfig> configConsumer);

    /**
     * Register a callback for the {@link DefaultCodecConfig configuration}
     * applied to default codecs. This allows custom codecs to follow general
     * guidelines applied to default ones, such as logging details and limiting
     * the amount of buffered data.
     *
     * @param codecsConfigConsumer the default codecs configuration callback
     */
    void withDefaultCodecConfig(Consumer<DefaultCodecConfig> codecsConfigConsumer);
  }

  /**
   * Exposes the values of properties configured through
   * {@link #defaultCodecs()} that are applied to default codecs.
   * The main purpose of this interface is to provide access to them so they
   * can also be applied to custom codecs if needed.
   *
   * @see CustomCodecs#registerWithDefaultConfig(Object, Consumer)
   * @since 4.0
   */
  interface DefaultCodecConfig {

    /**
     * Get the configured limit on the number of bytes that can be buffered whenever
     * the input stream needs to be aggregated.
     */
    @Nullable
    Integer maxInMemorySize();

    /**
     * Whether to log form data at DEBUG level, and headers at TRACE level.
     * Both may contain sensitive information.
     */
    @Nullable
    Boolean isEnableLoggingRequestDetails();
  }

  /**
   * Registry and container for multipart HTTP message writers.
   */
  interface MultipartCodecs {

    /**
     * Add a Part {@code Encoder}, internally wrapped with
     * {@link EncoderHttpMessageWriter}.
     *
     * @param encoder the encoder to add
     */
    MultipartCodecs encoder(Encoder<?> encoder);

    /**
     * Add a Part {@link HttpMessageWriter}. For writers of type
     * {@link EncoderHttpMessageWriter} consider using the shortcut
     * {@link #encoder(Encoder)} instead.
     *
     * @param writer the writer to add
     */
    MultipartCodecs writer(HttpMessageWriter<?> writer);
  }

}
