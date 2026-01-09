/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.codec.support;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.core.codec.AbstractDataBufferDecoder;
import infra.core.codec.ByteArrayDecoder;
import infra.core.codec.ByteArrayEncoder;
import infra.core.codec.ByteBufferDecoder;
import infra.core.codec.ByteBufferEncoder;
import infra.core.codec.CharSequenceEncoder;
import infra.core.codec.DataBufferDecoder;
import infra.core.codec.DataBufferEncoder;
import infra.core.codec.Decoder;
import infra.core.codec.Encoder;
import infra.core.codec.NettyByteBufDecoder;
import infra.core.codec.NettyByteBufEncoder;
import infra.core.codec.ResourceDecoder;
import infra.core.codec.StringDecoder;
import infra.http.codec.AbstractJacksonDecoder;
import infra.http.codec.CodecConfigurer;
import infra.http.codec.DecoderHttpMessageReader;
import infra.http.codec.EncoderHttpMessageWriter;
import infra.http.codec.FormHttpMessageReader;
import infra.http.codec.FormHttpMessageWriter;
import infra.http.codec.HttpMessageReader;
import infra.http.codec.HttpMessageWriter;
import infra.http.codec.ResourceHttpMessageReader;
import infra.http.codec.ResourceHttpMessageWriter;
import infra.http.codec.ServerSentEventHttpMessageReader;
import infra.http.codec.ServerSentEventHttpMessageWriter;
import infra.http.codec.cbor.JacksonCborDecoder;
import infra.http.codec.cbor.JacksonCborEncoder;
import infra.http.codec.json.JacksonJsonDecoder;
import infra.http.codec.json.JacksonJsonEncoder;
import infra.http.codec.multipart.DefaultPartHttpMessageReader;
import infra.http.codec.multipart.MultipartHttpMessageReader;
import infra.http.codec.multipart.MultipartHttpMessageWriter;
import infra.http.codec.multipart.PartEventHttpMessageReader;
import infra.http.codec.multipart.PartEventHttpMessageWriter;
import infra.http.codec.multipart.PartHttpMessageWriter;
import infra.http.codec.protobuf.ProtobufDecoder;
import infra.http.codec.protobuf.ProtobufEncoder;
import infra.http.codec.protobuf.ProtobufHttpMessageWriter;
import infra.http.codec.smile.JacksonSmileDecoder;
import infra.http.codec.smile.JacksonSmileEncoder;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

