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

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

import static cn.taketoday.http.MediaType.APPLICATION_NDJSON;

/**
 * Unit tests for a customized {@link Jackson2JsonEncoder}.
 *
 * @author Jason Laber
 */
public class CustomizedJackson2JsonEncoderTests extends AbstractEncoderTests<Jackson2JsonEncoder> {

  public CustomizedJackson2JsonEncoderTests() {
    super(new Jackson2JsonEncoderWithCustomization());
  }

  @Override
  public void canEncode() throws Exception {
    // Not Testing, covered under Jackson2JsonEncoderTests
  }

  @Override
  @Test
  public void encode() throws Exception {
    Flux<MyCustomizedEncoderBean> input = Flux.just(
            new MyCustomizedEncoderBean(MyCustomEncoderEnum.VAL1),
            new MyCustomizedEncoderBean(MyCustomEncoderEnum.VAL2)
    );

    testEncodeAll(input, ResolvableType.forClass(MyCustomizedEncoderBean.class), APPLICATION_NDJSON, null, step -> step
            .consumeNextWith(expectString("{\"property\":\"Value1\"}\n"))
            .consumeNextWith(expectString("{\"property\":\"Value2\"}\n"))
            .verifyComplete()
    );
  }

  @Test
  public void encodeNonStream() {
    Flux<MyCustomizedEncoderBean> input = Flux.just(
            new MyCustomizedEncoderBean(MyCustomEncoderEnum.VAL1),
            new MyCustomizedEncoderBean(MyCustomEncoderEnum.VAL2)
    );

    testEncode(input, MyCustomizedEncoderBean.class, step -> step
            .consumeNextWith(expectString("[{\"property\":\"Value1\"}").andThen(DataBufferUtils::release))
            .consumeNextWith(expectString(",{\"property\":\"Value2\"}").andThen(DataBufferUtils::release))
            .consumeNextWith(expectString("]").andThen(DataBufferUtils::release))
            .verifyComplete());
  }

  private static class MyCustomizedEncoderBean {

    private MyCustomEncoderEnum property;

    public MyCustomizedEncoderBean(MyCustomEncoderEnum property) {
      this.property = property;
    }

    public MyCustomEncoderEnum getProperty() {
      return property;
    }

    public void setProperty(MyCustomEncoderEnum property) {
      this.property = property;
    }
  }

  private enum MyCustomEncoderEnum {
    VAL1,
    VAL2;

    @Override
    public String toString() {
      return this == VAL1 ? "Value1" : "Value2";
    }
  }

  private static class Jackson2JsonEncoderWithCustomization extends Jackson2JsonEncoder {

    @Override
    protected ObjectWriter customizeWriter(
            ObjectWriter writer, MimeType mimeType, ResolvableType elementType, Map<String, Object> hints) {

      return writer.with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    }
  }

}
