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

package infra.web.server.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import infra.http.HttpHeaders;
import infra.util.MultiValueMap;
import infra.web.multipart.MaxUploadSizeExceededException;
import infra.web.multipart.NotMultipartRequestException;
import infra.web.multipart.Part;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 21:59
 */
class NettyMultipartRequestTests {

  @Test
  void shouldParseRequestWithFileUploadsAndFormData() throws IOException {
    // given
    NettyRequestContext context = mock(NettyRequestContext.class);
    HttpPostRequestDecoder requestDecoder = mock(HttpPostRequestDecoder.class);
    when(context.requestDecoder()).thenReturn(requestDecoder);

    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.getName()).thenReturn("fileParam");

    Attribute attribute = mock(Attribute.class);
    when(attribute.getName()).thenReturn("formParam");
    when(attribute.getValue()).thenReturn("formValue");

    when(requestDecoder.getBodyHttpDatas()).thenReturn(List.of(fileUpload, attribute));

    NettyMultipartRequest multipartRequest = new NettyMultipartRequest(context);

    // when
    MultiValueMap<String, Part> result = multipartRequest.parseRequest();

    // then
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.containsKey("fileParam")).isTrue();
    assertThat(result.containsKey("formParam")).isTrue();

    Part filePart = result.getFirst("fileParam");
    Part formPart = result.getFirst("formParam");

    assertThat(filePart).isInstanceOf(NettyMultipartFile.class);
    assertThat(formPart).isInstanceOf(NettyFormField.class);
  }

  @Test
  void shouldHandleEmptyBodyHttpDatas() {
    // given
    NettyRequestContext context = mock(NettyRequestContext.class);
    HttpPostRequestDecoder requestDecoder = mock(HttpPostRequestDecoder.class);
    when(context.requestDecoder()).thenReturn(requestDecoder);
    when(requestDecoder.getBodyHttpDatas()).thenReturn(List.of());

    NettyMultipartRequest multipartRequest = new NettyMultipartRequest(context);

    // when
    MultiValueMap<String, Part> result = multipartRequest.parseRequest();

    // then
    assertThat(result).isNotNull();
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void shouldThrowMaxUploadSizeExceededExceptionWhenTooLongFormField() {
    // given
    NettyRequestContext context = mock(NettyRequestContext.class);
    HttpPostRequestDecoder requestDecoder = mock(HttpPostRequestDecoder.class);
    when(context.requestDecoder()).thenReturn(requestDecoder);
    when(requestDecoder.getBodyHttpDatas()).thenThrow(new HttpPostRequestDecoder.TooLongFormFieldException());

    NettyMultipartRequest multipartRequest = new NettyMultipartRequest(context);

    // when & then
    assertThatThrownBy(multipartRequest::parseRequest)
            .isInstanceOf(MaxUploadSizeExceededException.class);
  }

  @Test
  void shouldThrowNotMultipartRequestExceptionWhenNotEnoughData() {
    // given
    NettyRequestContext context = mock(NettyRequestContext.class);
    HttpPostRequestDecoder requestDecoder = mock(HttpPostRequestDecoder.class);
    when(context.requestDecoder()).thenReturn(requestDecoder);
    when(requestDecoder.getBodyHttpDatas()).thenThrow(new HttpPostRequestDecoder.NotEnoughDataDecoderException());

    NettyMultipartRequest multipartRequest = new NettyMultipartRequest(context);

    // when & then
    assertThatThrownBy(multipartRequest::parseRequest)
            .isInstanceOf(NotMultipartRequestException.class);
  }

  @Test
  void shouldReturnMultipartHeadersForFileUpload() {
    // given
    NettyRequestContext context = mock(NettyRequestContext.class);
    HttpPostRequestDecoder requestDecoder = mock(HttpPostRequestDecoder.class);
    when(context.requestDecoder()).thenReturn(requestDecoder);

    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.getContentType()).thenReturn("image/png");

    when(requestDecoder.getBodyHttpDatas("testFile")).thenReturn(List.of(fileUpload));

    NettyMultipartRequest multipartRequest = new NettyMultipartRequest(context);

    // when
    HttpHeaders headers = multipartRequest.getMultipartHeaders("testFile");

    // then
    assertThat(headers).isNotNull();
    assertThat(headers.getContentType().toString()).isEqualTo("image/png");
  }

  @Test
  void shouldReturnNullMultipartHeadersWhenNoBodyHttpDatas() {
    // given
    NettyRequestContext context = mock(NettyRequestContext.class);
    HttpPostRequestDecoder requestDecoder = mock(HttpPostRequestDecoder.class);
    when(context.requestDecoder()).thenReturn(requestDecoder);
    when(requestDecoder.getBodyHttpDatas("nonExistent")).thenReturn(null);

    NettyMultipartRequest multipartRequest = new NettyMultipartRequest(context);

    // when
    HttpHeaders headers = multipartRequest.getMultipartHeaders("nonExistent");

    // then
    assertThat(headers).isNull();
  }

  @Test
  void shouldReturnNullMultipartHeadersWhenNoFileUploadInBodyHttpDatas() {
    // given
    NettyRequestContext context = mock(NettyRequestContext.class);
    HttpPostRequestDecoder requestDecoder = mock(HttpPostRequestDecoder.class);
    when(context.requestDecoder()).thenReturn(requestDecoder);

    Attribute attribute = mock(Attribute.class);
    when(requestDecoder.getBodyHttpDatas("formField")).thenReturn(List.of(attribute));

    NettyMultipartRequest multipartRequest = new NettyMultipartRequest(context);

    // when
    HttpHeaders headers = multipartRequest.getMultipartHeaders("formField");

    // then
    assertThat(headers).isNotNull();
    assertThat(headers.getContentType()).isNull();
  }

}