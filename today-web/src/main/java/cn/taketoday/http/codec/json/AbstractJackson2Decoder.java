/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.codec.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.CodecException;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.codec.HttpMessageDecoder;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * Abstract base class for Jackson 2.9 decoding, leveraging non-blocking parsing.
 *
 * <p>Compatible with Jackson 2.9.7 and higher.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://github.com/FasterXML/jackson-core/issues/57" target="_blank">
 * Add support for non-blocking ("async") JSON parsing</a>
 * @since 4.0
 */
public abstract class AbstractJackson2Decoder
        extends Jackson2CodecSupport implements HttpMessageDecoder<Object> {

  private int maxInMemorySize = 256 * 1024;

  /**
   * Constructor with a Jackson {@link ObjectMapper} to use.
   */
  protected AbstractJackson2Decoder(ObjectMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
  }

  /**
   * Set the max number of bytes that can be buffered by this decoder. This
   * is either the size of the entire input when decoding as a whole, or the
   * size of one top-level JSON object within a JSON stream. When the limit
   * is exceeded, {@link DataBufferLimitException} is raised.
   * <p>By default this is set to 256K.
   *
   * @param byteCount the max number of bytes to buffer, or -1 for unlimited
   */
  public void setMaxInMemorySize(int byteCount) {
    this.maxInMemorySize = byteCount;
  }

  /**
   * Return the {@link #setMaxInMemorySize configured} byte count limit.
   */
  public int getMaxInMemorySize() {
    return this.maxInMemorySize;
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    // Skip String: CharSequenceDecoder + "*/*" comes after
    if (CharSequence.class.isAssignableFrom(elementType.toClass()) || notSupportsMimeType(mimeType)) {
      return false;
    }

    ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
    if (mapper == null) {
      return false;
    }

    JavaType javaType = mapper.constructType(elementType.getType());
    if (!logger.isDebugEnabled()) {
      return mapper.canDeserialize(javaType);
    }
    else {
      AtomicReference<Throwable> causeRef = new AtomicReference<>();
      if (mapper.canDeserialize(javaType, causeRef)) {
        return true;
      }
      logWarningIfNecessary(javaType, causeRef.get());
      return false;
    }
  }

  @Override
  public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
    if (mapper == null) {
      return Flux.error(new IllegalStateException("No ObjectMapper for " + elementType));
    }

    boolean forceUseOfBigDecimal = mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    if (BigDecimal.class.equals(elementType.getType())) {
      forceUseOfBigDecimal = true;
    }

    Flux<DataBuffer> processed = processInput(input, elementType, mimeType, hints);
    Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(
            processed, mapper.getFactory(), mapper, true, forceUseOfBigDecimal, getMaxInMemorySize());

    return Flux.deferContextual(contextView -> {
      Map<String, Object> hintsToUse = contextView.isEmpty() ? hints :
                                       Hints.merge(hints, ContextView.class.getName(), contextView);

      ObjectReader reader = createObjectReader(mapper, elementType, hintsToUse);

      return tokens.handle((tokenBuffer, sink) -> {
        try {
          Object value = reader.readValue(tokenBuffer.asParser(mapper));
          logValue(value, hints);
          if (value != null) {
            sink.next(value);
          }
        }
        catch (IOException ex) {
          sink.error(processException(ex));
        }
      });
    });
  }

  /**
   * Process the input publisher into a flux. Default implementation returns
   * {@link Flux#from(Publisher)}, but subclasses can choose to customize
   * this behavior.
   *
   * @param input the {@code DataBuffer} input stream to process
   * @param elementType the expected type of elements in the output stream
   * @param mimeType the MIME type associated with the input stream (optional)
   * @param hints additional information about how to do encode
   * @return the processed flux
   */
  protected Flux<DataBuffer> processInput(Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.from(input);
  }

  @Override
  public Mono<Object> decodeToMono(
          Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Mono.deferContextual(contextView -> {

      Map<String, Object> hintsToUse =
              contextView.isEmpty() ? hints :
              Hints.merge(hints, ContextView.class.getName(), contextView);

      return DataBufferUtils.join(input, this.maxInMemorySize).flatMap(dataBuffer ->
              Mono.justOrEmpty(decode(dataBuffer, elementType, mimeType, hintsToUse)));
    });
  }

  @Override
  public Object decode(DataBuffer dataBuffer, ResolvableType targetType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {
    ObjectMapper mapper = selectObjectMapper(targetType, mimeType);
    if (mapper == null) {
      throw new IllegalStateException("No ObjectMapper for " + targetType);
    }
    try {
      ObjectReader objectReader = createObjectReader(mapper, targetType, hints);
      Object value = objectReader.readValue(dataBuffer.asInputStream());
      logValue(value, hints);
      return value;
    }
    catch (IOException ex) {
      throw processException(ex);
    }
    finally {
      DataBufferUtils.release(dataBuffer);
    }
  }

  private ObjectReader createObjectReader(ObjectMapper mapper,
          ResolvableType elementType, @Nullable Map<String, Object> hints) {
    Assert.notNull(elementType, "'elementType' must not be null");
    Class<?> contextClass = getContextClass(elementType);
    if (contextClass == null && hints != null) {
      contextClass = getContextClass((ResolvableType) hints.get(ACTUAL_TYPE_HINT));
    }
    JavaType javaType = getJavaType(elementType.getType(), contextClass);
    Class<?> jsonView = (hints != null ? (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT) : null);

    ObjectReader objectReader = jsonView != null ?
                                mapper.readerWithView(jsonView).forType(javaType) :
                                mapper.readerFor(javaType);

    return customizeReader(objectReader, elementType, hints);
  }

  /**
   * Subclasses can use this method to customize {@link ObjectReader} used
   * for reading values.
   *
   * @param reader the reader instance to customize
   * @param elementType the target type of element values to read to
   * @param hints a map with serialization hints;
   * the Reactor Context, when available, may be accessed under the key
   * {@code ContextView.class.getName()}
   * @return the customized {@code ObjectReader} to use
   */
  protected ObjectReader customizeReader(ObjectReader reader,
          ResolvableType elementType, @Nullable Map<String, Object> hints) {
    return reader;
  }

  @Nullable
  private Class<?> getContextClass(@Nullable ResolvableType elementType) {
    MethodParameter param = elementType != null ? getParameter(elementType) : null;
    return param != null ? param.getContainingClass() : null;
  }

  private void logValue(@Nullable Object value, @Nullable Map<String, Object> hints) {
    if (!Hints.isLoggingSuppressed(hints)) {
      LogFormatUtils.traceDebug(logger, traceOn -> {
        String formatted = LogFormatUtils.formatValue(value, !traceOn);
        return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
      });
    }
  }

  private CodecException processException(IOException ex) {
    if (ex instanceof InvalidDefinitionException) {
      JavaType type = ((InvalidDefinitionException) ex).getType();
      return new CodecException("Type definition error: " + type, ex);
    }
    if (ex instanceof JsonProcessingException) {
      String originalMessage = ((JsonProcessingException) ex).getOriginalMessage();
      return new DecodingException("JSON decoding error: " + originalMessage, ex);
    }
    return new DecodingException("I/O error while parsing input stream", ex);
  }

  // HttpMessageDecoder

  @Override
  public Map<String, Object> getDecodeHints(ResolvableType actualType,
          ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {
    return getHints(actualType);
  }

  @Override
  public List<MimeType> getDecodableMimeTypes() {
    return getMimeTypes();
  }

  @Override
  public List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
    return getMimeTypes(targetType);
  }

  // Jackson2CodecSupport

  @Override
  protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
    return parameter.getMethodAnnotation(annotType);
  }

}
