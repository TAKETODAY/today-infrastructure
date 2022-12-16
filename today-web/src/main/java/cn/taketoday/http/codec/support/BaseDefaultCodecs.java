/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.codec.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.core.codec.AbstractDataBufferDecoder;
import cn.taketoday.core.codec.ByteArrayDecoder;
import cn.taketoday.core.codec.ByteArrayEncoder;
import cn.taketoday.core.codec.ByteBufferDecoder;
import cn.taketoday.core.codec.ByteBufferEncoder;
import cn.taketoday.core.codec.CharSequenceEncoder;
import cn.taketoday.core.codec.DataBufferDecoder;
import cn.taketoday.core.codec.DataBufferEncoder;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.core.codec.Netty5BufferDecoder;
import cn.taketoday.core.codec.Netty5BufferEncoder;
import cn.taketoday.core.codec.NettyByteBufDecoder;
import cn.taketoday.core.codec.NettyByteBufEncoder;
import cn.taketoday.core.codec.ResourceDecoder;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.http.codec.CodecConfigurer;
import cn.taketoday.http.codec.DecoderHttpMessageReader;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.FormHttpMessageReader;
import cn.taketoday.http.codec.FormHttpMessageWriter;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.http.codec.ResourceHttpMessageReader;
import cn.taketoday.http.codec.ResourceHttpMessageWriter;
import cn.taketoday.http.codec.ServerSentEventHttpMessageReader;
import cn.taketoday.http.codec.ServerSentEventHttpMessageWriter;
import cn.taketoday.http.codec.json.AbstractJackson2Decoder;
import cn.taketoday.http.codec.json.Jackson2JsonDecoder;
import cn.taketoday.http.codec.json.Jackson2JsonEncoder;
import cn.taketoday.http.codec.json.Jackson2SmileDecoder;
import cn.taketoday.http.codec.json.Jackson2SmileEncoder;
import cn.taketoday.http.codec.multipart.DefaultPartHttpMessageReader;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageReader;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageWriter;
import cn.taketoday.http.codec.multipart.PartEventHttpMessageReader;
import cn.taketoday.http.codec.multipart.PartEventHttpMessageWriter;
import cn.taketoday.http.codec.multipart.PartHttpMessageWriter;
import cn.taketoday.http.codec.protobuf.ProtobufDecoder;
import cn.taketoday.http.codec.protobuf.ProtobufEncoder;
import cn.taketoday.http.codec.protobuf.ProtobufHttpMessageWriter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Default implementation of {@link CodecConfigurer.DefaultCodecs} that serves
 * as a base for client and server specific variants.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class BaseDefaultCodecs implements CodecConfigurer.DefaultCodecs, CodecConfigurer.DefaultCodecConfig {

  static final boolean jackson2Present;
  static final boolean nettyByteBufPresent;
  static final boolean synchronossMultipartPresent;

  private static final boolean protobufPresent;
  private static final boolean jackson2SmilePresent;
  static final boolean netty5BufferPresent;

  static {
    ClassLoader classLoader = BaseCodecConfigurer.class.getClassLoader();
    jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader)
            && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
    jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
    protobufPresent = ClassUtils.isPresent("com.google.protobuf.Message", classLoader);
    synchronossMultipartPresent = ClassUtils.isPresent("org.synchronoss.cloud.nio.multipart.NioMultipartParser", classLoader);
    nettyByteBufPresent = ClassUtils.isPresent("io.netty.buffer.ByteBuf", classLoader);
    netty5BufferPresent = ClassUtils.isPresent("io.netty5.buffer.api.Buffer", classLoader);
  }

  @Nullable
  private Decoder<?> jackson2JsonDecoder;

  @Nullable
  private Encoder<?> jackson2JsonEncoder;

  @Nullable
  private Encoder<?> jackson2SmileEncoder;

  @Nullable
  private Decoder<?> jackson2SmileDecoder;

  @Nullable
  private Decoder<?> protobufDecoder;

  @Nullable
  private Encoder<?> protobufEncoder;

  @Nullable
  private DefaultMultipartCodecs multipartCodecs;

  @Nullable
  private Supplier<List<HttpMessageWriter<?>>> partWritersSupplier;

  @Nullable
  private HttpMessageReader<?> multipartReader;

  @Nullable
  private Consumer<Object> codecConsumer;

  @Nullable
  private Integer maxInMemorySize;

  @Nullable
  private Boolean enableLoggingRequestDetails;

  private boolean registerDefaults = true;

  // The default reader and writer instances to use

  private final ArrayList<HttpMessageReader<?>> typedReaders = new ArrayList<>();
  private final ArrayList<HttpMessageReader<?>> objectReaders = new ArrayList<>();
  private final ArrayList<HttpMessageWriter<?>> typedWriters = new ArrayList<>();
  private final ArrayList<HttpMessageWriter<?>> objectWriters = new ArrayList<>();

  BaseDefaultCodecs() {
    initReaders();
    initWriters();
  }

  /**
   * Reset and initialize typed readers and object readers.
   */
  protected void initReaders() {
    initTypedReaders();
    initObjectReaders();
  }

  /**
   * Reset and initialize typed writers and object writers.
   */
  protected void initWriters() {
    initTypedWriters();
    initObjectWriters();
  }

  /**
   * Create a deep copy of the given {@link BaseDefaultCodecs}.
   */
  protected BaseDefaultCodecs(BaseDefaultCodecs other) {
    this.jackson2JsonDecoder = other.jackson2JsonDecoder;
    this.jackson2JsonEncoder = other.jackson2JsonEncoder;
    this.jackson2SmileDecoder = other.jackson2SmileDecoder;
    this.jackson2SmileEncoder = other.jackson2SmileEncoder;
    this.protobufDecoder = other.protobufDecoder;
    this.protobufEncoder = other.protobufEncoder;
    this.multipartCodecs = other.multipartCodecs != null ?
                           new DefaultMultipartCodecs(other.multipartCodecs) : null;
    this.multipartReader = other.multipartReader;
    this.codecConsumer = other.codecConsumer;
    this.maxInMemorySize = other.maxInMemorySize;
    this.enableLoggingRequestDetails = other.enableLoggingRequestDetails;
    this.registerDefaults = other.registerDefaults;
    this.typedReaders.addAll(other.typedReaders);
    this.objectReaders.addAll(other.objectReaders);
    this.typedWriters.addAll(other.typedWriters);
    this.objectWriters.addAll(other.objectWriters);
  }

  @Override
  public void jackson2JsonDecoder(Decoder<?> decoder) {
    this.jackson2JsonDecoder = decoder;
    initObjectReaders();
  }

  @Override
  public void jackson2JsonEncoder(Encoder<?> encoder) {
    this.jackson2JsonEncoder = encoder;
    initObjectWriters();
    initTypedWriters();
  }

  @Override
  public void jackson2SmileDecoder(Decoder<?> decoder) {
    this.jackson2SmileDecoder = decoder;
    initObjectReaders();
  }

  @Override
  public void jackson2SmileEncoder(Encoder<?> encoder) {
    this.jackson2SmileEncoder = encoder;
    initObjectWriters();
    initTypedWriters();
  }

  @Override
  public void protobufDecoder(Decoder<?> decoder) {
    this.protobufDecoder = decoder;
    initTypedReaders();
  }

  @Override
  public void protobufEncoder(Encoder<?> encoder) {
    this.protobufEncoder = encoder;
    initTypedWriters();
  }

  @Override
  public void configureDefaultCodec(Consumer<Object> codecConsumer) {
    this.codecConsumer = (this.codecConsumer != null ?
                          this.codecConsumer.andThen(codecConsumer) : codecConsumer);
    initReaders();
    initWriters();
  }

  @Override
  public void maxInMemorySize(int byteCount) {
    if (!ObjectUtils.nullSafeEquals(this.maxInMemorySize, byteCount)) {
      this.maxInMemorySize = byteCount;
      initReaders();
    }
  }

  @Override
  @Nullable
  public Integer maxInMemorySize() {
    return this.maxInMemorySize;
  }

  @Override
  public void enableLoggingRequestDetails(boolean enable) {
    if (!ObjectUtils.nullSafeEquals(this.enableLoggingRequestDetails, enable)) {
      this.enableLoggingRequestDetails = enable;
      initReaders();
      initWriters();
    }
  }

  @Override
  public CodecConfigurer.MultipartCodecs multipartCodecs() {
    if (this.multipartCodecs == null) {
      this.multipartCodecs = new DefaultMultipartCodecs();
    }
    return this.multipartCodecs;
  }

  @Override
  public void multipartReader(HttpMessageReader<?> multipartReader) {
    this.multipartReader = multipartReader;
    initTypedReaders();
  }

  /**
   * Set a supplier for part writers to use when
   * {@link #multipartCodecs()} are not explicitly configured.
   * That's the same set of writers as for general except for the multipart
   * writer itself.
   */
  void setPartWritersSupplier(Supplier<List<HttpMessageWriter<?>>> supplier) {
    this.partWritersSupplier = supplier;
    initTypedWriters();
  }

  @Override
  @Nullable
  public Boolean isEnableLoggingRequestDetails() {
    return this.enableLoggingRequestDetails;
  }

  /**
   * Delegate method used from {@link BaseCodecConfigurer#registerDefaults}.
   */
  void registerDefaults(boolean registerDefaults) {
    if (this.registerDefaults != registerDefaults) {
      this.registerDefaults = registerDefaults;
      initReaders();
      initWriters();
    }
  }

  /**
   * Return readers that support specific types.
   */
  final List<HttpMessageReader<?>> getTypedReaders() {
    return this.typedReaders;
  }

  /**
   * Reset and initialize typed readers.
   */
  protected void initTypedReaders() {
    this.typedReaders.clear();
    if (!this.registerDefaults) {
      return;
    }
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new DataBufferDecoder()));
    if (nettyByteBufPresent) {
      addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new NettyByteBufDecoder()));
    }
    if (netty5BufferPresent) {
      addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new Netty5BufferDecoder()));
    }
    addCodec(this.typedReaders, new ResourceHttpMessageReader(new ResourceDecoder()));
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
    if (protobufPresent) {
      addCodec(this.typedReaders, new DecoderHttpMessageReader<>(
              this.protobufDecoder != null ?
              (ProtobufDecoder) this.protobufDecoder : new ProtobufDecoder()));
    }
    addCodec(this.typedReaders, new FormHttpMessageReader());

    if (this.multipartReader != null) {
      addCodec(this.typedReaders, this.multipartReader);
    }
    else {
      DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
      addCodec(this.typedReaders, partReader);
      addCodec(this.typedReaders, new MultipartHttpMessageReader(partReader));
    }
    addCodec(this.typedReaders, new PartEventHttpMessageReader());

    // client vs server..
    extendTypedReaders(this.typedReaders);
  }

  /**
   * Initialize a codec and add it to the List.
   */
  protected <T> void addCodec(List<T> codecs, T codec) {
    initCodec(codec);
    codecs.add(codec);
  }

  /**
   * Apply {@link #maxInMemorySize()} and {@link #enableLoggingRequestDetails},
   * if configured by the application, to the given codec , including any
   * codec it contains.
   */
  @SuppressWarnings("rawtypes")
  private void initCodec(@Nullable Object codec) {
    if (codec instanceof DecoderHttpMessageReader) {
      codec = ((DecoderHttpMessageReader) codec).getDecoder();
    }
    else if (codec instanceof EncoderHttpMessageWriter) {
      codec = ((EncoderHttpMessageWriter<?>) codec).getEncoder();
    }

    if (codec == null) {
      return;
    }

    Integer size = this.maxInMemorySize;
    if (size != null) {
      if (codec instanceof AbstractDataBufferDecoder) {
        ((AbstractDataBufferDecoder<?>) codec).setMaxInMemorySize(size);
      }
      if (protobufPresent) {
        if (codec instanceof ProtobufDecoder) {
          ((ProtobufDecoder) codec).setMaxMessageSize(size);
        }
      }
      if (jackson2Present) {
        if (codec instanceof AbstractJackson2Decoder) {
          ((AbstractJackson2Decoder) codec).setMaxInMemorySize(size);
        }
      }
//      if (jaxb2Present && !shouldIgnoreXml) {
//        if (codec instanceof Jaxb2XmlDecoder) {
//          ((Jaxb2XmlDecoder) codec).setMaxInMemorySize(size);
//        }
//      }
      if (codec instanceof FormHttpMessageReader) {
        ((FormHttpMessageReader) codec).setMaxInMemorySize(size);
      }
      if (codec instanceof ServerSentEventHttpMessageReader) {
        ((ServerSentEventHttpMessageReader) codec).setMaxInMemorySize(size);
      }
      if (codec instanceof DefaultPartHttpMessageReader) {
        ((DefaultPartHttpMessageReader) codec).setMaxInMemorySize(size);
      }
      if (codec instanceof PartEventHttpMessageReader) {
        ((PartEventHttpMessageReader) codec).setMaxInMemorySize(size);
      }
    }

    Boolean enable = this.enableLoggingRequestDetails;
    if (enable != null) {
      if (codec instanceof FormHttpMessageReader) {
        ((FormHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof MultipartHttpMessageReader) {
        ((MultipartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof DefaultPartHttpMessageReader) {
        ((DefaultPartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof PartEventHttpMessageReader) {
        ((PartEventHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof FormHttpMessageWriter) {
        ((FormHttpMessageWriter) codec).setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof MultipartHttpMessageWriter) {
        ((MultipartHttpMessageWriter) codec).setEnableLoggingRequestDetails(enable);
      }
    }

    if (this.codecConsumer != null) {
      this.codecConsumer.accept(codec);
    }

    // Recurse for nested codecs
    if (codec instanceof MultipartHttpMessageReader) {
      initCodec(((MultipartHttpMessageReader) codec).getPartReader());
    }
    else if (codec instanceof MultipartHttpMessageWriter) {
      initCodec(((MultipartHttpMessageWriter) codec).getFormWriter());
    }
    else if (codec instanceof ServerSentEventHttpMessageReader) {
      initCodec(((ServerSentEventHttpMessageReader) codec).getDecoder());
    }
    else if (codec instanceof ServerSentEventHttpMessageWriter) {
      initCodec(((ServerSentEventHttpMessageWriter) codec).getEncoder());
    }
  }

  /**
   * Hook for client or server specific typed readers.
   */
  protected void extendTypedReaders(List<HttpMessageReader<?>> typedReaders) { }

  /**
   * Return Object readers (JSON, XML, SSE).
   */
  final List<HttpMessageReader<?>> getObjectReaders() {
    return this.objectReaders;
  }

  /**
   * Reset and initialize object readers.
   */
  protected void initObjectReaders() {
    this.objectReaders.clear();
    if (!this.registerDefaults) {
      return;
    }
    if (jackson2Present) {
      addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJackson2JsonDecoder()));
    }
    if (jackson2SmilePresent) {
      addCodec(this.objectReaders, new DecoderHttpMessageReader<>(
              this.jackson2SmileDecoder != null ?
              (Jackson2SmileDecoder) this.jackson2SmileDecoder : new Jackson2SmileDecoder()));
    }
//    if (jaxb2Present && !shouldIgnoreXml) {
//      addCodec(this.objectReaders, new DecoderHttpMessageReader<>(this.jaxb2Decoder != null ?
//                                                                  (Jaxb2XmlDecoder) this.jaxb2Decoder : new Jaxb2XmlDecoder()));
//    }

    // client vs server..
    extendObjectReaders(this.objectReaders);
  }

  /**
   * Hook for client or server specific Object readers.
   */
  protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {
  }

  /**
   * Return readers that need to be at the end, after all others.
   */
  final List<HttpMessageReader<?>> getCatchAllReaders() {
    if (!this.registerDefaults) {
      return Collections.emptyList();
    }
    List<HttpMessageReader<?>> readers = new ArrayList<>();
    addCodec(readers, new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    return readers;
  }

  /**
   * Return all writers that support specific types.
   */
  final List<HttpMessageWriter<?>> getTypedWriters() {
    return this.typedWriters;
  }

  /**
   * Reset and initialize typed writers.
   *
   * @since 4.0
   */
  protected void initTypedWriters() {
    this.typedWriters.clear();
    if (!this.registerDefaults) {
      return;
    }
    this.typedWriters.addAll(getBaseTypedWriters());
    extendTypedWriters(this.typedWriters);
  }

  /**
   * Return "base" typed writers only, i.e. common to client and server.
   */
  final List<HttpMessageWriter<?>> getBaseTypedWriters() {
    if (!this.registerDefaults) {
      return Collections.emptyList();
    }
    List<HttpMessageWriter<?>> writers = new ArrayList<>();
    addCodec(writers, new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
    addCodec(writers, new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
    addCodec(writers, new EncoderHttpMessageWriter<>(new DataBufferEncoder()));
    if (nettyByteBufPresent) {
      addCodec(writers, new EncoderHttpMessageWriter<>(new NettyByteBufEncoder()));
    }
    if (netty5BufferPresent) {
      addCodec(writers, new EncoderHttpMessageWriter<>(new Netty5BufferEncoder()));
    }
    addCodec(writers, new ResourceHttpMessageWriter());
    addCodec(writers, new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
    if (protobufPresent) {
      addCodec(writers, new ProtobufHttpMessageWriter(
              this.protobufEncoder != null ?
              (ProtobufEncoder) this.protobufEncoder : new ProtobufEncoder()));
    }

    addCodec(writers, new MultipartHttpMessageWriter(this::getPartWriters, new FormHttpMessageWriter()));
    addCodec(writers, new PartEventHttpMessageWriter());
    addCodec(writers, new PartHttpMessageWriter());
    return writers;
  }

  private List<HttpMessageWriter<?>> getPartWriters() {
    if (this.multipartCodecs != null) {
      return this.multipartCodecs.getWriters();
    }
    else if (this.partWritersSupplier != null) {
      return this.partWritersSupplier.get();
    }
    else {
      return Collections.emptyList();
    }
  }

  /**
   * Hook for client or server specific typed writers.
   */
  protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) { }

  /**
   * Return Object writers (JSON, XML, SSE).
   */
  final List<HttpMessageWriter<?>> getObjectWriters() {
    return this.objectWriters;
  }

  /**
   * Reset and initialize object writers.
   */
  protected void initObjectWriters() {
    this.objectWriters.clear();
    if (!this.registerDefaults) {
      return;
    }
    this.objectWriters.addAll(getBaseObjectWriters());
    extendObjectWriters(this.objectWriters);
  }

  /**
   * Return "base" object writers only, i.e. common to client and server.
   */
  final List<HttpMessageWriter<?>> getBaseObjectWriters() {
    List<HttpMessageWriter<?>> writers = new ArrayList<>();
    if (jackson2Present) {
      addCodec(writers, new EncoderHttpMessageWriter<>(getJackson2JsonEncoder()));
    }
    if (jackson2SmilePresent) {
      addCodec(writers, new EncoderHttpMessageWriter<>(
              this.jackson2SmileEncoder != null ?
              (Jackson2SmileEncoder) this.jackson2SmileEncoder : new Jackson2SmileEncoder()));
    }
//    if (jaxb2Present && !shouldIgnoreXml) {
//      addCodec(writers, new EncoderHttpMessageWriter<>(this.jaxb2Encoder != null ?
//                                                       (Jaxb2XmlEncoder) this.jaxb2Encoder : new Jaxb2XmlEncoder()));
//    }
    return writers;
  }

  /**
   * Hook for client or server specific Object writers.
   */
  protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
  }

  /**
   * Return writers that need to be at the end, after all others.
   */
  List<HttpMessageWriter<?>> getCatchAllWriters() {
    if (!this.registerDefaults) {
      return Collections.emptyList();
    }
    List<HttpMessageWriter<?>> result = new ArrayList<>();
    result.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
    return result;
  }

  void applyDefaultConfig(BaseCodecConfigurer.DefaultCustomCodecs customCodecs) {
    applyDefaultConfig(customCodecs.getTypedReaders());
    applyDefaultConfig(customCodecs.getObjectReaders());
    applyDefaultConfig(customCodecs.getTypedWriters());
    applyDefaultConfig(customCodecs.getObjectWriters());
    customCodecs.getDefaultConfigConsumers().forEach(consumer -> consumer.accept(this));
  }

  private void applyDefaultConfig(Map<?, Boolean> readers) {
    readers.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .forEach(this::initCodec);
  }

  // Accessors for use in subclasses...

  protected Decoder<?> getJackson2JsonDecoder() {
    if (this.jackson2JsonDecoder == null) {
      this.jackson2JsonDecoder = new Jackson2JsonDecoder();
    }
    return this.jackson2JsonDecoder;
  }

  protected Encoder<?> getJackson2JsonEncoder() {
    if (this.jackson2JsonEncoder == null) {
      this.jackson2JsonEncoder = new Jackson2JsonEncoder();
    }
    return this.jackson2JsonEncoder;
  }

  /**
   * Default implementation of {@link CodecConfigurer.MultipartCodecs}.
   */
  protected class DefaultMultipartCodecs implements CodecConfigurer.MultipartCodecs {

    private final List<HttpMessageWriter<?>> writers = new ArrayList<>();

    DefaultMultipartCodecs() { }

    DefaultMultipartCodecs(DefaultMultipartCodecs other) {
      this.writers.addAll(other.writers);
    }

    @Override
    public CodecConfigurer.MultipartCodecs encoder(Encoder<?> encoder) {
      writer(new EncoderHttpMessageWriter<>(encoder));
      initTypedWriters();
      return this;
    }

    @Override
    public CodecConfigurer.MultipartCodecs writer(HttpMessageWriter<?> writer) {
      this.writers.add(writer);
      initTypedWriters();
      return this;
    }

    List<HttpMessageWriter<?>> getWriters() {
      return this.writers;
    }
  }

}
