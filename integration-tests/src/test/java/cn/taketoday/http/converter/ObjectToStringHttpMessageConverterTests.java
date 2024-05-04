/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.http.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.server.ServletServerHttpRequest;
import cn.taketoday.http.server.ServletServerHttpResponse;
import cn.taketoday.lang.Constant;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test cases for {@link ObjectToStringHttpMessageConverter} class.
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 */
public class ObjectToStringHttpMessageConverterTests {

  private ObjectToStringHttpMessageConverter converter;

  private MockHttpServletResponse servletResponse;

  private ServletServerHttpResponse response;

  @BeforeEach
  public void setup() {
    ConversionService conversionService = new DefaultConversionService();
    this.converter = new ObjectToStringHttpMessageConverter(conversionService);

    this.servletResponse = new MockHttpServletResponse();
    this.response = new ServletServerHttpResponse(this.servletResponse);
  }

  @Test
  public void canRead() {
    assertThat(this.converter.canRead(Math.class, null)).isFalse();
    assertThat(this.converter.canRead(Resource.class, null)).isFalse();

    assertThat(this.converter.canRead(Locale.class, null)).isTrue();
    assertThat(this.converter.canRead(BigInteger.class, null)).isTrue();

    assertThat(this.converter.canRead(BigInteger.class, MediaType.TEXT_HTML)).isFalse();
    assertThat(this.converter.canRead(BigInteger.class, MediaType.TEXT_XML)).isFalse();
    assertThat(this.converter.canRead(BigInteger.class, MediaType.APPLICATION_XML)).isFalse();
  }

  @Test
  public void canWrite() {
    assertThat(this.converter.canWrite(Math.class, null)).isFalse();
    assertThat(this.converter.canWrite(Resource.class, null)).isFalse();

    assertThat(this.converter.canWrite(Locale.class, null)).isTrue();
    assertThat(this.converter.canWrite(Double.class, null)).isTrue();

    assertThat(this.converter.canWrite(BigInteger.class, MediaType.TEXT_HTML)).isFalse();
    assertThat(this.converter.canWrite(BigInteger.class, MediaType.TEXT_XML)).isFalse();
    assertThat(this.converter.canWrite(BigInteger.class, MediaType.APPLICATION_XML)).isFalse();

    assertThat(this.converter.canWrite(BigInteger.class, MediaType.valueOf("text/*"))).isTrue();
  }

  @Test
  public void defaultCharset() throws IOException {
    this.converter.write(Integer.valueOf(5), null, response);

    assertThat(servletResponse.getCharacterEncoding()).isEqualTo("UTF-8");
  }

  @Test
  public void defaultCharsetModified() throws IOException {
    ConversionService cs = new DefaultConversionService();
    ObjectToStringHttpMessageConverter converter = new ObjectToStringHttpMessageConverter(cs, StandardCharsets.UTF_16);
    converter.write((byte) 31, null, this.response);

    assertThat(this.servletResponse.getCharacterEncoding()).isEqualTo("UTF-16");
  }

  @Test
  public void writeAcceptCharset() throws IOException {
    this.converter.setWriteAcceptCharset(true);
    this.converter.write(new Date(), null, this.response);

    assertThat(this.servletResponse.getHeader("Accept-Charset")).isNotNull();
  }

  @Test
  public void writeAcceptCharsetTurnedOff() throws IOException {
    this.converter.setWriteAcceptCharset(false);
    this.converter.write(new Date(), null, this.response);

    assertThat(this.servletResponse.getHeader("Accept-Charset")).isNull();
  }

  @Test
  public void read() throws IOException {
    Short shortValue = Short.valueOf((short) 781);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContentType(MediaType.TEXT_PLAIN_VALUE);
    request.setContent(shortValue.toString().getBytes(Constant.DEFAULT_CHARSET));
    assertThat(this.converter.read(Short.class, new ServletServerHttpRequest(request))).isEqualTo(shortValue);

    Float floatValue = Float.valueOf(123);
    request = new MockHttpServletRequest();
    request.setContentType(MediaType.TEXT_PLAIN_VALUE);
    request.setCharacterEncoding("UTF-16");
    request.setContent(floatValue.toString().getBytes("UTF-16"));
    assertThat(this.converter.read(Float.class, new ServletServerHttpRequest(request))).isEqualTo(floatValue);

    Long longValue = Long.valueOf(55819182821331L);
    request = new MockHttpServletRequest();
    request.setContentType(MediaType.TEXT_PLAIN_VALUE);
    request.setCharacterEncoding("UTF-8");
    request.setContent(longValue.toString().getBytes("UTF-8"));
    assertThat(this.converter.read(Long.class, new ServletServerHttpRequest(request))).isEqualTo(longValue);
  }

  @Test
  public void write() throws IOException {
    this.converter.write((byte) -8, null, this.response);

    assertThat(this.servletResponse.getCharacterEncoding()).isEqualTo("UTF-8");
    assertThat(this.servletResponse.getContentType().startsWith(MediaType.TEXT_PLAIN_VALUE)).isTrue();
    assertThat(this.servletResponse.getContentLength()).isEqualTo(2);
    assertThat(this.servletResponse.getContentAsByteArray()).isEqualTo(new byte[] { '-', '8' });
  }

  @Test
  public void writeUtf16() throws IOException {
    MediaType contentType = new MediaType("text", "plain", StandardCharsets.UTF_16);
    this.converter.write(Integer.valueOf(958), contentType, this.response);

    assertThat(this.servletResponse.getCharacterEncoding()).isEqualTo("UTF-16");
    assertThat(this.servletResponse.getContentType().startsWith(MediaType.TEXT_PLAIN_VALUE)).isTrue();
    assertThat(this.servletResponse.getContentLength()).isEqualTo(8);
    // First two bytes: byte order mark
    assertThat(this.servletResponse.getContentAsByteArray()).isEqualTo(new byte[] { -2, -1, 0, '9', 0, '5', 0, '8' });
  }

  @Test
  public void testConversionServiceRequired() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ObjectToStringHttpMessageConverter(null));
  }

}
