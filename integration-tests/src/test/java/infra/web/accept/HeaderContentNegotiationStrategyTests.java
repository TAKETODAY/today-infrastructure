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

package infra.web.accept;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for HeaderContentNegotiationStrategy tests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class HeaderContentNegotiationStrategyTests {

  private final HeaderContentNegotiationStrategy strategy = new HeaderContentNegotiationStrategy();

  private final HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();

  final RequestContext context = new MockRequestContext(null, mockRequest, null);

  @Test
  public void resolveMediaTypes() throws Exception {
    this.mockRequest.addHeader("Accept", "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
    List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.context);

    assertThat(mediaTypes.size()).isEqualTo(4);
    assertThat(mediaTypes.get(0).toString()).isEqualTo("text/html");
    assertThat(mediaTypes.get(1).toString()).isEqualTo("text/x-c");
    assertThat(mediaTypes.get(2).toString()).isEqualTo("text/x-dvi;q=0.8");
    assertThat(mediaTypes.get(3).toString()).isEqualTo("text/plain;q=0.5");
  }

  @Test  // SPR-14506
  public void resolveMediaTypesFromMultipleHeaderValues() throws Exception {
    this.mockRequest.addHeader("Accept", "text/plain; q=0.5, text/html");
    this.mockRequest.addHeader("Accept", "text/x-dvi; q=0.8, text/x-c");
    List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.context);

    assertThat(mediaTypes.size()).isEqualTo(4);
    assertThat(mediaTypes.get(0).toString()).isEqualTo("text/html");
    assertThat(mediaTypes.get(1).toString()).isEqualTo("text/x-c");
    assertThat(mediaTypes.get(2).toString()).isEqualTo("text/x-dvi;q=0.8");
    assertThat(mediaTypes.get(3).toString()).isEqualTo("text/plain;q=0.5");
  }

  @Test
  public void resolveMediaTypesParseError() throws Exception {
    this.mockRequest.addHeader("Accept", "textplain; q=0.5");
    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
            this.strategy.resolveMediaTypes(this.context));
  }

  @Test
  void resolveMediaTypesWithMaxElements() throws Exception {
    String acceptHeaderValue = "text/plain, text/html,".repeat(25);
    this.mockRequest.addHeader("Accept", acceptHeaderValue);
    List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.context);

    assertThat(mediaTypes).hasSize(50);
    assertThat(mediaTypes.stream().map(Object::toString).distinct())
            .containsExactly("text/plain", "text/html");
  }

  @Test
  void resolveMediaTypesWithTooManyElements() {
    String acceptHeaderValue = "text/plain,".repeat(51);
    this.mockRequest.addHeader("Accept", acceptHeaderValue);
    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
            .isThrownBy(() -> this.strategy.resolveMediaTypes(this.context))
            .withMessageStartingWith("Could not parse 'Accept' header")
            .withMessageEndingWith("Too many elements");
  }

}
