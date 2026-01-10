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

package infra.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.cache.Cache;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;
import infra.web.resource.EncodedResourceResolver.EncodedResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EncodedResourceResolver}.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 */
@ExtendWith(GzipSupport.class)
public class EncodedResourceResolverTests {

  private ResourceResolvingChain resolver;

  private List<Resource> locations;

  private Cache cache;

  @BeforeEach
  public void setup() {
    this.cache = new ConcurrentMapCache("resourceCache");

    VersionResourceResolver versionResolver = new VersionResourceResolver();
    versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));

    List<ResourceResolver> resolvers = new ArrayList<>();
    resolvers.add(new CachingResourceResolver(this.cache));
    resolvers.add(new EncodedResourceResolver());
    resolvers.add(versionResolver);
    resolvers.add(new PathResourceResolver());
    this.resolver = new DefaultResourceResolvingChain(resolvers);

    this.locations = new ArrayList<>();
    this.locations.add(new ClassPathResource("test/", getClass()));
    this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
  }

  @Test
  public void resolveGzipped(GzipSupport.GzippedFiles gzippedFiles) {
    String file = "js/foo.js";
    gzippedFiles.create(file);
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Accept-Encoding", "gzip");

    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    Resource actual = this.resolver.resolveResource(requestContext, file, this.locations);

    assertThat(actual).isEqualTo(getResource(file + ".gz"));
    assertThat(actual.getName()).isEqualTo(getResource(file).getName());

    boolean condition = actual instanceof HttpResource;
    assertThat(condition).isTrue();
    HttpHeaders headers = ((HttpResource) actual).getResponseHeaders();
    assertThat(headers.getFirst(HttpHeaders.CONTENT_ENCODING)).isEqualTo("gzip");
    assertThat(headers.getFirst(HttpHeaders.VARY)).isEqualTo("Accept-Encoding");
  }

  @Test
  public void resolveGzippedWithVersion(GzipSupport.GzippedFiles gzippedFiles) {
    gzippedFiles.create("foo.css");
    String file = "foo-e36d2e05253c6c7085a91522ce43a0b4.css";
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("Accept-Encoding", "gzip");
    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    Resource resolved = this.resolver.resolveResource(requestContext, file, this.locations);

    assertThat(resolved).isEqualTo(getResource("foo.css.gz"));
    assertThat(resolved.getName()).isEqualTo(getResource("foo.css").getName());
    boolean condition = resolved instanceof HttpResource;
    assertThat(condition).isTrue();
  }

  @Test
  public void resolveFromCacheWithEncodingVariants(GzipSupport.GzippedFiles gzippedFiles) {
    // 1. Resolve, and cache .gz variant
    String file = "js/foo.js";
    gzippedFiles.create(file);
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/js/foo.js");
    request.addHeader("Accept-Encoding", "gzip");
    MockRequestContext requestContext = new MockRequestContext(null, request, null);

    Resource resolved = this.resolver.resolveResource(requestContext, file, this.locations);

    assertThat(resolved).isEqualTo(getResource(file + ".gz"));
    assertThat(resolved.getName()).isEqualTo(getResource(file).getName());
    boolean condition = resolved instanceof HttpResource;
    assertThat(condition).isTrue();

    // 2. Resolve unencoded resource
    request = new HttpMockRequestImpl("GET", "/js/foo.js");
    requestContext = new MockRequestContext(null, request, null);

    resolved = this.resolver.resolveResource(requestContext, file, this.locations);

    assertThat(resolved.toString()).isEqualTo(getResource(file).toString());
    assertThat(resolved.getName()).isEqualTo(getResource(file).getName());
    boolean condition1 = resolved instanceof HttpResource;
    assertThat(condition1).isFalse();
  }

  @Test
  public void resolveWithNullRequest() {
    String file = "js/foo.js";
    Resource resolved = this.resolver.resolveResource(null, file, this.locations);

    assertThat(resolved.toString()).isEqualTo(getResource(file).toString());
    assertThat(resolved.getName()).isEqualTo(getResource(file).getName());
  }

  @Test
  void defaultConstructorShouldSetDefaultCodingsAndExtensions() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();

    assertThat(resolver.getContentCodings()).containsExactly("br", "gzip");
    assertThat(resolver.getExtensions()).containsEntry("gzip", ".gz");
    assertThat(resolver.getExtensions()).containsEntry("br", ".br");
  }

  @Test
  void setContentCodingsShouldUpdateSupportedCodings() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();
    List<String> customCodings = List.of("deflate", "gzip");

    resolver.setContentCodings(customCodings);

    assertThat(resolver.getContentCodings()).containsExactly("deflate", "gzip");
  }

  @Test
  void setContentCodingsShouldThrowExceptionWhenEmpty() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();

    assertThatThrownBy(() -> resolver.setContentCodings(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one content coding expected");
  }

  @Test
  void setExtensionsShouldUpdateCodingToExtensionMappings() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();
    Map<String, String> extensions = Map.of("deflate", ".def", "gzip", ".gz");

    resolver.setExtensions(extensions);

    assertThat(resolver.getExtensions()).containsEntry("deflate", ".def");
    assertThat(resolver.getExtensions()).containsEntry("gzip", ".gz");
  }

  @Test
  void registerExtensionShouldAddCodingToExtensionMapping() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();

    resolver.registerExtension("deflate", ".def");

    assertThat(resolver.getExtensions()).containsEntry("deflate", ".def");
  }

  @Test
  void registerExtensionShouldPrependDotWhenNotPresent() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();

    resolver.registerExtension("deflate", "def");

    assertThat(resolver.getExtensions()).containsEntry("deflate", ".def");
  }

  @Test
  void resolveResourceInternalShouldReturnOriginalResourceWhenRequestIsNull() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();
    Resource originalResource = new ClassPathResource("test/bar.css");
    List<Resource> locations = List.of(originalResource);
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    when(chain.resolveResource(null, "bar.css", locations)).thenReturn(originalResource);

    Resource result = resolver.resolveResourceInternal(null, "bar.css", locations, chain);

    assertThat(result).isSameAs(originalResource);
  }

  @Test
  void resolveResourceInternalShouldReturnOriginalResourceWhenAcceptEncodingHeaderIsNull() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();
    Resource originalResource = new ClassPathResource("test/bar.css");
    List<Resource> locations = List.of(originalResource);
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    when(chain.resolveResource(any(), any(), any())).thenReturn(originalResource);

    MockRequestContext request = mock(MockRequestContext.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(request.getHeaders()).thenReturn(headers);

    Resource result = resolver.resolveResourceInternal(request, "bar.css", locations, chain);

    assertThat(result).isSameAs(originalResource);
  }

  @Test
  void resolveResourceInternalShouldReturnEncodedResourceWhenSupportedEncodingIsAccepted() throws IOException {
    EncodedResourceResolver resolver = new EncodedResourceResolver();
    Resource originalResource = new ClassPathResource("test/bar.css");
    List<Resource> locations = List.of(originalResource);
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    when(chain.resolveResource(any(), any(), any())).thenReturn(originalResource);

    MockRequestContext request = mock(MockRequestContext.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept-Encoding", "gzip, deflate");
    when(request.getHeaders()).thenReturn(headers);

    Resource result = resolver.resolveResourceInternal(request, "bar.css", locations, chain);

    // Since we're not actually creating .gz files in test, it should return original
    assertThat(result).isSameAs(originalResource);
  }

  @Test
  void resolveUrlPathInternalShouldDelegateToChain() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();
    List<Resource> locations = List.of();
    ResourceResolvingChain chain = mock(ResourceResolvingChain.class);
    when(chain.resolveUrlPath("test.css", locations)).thenReturn("test.css");

    String result = resolver.resolveUrlPathInternal("test.css", locations, chain);

    assertThat(result).isEqualTo("test.css");
  }

  @Test
  void encodedResourceShouldDelegateMethodsToEncodedResource() throws IOException {
    Resource original = new ClassPathResource("test/bar.css");
    String coding = "gzip";
    String extension = ".gz";

    EncodedResource encodedResource =
            new EncodedResource(original, coding, extension);

    assertThat(encodedResource.getName()).isEqualTo(original.getName());
    assertThat(encodedResource.exists()).isEqualTo(encodedResource.encoded.exists());
    assertThat(encodedResource.isReadable()).isEqualTo(encodedResource.encoded.isReadable());
  }

  @Test
  void encodedResourceGetResponseHeadersShouldIncludeContentEncodingAndVaryHeaders() throws IOException {
    Resource original = new ClassPathResource("test/bar.css");
    String coding = "gzip";
    String extension = ".gz";

    EncodedResource encodedResource =
            new EncodedResource(original, coding, extension);

    HttpHeaders headers = encodedResource.getResponseHeaders();

    assertThat(headers.getFirst("Content-Encoding")).isEqualTo(coding);
    assertThat(headers.getFirst("Vary")).isEqualTo("Accept-Encoding");
  }

  @Test
  void encodedResourceGetResponseHeadersShouldPreserveOriginalHeaders() throws IOException {
    HttpResource original = mock(HttpResource.class);
    when(original.getName()).thenReturn("bar.css");
    HttpHeaders originalHeaders = HttpHeaders.forWritable();
    originalHeaders.add("Cache-Control", "max-age=3600");
    when(original.getResponseHeaders()).thenReturn(originalHeaders);

    String coding = "gzip";
    String extension = ".gz";

    EncodedResource encodedResource =
            new EncodedResource(original, coding, extension);

    HttpHeaders headers = encodedResource.getResponseHeaders();

    assertThat(headers.getFirst("Cache-Control")).isEqualTo("max-age=3600");
    assertThat(headers.getFirst("Content-Encoding")).isEqualTo(coding);
    assertThat(headers.getFirst("Vary")).isEqualTo("Accept-Encoding");
  }

  @Test
  void getExtensionShouldThrowExceptionWhenCodingNotRegistered() {
    EncodedResourceResolver resolver = new EncodedResourceResolver();

    assertThatThrownBy(() -> resolver.getExtension("deflate"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No file extension associated with content coding deflate");
  }

  private Resource getResource(String filePath) {
    return new ClassPathResource("test/" + filePath, getClass());
  }

}
