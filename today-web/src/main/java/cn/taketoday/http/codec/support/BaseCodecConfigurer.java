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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.http.codec.CodecConfigurer;
import cn.taketoday.http.codec.DecoderHttpMessageReader;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.lang.Assert;

/**
 * Default implementation of {@link CodecConfigurer} that serves as a base for
 * client and server specific variants.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
abstract class BaseCodecConfigurer implements CodecConfigurer {

  protected final BaseDefaultCodecs defaultCodecs;
  protected final DefaultCustomCodecs customCodecs;

  /**
   * Constructor with the base {@link BaseDefaultCodecs} to use, which can be
   * a client or server specific variant.
   */
  BaseCodecConfigurer(BaseDefaultCodecs defaultCodecs) {
    Assert.notNull(defaultCodecs, "'defaultCodecs' is required");
    this.defaultCodecs = defaultCodecs;
    this.customCodecs = new DefaultCustomCodecs();
    this.defaultCodecs.setPartWritersSupplier(this::getWriters);
  }

  /**
   * Create a deep copy of the given {@link BaseCodecConfigurer}.
   */
  protected BaseCodecConfigurer(BaseCodecConfigurer other) {
    this.defaultCodecs = other.cloneDefaultCodecs();
    this.customCodecs = new DefaultCustomCodecs(other.customCodecs);
    this.defaultCodecs.setPartWritersSupplier(this::getWriters);

  }

  /**
   * Sub-classes should override this to create a deep copy of
   * {@link BaseDefaultCodecs} which can be client or server specific.
   */
  protected abstract BaseDefaultCodecs cloneDefaultCodecs();

  @Override
  public DefaultCodecs defaultCodecs() {
    return this.defaultCodecs;
  }

  @Override
  public void registerDefaults(boolean shouldRegister) {
    this.defaultCodecs.registerDefaults(shouldRegister);
  }

  @Override
  public CustomCodecs customCodecs() {
    return this.customCodecs;
  }

  @Override
  public List<HttpMessageReader<?>> getReaders() {
    this.defaultCodecs.applyDefaultConfig(this.customCodecs);

    ArrayList<HttpMessageReader<?>> result = new ArrayList<>();
    result.addAll(this.customCodecs.getTypedReaders().keySet());
    result.addAll(this.defaultCodecs.getTypedReaders());
    result.addAll(this.customCodecs.getObjectReaders().keySet());
    result.addAll(this.defaultCodecs.getObjectReaders());
    result.addAll(this.defaultCodecs.getCatchAllReaders());
    return result;
  }

  @Override
  public List<HttpMessageWriter<?>> getWriters() {
    this.defaultCodecs.applyDefaultConfig(this.customCodecs);

    ArrayList<HttpMessageWriter<?>> result = new ArrayList<>();
    result.addAll(this.customCodecs.getTypedWriters().keySet());
    result.addAll(this.defaultCodecs.getTypedWriters());
    result.addAll(this.customCodecs.getObjectWriters().keySet());
    result.addAll(this.defaultCodecs.getObjectWriters());
    result.addAll(this.defaultCodecs.getCatchAllWriters());
    return result;
  }

  @Override
  public abstract CodecConfigurer clone();

  /**
   * Default implementation of {@code CustomCodecs}.
   */
  protected static final class DefaultCustomCodecs implements CustomCodecs {

    private final LinkedHashMap<HttpMessageReader<?>, Boolean> typedReaders = new LinkedHashMap<>(4);
    private final LinkedHashMap<HttpMessageWriter<?>, Boolean> typedWriters = new LinkedHashMap<>(4);
    private final LinkedHashMap<HttpMessageReader<?>, Boolean> objectReaders = new LinkedHashMap<>(4);
    private final LinkedHashMap<HttpMessageWriter<?>, Boolean> objectWriters = new LinkedHashMap<>(4);
    private final ArrayList<Consumer<DefaultCodecConfig>> defaultConfigConsumers = new ArrayList<>(4);

    DefaultCustomCodecs() { }

    /**
     * Create a deep copy of the given {@link DefaultCustomCodecs}.
     */
    DefaultCustomCodecs(DefaultCustomCodecs other) {
      this.typedReaders.putAll(other.typedReaders);
      this.typedWriters.putAll(other.typedWriters);
      this.objectReaders.putAll(other.objectReaders);
      this.objectWriters.putAll(other.objectWriters);
    }

    @Override
    public void register(Object codec) {
      addCodec(codec, false);
    }

    @Override
    public void registerWithDefaultConfig(Object codec) {
      addCodec(codec, true);
    }

    @Override
    public void registerWithDefaultConfig(Object codec, Consumer<DefaultCodecConfig> configConsumer) {
      addCodec(codec, false);
      this.defaultConfigConsumers.add(configConsumer);
    }

    @Override
    public void withDefaultCodecConfig(Consumer<DefaultCodecConfig> codecsConfigConsumer) {
      this.defaultConfigConsumers.add(codecsConfigConsumer);
    }

    private void addCodec(Object codec, boolean applyDefaultConfig) {
      if (codec instanceof Decoder) {
        codec = new DecoderHttpMessageReader<>((Decoder<?>) codec);
      }
      else if (codec instanceof Encoder) {
        codec = new EncoderHttpMessageWriter<>((Encoder<?>) codec);
      }

      if (codec instanceof HttpMessageReader<?> reader) {
        boolean canReadToObject = reader.canRead(ResolvableType.fromClass(Object.class), null);
        (canReadToObject ? this.objectReaders : this.typedReaders).put(reader, applyDefaultConfig);
      }
      else if (codec instanceof HttpMessageWriter<?> writer) {
        boolean canWriteObject = writer.canWrite(ResolvableType.fromClass(Object.class), null);
        (canWriteObject ? this.objectWriters : this.typedWriters).put(writer, applyDefaultConfig);
      }
      else {
        throw new IllegalArgumentException("Unexpected codec type: " + codec.getClass().getName());
      }
    }

    // Package private accessors...

    Map<HttpMessageReader<?>, Boolean> getTypedReaders() {
      return this.typedReaders;
    }

    Map<HttpMessageWriter<?>, Boolean> getTypedWriters() {
      return this.typedWriters;
    }

    Map<HttpMessageReader<?>, Boolean> getObjectReaders() {
      return this.objectReaders;
    }

    Map<HttpMessageWriter<?>, Boolean> getObjectWriters() {
      return this.objectWriters;
    }

    List<Consumer<DefaultCodecConfig>> getDefaultConfigConsumers() {
      return this.defaultConfigConsumers;
    }
  }

}
