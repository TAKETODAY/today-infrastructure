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

package infra.web.config.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.RequestContext;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.ContentNegotiationStrategy;
import infra.web.accept.FixedContentNegotiationStrategy;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

  @Test
  void strategiesMethodSetsCustomStrategies() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();
    ContentNegotiationStrategy strategy1 = mock(ContentNegotiationStrategy.class);
    ContentNegotiationStrategy strategy2 = mock(ContentNegotiationStrategy.class);
    List<ContentNegotiationStrategy> strategies = Arrays.asList(strategy1, strategy2);

    configurer.strategies(strategies);

    ContentNegotiationManager manager = configurer.buildContentNegotiationManager();

    assertThat(manager).isNotNull();
  }

  @Test
  void favorParameterEnablesParameterStrategy() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    ContentNegotiationConfigurer result = configurer.favorParameter(true);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void parameterNameSetsCustomParameterName() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();
    String parameterName = "customFormat";

    ContentNegotiationConfigurer result = configurer.parameterName(parameterName);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void mediaTypeAddsSingleMediaTypeMapping() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    ContentNegotiationConfigurer result = configurer.mediaType("json", MediaType.APPLICATION_JSON);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.mediaTypes).containsEntry("json", MediaType.APPLICATION_JSON);
  }

  @Test
  void mediaTypesAddsMultipleMediaTypeMappings() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();
    Map<String, MediaType> mediaTypes = Map.of("json", MediaType.APPLICATION_JSON, "xml", MediaType.APPLICATION_XML);

    ContentNegotiationConfigurer result = configurer.mediaTypes(mediaTypes);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.mediaTypes).containsEntry("json", MediaType.APPLICATION_JSON)
            .containsEntry("xml", MediaType.APPLICATION_XML);
  }

  @Test
  void mediaTypesWithNullMap() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    ContentNegotiationConfigurer result = configurer.mediaTypes(null);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.mediaTypes).isEmpty();
  }

  @Test
  void replaceMediaTypesClearsAndAddsNewMappings() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();
    configurer.mediaTypes(Map.of("old", MediaType.TEXT_PLAIN));
    Map<String, MediaType> newMediaTypes = Map.of("json", MediaType.APPLICATION_JSON, "xml", MediaType.APPLICATION_XML);

    ContentNegotiationConfigurer result = configurer.replaceMediaTypes(newMediaTypes);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.mediaTypes).doesNotContainKey("old")
            .containsEntry("json", MediaType.APPLICATION_JSON)
            .containsEntry("xml", MediaType.APPLICATION_XML);
  }

  @Test
  void useRegisteredExtensionsOnlySetsFlag() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    ContentNegotiationConfigurer result = configurer.useRegisteredExtensionsOnly(true);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void ignoreAcceptHeaderDisablesHeaderStrategy() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    ContentNegotiationConfigurer result = configurer.ignoreAcceptHeader(true);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void defaultContentTypeSetsSingleMediaType() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    ContentNegotiationConfigurer result = configurer.defaultContentType(MediaType.APPLICATION_JSON);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void defaultContentTypeSetsMultipleMediaTypes() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    ContentNegotiationConfigurer result = configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void defaultContentTypeStrategySetsCustomStrategy() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();
    ContentNegotiationStrategy strategy = mock(ContentNegotiationStrategy.class);

    ContentNegotiationConfigurer result = configurer.defaultContentTypeStrategy(strategy);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void buildContentNegotiationManagerWithMediaTypes() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();
    configurer.mediaTypes(Map.of("json", MediaType.APPLICATION_JSON));

    ContentNegotiationManager manager = configurer.buildContentNegotiationManager();

    assertThat(manager).isNotNull();
  }

  @Test
  void chainMethodsReturnSameInstance() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    ContentNegotiationConfigurer result1 = configurer.favorParameter(true);
    ContentNegotiationConfigurer result2 = result1.parameterName("f");
    ContentNegotiationConfigurer result3 = result2.mediaType("json", MediaType.APPLICATION_JSON);
    ContentNegotiationConfigurer result4 = result3.ignoreAcceptHeader(true);
    ContentNegotiationConfigurer result5 = result4.defaultContentType(MediaType.APPLICATION_JSON);

    assertThat(result1).isSameAs(configurer);
    assertThat(result2).isSameAs(configurer);
    assertThat(result3).isSameAs(configurer);
    assertThat(result4).isSameAs(configurer);
    assertThat(result5).isSameAs(configurer);
  }

  @Test
  void defaultConstructorInitializesMediaTypesMap() {
    ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();

    assertThat(configurer.mediaTypes).isNotNull();
    assertThat(configurer.mediaTypes).isEmpty();
  }

}
