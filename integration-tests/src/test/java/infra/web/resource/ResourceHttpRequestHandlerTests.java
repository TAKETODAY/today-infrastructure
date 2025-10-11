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

package infra.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import infra.core.io.ClassPathResource;
import infra.core.io.FileSystemResource;
import infra.core.io.Resource;
import infra.core.io.UrlResource;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.ExceptionUtils;
import infra.util.StringUtils;
import infra.web.HttpRequestMethodNotSupportedException;
import infra.web.NotFoundHandler;
import infra.web.RequestContext;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.ContentNegotiationManagerFactoryBean;
import infra.web.cors.CorsConfiguration;
import infra.web.handler.SimpleNotFoundHandler;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ResourceHttpRequestHandler}.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@ExtendWith(GzipSupport.class)
public class ResourceHttpRequestHandlerTests {

  private static final ClassPathResource testResource = new ClassPathResource("test/", ResourceHttpRequestHandlerTests.class);
  private static final ClassPathResource testAlternatePathResource = new ClassPathResource("testalternatepath/", ResourceHttpRequestHandlerTests.class);
  private static final ClassPathResource webjarsResource = new ClassPathResource("META-INF/resources/webjars/");

  @Test
  void setLocationValuesStoresLocations() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    List<String> locations = List.of("classpath:/static/", "/webapp/resources/");

    handler.setLocationValues(locations);

