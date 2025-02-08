/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.codec.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.async.ByteBufferFeeder;
import com.fasterxml.jackson.core.async.NonBlockingInputFeeder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import infra.core.codec.DecodingException;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferLimitException;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

/**
 * {@link Function} to transform a JSON stream of arbitrary size, byte array
 * chunks into a {@code Flux<TokenBuffer>} where each token buffer is a
 * well-formed JSON object.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class Jackson2Tokenizer {

  private final JsonParser parser;

  private final DeserializationContext deserializationContext;

  private final NonBlockingInputFeeder inputFeeder;

  private final boolean tokenizeArrayElements;

  private final boolean forceUseOfBigDecimal;

  private final int maxInMemorySize;

  private int objectDepth;

  private int arrayDepth;

  private int byteCount;

  private TokenBuffer tokenBuffer;

  private Jackson2Tokenizer(JsonParser parser, DeserializationContext deserializationContext,
          boolean tokenizeArrayElements, boolean forceUseOfBigDecimal, int maxInMemorySize) {

    this.parser = parser;
    this.deserializationContext = deserializationContext;
    this.inputFeeder = this.parser.getNonBlockingInputFeeder();
    this.tokenizeArrayElements = tokenizeArrayElements;
    this.forceUseOfBigDecimal = forceUseOfBigDecimal;
    this.maxInMemorySize = maxInMemorySize;
    this.tokenBuffer = createToken();
  }

  private List<TokenBuffer> tokenize(DataBuffer dataBuffer) {
    try {
      int bufferSize = dataBuffer.readableBytes();
      ArrayList<TokenBuffer> tokens = new ArrayList<>();
      if (this.inputFeeder instanceof ByteBufferFeeder byteBufferFeeder) {
        try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
          while (iterator.hasNext()) {
            byteBufferFeeder.feedInput(iterator.next());
            parseTokens(tokens);
          }
        }
      }
      else if (this.inputFeeder instanceof ByteArrayFeeder byteArrayFeeder) {
        byte[] bytes = new byte[bufferSize];
        dataBuffer.read(bytes);
        byteArrayFeeder.feedInput(bytes, 0, bufferSize);
        parseTokens(tokens);
      }
      assertInMemorySize(bufferSize, tokens);
      return tokens;
    }
    catch (JsonProcessingException ex) {
      throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
    }
    catch (IOException ex) {
      throw Exceptions.propagate(ex);
    }
    finally {
      dataBuffer.release();
    }
  }

  private Flux<TokenBuffer> endOfInput() {
    return Flux.defer(() -> {
      this.inputFeeder.endOfInput();
      try {
        ArrayList<TokenBuffer> tokens = new ArrayList<>();
        parseTokens(tokens);
        return Flux.fromIterable(tokens);
      }
      catch (JsonProcessingException ex) {
        throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
      }
      catch (IOException ex) {
        throw Exceptions.propagate(ex);
      }
    });
  }

  private void parseTokens(ArrayList<TokenBuffer> tokens) throws IOException {
    boolean previousNull = false;
    while (!this.parser.isClosed()) {
      JsonToken token = this.parser.nextToken();
      if (token == JsonToken.NOT_AVAILABLE ||
              token == null && previousNull) {
        break;
      }
      else if (token == null) { // !previousNull
        previousNull = true;
        continue;
      }
      else {
        previousNull = false;
      }
      updateDepth(token);
      if (!this.tokenizeArrayElements) {
        processTokenNormal(token, tokens);
      }
      else {
        processTokenArray(token, tokens);
      }
    }
  }

  private void updateDepth(JsonToken token) {
    switch (token) {
      case START_OBJECT -> this.objectDepth++;
      case END_OBJECT -> this.objectDepth--;
      case START_ARRAY -> this.arrayDepth++;
      case END_ARRAY -> this.arrayDepth--;
    }
  }

  private void processTokenNormal(JsonToken token, ArrayList<TokenBuffer> result) throws IOException {
    this.tokenBuffer.copyCurrentEvent(this.parser);

    if ((token.isStructEnd() || token.isScalarValue()) && this.objectDepth == 0 && this.arrayDepth == 0) {
      result.add(this.tokenBuffer);
      this.tokenBuffer = createToken();
    }
  }

  private void processTokenArray(JsonToken token, ArrayList<TokenBuffer> result) throws IOException {
    if (!isTopLevelArrayToken(token)) {
      this.tokenBuffer.copyCurrentEvent(this.parser);
    }

    if (this.objectDepth == 0
            && (this.arrayDepth == 0 || this.arrayDepth == 1)
            && (token == JsonToken.END_OBJECT || token.isScalarValue())) {
      result.add(this.tokenBuffer);
      this.tokenBuffer = createToken();
    }
  }

  private TokenBuffer createToken() {
    TokenBuffer tokenBuffer = new TokenBuffer(this.parser, this.deserializationContext);
    tokenBuffer.forceUseOfBigDecimal(this.forceUseOfBigDecimal);
    return tokenBuffer;
  }

  private boolean isTopLevelArrayToken(JsonToken token) {
    return this.objectDepth == 0 && ((token == JsonToken.START_ARRAY && this.arrayDepth == 1) ||
            (token == JsonToken.END_ARRAY && this.arrayDepth == 0));
  }

  private void assertInMemorySize(int currentBufferSize, List<TokenBuffer> result) {
    if (this.maxInMemorySize >= 0) {
      if (!result.isEmpty()) {
        this.byteCount = 0;
      }
      else if (currentBufferSize > Integer.MAX_VALUE - this.byteCount) {
        raiseLimitException();
      }
      else {
        this.byteCount += currentBufferSize;
        if (this.byteCount > this.maxInMemorySize) {
          raiseLimitException();
        }
      }
    }
  }

  private void raiseLimitException() {
    throw new DataBufferLimitException(
            "Exceeded limit on max bytes per JSON object: " + this.maxInMemorySize);
  }

  /**
   * Tokenize the given {@code Flux<DataBuffer>} into {@code Flux<TokenBuffer>}.
   *
   * @param dataBuffers the source data buffers
   * @param jsonFactory the factory to use
   * @param objectMapper the current mapper instance
   * @param tokenizeArrays if {@code true} and the "top level" JSON object is
   * an array, each element is returned individually immediately after it is received
   * @param forceUseOfBigDecimal if {@code true}, any floating point values encountered
   * in source will use {@link java.math.BigDecimal}
   * @param maxInMemorySize maximum memory size
   * @return the resulting token buffers
   */
  public static Flux<TokenBuffer> tokenize(Flux<DataBuffer> dataBuffers, JsonFactory jsonFactory,
          ObjectMapper objectMapper, boolean tokenizeArrays, boolean forceUseOfBigDecimal, int maxInMemorySize) {

    try {
      JsonParser parser;
      if (jsonFactory.getFormatName().equals(SmileFactory.FORMAT_NAME_SMILE)) {
        // ByteBufferFeeder is not supported for Smile
        parser = jsonFactory.createNonBlockingByteArrayParser();
      }
      else {
        parser = jsonFactory.createNonBlockingByteBufferParser();
      }
      DeserializationContext context = objectMapper.getDeserializationContext();
      if (context instanceof DefaultDeserializationContext ddc) {
        context = ddc.createInstance(objectMapper.getDeserializationConfig(),
                parser, objectMapper.getInjectableValues());
      }
      Jackson2Tokenizer tokenizer =
              new Jackson2Tokenizer(parser, context, tokenizeArrays, forceUseOfBigDecimal, maxInMemorySize);
      return dataBuffers.concatMapIterable(tokenizer::tokenize).concatWith(tokenizer.endOfInput());
    }
    catch (IOException ex) {
      return Flux.error(ex);
    }
  }

}
