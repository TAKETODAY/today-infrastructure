/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.config.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import infra.cache.concurrent.ConcurrentMapCache;
import infra.core.io.Resource;
import infra.http.CacheControl;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.accept.ContentNegotiationManager;
import infra.web.handler.SimpleUrlHandlerMapping;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.GenericWebApplicationContext;
import infra.web.resource.CachingResourceResolver;
import infra.web.resource.CachingResourceTransformer;
import infra.web.resource.CssLinkResourceTransformer;
import infra.web.resource.LiteWebJarsResourceResolver;
import infra.web.resource.PathResourceResolver;
import infra.web.resource.ResourceHttpRequestHandler;
import infra.web.resource.ResourceResolver;
import infra.web.resource.ResourceTransformer;
import infra.web.resource.VersionResourceResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 22:32
 */
class ResourceHandlerRegistryTests {

  private ResourceHandlerRegistry registry;

  private ResourceHandlerRegistration registration;

  private MockHttpResponseImpl response;

  @BeforeEach
  public void setup() {
    GenericWebApplicationContext appContext = new GenericWebApplicationContext();
    appContext.refresh();

    this.registry = new ResourceHandlerRegistry(appContext, new ContentNegotiationManager());

    this.registration = this.registry.addResourceHandler("/resources/**");
    this.registration.addResourceLocations("classpath:infra/web/config/");
    this.response = new MockHttpResponseImpl();
  }

  private ResourceHttpRequestHandler getHandler(String pathPattern) {
    SimpleUrlHandlerMapping hm = this.registry.getHandlerMapping();
    return (ResourceHttpRequestHandler) hm.getUrlMap().get(pathPattern);
  }

  @Test
  public void noResourceHandlers() {
    this.registry = new ResourceHandlerRegistry(new GenericWebApplicationContext());
    assertThat((Object) this.registry.getHandlerMapping()).isNull();
  }

  @Test
  public void mapPathToLocation() throws Throwable {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setMethod("GET");
    request.setRequestURI("/testStylesheet.css");

    ResourceHttpRequestHandler handler = getHandler("/resources/**");
    handler.handleRequest(new MockRequestContext(null, request, this.response));

    assertThat(this.response.getContentAsString()).isEqualTo("test stylesheet content");
  }

  @Test
  public void cachePeriod() {
    assertThat(getHandler("/resources/**").getCacheSeconds()).isEqualTo(-1);

    this.registration.setCachePeriod(0);
    assertThat(getHandler("/resources/**").getCacheSeconds()).isEqualTo(0);
  }

  @Test
  public void cacheControl() {
    assertThat(getHandler("/resources/**").getCacheControl()).isNull();

    this.registration.setCacheControl(CacheControl.noCache().cachePrivate());
    assertThat(getHandler("/resources/**").getCacheControl().getHeaderValue())
            .isEqualTo(CacheControl.noCache().cachePrivate().getHeaderValue());
  }

  @Test
  public void order() {
    assertThat(registry.getHandlerMapping().getOrder()).isEqualTo(Integer.MAX_VALUE - 1);

    registry.setOrder(0);
    assertThat(registry.getHandlerMapping().getOrder()).isEqualTo(0);
  }

  @Test
  public void hasMappingForPattern() {
    assertThat(this.registry.hasMappingForPattern("/resources/**")).isTrue();
    assertThat(this.registry.hasMappingForPattern("/whatever")).isFalse();
  }

  @Test
  public void resourceChain() {
    ResourceResolver mockResolver = mock(ResourceResolver.class);
    ResourceTransformer mockTransformer = mock(ResourceTransformer.class);
    this.registration.resourceChain(true).addResolver(mockResolver).addTransformer(mockTransformer);

    ResourceHttpRequestHandler handler = getHandler("/resources/**");
    List<ResourceResolver> resolvers = handler.getResourceResolvers();
    assertThat(resolvers).hasSize(4);
    assertThat(resolvers.get(0)).isInstanceOf(CachingResourceResolver.class);
    CachingResourceResolver cachingResolver = (CachingResourceResolver) resolvers.get(0);
    assertThat(cachingResolver.getCache()).isInstanceOf(ConcurrentMapCache.class);
    assertThat(resolvers.get(1)).isEqualTo(mockResolver);
    assertThat(resolvers.get(2)).isInstanceOf(LiteWebJarsResourceResolver.class);
    assertThat(resolvers.get(3)).isInstanceOf(PathResourceResolver.class);

    List<ResourceTransformer> transformers = handler.getResourceTransformers();
    assertThat(transformers).hasSize(2);
    assertThat(transformers.get(0)).isInstanceOf(CachingResourceTransformer.class);
    assertThat(transformers.get(1)).isEqualTo(mockTransformer);
  }

