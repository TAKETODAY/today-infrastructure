/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import infra.cache.CacheManager;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.context.support.GenericApplicationContext;
import infra.core.io.ByteArrayResource;
import infra.core.io.FileSystemResource;
import infra.core.io.Resource;
import infra.core.io.UrlResource;
import infra.http.HttpHeaders;
import infra.http.converter.ResourceHttpMessageConverter;
import infra.http.converter.ResourceRegionHttpMessageConverter;
import infra.http.server.RequestPath;
import infra.util.PathMatcher;
import infra.web.ServerHttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ResourceResolutionConfigurationTests {

  @Test
  void resolvesNamedCacheAndRejectsAnUnknownCache() {
    CacheManager manager = mock(CacheManager.class);
    var cache = new ConcurrentMapCache("assets");
    given(manager.getCache("assets")).willReturn(cache);

    assertThat(new CachingResourceResolver(manager, "assets").getCache()).isSameAs(cache);
    assertThatIllegalArgumentException().isThrownBy(() -> new CachingResourceResolver(manager, "missing"))
            .withMessageContaining("missing");
  }

  @Test
  void cachesOnlyConfiguredEncodingsSeparately() {
    CachingResourceResolver resolver = new CachingResourceResolver(new ConcurrentMapCache("assets"));
    resolver.setContentCodings(List.of("deflate"));
    assertThat(resolver.getContentCodings()).containsExactly("deflate");
    assertThatThrownBy(() -> resolver.getContentCodings().add("gzip"))
            .isInstanceOf(UnsupportedOperationException.class);
    assertThatIllegalArgumentException().isThrownBy(() -> resolver.setContentCodings(List.of()));

    Resource plain = new ByteArrayResource(new byte[] { 1 });
    Resource compressed = new ByteArrayResource(new byte[] { 2 });
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    List<Resource> locations = List.of();
    ServerHttpExchange deflate = requestAccepting("deflate");
    ServerHttpExchange gzip = requestAccepting("gzip");
    given(chain.resolveResource(null, "site.css", locations)).willReturn(plain);
    given(chain.resolveResource(deflate, "site.css", locations)).willReturn(compressed);

    assertThat(resolver.resolveResource(null, "site.css", locations, chain)).isSameAs(plain);
    assertThat(resolver.resolveResource(deflate, "site.css", locations, chain)).isSameAs(compressed);
    assertThat(resolver.resolveResource(gzip, "site.css", locations, chain)).isSameAs(plain);
  }

  @Test
  void servesEncodedBytesWhileLeavingTheOriginalReaderAvailable(@TempDir Path directory) throws IOException {
    Path originalPath = Files.writeString(directory.resolve("site.css"), "plain text");
    Path encodedPath = Files.writeString(directory.resolve("site.css.test"), "encoded data");
    Resource original = new FileSystemResource(originalPath);
    ServerHttpExchange request = requestAccepting("test");
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    given(chain.resolveResource(request, "site.css", List.of())).willReturn(original);
    EncodedResourceResolver resolver = new EncodedResourceResolver();
    resolver.setContentCodings(List.of("test"));
    resolver.setExtensions(Map.of("test", "test"));

    Resource encoded = resolver.resolveResource(request, "site.css", List.of(), chain);
    assertThat(encoded).isInstanceOf(HttpResource.class);
    assertThat(encoded.getName()).isEqualTo("site.css");
    assertThat(encoded.getURI()).isEqualTo(encodedPath.toUri());
    assertThat(encoded.getFilePath()).isEqualTo(encodedPath);
    assertThat(encoded.isOpen()).isFalse();
    assertThat(encoded.getContentAsByteArray()).isEqualTo("encoded data".getBytes(StandardCharsets.UTF_8));
    assertThat(encoded.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("encoded data");
    try (var stream = encoded.getInputStream()) {
      assertThat(stream.read()).isEqualTo('e');
    }
    try (var channel = encoded.readableChannel()) {
      assertThat(channel.isOpen()).isTrue();
    }
    try (var reader = encoded.getReader()) {
      assertThat(reader.read()).isEqualTo('p');
    }
    try (var reader = encoded.getReader(StandardCharsets.UTF_8)) {
      assertThat(reader.read()).isEqualTo('p');
    }
    assertThat(encoded.createRelative("site.css").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("plain text");
    assertThat(((HttpResource) encoded).getResponseHeaders().getFirst(HttpHeaders.CONTENT_ENCODING)).isEqualTo("test");
  }

  @Test
  void fallsBackToOriginalWhenNoEncodedVariantCanBeOpened() throws IOException {
    Resource original = mock(Resource.class);
    given(original.getName()).willReturn("site.css");
    given(original.createRelative("site.css.gz")).willThrow(new IOException("unavailable variant"));
    ServerHttpExchange request = requestAccepting("gzip");
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    given(chain.resolveResource(request, "site.css", List.of())).willReturn(original);

    assertThat(new EncodedResourceResolver().resolveResource(request, "site.css", List.of(), chain))
            .isSameAs(original);
  }

  @Test
  void appliesConfiguredLocationCharsetAndKeepsCustomConverters(@TempDir Path directory) throws Exception {
    String location = directory.toUri().toString();
    var resourceConverter = new ResourceHttpMessageConverter();
    var regionConverter = new ResourceRegionHttpMessageConverter();
    var resolver = new PathResourceResolver();
    try (var application = new GenericApplicationContext()) {
      application.refresh();
      var handler = new ResourceHttpRequestHandler();
      handler.setApplicationContext(application);
      handler.setLocationValues(List.of("${assets}"));
      handler.setEmbeddedValueResolver(value -> "[charset=ISO-8859-1]" + location);
      handler.setResourceResolvers(List.of(resolver));
      handler.setResourceHttpMessageConverter(resourceConverter);
      handler.setResourceRegionHttpMessageConverter(regionConverter);

      handler.afterPropertiesSet();

      var expected = new UrlResource(directory.toUri().toURL());
      assertThat(handler.getLocations()).containsExactly(expected);
      assertThat(resolver.getAllowedLocations()).containsExactly(expected);
      assertThat(resolver.getLocationCharsets()).containsEntry(expected, StandardCharsets.ISO_8859_1);
      assertThat(handler.getResourceHttpMessageConverter()).isSameAs(resourceConverter);
      assertThat(handler.getResourceRegionHttpMessageConverter()).isSameAs(regionConverter);
    }
  }

  @Test
  void rejectsUnresolvedLocationsAndInvalidCharsetConfiguration() {
    try (var application = new GenericApplicationContext()) {
      application.refresh();
      var handler = new ResourceHttpRequestHandler();
      handler.setApplicationContext(application);
      handler.setLocationValues(List.of("${assets}"));
      handler.setEmbeddedValueResolver(value -> null);
      assertThatIllegalArgumentException().isThrownBy(handler::afterPropertiesSet)
              .withMessageContaining("Location resolved to null");

      handler.setEmbeddedValueResolver(null);
      handler.setLocationValues(List.of("[charset=UTF-8"));
      assertThatIllegalArgumentException().isThrownBy(handler::afterPropertiesSet)
              .withMessageContaining("Invalid charset syntax");

      handler.setLocationValues(List.of("[charset=UTF-8]classpath:/assets/"));
      assertThatIllegalArgumentException().isThrownBy(handler::afterPropertiesSet)
              .withMessageContaining("Unexpected charset for non-UrlResource");
    }
  }

  @Test
  void versionedResourceKeepsOriginalMetadataAndContent(@TempDir Path directory) throws IOException {
    Path file = Files.writeString(directory.resolve("site.css"), "body {}");
    Resource original = new FileSystemResource(file);
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    given(chain.resolveResource(null, "/site.css", List.of())).willReturn(original);
    var resolver = new VersionResourceResolver().addFixedVersionStrategy("v1", "/**");

    Resource versioned = resolver.resolveResource(null, "v1/site.css", List.of(), chain);

    assertThat(versioned).isInstanceOf(HttpResource.class);
    assertThat(((HttpResource) versioned).getResponseHeaders().getETag()).isEqualTo("W/\"v1\"");
    assertThat(versioned.exists()).isTrue();
    assertThat(versioned.isReadable()).isTrue();
    assertThat(versioned.isOpen()).isFalse();
    assertThat(versioned.getURL()).isEqualTo(original.getURL());
    assertThat(versioned.getURI()).isEqualTo(file.toUri());
    assertThat(versioned.getFilePath()).isEqualTo(file);
    assertThat(versioned.getContentAsByteArray()).isEqualTo("body {}".getBytes(StandardCharsets.UTF_8));
    assertThat(versioned.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("body {}");
    try (var stream = versioned.getInputStream()) {
      assertThat(stream.read()).isEqualTo('b');
    }
    try (var channel = versioned.readableChannel()) {
      assertThat(channel.isOpen()).isTrue();
    }
    try (var reader = versioned.getReader()) {
      assertThat(reader.read()).isEqualTo('b');
    }
    try (var reader = versioned.getReader(StandardCharsets.UTF_8)) {
      assertThat(reader.read()).isEqualTo('b');
    }
  }

  @Test
  void usesCustomPathMatcherForResourceMappings() {
    var provider = new ResourceUrlProvider();
    PathMatcher matcher = mock(PathMatcher.class);
    String path = "/assets/logo.png";
    String pattern = "/assets/**";
    given(matcher.match(pattern, path)).willReturn(true);
    given(matcher.extractPathWithinPattern(pattern, path)).willReturn("logo.png");

    var handler = new ResourceHttpRequestHandler();
    ResourceResolver resolver = mock(ResourceResolver.class);
    given(resolver.resolveUrlPath(eq("logo.png"), eq(handler.getLocations()), any(ResourceResolvingChain.class)))
            .willReturn("logo.v1.png");
    handler.setResourceResolvers(List.of(resolver));
    provider.setHandlerMap(Map.of(pattern, handler));
    provider.setPathMatcher(matcher);

    assertThat(provider.getPathMatcher()).isSameAs(matcher);
    assertThat(provider.getForLookupPath(path)).isEqualTo("/assets/logo.v1.png");
    assertThat(provider.getForLookupPath("/unmapped/logo.png")).isNull();
  }

  @Test
  void returnsNullWhenRequestUrlHasNoPathToResolve() {
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    given(request.getRequestURI()).willReturn("/app/assets/logo.png");
    given(request.getRequestPath()).willReturn(RequestPath.parse("/assets/logo.png", ""));

    assertThat(new ResourceUrlProvider().getForRequestUrl(request, "/app?size=2")).isNull();
  }

  private static ServerHttpExchange requestAccepting(String encoding) {
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    given(request.getHeader(HttpHeaders.ACCEPT_ENCODING)).willReturn(encoding);
    return request;
  }

}
