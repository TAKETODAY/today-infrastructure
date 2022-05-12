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

package cn.taketoday.web.accept;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.http.MediaType;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.servlet.MockServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for {@link ContentNegotiationManagerFactoryBean} tests.
 *
 * @author Rossen Stoyanchev
 */
class ContentNegotiationManagerFactoryBeanTests {

  private ContentNegotiationManagerFactoryBean factoryBean;

  private MockHttpServletRequest servletRequest;

  MockServletRequestContext webRequest;

  @BeforeEach
  void setup() {
    TestServletContext servletContext = new TestServletContext();
    servletContext.getMimeTypes().put("foo", "application/foo");

    this.servletRequest = new MockHttpServletRequest(servletContext);
    webRequest = new MockServletRequestContext(servletRequest, null);
    this.factoryBean = new ContentNegotiationManagerFactoryBean();
  }

  @Test
  void defaultSettings() throws Exception {
    this.factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = this.factoryBean.getObject();

    this.servletRequest.setRequestURI("/flower.gif");

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .as("Should not resolve file extensions by default")
            .containsExactly(MediaType.ALL);

    this.servletRequest.setRequestURI("/flower.foobarbaz");

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .as("Should ignore unknown extensions by default")
            .isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

    this.servletRequest.setRequestURI("/flower");
    this.servletRequest.setParameter("format", "gif");

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .as("Should not resolve request parameters by default")
            .isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

    this.servletRequest.setRequestURI("/flower");
    this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

    webRequest.setRequestHeaders(null);
    assertThat(manager.resolveMediaTypes(this.webRequest))
            .as("Should resolve Accept header by default")
            .isEqualTo(Collections.singletonList(MediaType.IMAGE_GIF));
  }

  @Test
  void explicitStrategies() throws Exception {
    Map<String, MediaType> mediaTypes = Collections.singletonMap("bar", new MediaType("application", "bar"));
    ParameterContentNegotiationStrategy strategy1 = new ParameterContentNegotiationStrategy(mediaTypes);
    HeaderContentNegotiationStrategy strategy2 = new HeaderContentNegotiationStrategy();
    List<ContentNegotiationStrategy> strategies = Arrays.asList(strategy1, strategy2);
    this.factoryBean.setStrategies(strategies);
    this.factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = this.factoryBean.getObject();

    assertThat(manager.getStrategies()).isEqualTo(strategies);

    this.servletRequest.setRequestURI("/flower");
    this.servletRequest.addParameter("format", "bar");
    assertThat(manager.resolveMediaTypes(this.webRequest))
            .isEqualTo(Collections.singletonList(new MediaType("application", "bar")));

  }

  @Test
  void favorParameter() throws Exception {
    this.factoryBean.setFavorParameter(true);
    this.factoryBean.addMediaType("json", MediaType.APPLICATION_JSON);

    this.factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = this.factoryBean.getObject();

    this.servletRequest.setRequestURI("/flower");
    this.servletRequest.addParameter("format", "json");

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
  }

  @Test
    // SPR-10170
  void favorParameterWithUnknownMediaType() {
    this.factoryBean.setFavorParameter(true);
    this.factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = this.factoryBean.getObject();

    this.servletRequest.setRequestURI("/flower");
    this.servletRequest.setParameter("format", "invalid");

    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
            .isThrownBy(() -> manager.resolveMediaTypes(this.webRequest));
  }

  @Test
  @SuppressWarnings("deprecation")
  void mediaTypeMappingsWithoutPathAndParameterStrategies() {
    this.factoryBean.setFavorParameter(false);

    Properties properties = new Properties();
    properties.put("JSon", "application/json");

    this.factoryBean.setMediaTypes(properties);
    this.factoryBean.addMediaType("pdF", MediaType.APPLICATION_PDF);
    this.factoryBean.addMediaTypes(Collections.singletonMap("xML", MediaType.APPLICATION_XML));

    ContentNegotiationManager manager = this.factoryBean.build();
    assertThat(manager.getMediaTypeMappings())
            .hasSize(3)
            .containsEntry("json", MediaType.APPLICATION_JSON)
            .containsEntry("pdf", MediaType.APPLICATION_PDF)
            .containsEntry("xml", MediaType.APPLICATION_XML);
  }

  @Test
  @SuppressWarnings("deprecation")
  void fileExtensions() {
    this.factoryBean.setFavorParameter(false);

    Properties properties = new Properties();
    properties.put("json", "application/json");
    properties.put("pdf", "application/pdf");
    properties.put("xml", "application/xml");
    this.factoryBean.setMediaTypes(properties);

    this.factoryBean.addMediaType("jsON", MediaType.APPLICATION_JSON);
    this.factoryBean.addMediaType("pdF", MediaType.APPLICATION_PDF);

    this.factoryBean.addMediaTypes(Collections.singletonMap("JSon", MediaType.APPLICATION_JSON));
    this.factoryBean.addMediaTypes(Collections.singletonMap("xML", MediaType.APPLICATION_XML));

    ContentNegotiationManager manager = this.factoryBean.build();
    assertThat(manager.getAllFileExtensions()).containsExactlyInAnyOrder("json", "xml", "pdf");

  }

  @Test
  void ignoreAcceptHeader() throws Exception {
    this.factoryBean.setIgnoreAcceptHeader(true);
    this.factoryBean.setFavorParameter(true);
    this.factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = this.factoryBean.getObject();

    this.servletRequest.setRequestURI("/flower");
    this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
  }

  @Test
  void setDefaultContentType() throws Exception {
    this.factoryBean.setDefaultContentType(MediaType.APPLICATION_JSON);
    this.factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = this.factoryBean.getObject();

    assertThat(manager.resolveMediaTypes(this.webRequest).get(0)).isEqualTo(MediaType.APPLICATION_JSON);

    // SPR-10513
    this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
    assertThat(manager.resolveMediaTypes(this.webRequest).get(0)).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
    // SPR-15367
  void setDefaultContentTypes() throws Exception {
    List<MediaType> mediaTypes = Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL);
    this.factoryBean.setDefaultContentTypes(mediaTypes);
    this.factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = this.factoryBean.getObject();

    assertThat(manager.resolveMediaTypes(this.webRequest)).isEqualTo(mediaTypes);

    this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
    assertThat(manager.resolveMediaTypes(this.webRequest)).isEqualTo(mediaTypes);
  }

  @Test
    // SPR-12286
  void setDefaultContentTypeWithStrategy() throws Exception {
    this.factoryBean.setDefaultContentTypeStrategy(new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON));
    this.factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = this.factoryBean.getObject();

    assertThat(manager.resolveMediaTypes(this.webRequest))
            .isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));

    this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
    assertThat(manager.resolveMediaTypes(this.webRequest))
            .isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
  }

  private static class TestServletContext extends MockServletContext {

    private final Map<String, String> mimeTypes = new HashMap<>();

    public Map<String, String> getMimeTypes() {
      return this.mimeTypes;
    }

    @Override
    public String getMimeType(String filePath) {
      String extension = StringUtils.getFilenameExtension(filePath);
      return getMimeTypes().get(extension);
    }
  }

}