  @Test
  public void resourceChainWithoutCaching() {
    this.registration.resourceChain(false);

    ResourceHttpRequestHandler handler = getHandler("/resources/**");
    List<ResourceResolver> resolvers = handler.getResourceResolvers();
    assertThat(resolvers).hasSize(2);
    assertThat(resolvers.get(0)).isInstanceOf(LiteWebJarsResourceResolver.class);
    assertThat(resolvers.get(1)).isInstanceOf(PathResourceResolver.class);

    List<ResourceTransformer> transformers = handler.getResourceTransformers();
    assertThat(transformers).isEmpty();
  }

  @Test
  public void resourceChainWithVersionResolver() {
    VersionResourceResolver versionResolver = new VersionResourceResolver()
            .addFixedVersionStrategy("fixed", "/**/*.js")
            .addContentVersionStrategy("/**");

    this.registration.resourceChain(true).addResolver(versionResolver);

    ResourceHttpRequestHandler handler = getHandler("/resources/**");
    List<ResourceResolver> resolvers = handler.getResourceResolvers();
    assertThat(resolvers).hasSize(4);
    assertThat(resolvers.get(0)).isInstanceOf(CachingResourceResolver.class);
    assertThat(resolvers.get(1)).isSameAs(versionResolver);
    assertThat(resolvers.get(2)).isInstanceOf(LiteWebJarsResourceResolver.class);
    assertThat(resolvers.get(3)).isInstanceOf(PathResourceResolver.class);

    List<ResourceTransformer> transformers = handler.getResourceTransformers();
    assertThat(transformers).hasSize(2);
    assertThat(transformers.get(0)).isInstanceOf(CachingResourceTransformer.class);
    assertThat(transformers.get(1)).isInstanceOf(CssLinkResourceTransformer.class);
  }

  @Test
  public void resourceChainWithOverrides() {
    CachingResourceResolver cachingResolver = mock(CachingResourceResolver.class);
    VersionResourceResolver versionResolver = mock(VersionResourceResolver.class);
    LiteWebJarsResourceResolver webjarsResolver = mock(LiteWebJarsResourceResolver.class);
    PathResourceResolver pathResourceResolver = new PathResourceResolver();
    CachingResourceTransformer cachingTransformer = mock(CachingResourceTransformer.class);
    CssLinkResourceTransformer cssLinkTransformer = new CssLinkResourceTransformer();

    this.registration.setCachePeriod(3600)
            .resourceChain(false)
            .addResolver(cachingResolver)
            .addResolver(versionResolver)
            .addResolver(webjarsResolver)
            .addResolver(pathResourceResolver)
            .addTransformer(cachingTransformer)
            .addTransformer(cssLinkTransformer);

    ResourceHttpRequestHandler handler = getHandler("/resources/**");
    List<ResourceResolver> resolvers = handler.getResourceResolvers();
    assertThat(resolvers).hasSize(4);
    assertThat(resolvers.get(0)).isSameAs(cachingResolver);
    assertThat(resolvers.get(1)).isSameAs(versionResolver);
    assertThat(resolvers.get(2)).isSameAs(webjarsResolver);
    assertThat(resolvers.get(3)).isSameAs(pathResourceResolver);

    List<ResourceTransformer> transformers = handler.getResourceTransformers();
    assertThat(transformers).hasSize(2);
    assertThat(transformers.get(0)).isSameAs(cachingTransformer);
    assertThat(transformers.get(1)).isSameAs(cssLinkTransformer);
  }

  @Test
  public void urlResourceWithCharset() {
    this.registration.addResourceLocations("[charset=ISO-8859-1]file:///tmp/");
    this.registration.resourceChain(true);

    ResourceHttpRequestHandler handler = getHandler("/resources/**");

    List<ResourceResolver> resolvers = handler.getResourceResolvers();
    PathResourceResolver resolver = (PathResourceResolver) resolvers.get(resolvers.size() - 1);
    Map<Resource, Charset> locationCharsets = resolver.getLocationCharsets();
    assertThat(locationCharsets.size()).isEqualTo(1);
    assertThat(locationCharsets.values().iterator().next()).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  public void lastModifiedDisabled() {
    this.registration.setUseLastModified(false);
    ResourceHttpRequestHandler handler = getHandler("/resources/**");
    assertThat(handler.isUseLastModified()).isFalse();
  }

}