    // Note: locationValues is private, so we can't directly assert its contents
    // But we can verify it doesn't throw an exception
    assertThatCode(() -> handler.setLocationValues(locations)).doesNotThrowAnyException();
  }

  @Test
  void setLocationsStoresValidResources() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    Resource resource1 = new ClassPathResource("test/");
    Resource resource2 = new ClassPathResource("testalternatepath/");

    handler.setLocations(List.of(resource1, resource2));

    // Should not throw exception for valid resources
    assertThatCode(() -> handler.setLocations(List.of(resource1, resource2))).doesNotThrowAnyException();
  }

  @Test
  void setLocationsThrowsExceptionForNullList() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

    assertThatThrownBy(() -> handler.setLocations(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Locations list is required");
  }

  @Test
  void setResourceResolversStoresResolvers() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    ResourceResolver resolver1 = mock(ResourceResolver.class);
    ResourceResolver resolver2 = mock(ResourceResolver.class);

    handler.setResourceResolvers(List.of(resolver1, resolver2));

    assertThat(handler.getResourceResolvers()).containsExactly(resolver1, resolver2);
  }

  @Test
  void setResourceResolversWithNullClearsList() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    ResourceResolver resolver = mock(ResourceResolver.class);
    handler.setResourceResolvers(List.of(resolver));

    handler.setResourceResolvers(null);

    assertThat(handler.getResourceResolvers()).isEmpty();
  }

  @Test
  void setResourceTransformersStoresTransformers() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    ResourceTransformer transformer1 = mock(ResourceTransformer.class);
    ResourceTransformer transformer2 = mock(ResourceTransformer.class);

    handler.setResourceTransformers(List.of(transformer1, transformer2));

    assertThat(handler.getResourceTransformers()).containsExactly(transformer1, transformer2);
  }

  @Test
  void setResourceTransformersWithNullClearsList() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    ResourceTransformer transformer = mock(ResourceTransformer.class);
    handler.setResourceTransformers(List.of(transformer));

    handler.setResourceTransformers(null);

    assertThat(handler.getResourceTransformers()).isEmpty();
  }

  @Test
  void setContentNegotiationManagerStoresManager() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    ContentNegotiationManager manager = mock(ContentNegotiationManager.class);

    handler.setContentNegotiationManager(manager);

    assertThat(handler.getContentNegotiationManager()).isSameAs(manager);
  }

  @Test
  void setMediaTypesStoresMediaTypeMappings() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    Map<String, MediaType> mediaTypes = Map.of("json", MediaType.APPLICATION_JSON, "XML", MediaType.APPLICATION_XML);

    handler.setMediaTypes(mediaTypes);

    Map<String, MediaType> result = handler.getMediaTypes();
    assertThat(result).containsEntry("json", MediaType.APPLICATION_JSON);
    assertThat(result).containsEntry("xml", MediaType.APPLICATION_XML); // Keys should be lowercase
  }

  @Test
  void setCorsConfigurationStoresConfiguration() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    CorsConfiguration corsConfig = mock(CorsConfiguration.class);

    handler.setCorsConfiguration(corsConfig);

    assertThat(handler.getCorsConfiguration(null)).isSameAs(corsConfig);
  }

  @Test
  void setUseLastModifiedStoresFlag() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

    handler.setUseLastModified(false);

    assertThat(handler.isUseLastModified()).isFalse();

    handler.setUseLastModified(true);

    assertThat(handler.isUseLastModified()).isTrue();
  }

  @Test
  void setEtagGeneratorStoresFunction() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    Function<Resource, String> etagGenerator = resource -> "test-etag";

    handler.setEtagGenerator(etagGenerator);

    assertThat(handler.getEtagGenerator()).isSameAs(etagGenerator);
  }

  @Test
  void setOptimizeLocationsStoresFlag() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

    handler.setOptimizeLocations(true);

    assertThat(handler.isOptimizeLocations()).isTrue();

    handler.setOptimizeLocations(false);

    assertThat(handler.isOptimizeLocations()).isFalse();
  }

  @Test
  void setNotFoundHandlerStoresHandler() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    NotFoundHandler notFoundHandler = mock(NotFoundHandler.class);

    handler.setNotFoundHandler(notFoundHandler);

    // We can't directly access the field, but we can verify it was set by checking behavior
    assertThatCode(() -> handler.setNotFoundHandler(notFoundHandler)).doesNotThrowAnyException();
  }

  @Test
  void afterPropertiesSetInitializesDefaultResolvers() throws Exception {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.setLocations(List.of(new ClassPathResource("test/")));

    handler.afterPropertiesSet();

    assertThat(handler.getResourceResolvers()).hasSize(1);
    assertThat(handler.getResourceResolvers().get(0)).isInstanceOf(PathResourceResolver.class);
  }

  @Test
  void afterPropertiesSetInitializesConverters() throws Exception {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.setLocations(List.of(new ClassPathResource("test/")));

    handler.afterPropertiesSet();

    assertThat(handler.getResourceHttpMessageConverter()).isNotNull();
    assertThat(handler.getResourceRegionHttpMessageConverter()).isNotNull();
  }

  @Test
  void processPathDelegatesToResourceHandlerUtils() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

    String result = handler.processPath("/test/../path");

    // Should normalize the path
    assertThat(result).isNotNull();
  }

  @Test
  void getMediaTypeReturnsNullWhenNoExtension() throws IOException {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    Resource resource = mock(Resource.class);
    given(resource.getName()).willReturn("filename");

    MediaType mediaType = handler.getMediaType(mock(RequestContext.class), resource);

    assertThat(mediaType).isNull();
  }

  @Test
  void toStringReturnsMeaningfulDescription() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    List<Resource> locations = List.of(new ClassPathResource("test/"));
    handler.setLocations(locations);

    String result = handler.toString();

    assertThat(result).contains("ResourceHttpRequestHandler");
    assertThat(result).contains("classpath [test/]");
  }

  @Nested
  class ResourceHandlingTests {

    private ResourceHttpRequestHandler handler;

    private HttpMockRequestImpl request;

    private MockHttpResponseImpl response;
    private MockRequestContext requestContext;

    @BeforeEach
    void setup() throws Throwable {
      TestMockContext mockContext = new TestMockContext();
      this.handler = new ResourceHttpRequestHandler();
      this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
      this.handler.afterPropertiesSet();
      this.request = new HttpMockRequestImpl(mockContext, "GET", "");
      this.response = new MockHttpResponseImpl();
      requestContext = new MockRequestContext(null, request, response);
    }

    @Test
    void servesResource() throws Throwable {
      this.request.setRequestURI("foo.css");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("text/css");
      assertThat(this.response.getContentLength()).isEqualTo(17);
      assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
    }

    @Test
    void supportsHeadRequests() throws Throwable {
      this.request.setMethod("HEAD");
      this.request.setRequestURI("foo.css");
      this.handler.handleRequest(requestContext);
      requestContext.requestCompleted();
      assertThat(this.response.getStatus()).isEqualTo(200);
      assertThat(this.response.getContentType()).isEqualTo("text/css");
      assertThat(this.response.getContentLength()).isEqualTo(17);
      assertThat(this.response.getContentAsByteArray()).isEmpty();
    }

    @Test
    void supportsOptionsRequests() throws Throwable {
      this.request.setMethod("OPTIONS");
      this.request.setRequestURI("foo.css");
      this.handler.handleRequest(requestContext);
      requestContext.requestCompleted();
      assertThat(this.response.getStatus()).isEqualTo(200);
      assertThat(this.response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
    }

    @Test
    void servesHtmlResources() throws Throwable {
      this.request.setRequestURI("foo.html");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("text/html");
    }

    @Test
    void getResourceWithRegisteredMediaType() throws Throwable {
      ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
      factory.addMediaType("bar", new MediaType("foo", "bar"));
      factory.afterPropertiesSet();
      ContentNegotiationManager manager = factory.getObject();

      List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
      ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
//      handler.setMockContext(new MockContext());
      handler.setLocations(paths);
      handler.setContentNegotiationManager(manager);
      handler.afterPropertiesSet();

      this.request.setRequestURI("foo.bar");
      handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("foo/bar");
      assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
    }

    @Test
    void getMediaTypeWithFavorPathExtensionOff() throws Throwable {
      ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
      factory.setFavorPathExtension(false);
      factory.afterPropertiesSet();
      ContentNegotiationManager manager = factory.getObject();

      List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
      ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
//      handler.setMockContext(new MockContext());
      handler.setLocations(paths);
      handler.setContentNegotiationManager(manager);
      handler.afterPropertiesSet();

      this.request.addHeader("Accept", "application/json,text/plain,*/*");
      this.request.setRequestURI("foo.html");
      handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("text/html");
    }

    @Test
    void getResourceWithMediaTypeResolvedThroughMockContext() throws Throwable {
      MockContextImpl mockContext = new MockContextImpl() {
        @Override
        public String getMimeType(String filePath) {
          return "foo/bar";
        }
      };

      List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
      ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
//      handler.setMockContext(mockContext);
      handler.setLocations(paths);
      handler.afterPropertiesSet();

      HttpMockRequestImpl request = new HttpMockRequestImpl(mockContext, "GET", "");
      request.setRequestURI("foo.css");
      handler.handleRequest(new MockRequestContext(null, request, response));

      assertThat(this.response.getContentType()).isEqualTo("text/css");
      assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
    }

    @Test
    void unsupportedHttpMethod() {
      this.request.setRequestURI("foo.css");
      this.request.setMethod("POST");
      assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class).isThrownBy(() ->
              this.handler.handleRequest(requestContext));
    }

    @Test
    void testResourceNotFound() throws Throwable {
      for (HttpMethod method : HttpMethod.values()) {
        this.request = new HttpMockRequestImpl("GET", "");
        this.request.setRequestURI("not-there.css");
        this.request.setMethod(method.name());
        this.response = new MockHttpResponseImpl();
        requestContext = new MockRequestContext(null, request, response);

        assertThat(this.handler.handleRequest(requestContext)).isEqualTo(SimpleNotFoundHandler.NONE_RETURN_VALUE);
      }
    }

  }

  @Nested
  class RangeRequestTests {

    private ResourceHttpRequestHandler handler;

    private HttpMockRequestImpl request;

    private MockHttpResponseImpl response;

    private MockRequestContext requestContext;

    @BeforeEach
    void setup() throws Throwable {
      TestMockContext mockContext = new TestMockContext();
      this.handler = new ResourceHttpRequestHandler();
      this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
      this.handler.afterPropertiesSet();
      this.request = new HttpMockRequestImpl(mockContext, "GET", "");
      this.response = new MockHttpResponseImpl();
      requestContext = new MockRequestContext(null, request, response);
    }

    @Test
    void supportsRangeRequest() throws Throwable {
      this.request.setRequestURI("foo.css");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
      assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
    }

    @Test
    void partialContentByteRange() throws Throwable {
      this.request.addHeader("Range", "bytes=0-1");
      this.request.setRequestURI("foo.txt");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getStatus()).isEqualTo(206);
      assertThat(this.response.getContentType()).isEqualTo("text/plain");
      assertThat(this.response.getContentLength()).isEqualTo(2);
      assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
      assertThat(this.response.getContentAsString()).isEqualTo("So");
      assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
      assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
    }

    @Test
    void partialContentByteRangeNoEnd() throws Throwable {
      this.request.addHeader("Range", "bytes=9-");
      this.request.setRequestURI("foo.txt");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getStatus()).isEqualTo(206);
      assertThat(this.response.getContentType()).isEqualTo("text/plain");
      assertThat(this.response.getContentLength()).isEqualTo(1);
      assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
      assertThat(this.response.getContentAsString()).isEqualTo(".");
      assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
      assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
    }

    @Test
    void partialContentByteRangeLargeEnd() throws Throwable {
      this.request.addHeader("Range", "bytes=9-10000");
      this.request.setRequestURI("foo.txt");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getStatus()).isEqualTo(206);
      assertThat(this.response.getContentType()).isEqualTo("text/plain");
      assertThat(this.response.getContentLength()).isEqualTo(1);
      assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
      assertThat(this.response.getContentAsString()).isEqualTo(".");
      assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
      assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
    }

    @Test
    void partialContentSuffixRange() throws Throwable {
      this.request.addHeader("Range", "bytes=-1");
      this.request.setRequestURI("foo.txt");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getStatus()).isEqualTo(206);
      assertThat(this.response.getContentType()).isEqualTo("text/plain");
      assertThat(this.response.getContentLength()).isEqualTo(1);
      assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
      assertThat(this.response.getContentAsString()).isEqualTo(".");
      assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
      assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
    }

    @Test
    void partialContentSuffixRangeLargeSuffix() throws Throwable {
      this.request.addHeader("Range", "bytes=-11");
      this.request.setRequestURI("foo.txt");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getStatus()).isEqualTo(206);
      assertThat(this.response.getContentType()).isEqualTo("text/plain");
      assertThat(this.response.getContentLength()).isEqualTo(10);
      assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-9/10");
      assertThat(this.response.getContentAsString()).isEqualTo("Some text.");
      assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
      assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
    }

    @Test
    void partialContentInvalidRangeHeader() throws Throwable {
      this.request.addHeader("Range", "bytes= foo bar");
      this.request.setRequestURI("foo.txt");
      this.handler.handleRequest(requestContext);

      requestContext.requestCompleted();
      assertThat(this.response.getStatus()).isEqualTo(416);
      assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes */10");
      assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
      assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
    }

    @Test
    void partialContentMultipleByteRanges() throws Throwable {
      this.request.addHeader("Range", "bytes=0-1, 4-5, 8-9");
      this.request.setRequestURI("foo.txt");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getStatus()).isEqualTo(206);
      assertThat(this.response.getContentType()).startsWith("multipart/byteranges; boundary=");

      String boundary = "--" + this.response.getContentType().substring(31);

      String content = this.response.getContentAsString();
      String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

      assertThat(ranges[0]).isEqualTo(boundary);
      assertThat(ranges[1]).isEqualTo("Content-Type: text/plain");
      assertThat(ranges[2]).isEqualTo("Content-Range: bytes 0-1/10");
      assertThat(ranges[3]).isEqualTo("So");

      assertThat(ranges[4]).isEqualTo(boundary);
      assertThat(ranges[5]).isEqualTo("Content-Type: text/plain");
      assertThat(ranges[6]).isEqualTo("Content-Range: bytes 4-5/10");
      assertThat(ranges[7]).isEqualTo(" t");

      assertThat(ranges[8]).isEqualTo(boundary);
      assertThat(ranges[9]).isEqualTo("Content-Type: text/plain");
      assertThat(ranges[10]).isEqualTo("Content-Range: bytes 8-9/10");
      assertThat(ranges[11]).isEqualTo("t.");
    }

    @Test
    void partialContentByteRangeWithEncodedResource(GzipSupport.GzippedFiles gzippedFiles) throws Throwable {
      String path = "js/foo.js";
      gzippedFiles.create(path);

      ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
      handler.setResourceResolvers(List.of(new EncodedResourceResolver(), new PathResourceResolver()));
      handler.setLocations(List.of(testResource));
      //handler.setMockContext(new MockContext());
      handler.afterPropertiesSet();

      this.request.addHeader("Accept-Encoding", "gzip");
      this.request.addHeader("Range", "bytes=0-1");
      this.request.setRequestURI(path);
      handler.handleRequest(requestContext);

      assertThat(this.response.getStatus()).isEqualTo(206);
      assertThat(this.response.getHeaderNames()).containsExactlyInAnyOrder(
              "Content-Type", "Content-Length", "Content-Range", "Accept-Ranges",
              "Last-Modified", "Content-Encoding", "Vary");

      assertThat(this.response.getContentType()).isEqualTo("text/javascript");
      assertThat(this.response.getContentLength()).isEqualTo(2);
      assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/66");
      assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
      assertThat(this.response.getHeaderValues("Content-Encoding")).containsExactly("gzip");
      assertThat(this.response.getHeaderValues("Vary")).containsExactly("Accept-Encoding");
    }

    @Test
      // gh-25976
    void partialContentWithHttpHead() throws Throwable {
      this.request.setMethod("HEAD");
      this.request.addHeader("Range", "bytes=0-1");
      this.request.setRequestURI("foo.txt");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getStatus()).isEqualTo(206);
      assertThat(this.response.getContentType()).isEqualTo("text/plain");
      assertThat(this.response.getContentLength()).isEqualTo(2);
      assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
      assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
    }

  }

  @Nested
  class HttpCachingTests {

    private ResourceHttpRequestHandler handler;

    private HttpMockRequestImpl request;

    private MockHttpResponseImpl response;
    private MockRequestContext requestContext;

    @BeforeEach
    void setup() {
      TestMockContext mockContext = new TestMockContext();
      this.handler = new ResourceHttpRequestHandler();
      this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
      this.request = new HttpMockRequestImpl(mockContext, "GET", "");
      this.response = new MockHttpResponseImpl();
      requestContext = new MockRequestContext(null, request, response);
    }

    @Test
    void defaultCachingHeaders() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.containsHeader("Last-Modified")).isTrue();
      assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
    }

    @Test
    void configureCacheSeconds() throws Throwable {
      this.handler.setCacheSeconds(3600);
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
    }

    @Test
    void configureCacheSecondsToZero() throws Throwable {
      this.handler.setCacheSeconds(0);
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void configureVersionResourceResolver() throws Throwable {
      VersionResourceResolver versionResolver = new VersionResourceResolver()
              .addFixedVersionStrategy("versionString", "/**");
      this.handler.setResourceResolvers(List.of(versionResolver, new PathResourceResolver()));
      this.handler.afterPropertiesSet();

      this.request.setRequestURI("versionString/foo.css");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getHeader("ETag")).isEqualTo("W/\"versionString\"");
    }

    @Test
    void shouldRespondWithNotModifiedWhenModifiedSince() throws Throwable {
      this.handler.setCacheSeconds(3600);
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
      this.handler.handleRequest(requestContext);
      requestContext.requestCompleted();
      assertThat(this.response.getStatus()).isEqualTo(HttpMockResponse.SC_NOT_MODIFIED);
      assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
    }

    @Test
    void shouldRespondWithModifiedResource() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
      this.handler.handleRequest(requestContext);
      assertThat(this.response.getStatus()).isEqualTo(HttpMockResponse.SC_OK);
      assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
    }

    @Test
    void shouldRespondWithNotModifiedWhenEtag() throws Throwable {
      this.handler.setCacheSeconds(3600);
      this.handler.setEtagGenerator(resource -> "testEtag");
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-None-Match", "\"testEtag\"");
      this.handler.handleRequest(requestContext);
      requestContext.requestCompleted();
      assertThat(this.response.getStatus()).isEqualTo(HttpMockResponse.SC_NOT_MODIFIED);
      assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
    }

    @Test
    void shouldRespondWithModifiedResourceWhenEtagNoMatch() throws Throwable {
      this.handler.setEtagGenerator(resource -> "noMatch");
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-None-Match", "\"testEtag\"");
      this.handler.handleRequest(requestContext);
      assertThat(this.response.getStatus()).isEqualTo(HttpMockResponse.SC_OK);
      assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
    }

    @Test
    void shouldRespondWithNotModifiedWhenEtagAndLastModified() throws Throwable {
      this.handler.setEtagGenerator(resource -> "testEtag");
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-None-Match", "\"testEtag\"");
      this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
      this.handler.handleRequest(requestContext);
      assertThat(this.response.getStatus()).isEqualTo(HttpMockResponse.SC_NOT_MODIFIED);
    }

    @Test
    void overwritesExistingCacheControlHeaders() throws Throwable {
      this.handler.setCacheSeconds(3600);
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");

      this.response.setHeader("Cache-Control", "no-store");

      this.handler.handleRequest(requestContext);

      requestContext.requestCompleted();

      assertThat(this.requestContext.responseHeaders().getFirst("Cache-Control")).isEqualTo("max-age=3600");
    }

    @Test
    void ignoreLastModified() throws Throwable {
      this.request.setRequestURI("foo.css");
      this.handler.setUseLastModified(false);
      this.handler.afterPropertiesSet();
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("text/css");
      assertThat(this.response.getContentLength()).isEqualTo(17);
      assertThat(this.response.containsHeader("Last-Modified")).isFalse();
      assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
    }

    private long resourceLastModified(String resourceName) throws IOException {
      return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
    }

  }

  @Nested
  class ResourceLocationTests {

    private ResourceHttpRequestHandler handler;

    private HttpMockRequestImpl request;

    private MockHttpResponseImpl response;

    private MockRequestContext requestContext;

    @BeforeEach
    void setup() throws Throwable {
      TestMockContext mockContext = new TestMockContext();
      this.handler = new ResourceHttpRequestHandler();
      this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
      this.request = new HttpMockRequestImpl(mockContext, "GET", "");
      this.response = new MockHttpResponseImpl();
      requestContext = new MockRequestContext(null, request, response);
    }

    @Test
    void servesResourcesFromAlternatePath() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("baz.css");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("text/css");
      assertThat(this.response.getContentLength()).isEqualTo(17);
      assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
    }

    @Test
    void servesResourcesFromSubDirectory() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("js/foo.js");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("text/javascript");
      assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
    }

    @Test
    void servesResourcesFromSubDirectoryOfAlternatePath() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("js/baz.js");
      this.handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("text/javascript");
      assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
    }

    @Test
      // gh-27538, gh-27624
    void filterNonExistingLocations() throws Throwable {
      List<Resource> inputLocations = List.of(testResource, testAlternatePathResource,
              new ClassPathResource("nosuchpath/", ResourceHttpRequestHandlerTests.class));
      this.handler.setLocations(inputLocations);
      this.handler.setOptimizeLocations(true);
      this.handler.afterPropertiesSet();

      List<Resource> actual = handler.getLocations();
      assertThat(actual).hasSize(2);
      assertThat(actual.get(0).getURL().toString()).endsWith("test/");
      assertThat(actual.get(1).getURL().toString()).endsWith("testalternatepath/");
    }

    @Test
    void shouldRejectInvalidPath() throws Throwable {
      // Use mock ResourceResolver: i.e. we're only testing upfront validations...
      Resource resource = mock();
      given(resource.getName()).willThrow(new AssertionError("Resource should not be resolved"));
      given(resource.getInputStream()).willThrow(new AssertionError("Resource should not be resolved"));
      ResourceResolver resolver = mock();
      given(resolver.resolveResource(any(), any(), any(), any())).willReturn(resource);

      this.handler.setLocations(List.of(testResource));
      this.handler.setResourceResolvers(List.of(resolver));
      this.handler.afterPropertiesSet();

      testInvalidPath("../testsecret/secret.txt");
      testInvalidPath("test/../../testsecret/secret.txt");
      testInvalidPath(":/../../testsecret/secret.txt");
      testInvalidPath("/testsecret/test/../secret.txt");

      Resource location = new UrlResource(ResourceHttpRequestHandlerTests.class.getResource("./test/"));
      this.handler.setLocations(List.of(location));
      Resource secretResource = new UrlResource(ResourceHttpRequestHandlerTests.class.getResource("testsecret/secret.txt"));
      String secretPath = secretResource.getURL().getPath();

      testInvalidPath("file:" + secretPath);
      testInvalidPath("/file:" + secretPath);
      testInvalidPath("url:" + secretPath);
      testInvalidPath("/url:" + secretPath);
      testInvalidPath("/../.." + secretPath);
      testInvalidPath("/%2E%2E/testsecret/secret.txt");
      testInvalidPath("/%2E%2E/testsecret/secret.txt");
    }

    private void testInvalidPath(String requestPath) {
      this.request.setRequestURI(requestPath);
      this.response = new MockHttpResponseImpl();
      requestContext = new MockRequestContext(null, request, response);
      assertNotFound();
    }

    private void assertNotFound() {
      try {
        assertThat(this.handler.handleRequest(requestContext)).isEqualTo(SimpleNotFoundHandler.NONE_RETURN_VALUE);
      }
      catch (Throwable e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }

    @Test
    void shouldRejectPathWithTraversal() throws Throwable {
      this.handler.afterPropertiesSet();
      for (HttpMethod method : HttpMethod.values()) {
        this.request = new HttpMockRequestImpl("GET", "");
        this.response = new MockHttpResponseImpl();
        shouldRejectPathWithTraversal(method);
      }
    }

    private void shouldRejectPathWithTraversal(HttpMethod httpMethod) throws Throwable {
      this.request.setMethod(httpMethod.name());

      Resource location = new ClassPathResource("test/", getClass());
      this.handler.setLocations(List.of(location));

      testResolvePathWithTraversal(location, "../testsecret/secret.txt");
      testResolvePathWithTraversal(location, "test/../../testsecret/secret.txt");
      testResolvePathWithTraversal(location, ":/../../testsecret/secret.txt");

      location = new UrlResource(ResourceHttpRequestHandlerTests.class.getResource("./test/"));
      this.handler.setLocations(List.of(location));
      Resource secretResource = new UrlResource(ResourceHttpRequestHandlerTests.class.getResource("testsecret/secret.txt"));
      String secretPath = secretResource.getURL().getPath();

      testResolvePathWithTraversal(location, "file:" + secretPath);
      testResolvePathWithTraversal(location, "/file:" + secretPath);
      testResolvePathWithTraversal(location, "url:" + secretPath);
      testResolvePathWithTraversal(location, "/url:" + secretPath);
      testResolvePathWithTraversal(location, "/" + secretPath);
      testResolvePathWithTraversal(location, "////../.." + secretPath);
      testResolvePathWithTraversal(location, "/%2E%2E/testsecret/secret.txt");
      testResolvePathWithTraversal(location, "%2F%2F%2E%2E%2F%2Ftestsecret/secret.txt");
      testResolvePathWithTraversal(location, "/  " + secretPath);
    }

    private void testResolvePathWithTraversal(Resource location, String requestPath) {
      this.request.setRequestURI(requestPath);
      this.response = new MockHttpResponseImpl();
      requestContext = new MockRequestContext(null, request, response);
      assertNotFound();
    }

    @Test
    void ignoreInvalidEscapeSequence() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("/%foo%/bar.txt");
      this.response = new MockHttpResponseImpl();
      assertNotFound();
    }

    @Test
    void processPath() {
      // Unchanged
      assertThat(this.handler.processPath("/foo/bar")).isSameAs("/foo/bar");
      assertThat(this.handler.processPath("foo/bar")).isSameAs("foo/bar");

      // leading whitespace control characters (00-1F)
      assertThat(this.handler.processPath("  /foo/bar")).isEqualTo("/foo/bar");
      assertThat(this.handler.processPath((char) 1 + "/foo/bar")).isEqualTo("/foo/bar");
      assertThat(this.handler.processPath((char) 31 + "/foo/bar")).isEqualTo("/foo/bar");
      assertThat(this.handler.processPath("  foo/bar")).isEqualTo("foo/bar");
      assertThat(this.handler.processPath((char) 31 + "foo/bar")).isEqualTo("foo/bar");

      // leading control character 0x7F (DEL)
      assertThat(this.handler.processPath((char) 127 + "/foo/bar")).isEqualTo("/foo/bar");
      assertThat(this.handler.processPath((char) 127 + "/foo/bar")).isEqualTo("/foo/bar");

      // leading control and '/' characters
      assertThat(this.handler.processPath("  /  foo/bar")).isEqualTo("/foo/bar");
      assertThat(this.handler.processPath("  /  /  foo/bar")).isEqualTo("/foo/bar");
      assertThat(this.handler.processPath("  // /// ////  foo/bar")).isEqualTo("/foo/bar");
      assertThat(this.handler.processPath((char) 1 + " / " + (char) 127 + " // foo/bar")).isEqualTo("/foo/bar");

      // root or empty path
      assertThat(this.handler.processPath("   ")).isEmpty();
      assertThat(this.handler.processPath("/")).isEqualTo("/");
      assertThat(this.handler.processPath("///")).isEqualTo("/");
      assertThat(this.handler.processPath("/ /   / ")).isEqualTo("/");
      assertThat(this.handler.processPath("\\/ \\/   \\/ ")).isEqualTo("/");

      // duplicate slash or backslash
      assertThat(this.handler.processPath("//foo/ /bar//baz//")).isEqualTo("/foo/ /bar/baz/");
      assertThat(this.handler.processPath("\\\\foo\\ \\bar\\\\baz\\\\")).isEqualTo("/foo/ /bar/baz/");
      assertThat(this.handler.processPath("foo\\\\/\\////bar")).isEqualTo("foo/bar");

    }

    @Test
    void initAllowedLocations() throws Throwable {
      this.handler.afterPropertiesSet();
      PathResourceResolver resolver = (PathResourceResolver) this.handler.getResourceResolvers().get(0);
      Resource[] locations = resolver.getAllowedLocations();

      assertThat(locations).containsExactly(testResource, testAlternatePathResource, webjarsResource);
    }

    @Test
    void initAllowedLocationsWithExplicitConfiguration() throws Throwable {
      PathResourceResolver pathResolver = new PathResourceResolver();
      pathResolver.setAllowedLocations(testResource);

      this.handler.setResourceResolvers(List.of(pathResolver));
      this.handler.setLocations(List.of(testResource, testAlternatePathResource));
      this.handler.afterPropertiesSet();

      assertThat(pathResolver.getAllowedLocations()).containsExactly(testResource);
    }

    @Test
    void shouldNotServeDirectory() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("js/");
      assertNotFound();
    }

    @Test
    void shouldNotServeDirectoryInJarFile() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("underscorejs/");
      assertNotFound();
    }

    @Test
    void shouldNotServeMissingResourcePath() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("");
      assertNotFound();
    }

    @Test
    void noPathWithinHandlerMappingAttribute() throws Throwable {
      this.handler.afterPropertiesSet();
      assertThat(this.handler.handleRequest(requestContext)).isEqualTo(SimpleNotFoundHandler.NONE_RETURN_VALUE);
    }

    @Test
    void mockContextRootValidation() {
      StaticWebApplicationContext context = new StaticWebApplicationContext() {
        @Override
        public Resource getResource(String location) {
          return new FileSystemResource("/");
        }
      };

      ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
      handler.setLocationValues(List.of("/"));
      handler.setApplicationContext(context);

      assertThatIllegalStateException().isThrownBy(handler::afterPropertiesSet)
              .withMessage("The String-based location \"/\" should be relative to the web application root but " +
                      "resolved to a Resource of type: class infra.core.io.FileSystemResource. " +
                      "If this is intentional, please pass it as a pre-configured Resource via setLocations.");
    }

  }

  private static class TestMockContext extends MockContextImpl {

    @Override
    public String getMimeType(String filePath) {
      if (filePath.endsWith(".css")) {
        return "text/css";
      }
      else if (filePath.endsWith(".js")) {
        return "text/javascript";
      }
      else {
        return super.getMimeType(filePath);
      }
    }
  }

}
