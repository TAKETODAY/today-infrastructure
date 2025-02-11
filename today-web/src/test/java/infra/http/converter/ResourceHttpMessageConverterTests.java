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

package infra.http.converter;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import infra.core.io.ByteArrayResource;
import infra.core.io.ClassPathResource;
import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import infra.http.ContentDisposition;
import infra.http.MediaType;
import infra.http.MockHttpInputMessage;
import infra.http.MockHttpOutputMessage;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 * @author Kazuki Shimizu
 * @author Brian Clozel
 */
public class ResourceHttpMessageConverterTests {

  private final ResourceHttpMessageConverter converter = new ResourceHttpMessageConverter();

  @Test
  public void canReadResource() {
    assertThat(converter.canRead(Resource.class, new MediaType("application", "octet-stream"))).isTrue();
  }

  @Test
  public void canWriteResource() {
    assertThat(converter.canWrite(Resource.class, new MediaType("application", "octet-stream"))).isTrue();
    assertThat(converter.canWrite(Resource.class, MediaType.ALL)).isTrue();
  }

  @Test
  public void shouldReadImageResource() throws IOException {
    byte[] body = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream("logo.jpg"));
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
    inputMessage.getHeaders().setContentDisposition(
            ContentDisposition.attachment().filename("yourlogo.jpg").build().toString());
    Resource actualResource = converter.read(Resource.class, inputMessage);
    assertThat(FileCopyUtils.copyToByteArray(actualResource.getInputStream())).isEqualTo(body);
    assertThat(actualResource.getName()).isEqualTo("yourlogo.jpg");
  }

  @Test
  public void shouldReadInputStreamResource() throws IOException {
    try (InputStream body = getClass().getResourceAsStream("logo.jpg")) {
      MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
      inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
      inputMessage.getHeaders().setContentDisposition(
              ContentDisposition.attachment().filename("yourlogo.jpg").build());
      inputMessage.getHeaders().setContentLength(123);
      Resource actualResource = converter.read(InputStreamResource.class, inputMessage);
      assertThat(actualResource).isInstanceOf(InputStreamResource.class);
      assertThat(actualResource.getInputStream()).isEqualTo(body);
      assertThat(actualResource.getName()).isEqualTo("yourlogo.jpg");
      assertThat(actualResource.contentLength()).isEqualTo(123);
    }
  }

  @Test
  public void shouldNotReadInputStreamResource() throws IOException {
    ResourceHttpMessageConverter noStreamConverter = new ResourceHttpMessageConverter(false);
    try (InputStream body = getClass().getResourceAsStream("logo.jpg")) {
      MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
      inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
      assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
              noStreamConverter.read(InputStreamResource.class, inputMessage));
    }
  }

  @Test
  public void shouldWriteImageResource() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    Resource body = new ClassPathResource("logo.jpg", getClass());
    converter.write(body, null, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(MediaType.IMAGE_JPEG);
    assertThat(outputMessage.getHeaders().getContentLength()).as("Invalid content-length").isEqualTo(body.getFile().length());
  }

  @Test
  public void writeByteArrayNullMediaType() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    byte[] byteArray = { 1, 2, 3 };
    Resource body = new ByteArrayResource(byteArray);
    converter.write(body, null, outputMessage);

    assertThat(Arrays.equals(byteArray, outputMessage.getBodyAsBytes())).isTrue();
  }

  @Test
  public void writeContentInputStreamThrowingNullPointerException() throws Exception {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    Resource resource = mock(Resource.class);
    InputStream in = mock(InputStream.class);
    given(resource.getInputStream()).willReturn(in);
    given(in.read(ArgumentMatchers.any())).willThrow(NullPointerException.class);
    converter.write(resource, MediaType.APPLICATION_OCTET_STREAM, outputMessage);

    assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(0);
  }

}