/**
 * Default implementation of {@link CodecConfigurer.DefaultCodecs} that serves
 * as a base for client and server specific variants.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class BaseDefaultCodecs implements CodecConfigurer.DefaultCodecs, CodecConfigurer.DefaultCodecConfig {

  static final boolean JACKSON_PRESENT;

  private static final boolean JACKSON_SMILE_PRESENT;

  private static final boolean JACKSON_CBOR_PRESENT;

  private static final boolean PROTOBUF_PRESENT;

  static final boolean NETTY_BYTE_BUF_PRESENT;

  static {
    ClassLoader classLoader = BaseCodecConfigurer.class.getClassLoader();
    JACKSON_PRESENT = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
    JACKSON_SMILE_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", classLoader);
    JACKSON_CBOR_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", classLoader);
    PROTOBUF_PRESENT = ClassUtils.isPresent("com.google.protobuf.Message", classLoader);
    NETTY_BYTE_BUF_PRESENT = ClassUtils.isPresent("io.netty.buffer.ByteBuf", classLoader);
  }

  private @Nullable Decoder<?> jacksonJsonDecoder;

  private @Nullable Encoder<?> jacksonJsonEncoder;

  private @Nullable Decoder<?> gsonDecoder;

  private @Nullable Encoder<?> gsonEncoder;

  private @Nullable Encoder<?> jacksonSmileEncoder;

  private @Nullable Decoder<?> jacksonSmileDecoder;

  private @Nullable Encoder<?> jacksonCborEncoder;

  private @Nullable Decoder<?> jacksonCborDecoder;

  private @Nullable Decoder<?> protobufDecoder;

  private @Nullable Encoder<?> protobufEncoder;

  private @Nullable DefaultMultipartCodecs multipartCodecs;

  private @Nullable Supplier<List<HttpMessageWriter<?>>> partWritersSupplier;

  private @Nullable HttpMessageReader<?> multipartReader;

  private @Nullable Consumer<Object> codecConsumer;

  private @Nullable Integer maxInMemorySize;

  private @Nullable Boolean enableLoggingRequestDetails;

  private boolean registerDefaults = true;

  // The default reader and writer instances to use

  private final List<HttpMessageReader<?>> typedReaders = new ArrayList<>();

  private final List<HttpMessageReader<?>> objectReaders = new ArrayList<>();

  private final List<HttpMessageWriter<?>> typedWriters = new ArrayList<>();

  private final List<HttpMessageWriter<?>> objectWriters = new ArrayList<>();

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
    this.jacksonJsonDecoder = other.jacksonJsonDecoder;
    this.jacksonJsonEncoder = other.jacksonJsonEncoder;
    this.gsonDecoder = other.gsonDecoder;
    this.gsonEncoder = other.gsonEncoder;
    this.jacksonSmileDecoder = other.jacksonSmileDecoder;
    this.jacksonSmileEncoder = other.jacksonSmileEncoder;
    this.jacksonCborDecoder = other.jacksonCborDecoder;
    this.jacksonCborEncoder = other.jacksonCborEncoder;
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
  public void jacksonJsonDecoder(Decoder<?> decoder) {
    this.jacksonJsonDecoder = decoder;
    initObjectReaders();
  }

  @Override
  public void jacksonJsonEncoder(Encoder<?> encoder) {
    this.jacksonJsonEncoder = encoder;
    initObjectWriters();
    initTypedWriters();
  }

  @Override
  public void jacksonSmileDecoder(Decoder<?> decoder) {
    this.jacksonSmileDecoder = decoder;
    initObjectReaders();
  }

  @Override
  public void jacksonSmileEncoder(Encoder<?> encoder) {
    this.jacksonSmileEncoder = encoder;
    initObjectWriters();
    initTypedWriters();
  }

  @Override
  public void jacksonCborDecoder(Decoder<?> decoder) {
    this.jacksonCborDecoder = decoder;
    initObjectReaders();
  }

  @Override
  public void jacksonCborEncoder(Encoder<?> encoder) {
    this.jacksonCborEncoder = encoder;
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
  public @Nullable Integer maxInMemorySize() {
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
  public @Nullable Boolean isEnableLoggingRequestDetails() {
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
    if (NETTY_BYTE_BUF_PRESENT) {
      addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new NettyByteBufDecoder()));
    }
    addCodec(this.typedReaders, new ResourceHttpMessageReader(new ResourceDecoder()));
    addCodec(this.typedReaders, new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
    if (PROTOBUF_PRESENT) {
      addCodec(this.typedReaders, new DecoderHttpMessageReader<>(this.protobufDecoder != null ?
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
  private void initCodec(@Nullable Object codec) {
    if (codec instanceof DecoderHttpMessageReader<?> decoderHttpMessageReader) {
      codec = decoderHttpMessageReader.getDecoder();
    }
    else if (codec instanceof EncoderHttpMessageWriter<?> encoderHttpMessageWriter) {
      codec = encoderHttpMessageWriter.getEncoder();
    }

    if (codec == null) {
      return;
    }

    Integer size = this.maxInMemorySize;
    if (size != null) {
      if (codec instanceof AbstractDataBufferDecoder<?> abstractDataBufferDecoder) {
        abstractDataBufferDecoder.setMaxInMemorySize(size);
      }
      // Pattern variables in the following if-blocks cannot be named the same as instance fields
      // due to lacking support in Checkstyle: https://github.com/checkstyle/checkstyle/issues/10969
      if (PROTOBUF_PRESENT) {
        if (codec instanceof ProtobufDecoder protobufDec) {
          protobufDec.setMaxMessageSize(size);
        }
      }
      if (JACKSON_PRESENT) {
        if (codec instanceof AbstractJacksonDecoder<?> abstractJacksonDecoder) {
          abstractJacksonDecoder.setMaxInMemorySize(size);
        }
      }
      if (codec instanceof FormHttpMessageReader formHttpMessageReader) {
        formHttpMessageReader.setMaxInMemorySize(size);
      }
      if (codec instanceof ServerSentEventHttpMessageReader serverSentEventHttpMessageReader) {
        serverSentEventHttpMessageReader.setMaxInMemorySize(size);
      }
      if (codec instanceof DefaultPartHttpMessageReader defaultPartHttpMessageReader) {
        defaultPartHttpMessageReader.setMaxInMemorySize(size);
      }
      if (codec instanceof PartEventHttpMessageReader partEventHttpMessageReader) {
        partEventHttpMessageReader.setMaxInMemorySize(size);
      }
    }

    Boolean enable = this.enableLoggingRequestDetails;
    if (enable != null) {
      if (codec instanceof FormHttpMessageReader formHttpMessageReader) {
        formHttpMessageReader.setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof MultipartHttpMessageReader multipartHttpMessageReader) {
        multipartHttpMessageReader.setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof DefaultPartHttpMessageReader defaultPartHttpMessageReader) {
        defaultPartHttpMessageReader.setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof PartEventHttpMessageReader partEventHttpMessageReader) {
        partEventHttpMessageReader.setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof FormHttpMessageWriter formHttpMessageWriter) {
        formHttpMessageWriter.setEnableLoggingRequestDetails(enable);
      }
      if (codec instanceof MultipartHttpMessageWriter multipartHttpMessageWriter) {
        multipartHttpMessageWriter.setEnableLoggingRequestDetails(enable);
      }
    }

    if (this.codecConsumer != null) {
      this.codecConsumer.accept(codec);
    }

    // Recurse for nested codecs
    if (codec instanceof MultipartHttpMessageReader multipartHttpMessageReader) {
      initCodec(multipartHttpMessageReader.getPartReader());
    }
    else if (codec instanceof MultipartHttpMessageWriter multipartHttpMessageWriter) {
      initCodec(multipartHttpMessageWriter.getFormWriter());
    }
    else if (codec instanceof ServerSentEventHttpMessageReader serverSentEventHttpMessageReader) {
      initCodec(serverSentEventHttpMessageReader.getDecoder());
    }
    else if (codec instanceof ServerSentEventHttpMessageWriter serverSentEventHttpMessageWriter) {
      initCodec(serverSentEventHttpMessageWriter.getEncoder());
    }
  }

  /**
   * Hook for client or server specific typed readers.
   */
  protected void extendTypedReaders(List<HttpMessageReader<?>> typedReaders) {
  }

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
    if (JACKSON_PRESENT) {
      addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJacksonJsonDecoder()));
    }
    if (JACKSON_SMILE_PRESENT) {
      addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJacksonSmileDecoder()));
    }
    if (JACKSON_CBOR_PRESENT) {
      addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJacksonCborDecoder()));
    }

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
    if (NETTY_BYTE_BUF_PRESENT) {
      addCodec(writers, new EncoderHttpMessageWriter<>(new NettyByteBufEncoder()));
    }
    addCodec(writers, new ResourceHttpMessageWriter());
    addCodec(writers, new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
    if (PROTOBUF_PRESENT) {
      addCodec(writers, new ProtobufHttpMessageWriter(this.protobufEncoder != null ?
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
  protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
  }

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
    if (JACKSON_PRESENT) {
      addCodec(writers, new EncoderHttpMessageWriter<>(getJacksonJsonEncoder()));
    }
    if (JACKSON_SMILE_PRESENT) {
      addCodec(writers, new EncoderHttpMessageWriter<>(getJacksonSmileEncoder()));
    }
    if (JACKSON_CBOR_PRESENT) {
      addCodec(writers, new EncoderHttpMessageWriter<>(getJacksonCborEncoder()));
    }
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

  protected Decoder<?> getJacksonJsonDecoder() {
    if (this.jacksonJsonDecoder == null) {
      if (JACKSON_PRESENT) {
        this.jacksonJsonDecoder = new JacksonJsonDecoder();
      }
      else {
        throw new IllegalStateException("Jackson not present");
      }
    }
    return this.jacksonJsonDecoder;
  }

  protected Encoder<?> getJacksonJsonEncoder() {
    if (this.jacksonJsonEncoder == null) {
      if (JACKSON_PRESENT) {
        this.jacksonJsonEncoder = new JacksonJsonEncoder();
      }
      else {
        throw new IllegalStateException("Jackson not present");
      }
    }
    return this.jacksonJsonEncoder;
  }

  protected Decoder<?> getJacksonSmileDecoder() {
    if (this.jacksonSmileDecoder == null) {
      if (JACKSON_SMILE_PRESENT) {
        this.jacksonSmileDecoder = new JacksonSmileDecoder();
      }
      else {
        throw new IllegalStateException("Jackson Smile support not present");
      }
    }
    return this.jacksonSmileDecoder;
  }

  protected Encoder<?> getJacksonSmileEncoder() {
    if (this.jacksonSmileEncoder == null) {
      if (JACKSON_SMILE_PRESENT) {
        this.jacksonSmileEncoder = new JacksonSmileEncoder();
      }
      else {
        throw new IllegalStateException("Jackson Smile support not present");
      }
    }
    return this.jacksonSmileEncoder;
  }

  protected Decoder<?> getJacksonCborDecoder() {
    if (this.jacksonCborDecoder == null) {
      if (JACKSON_CBOR_PRESENT) {
        this.jacksonCborDecoder = new JacksonCborDecoder();
      }
      else {
        throw new IllegalStateException("Jackson CBOR support not present");
      }
    }
    return this.jacksonCborDecoder;
  }

  protected Encoder<?> getJacksonCborEncoder() {
    if (this.jacksonCborEncoder == null) {
      if (JACKSON_CBOR_PRESENT) {
        this.jacksonCborEncoder = new JacksonCborEncoder();
      }
      else {
        throw new IllegalStateException("Jackson CBOR support not present");
      }
    }
    return this.jacksonCborEncoder;
  }

  /**
   * Default implementation of {@link CodecConfigurer.MultipartCodecs}.
   */
  protected class DefaultMultipartCodecs implements CodecConfigurer.MultipartCodecs {

    private final List<HttpMessageWriter<?>> writers = new ArrayList<>();

    DefaultMultipartCodecs() {
    }

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
