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

package cn.taketoday.web;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.FieldError;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MissingMatrixVariableException;
import cn.taketoday.web.bind.MissingPathVariableException;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.UnsatisfiedRequestParameterException;
import cn.taketoday.web.bind.resolver.MissingRequestCookieException;
import cn.taketoday.web.bind.resolver.MissingRequestHeaderException;
import cn.taketoday.web.bind.resolver.MissingRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 15:56
 */
class ErrorResponseExceptionTests {

  private final MethodParameter methodParameter =
          new MethodParameter(ResolvableMethod.on(getClass()).resolveMethod("handle"), 0);

  @Test
  void httpMediaTypeNotSupportedException() {

    List<MediaType> mediaTypes =
            Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_CBOR);

    ErrorResponse ex = new HttpMediaTypeNotSupportedException(
            MediaType.APPLICATION_XML, mediaTypes, HttpMethod.PATCH, "Custom message");

    assertStatus(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertDetail(ex, "Content-Type 'application/xml' is not supported.");

    HttpHeaders headers = ex.getHeaders();
    assertThat(headers.getAccept()).isEqualTo(mediaTypes);
    assertThat(headers.getAcceptPatch()).isEqualTo(mediaTypes);
  }

  @Test
  void httpMediaTypeNotSupportedExceptionWithParseError() {

    ErrorResponse ex = new HttpMediaTypeNotSupportedException(
            "Could not parse Accept header: Invalid mime type \"foo\": does not contain '/'");

    assertStatus(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertDetail(ex, "Could not parse Content-Type.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void httpMediaTypeNotAcceptableException() {

    List<MediaType> mediaTypes = Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_CBOR);
    ErrorResponse ex = new HttpMediaTypeNotAcceptableException(mediaTypes);

    assertStatus(ex, HttpStatus.NOT_ACCEPTABLE);
    assertDetail(ex, "Acceptable representations: 'application/json, application/cbor'.");

    assertThat(ex.getHeaders()).hasSize(1);
    assertThat(ex.getHeaders().getAccept()).isEqualTo(mediaTypes);
  }

  @Test
  void httpMediaTypeNotAcceptableExceptionWithParseError() {

    ErrorResponse ex = new HttpMediaTypeNotAcceptableException(
            "Could not parse Accept header: Invalid mime type \"foo\": does not contain '/'");

    assertStatus(ex, HttpStatus.NOT_ACCEPTABLE);
    assertDetail(ex, "Could not parse Accept header.");
    assertThat(ex.getHeaders()).isEmpty();
  }
//
//  @Test
//  void asyncRequestTimeoutException() {
//
//    ErrorResponse ex = new AsyncRequestTimeoutException();
//
//    assertStatus(ex, HttpStatus.SERVICE_UNAVAILABLE);
//    assertDetail(ex, null);
//    assertThat(ex.getHeaders()).isEmpty();
//  }

  @Test
  void httpRequestMethodNotSupportedException() {

    String[] supportedMethods = new String[] { "GET", "POST" };
    ErrorResponse ex = new HttpRequestMethodNotSupportedException("PUT", supportedMethods, "Custom message");

    assertStatus(ex, HttpStatus.METHOD_NOT_ALLOWED);
    assertDetail(ex, "Method 'PUT' is not supported.");

    assertThat(ex.getHeaders()).hasSize(1);
    assertThat(ex.getHeaders().getAllow()).containsExactly(HttpMethod.GET, HttpMethod.POST);
  }

  @Test
  void missingRequestHeaderException() {

    ErrorResponse ex = new MissingRequestHeaderException("Authorization", this.methodParameter);

    assertStatus(ex, HttpStatus.BAD_REQUEST);
    assertDetail(ex, "Required header 'Authorization' is not present.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void missingMockRequestParameterException() {

    ErrorResponse ex = new MissingRequestParameterException("query", "String");

    assertStatus(ex, HttpStatus.BAD_REQUEST);
    assertDetail(ex, "Required parameter 'query' is not present.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void missingMatrixVariableException() {

    ErrorResponse ex = new MissingMatrixVariableException("region", this.methodParameter);

    assertStatus(ex, HttpStatus.BAD_REQUEST);
    assertDetail(ex, "Required path parameter 'region' is not present.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void missingPathVariableException() {

    ErrorResponse ex = new MissingPathVariableException("id", this.methodParameter);

    assertStatus(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    assertDetail(ex, "Required path variable 'id' is not present.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void missingRequestCookieException() {

    ErrorResponse ex = new MissingRequestCookieException("oreo", this.methodParameter);

    assertStatus(ex, HttpStatus.BAD_REQUEST);
    assertDetail(ex, "Required cookie 'oreo' is not present.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void unsatisfiedMockRequestParameterException() {
    LinkedMultiValueMap<String, String> copied = MultiValueMap.copyOf(Collections.singletonMap("q", List.of("1")));
    ErrorResponse ex = new UnsatisfiedRequestParameterException(
            new String[] { "foo=bar", "bar=baz" }, copied);

    assertStatus(ex, HttpStatus.BAD_REQUEST);
    assertDetail(ex, "Invalid request parameters.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void missingMockRequestPartException() {

    ErrorResponse ex = new MissingRequestPartException("file");

    assertStatus(ex, HttpStatus.BAD_REQUEST);
    assertDetail(ex, "Required part 'file' is not present.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void methodArgumentNotValidException() {

    BindingResult bindingResult = new BindException(new Object(), "object");
    bindingResult.addError(new FieldError("object", "field", "message"));

    ErrorResponse ex = new MethodArgumentNotValidException(this.methodParameter, bindingResult);

    assertStatus(ex, HttpStatus.BAD_REQUEST);
    assertDetail(ex, "Invalid request content.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void unsupportedMediaTypeStatusException() {

    List<MediaType> mediaTypes =
            Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_CBOR);

    ErrorResponse ex = new UnsupportedMediaTypeStatusException(
            MediaType.APPLICATION_XML, mediaTypes, HttpMethod.PATCH);

    assertStatus(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertDetail(ex, "Content-Type 'application/xml' is not supported.");

    HttpHeaders headers = ex.getHeaders();
    assertThat(headers.getAccept()).isEqualTo(mediaTypes);
    assertThat(headers.getAcceptPatch()).isEqualTo(mediaTypes);
  }

  @Test
  void unsupportedMediaTypeStatusExceptionWithParseError() {

    ErrorResponse ex = new UnsupportedMediaTypeStatusException(
            "Could not parse Accept header: Invalid mime type \"foo\": does not contain '/'");

    assertStatus(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertDetail(ex, "Could not parse Content-Type.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  @Test
  void notAcceptableStatusException() {

    List<MediaType> mediaTypes =
            Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_CBOR);

    ErrorResponse ex = new NotAcceptableStatusException(mediaTypes);

    assertStatus(ex, HttpStatus.NOT_ACCEPTABLE);
    assertDetail(ex, "Acceptable representations: 'application/json, application/cbor'.");

    assertThat(ex.getHeaders()).hasSize(1);
    assertThat(ex.getHeaders().getAccept()).isEqualTo(mediaTypes);
  }

  @Test
  void notAcceptableStatusExceptionWithParseError() {

    ErrorResponse ex = new NotAcceptableStatusException(
            "Could not parse Accept header: Invalid mime type \"foo\": does not contain '/'");

    assertStatus(ex, HttpStatus.NOT_ACCEPTABLE);
    assertDetail(ex, "Could not parse Accept header.");
    assertThat(ex.getHeaders()).isEmpty();
  }

//	@Test
//	void webExchangeBindException() {
//
//		BindingResult bindingResult = new BindException(new Object(), "object");
//		bindingResult.addError(new FieldError("object", "field", "message"));
//
//		ErrorResponse ex = new WebExchangeBindException(this.methodParameter, bindingResult);
//
//		assertStatus(ex, HttpStatus.BAD_REQUEST);
//		assertDetail(ex, "Invalid request content.");
//		assertThat(ex.getHeaders()).isEmpty();
//	}

  @Test
  void methodNotAllowedException() {

    List<HttpMethod> supportedMethods = Arrays.asList(HttpMethod.GET, HttpMethod.POST);
    ErrorResponse ex = new MethodNotAllowedException(HttpMethod.PUT, supportedMethods);

    assertStatus(ex, HttpStatus.METHOD_NOT_ALLOWED);
    assertDetail(ex, "Supported methods: 'GET', 'POST'");

    assertThat(ex.getHeaders()).hasSize(1);
    assertThat(ex.getHeaders().getAllow()).containsExactly(HttpMethod.GET, HttpMethod.POST);
  }

  @Test
  void methodNotAllowedExceptionWithoutSupportedMethods() {

    ErrorResponse ex = new MethodNotAllowedException(HttpMethod.PUT, Collections.emptyList());

    assertStatus(ex, HttpStatus.METHOD_NOT_ALLOWED);
    assertDetail(ex, "Request method 'PUT' is not supported.");
    assertThat(ex.getHeaders()).isEmpty();
  }

  private void assertStatus(ErrorResponse ex, HttpStatus status) {
    ProblemDetail body = ex.getBody();
    assertThat(ex.getStatusCode()).isEqualTo(status);
    assertThat(body.getStatus()).isEqualTo(status.value());
    assertThat(body.getTitle()).isEqualTo(status.getReasonPhrase());
  }

  private void assertDetail(ErrorResponse ex, @Nullable String detail) {
    if (detail != null) {
      assertThat(ex.getBody().getDetail()).isEqualTo(detail);
    }
    else {
      assertThat(ex.getBody().getDetail()).isNull();
    }
  }

  @SuppressWarnings("unused")
  private void handle(String arg) { }

}
