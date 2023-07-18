/*
 * Copyright 2017 - 2023 the original author or authors.
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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.testfixture.codec.AbstractDecoderTests;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Unit tests for a customized {@link Jackson2JsonDecoder}.
 *
 * @author Jason Laber
 */
public class CustomizedJackson2JsonDecoderTests extends AbstractDecoderTests<Jackson2JsonDecoder> {

  public CustomizedJackson2JsonDecoderTests() {
    super(new Jackson2JsonDecoderWithCustomization());
  }

  @Override
  public void canDecode() throws Exception {
    // Not Testing, covered under Jackson2JsonDecoderTests
  }

  @Override
  @Test
  public void decode() throws Exception {
    Flux<DataBuffer> input = Flux.concat(stringBuffer("{\"property\":\"Value1\"}"));

    testDecodeAll(input, MyCustomizedDecoderBean.class, step -> step
            .expectNextMatches(obj -> obj.getProperty() == MyCustomDecoderEnum.VAL1)
            .verifyComplete());
  }

  @Override
  @Test
  public void decodeToMono() throws Exception {
    Mono<DataBuffer> input = stringBuffer("{\"property\":\"Value2\"}");

    ResolvableType elementType = ResolvableType.forClass(MyCustomizedDecoderBean.class);

    testDecodeToMono(input, elementType, step -> step
            .expectNextMatches(obj -> ((MyCustomizedDecoderBean) obj).getProperty() == MyCustomDecoderEnum.VAL2)
            .expectComplete()
            .verify(), null, null);
  }

  private Mono<DataBuffer> stringBuffer(String value) {
    return stringBuffer(value, StandardCharsets.UTF_8);
  }

  private Mono<DataBuffer> stringBuffer(String value, Charset charset) {
    return Mono.defer(() -> {
      byte[] bytes = value.getBytes(charset);
      DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
      buffer.write(bytes);
      return Mono.just(buffer);
    });
  }

  private static class MyCustomizedDecoderBean {

    private MyCustomDecoderEnum property;

    public MyCustomDecoderEnum getProperty() {
      return property;
    }

    public void setProperty(MyCustomDecoderEnum property) {
      this.property = property;
    }
  }

  private enum MyCustomDecoderEnum {
    VAL1,
    VAL2;

    @Override
    public String toString() {
      return this == VAL1 ? "Value1" : "Value2";
    }
  }

  private static class Jackson2JsonDecoderWithCustomization extends Jackson2JsonDecoder {

    @Override
    protected ObjectReader customizeReader(
            ObjectReader reader, ResolvableType elementType, Map<String, Object> hints) {

      return reader.with(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }
  }

}
