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

package cn.taketoday.http.converter.smile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Jackson 2.x Smile converter tests.
 *
 * @author Sebastien Deleuze
 */
public class MappingJackson2SmileHttpMessageConverterTests {

  private final MappingJackson2SmileHttpMessageConverter converter = new MappingJackson2SmileHttpMessageConverter();
  private final ObjectMapper mapper = new ObjectMapper(new SmileFactory());

  @Test
  public void canRead() {
    assertThat(converter.canRead(MyBean.class, new MediaType("application", "x-jackson-smile"))).isTrue();
    assertThat(converter.canRead(MyBean.class, new MediaType("application", "json"))).isFalse();
    assertThat(converter.canRead(MyBean.class, new MediaType("application", "xml"))).isFalse();
  }

  @Test
  public void canWrite() {
    assertThat(converter.canWrite(MyBean.class, new MediaType("application", "x-jackson-smile"))).isTrue();
    assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json"))).isFalse();
    assertThat(converter.canWrite(MyBean.class, new MediaType("application", "xml"))).isFalse();
  }

  @Test
  public void read() throws IOException {
    MyBean body = new MyBean();
    body.setString("Foo");
    body.setNumber(42);
    body.setFraction(42F);
    body.setArray(new String[] { "Foo", "Bar" });
    body.setBool(true);
    body.setBytes(new byte[] { 0x1, 0x2 });
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(mapper.writeValueAsBytes(body));
    inputMessage.getHeaders().setContentType(new MediaType("application", "x-jackson-smile"));
    MyBean result = (MyBean) converter.read(MyBean.class, inputMessage);
    assertThat(result.getString()).isEqualTo("Foo");
    assertThat(result.getNumber()).isEqualTo(42);
    assertThat(result.getFraction()).isCloseTo(42F, Assertions.within(0F));

    assertThat(result.getArray()).isEqualTo(new String[] { "Foo", "Bar" });
    assertThat(result.isBool()).isTrue();
    assertThat(result.getBytes()).isEqualTo(new byte[] { 0x1, 0x2 });
  }

  @Test
  public void write() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MyBean body = new MyBean();
    body.setString("Foo");
    body.setNumber(42);
    body.setFraction(42F);
    body.setArray(new String[] { "Foo", "Bar" });
    body.setBool(true);
    body.setBytes(new byte[] { 0x1, 0x2 });
    converter.write(body, null, outputMessage);
    assertThat(outputMessage.getBodyAsBytes()).isEqualTo(mapper.writeValueAsBytes(body));
    assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(new MediaType("application", "x-jackson-smile"));
  }

  public static class MyBean {

    private String string;

    private int number;

    private float fraction;

    private String[] array;

    private boolean bool;

    private byte[] bytes;

    public byte[] getBytes() {
      return bytes;
    }

    public void setBytes(byte[] bytes) {
      this.bytes = bytes;
    }

    public boolean isBool() {
      return bool;
    }

    public void setBool(boolean bool) {
      this.bool = bool;
    }

    public String getString() {
      return string;
    }

    public void setString(String string) {
      this.string = string;
    }

    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    public float getFraction() {
      return fraction;
    }

    public void setFraction(float fraction) {
      this.fraction = fraction;
    }

    public String[] getArray() {
      return array;
    }

    public void setArray(String[] array) {
      this.array = array;
    }
  }

}
