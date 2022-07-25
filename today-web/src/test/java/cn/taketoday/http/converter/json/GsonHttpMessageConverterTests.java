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

package cn.taketoday.http.converter.json;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.TypeReference;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

/**
 * Gson 2.x converter tests.
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 */
public class GsonHttpMessageConverterTests {

  private final GsonHttpMessageConverter converter = new GsonHttpMessageConverter();

  @Test
  public void canRead() {
    assertThat(this.converter.canRead(MyBean.class, new MediaType("application", "json"))).isTrue();
    assertThat(this.converter.canRead(Map.class, new MediaType("application", "json"))).isTrue();
  }

  @Test
  public void canWrite() {
    assertThat(this.converter.canWrite(MyBean.class, new MediaType("application", "json"))).isTrue();
    assertThat(this.converter.canWrite(Map.class, new MediaType("application", "json"))).isTrue();
  }

  @Test
  public void canReadAndWriteMicroformats() {
    assertThat(this.converter.canRead(MyBean.class, new MediaType("application", "vnd.test-micro-type+json"))).isTrue();
    assertThat(this.converter.canWrite(MyBean.class, new MediaType("application", "vnd.test-micro-type+json"))).isTrue();
  }

  @Test
  public void readTyped() throws IOException {
    String body = "{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
            "\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
    inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
    MyBean result = (MyBean) this.converter.read(MyBean.class, inputMessage);

    assertThat(result.getString()).isEqualTo("Foo");
    assertThat(result.getNumber()).isEqualTo(42);
    assertThat(result.getFraction()).isCloseTo(42F, Assertions.within(0F));

    assertThat(result.getArray()).isEqualTo(new String[] { "Foo", "Bar" });
    assertThat(result.isBool()).isTrue();
    assertThat(result.getBytes()).isEqualTo(new byte[] { 0x1, 0x2 });
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readUntyped() throws IOException {
    String body = "{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
            "\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
    inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
    HashMap<String, Object> result = (HashMap<String, Object>) this.converter.read(HashMap.class, inputMessage);
    assertThat(result.get("string")).isEqualTo("Foo");
    Number n = (Number) result.get("number");
    assertThat(n.longValue()).isEqualTo(42);
    n = (Number) result.get("fraction");
    assertThat(n.doubleValue()).isCloseTo(42D, Assertions.within(0D));

    List<String> array = new ArrayList<>();
    array.add("Foo");
    array.add("Bar");
    assertThat(result.get("array")).isEqualTo(array);
    assertThat(result.get("bool")).isEqualTo(Boolean.TRUE);
    byte[] bytes = new byte[2];
    List<Number> resultBytes = (ArrayList<Number>) result.get("bytes");
    for (int i = 0; i < 2; i++) {
      bytes[i] = resultBytes.get(i).byteValue();
    }
    assertThat(bytes).isEqualTo(new byte[] { 0x1, 0x2 });
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
    this.converter.write(body, null, outputMessage);
    Charset utf8 = StandardCharsets.UTF_8;
    String result = outputMessage.getBodyAsString(utf8);
    assertThat(result.contains("\"string\":\"Foo\"")).isTrue();
    assertThat(result.contains("\"number\":42")).isTrue();
    assertThat(result.contains("fraction\":42.0")).isTrue();
    assertThat(result.contains("\"array\":[\"Foo\",\"Bar\"]")).isTrue();
    assertThat(result.contains("\"bool\":true")).isTrue();
    assertThat(result.contains("\"bytes\":[1,2]")).isTrue();
    assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(new MediaType("application", "json", utf8));
  }

  @Test
  public void writeWithBaseType() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MyBean body = new MyBean();
    body.setString("Foo");
    body.setNumber(42);
    body.setFraction(42F);
    body.setArray(new String[] { "Foo", "Bar" });
    body.setBool(true);
    body.setBytes(new byte[] { 0x1, 0x2 });
    this.converter.write(body, MyBase.class, null, outputMessage);
    Charset utf8 = StandardCharsets.UTF_8;
    String result = outputMessage.getBodyAsString(utf8);
    assertThat(result.contains("\"string\":\"Foo\"")).isTrue();
    assertThat(result.contains("\"number\":42")).isTrue();
    assertThat(result.contains("fraction\":42.0")).isTrue();
    assertThat(result.contains("\"array\":[\"Foo\",\"Bar\"]")).isTrue();
    assertThat(result.contains("\"bool\":true")).isTrue();
    assertThat(result.contains("\"bytes\":[1,2]")).isTrue();
    assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(new MediaType("application", "json", utf8));
  }

