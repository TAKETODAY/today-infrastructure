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

package cn.taketoday.http.codec.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class Jackson2TokenizerTests extends AbstractLeakCheckingTests {

  private JsonFactory jsonFactory;

  private ObjectMapper objectMapper;

  @BeforeEach
  public void createParser() {
    this.jsonFactory = new JsonFactory();
    this.objectMapper = new ObjectMapper(this.jsonFactory);
  }

  @Test
  public void doNotTokenizeArrayElements() {
    testTokenize(
            singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"),
            singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"), false);

    testTokenize(
            asList(
                    "{\"foo\": \"foofoo\"",
                    ", \"bar\": \"barbar\"}"
            ),
            singletonList("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"), false);

    testTokenize(
            singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
            singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
            false);

    testTokenize(
            singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"),
            singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"), false);

    testTokenize(
            asList(
                    "[{\"foo\": \"foofoo\", \"bar\"",
                    ": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"
            ),
            singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
            false);

    testTokenize(
            asList(
                    "[",
                    "{\"id\":1,\"name\":\"Robert\"}", ",",
                    "{\"id\":2,\"name\":\"Raide\"}", ",",
                    "{\"id\":3,\"name\":\"Ford\"}", "]"
            ),
            singletonList("[{\"id\":1,\"name\":\"Robert\"},{\"id\":2,\"name\":\"Raide\"},{\"id\":3,\"name\":\"Ford\"}]"),
            false);

    // SPR-16166: top-level JSON values
    testTokenize(asList("\"foo", "bar\""), singletonList("\"foobar\""), false);
    testTokenize(asList("12", "34"), singletonList("1234"), false);
    testTokenize(asList("12.", "34"), singletonList("12.34"), false);

    // note that we do not test for null, true, or false, which are also valid top-level values,
    // but are unsupported by JSONassert
  }

  @Test
  public void tokenizeArrayElements() {
    testTokenize(
            singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"),
            singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"), true);

    testTokenize(
            asList(
                    "{\"foo\": \"foofoo\"",
                    ", \"bar\": \"barbar\"}"
            ),
            singletonList("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"), true);

    testTokenize(
            singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
            asList(
                    "{\"foo\": \"foofoo\", \"bar\": \"barbar\"}",
                    "{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}"
            ),
            true);

    testTokenize(
            singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"),
            asList(
                    "{\"foo\": \"bar\"}",
                    "{\"foo\": \"baz\"}"
            ),
            true);

    // SPR-15803: nested array
    testTokenize(
            singletonList("[" +
                    "{\"id\":\"0\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}," +
                    "{\"id\":\"1\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}," +
                    "{\"id\":\"2\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}" +
                    "]"),
            asList(
                    "{\"id\":\"0\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}",
                    "{\"id\":\"1\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}",
                    "{\"id\":\"2\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}"
            ),
            true);

    // SPR-15803: nested array, no top-level array
    testTokenize(
            singletonList("{\"speakerIds\":[\"tastapod\"],\"language\":\"ENGLISH\"}"),
            singletonList("{\"speakerIds\":[\"tastapod\"],\"language\":\"ENGLISH\"}"), true);

    testTokenize(
            asList(
                    "[{\"foo\": \"foofoo\", \"bar\"",
                    ": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"
            ),
            asList(
                    "{\"foo\": \"foofoo\", \"bar\": \"barbar\"}",
                    "{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}"), true);

    testTokenize(
            asList(
                    "[",
                    "{\"id\":1,\"name\":\"Robert\"}",
                    ",",
                    "{\"id\":2,\"name\":\"Raide\"}",
                    ",",
                    "{\"id\":3,\"name\":\"Ford\"}",
                    "]"
            ),
            asList(
                    "{\"id\":1,\"name\":\"Robert\"}",
                    "{\"id\":2,\"name\":\"Raide\"}",
                    "{\"id\":3,\"name\":\"Ford\"}"
            ),
            true);

    // SPR-16166: top-level JSON values
    testTokenize(asList("\"foo", "bar\""), singletonList("\"foobar\""), true);
    testTokenize(asList("12", "34"), singletonList("1234"), true);
    testTokenize(asList("12.", "34"), singletonList("12.34"), true);

    // SPR-16407
    testTokenize(asList("[1", ",2,", "3]"), asList("1", "2", "3"), true);
  }

  @Test
  void tokenizeStream() {

    // NDJSON (Newline Delimited JSON), JSON Lines
    testTokenize(
            asList(
                    "{\"id\":1,\"name\":\"Robert\"}",
                    "\n",
                    "{\"id\":2,\"name\":\"Raide\"}",
                    "\n",
                    "{\"id\":3,\"name\":\"Ford\"}"
            ),
            asList(
                    "{\"id\":1,\"name\":\"Robert\"}",
                    "{\"id\":2,\"name\":\"Raide\"}",
                    "{\"id\":3,\"name\":\"Ford\"}"
            ),
            true);

    // JSON Sequence with newline separator
    testTokenize(
            asList(
                    "\n",
                    "{\"id\":1,\"name\":\"Robert\"}",
                    "\n",
                    "{\"id\":2,\"name\":\"Raide\"}",
                    "\n",
                    "{\"id\":3,\"name\":\"Ford\"}"
            ),
            asList(
                    "{\"id\":1,\"name\":\"Robert\"}",
                    "{\"id\":2,\"name\":\"Raide\"}",
                    "{\"id\":3,\"name\":\"Ford\"}"
            ),
            true);
  }

  private void testTokenize(List<String> input, List<String> output, boolean tokenize) {
    StepVerifier.FirstStep<String> builder = StepVerifier.create(decode(input, tokenize, -1));
    output.forEach(expected -> builder.assertNext(actual -> {
      try {
        JSONAssert.assertEquals(expected, actual, true);
      }
      catch (JSONException ex) {
        throw new RuntimeException(ex);
      }
    }));
    builder.verifyComplete();
  }

  @Test
  public void testLimit() {
    List<String> source = asList(
            "[",
            "{", "\"id\":1,\"name\":\"Dan\"", "},",
            "{", "\"id\":2,\"name\":\"Ron\"", "},",
            "{", "\"id\":3,\"name\":\"Bartholomew\"", "}",
            "]"
    );

    String expected = String.join("", source);
    int maxInMemorySize = expected.length();

    StepVerifier.create(decode(source, false, maxInMemorySize))
            .expectNext(expected)
            .verifyComplete();

    StepVerifier.create(decode(source, false, maxInMemorySize - 2))
            .verifyError(DataBufferLimitException.class);
  }

  @Test
  public void testLimitTokenized() {

    List<String> source = asList(
            "[",
            "{", "\"id\":1, \"name\":\"Dan\"", "},",
            "{", "\"id\":2, \"name\":\"Ron\"", "},",
            "{", "\"id\":3, \"name\":\"Bartholomew\"", "}",
            "]"
    );

    String expected = "{\"id\":3,\"name\":\"Bartholomew\"}";
    int maxInMemorySize = expected.length();

    StepVerifier.create(decode(source, true, maxInMemorySize))
            .expectNext("{\"id\":1,\"name\":\"Dan\"}")
            .expectNext("{\"id\":2,\"name\":\"Ron\"}")
            .expectNext(expected)
            .verifyComplete();

    StepVerifier.create(decode(source, true, maxInMemorySize - 1))
            .expectNext("{\"id\":1,\"name\":\"Dan\"}")
            .expectNext("{\"id\":2,\"name\":\"Ron\"}")
            .verifyError(DataBufferLimitException.class);
  }

  @Test
  public void errorInStream() {
    DataBuffer buffer = stringBuffer("{\"id\":1,\"name\":");
    Flux<DataBuffer> source = Flux.just(buffer).concatWith(Flux.error(new RuntimeException()));
    Flux<TokenBuffer> result = Jackson2Tokenizer.tokenize(source, this.jsonFactory, this.objectMapper, true,
            false, -1);

    StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
  }

  @Test  // SPR-16521
  public void jsonEOFExceptionIsWrappedAsDecodingError() {
    Flux<DataBuffer> source = Flux.just(stringBuffer("{\"status\": \"noClosingQuote}"));
    Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(source, this.jsonFactory, this.objectMapper, false,
            false, -1);

    StepVerifier.create(tokens)
            .expectError(DecodingException.class)
            .verify();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void useBigDecimalForFloats(boolean useBigDecimalForFloats) {
    Flux<DataBuffer> source = Flux.just(stringBuffer("1E+2"));
    Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(
            source, this.jsonFactory, this.objectMapper, false, useBigDecimalForFloats, -1);

    StepVerifier.create(tokens)
            .assertNext(tokenBuffer -> {
              try {
                JsonParser parser = tokenBuffer.asParser();
                JsonToken token = parser.nextToken();
                assertThat(token).isEqualTo(JsonToken.VALUE_NUMBER_FLOAT);
                JsonParser.NumberType numberType = parser.getNumberType();
                if (useBigDecimalForFloats) {
                  assertThat(numberType).isEqualTo(JsonParser.NumberType.BIG_DECIMAL);
                }
                else {
                  assertThat(numberType).isEqualTo(JsonParser.NumberType.DOUBLE);
                }
              }
              catch (IOException ex) {
                fail(ex);
              }
            })
            .verifyComplete();
  }

  private Flux<String> decode(List<String> source, boolean tokenize, int maxInMemorySize) {

    Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(
            Flux.fromIterable(source).map(this::stringBuffer),
            this.jsonFactory, this.objectMapper, tokenize, false, maxInMemorySize);

    return tokens
            .map(tokenBuffer -> {
              try {
                TreeNode root = this.objectMapper.readTree(tokenBuffer.asParser());
                return this.objectMapper.writeValueAsString(root);
              }
              catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            });
  }

  private DataBuffer stringBuffer(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
    buffer.write(bytes);
    return buffer;
  }

}
