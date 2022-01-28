/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.util.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class StringHttpMessageConverterTests {

  public static final MediaType TEXT_PLAIN_UTF_8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

  private StringHttpMessageConverter converter;

  private MockHttpOutputMessage outputMessage;

  @BeforeEach
  public void setUp() {
    this.converter = new StringHttpMessageConverter();
    this.outputMessage = new MockHttpOutputMessage();
  }

  @Test
  public void canRead() {
    assertThat(this.converter.canRead(String.class, MediaType.TEXT_PLAIN)).isTrue();
  }

  @Test
  public void canWrite() {
    assertThat(this.converter.canWrite(String.class, MediaType.TEXT_PLAIN)).isTrue();
    assertThat(this.converter.canWrite(String.class, MediaType.ALL)).isTrue();
  }

  @Test
  public void read() throws IOException {
    String body = "Hello World";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
    inputMessage.getHeaders().setContentType(TEXT_PLAIN_UTF_8);
    String result = this.converter.read(String.class, inputMessage);

    assertThat(result).as("Invalid result").isEqualTo(body);
  }

  @Test // gh-24123
  public void readJson() throws IOException {
    String body = "{\"result\":\"\u0414\u0410\"}";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
    inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String result = this.converter.read(String.class, inputMessage);

    assertThat(result).as("Invalid result").isEqualTo(body);
  }

  @Test // gh-25328
  public void readJsonApi() throws IOException {
    String body = "{\"result\":\"\u0414\u0410\"}";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
    inputMessage.getHeaders().setContentType(new MediaType("application", "vnd.api.v1+json"));
    String result = this.converter.read(String.class, inputMessage);

    assertThat(result).as("Invalid result").isEqualTo(body);
  }

  @Test
  public void writeDefaultCharset() throws IOException {
    String body = "H\u00e9llo W\u00f6rld";
    this.converter.write(body, null, this.outputMessage);

    HttpHeaders headers = this.outputMessage.getHeaders();
    assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
    assertThat(headers.getContentType()).isEqualTo(new MediaType("text", "plain", StandardCharsets.UTF_8));
    assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
    assertThat(headers.getAcceptCharset().isEmpty()).isTrue();
  }

  @Test  // gh-24123
  public void writeJson() throws IOException {
    String body = "{\"føø\":\"bår\"}";
    this.converter.write(body, MediaType.APPLICATION_JSON, this.outputMessage);

    HttpHeaders headers = this.outputMessage.getHeaders();
    assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
    assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
    assertThat(headers.getAcceptCharset().isEmpty()).isTrue();
  }

  @Test  // gh-25328
  public void writeJsonApi() throws IOException {
    String body = "{\"føø\":\"bår\"}";
    MediaType contentType = new MediaType("application", "vnd.api.v1+json");
    this.converter.write(body, contentType, this.outputMessage);

    HttpHeaders headers = this.outputMessage.getHeaders();
    assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
    assertThat(headers.getContentType()).isEqualTo(contentType);
    assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
    assertThat(headers.getAcceptCharset().isEmpty()).isTrue();
  }

  @Test
  public void writeUTF8() throws IOException {
    String body = "H\u00e9llo W\u00f6rld";
    this.converter.write(body, TEXT_PLAIN_UTF_8, this.outputMessage);

    HttpHeaders headers = this.outputMessage.getHeaders();
    assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
    assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN_UTF_8);
    assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
    assertThat(headers.getAcceptCharset().isEmpty()).isTrue();
  }

  @Test  // SPR-8867
  public void writeOverrideRequestedContentType() throws IOException {
    String body = "H\u00e9llo W\u00f6rld";
    MediaType requestedContentType = new MediaType("text", "html");

    HttpHeaders headers = this.outputMessage.getHeaders();
    headers.setContentType(TEXT_PLAIN_UTF_8);
    this.converter.write(body, requestedContentType, this.outputMessage);

    assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
    assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN_UTF_8);
    assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
    assertThat(headers.getAcceptCharset().isEmpty()).isTrue();
  }

  @Test // gh-24283
  public void writeWithWildCardMediaType() throws IOException {
    String body = "Hello World";
    this.converter.write(body, MediaType.ALL, this.outputMessage);

    HttpHeaders headers = this.outputMessage.getHeaders();
    assertThat(this.outputMessage.getBodyAsString(StandardCharsets.US_ASCII)).isEqualTo(body);
    assertThat(headers.getContentType()).isEqualTo(new MediaType("text", "plain", StandardCharsets.UTF_8));
    assertThat(headers.getContentLength()).isEqualTo(body.getBytes().length);
    assertThat(headers.getAcceptCharset().isEmpty()).isTrue();
  }

}
