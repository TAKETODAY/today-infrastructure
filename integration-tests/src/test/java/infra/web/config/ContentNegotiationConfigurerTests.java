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

package infra.web.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.RequestContext;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.ContentNegotiationStrategy;
import infra.web.accept.FixedContentNegotiationStrategy;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 16:14
 */
class ContentNegotiationConfigurerTests {

  private ContentNegotiationConfigurer configurer;

  private RequestContext webRequest;

  private HttpMockRequestImpl mockRequest;

  @BeforeEach
  public void setup() {
    this.mockRequest = new HttpMockRequestImpl();
    this.webRequest = new MockRequestContext(this.mockRequest, null);
    this.configurer = new ContentNegotiationConfigurer();
  }

  @Test
  public void defaultSettings() throws Exception {
    ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

    this.mockRequest.setRequestURI("/flower.gif");

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .as("Should not resolve file extensions by default")
            .containsExactly(MediaType.ALL);

    this.mockRequest.setRequestURI("/flower?format=gif");
    this.mockRequest.addParameter("format", "gif");

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .as("Should not resolve request parameters by default")
            .isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

    this.mockRequest.setRequestURI("/flower");

    webRequest.requestHeaders().add("Accept", MediaType.IMAGE_GIF_VALUE);

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .as("Should resolve Accept header by default")
            .containsExactly(MediaType.IMAGE_GIF);
  }

  @Test
  public void addMediaTypes() throws Exception {
    this.configurer.favorParameter(true);
    this.configurer.mediaTypes(Collections.singletonMap("json", MediaType.APPLICATION_JSON));
    ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

    this.mockRequest.setRequestURI("/flower");
    this.mockRequest.addParameter("format", "json");
    assertThat(manager.resolveMediaTypes(this.webRequest)).containsExactly(MediaType.APPLICATION_JSON);
  }

  @Test
  public void favorParameter() throws Exception {
    this.configurer.favorParameter(true);
    this.configurer.parameterName("f");
    this.configurer.mediaTypes(Collections.singletonMap("json", MediaType.APPLICATION_JSON));
    ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

    this.mockRequest.setRequestURI("/flower");
    this.mockRequest.addParameter("f", "json");

    assertThat(manager.resolveMediaTypes(this.webRequest).get(0)).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  public void ignoreAcceptHeader() throws Exception {
    this.configurer.ignoreAcceptHeader(true);
    this.configurer.favorParameter(true);
    ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

    this.mockRequest.setRequestURI("/flower");
    this.mockRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

    assertThat(manager.resolveMediaTypes(this.webRequest)).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
  }

  @Test
  public void setDefaultContentType() throws Exception {
    this.configurer.defaultContentType(MediaType.APPLICATION_JSON);
    ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

    assertThat(manager.resolveMediaTypes(this.webRequest).get(0)).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  public void setMultipleDefaultContentTypes() throws Exception {
    this.configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
    ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

    assertThat(manager.resolveMediaTypes(this.webRequest)).isEqualTo(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL));
  }

  @Test
  public void setDefaultContentTypeStrategy() throws Exception {
    this.configurer.defaultContentTypeStrategy(new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON));
    ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

    assertThat(manager.resolveMediaTypes(this.webRequest).get(0)).isEqualTo(MediaType.APPLICATION_JSON);
  }

}
