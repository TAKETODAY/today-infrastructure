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

package infra.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.web.handler.SimpleUrlHandlerMapping;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ResourceUrlProvider}.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class ResourceUrlProviderTests {

  private final List<Resource> locations = new ArrayList<>();

  private final ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

  private final Map<String, ResourceHttpRequestHandler> handlerMap = new HashMap<>();

  private final ResourceUrlProvider urlProvider = new ResourceUrlProvider();

  @BeforeEach
  void setUp() throws Exception {
    this.locations.add(new ClassPathResource("test/", getClass()));
    this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
    this.handler.setLocations(locations);
    this.handler.afterPropertiesSet();
    this.handlerMap.put("/resources/**", this.handler);
    this.urlProvider.setHandlerMap(this.handlerMap);
  }

  @Test
  void getStaticResourceUrl() {
    String url = this.urlProvider.getForLookupPath("/resources/foo.css");
    assertThat(url).isEqualTo("/resources/foo.css");
  }

  @Test
  void getStaticResourceUrlRequestWithQueryOrHash() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setRequestURI("/");
    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    String url = "/resources/foo.css?foo=bar&url=https://example.org";
    String resolvedUrl = this.urlProvider.getForRequestUrl(requestContext, url);
    assertThat(resolvedUrl).isEqualTo("/resources/foo.css?foo=bar&url=https://example.org");

    url = "/resources/foo.css#hash";
    resolvedUrl = this.urlProvider.getForRequestUrl(requestContext, url);
    assertThat(resolvedUrl).isEqualTo("/resources/foo.css#hash");
  }

  @Test
  void getFingerprintedResourceUrl() {
    Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
    versionStrategyMap.put("/**", new ContentVersionStrategy());
    VersionResourceResolver versionResolver = new VersionResourceResolver();
    versionResolver.setStrategyMap(versionStrategyMap);

    List<ResourceResolver> resolvers = new ArrayList<>();
    resolvers.add(versionResolver);
    resolvers.add(new PathResourceResolver());
    this.handler.setResourceResolvers(resolvers);

    String url = this.urlProvider.getForLookupPath("/resources/foo.css");
    assertThat(url).isEqualTo("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
  }

  @Test

  void bestPatternMatch() throws Exception {
    ResourceHttpRequestHandler otherHandler = new ResourceHttpRequestHandler();
    otherHandler.setLocations(this.locations);
    Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
    versionStrategyMap.put("/**", new ContentVersionStrategy());
    VersionResourceResolver versionResolver = new VersionResourceResolver();
    versionResolver.setStrategyMap(versionStrategyMap);

    List<ResourceResolver> resolvers = new ArrayList<>();
    resolvers.add(versionResolver);
    resolvers.add(new PathResourceResolver());
    otherHandler.setResourceResolvers(resolvers);

    this.handlerMap.put("/resources/*.css", otherHandler);
    this.urlProvider.setHandlerMap(this.handlerMap);

    String url = this.urlProvider.getForLookupPath("/resources/foo.css");
    assertThat(url).isEqualTo("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
  }

  @Test
  @SuppressWarnings("resource")
  void initializeOnce() throws Exception {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setMockContext(new MockContextImpl());
    context.register(HandlerMappingConfiguration.class);
    context.refresh();

    ResourceUrlProvider urlProviderBean = context.getBean(ResourceUrlProvider.class);
    assertThat(urlProviderBean.getHandlerMap()).containsKey("/resources/**");
    assertThat(urlProviderBean.isAutodetect()).isFalse();
  }

  @Test
  @SuppressWarnings("resource")
  void initializeOnCurrentContext() {
    AnnotationConfigWebApplicationContext parentContext = new AnnotationConfigWebApplicationContext();
    parentContext.setMockContext(new MockContextImpl());
    parentContext.register(ParentHandlerMappingConfiguration.class);

    AnnotationConfigWebApplicationContext childContext = new AnnotationConfigWebApplicationContext();
    childContext.setParent(parentContext);
    childContext.setMockContext(new MockContextImpl());
    childContext.register(HandlerMappingConfiguration.class);

    parentContext.refresh();
    childContext.refresh();

    ResourceUrlProvider parentUrlProvider = parentContext.getBean(ResourceUrlProvider.class);
    assertThat(parentUrlProvider.getHandlerMap()).isEmpty();
    assertThat(parentUrlProvider.isAutodetect()).isTrue();
    ResourceUrlProvider childUrlProvider = childContext.getBean(ResourceUrlProvider.class);
    assertThat(childUrlProvider.getHandlerMap()).containsOnlyKeys("/resources/**");
    assertThat(childUrlProvider.isAutodetect()).isFalse();
  }

  @Test

  void getForLookupPathShouldNotFailIfPathContainsDoubleSlashes() {
    // given
    ResourceResolver mockResourceResolver = mock(ResourceResolver.class);
    given(mockResourceResolver.resolveUrlPath(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn("some-path");

    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.getResourceResolvers().add(mockResourceResolver);

    ResourceUrlProvider provider = new ResourceUrlProvider();
    provider.getHandlerMap().put("/some-pattern/**", handler);

    // when
    String lookupForPath = provider.getForLookupPath("/some-pattern/some-lib//some-resource");

    // then
    assertThat(lookupForPath).isEqualTo("/some-pattern/some-path");
  }

  @Configuration
  @SuppressWarnings({ "unused", "WeakerAccess" })
  static class HandlerMappingConfiguration {

    @Bean
    public SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
      return new SimpleUrlHandlerMapping(
              Collections.singletonMap("/resources/**", new ResourceHttpRequestHandler()));
    }

    @Bean
    public ResourceUrlProvider resourceUrlProvider() {
      return new ResourceUrlProvider();
    }
  }

  @Configuration
  @SuppressWarnings({ "unused", "WeakerAccess" })
  static class ParentHandlerMappingConfiguration {

    @Bean
    public ResourceUrlProvider resourceUrlProvider() {
      return new ResourceUrlProvider();
    }
  }

}