  @Test
  public void writeUTF16() throws IOException {
    MediaType contentType = new MediaType("application", "json", StandardCharsets.UTF_16BE);
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    String body = "H\u00e9llo W\u00f6rld";
    this.converter.write(body, contentType, outputMessage);
    assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_16BE)).as("Invalid result").isEqualTo(("\"" + body + "\""));
    assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(contentType);
  }

  @Test
  public void readInvalidJson() throws IOException {
    String body = "FooBar";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
    inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
    assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
            this.converter.read(MyBean.class, inputMessage));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readAndWriteGenerics() throws Exception {
    Field beansList = ListHolder.class.getField("listField");

    String body = "[{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
            "\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
    inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

    Type genericType = beansList.getGenericType();
    List<MyBean> results = (List<MyBean>) converter.read(genericType, MyBeanListHolder.class, inputMessage);
    assertThat(results.size()).isEqualTo(1);
    MyBean result = results.get(0);
    assertThat(result.getString()).isEqualTo("Foo");
    assertThat(result.getNumber()).isEqualTo(42);
    assertThat(result.getFraction()).isCloseTo(42F, Assertions.within(0F));

    assertThat(result.getArray()).isEqualTo(new String[] { "Foo", "Bar" });
    assertThat(result.isBool()).isTrue();
    assertThat(result.getBytes()).isEqualTo(new byte[] { 0x1, 0x2 });

    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    converter.write(results, genericType, new MediaType("application", "json"), outputMessage);
    JSONAssert.assertEquals(body, outputMessage.getBodyAsString(StandardCharsets.UTF_8), true);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readAndWriteParameterizedType() throws Exception {
    TypeReference<List<MyBean>> beansList = new TypeReference<List<MyBean>>() {
    };

    String body = "[{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
            "\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
    inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

    List<MyBean> results = (List<MyBean>) converter.read(beansList.getType(), null, inputMessage);
    assertThat(results.size()).isEqualTo(1);
    MyBean result = results.get(0);
    assertThat(result.getString()).isEqualTo("Foo");
    assertThat(result.getNumber()).isEqualTo(42);
    assertThat(result.getFraction()).isCloseTo(42F, Assertions.within(0F));

    assertThat(result.getArray()).isEqualTo(new String[] { "Foo", "Bar" });
    assertThat(result.isBool()).isTrue();
    assertThat(result.getBytes()).isEqualTo(new byte[] { 0x1, 0x2 });

    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    converter.write(results, beansList.getType(), new MediaType("application", "json"), outputMessage);
    JSONAssert.assertEquals(body, outputMessage.getBodyAsString(StandardCharsets.UTF_8), true);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void writeParameterizedBaseType() throws Exception {
    TypeReference<List<MyBean>> beansList = new TypeReference<List<MyBean>>() { };
    TypeReference<List<MyBase>> baseList = new TypeReference<List<MyBase>>() { };

    String body = "[{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
            "\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
    inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

    List<MyBean> results = (List<MyBean>) converter.read(beansList.getType(), null, inputMessage);
    assertThat(results.size()).isEqualTo(1);
    MyBean result = results.get(0);
    assertThat(result.getString()).isEqualTo("Foo");
    assertThat(result.getNumber()).isEqualTo(42);
    assertThat(result.getFraction()).isCloseTo(42F, Assertions.within(0F));

    assertThat(result.getArray()).isEqualTo(new String[] { "Foo", "Bar" });
    assertThat(result.isBool()).isTrue();
    assertThat(result.getBytes()).isEqualTo(new byte[] { 0x1, 0x2 });

    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    converter.write(results, baseList.getType(), new MediaType("application", "json"), outputMessage);
    JSONAssert.assertEquals(body, outputMessage.getBodyAsString(StandardCharsets.UTF_8), true);
  }

  @Test
  public void prefixJson() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    this.converter.setPrefixJson(true);
    this.converter.writeInternal("foo", null, outputMessage);
    assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(")]}', \"foo\"");
  }

  @Test
  public void prefixJsonCustom() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    this.converter.setJsonPrefix(")))");
    this.converter.writeInternal("foo", null, outputMessage);
    assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(")))\"foo\"");
  }

  public static class MyBase {

    private String string;

    public String getString() {
      return string;
    }

    public void setString(String string) {
      this.string = string;
    }
  }

  public static class MyBean extends MyBase {

    private int number;

    private float fraction;

    private String[] array;

    private boolean bool;

    private byte[] bytes;

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

    public boolean isBool() {
      return bool;
    }

    public void setBool(boolean bool) {
      this.bool = bool;
    }

    public byte[] getBytes() {
      return bytes;
    }

    public void setBytes(byte[] bytes) {
      this.bytes = bytes;
    }
  }

  public static class ListHolder<E> {

    public List<E> listField;
  }

  public static class MyBeanListHolder extends ListHolder<MyBean> {
  }

}
