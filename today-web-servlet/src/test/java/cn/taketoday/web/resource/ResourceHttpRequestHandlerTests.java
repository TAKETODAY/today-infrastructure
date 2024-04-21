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

package cn.taketoday.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.ContentNegotiationManagerFactoryBean;
import cn.taketoday.web.handler.SimpleNotFoundHandler;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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

  @Nested
  class ResourceHandlingTests {

    private ResourceHttpRequestHandler handler;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;
    private ServletRequestContext requestContext;

    @BeforeEach
    void setup() throws Throwable {
      TestServletContext servletContext = new TestServletContext();
      this.handler = new ResourceHttpRequestHandler();
      this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
      this.handler.afterPropertiesSet();
      this.request = new MockHttpServletRequest(servletContext, "GET", "");
      this.response = new MockHttpServletResponse();
      requestContext = new ServletRequestContext(null, request, response);
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
      // SPR-13658
    void getResourceWithRegisteredMediaType() throws Throwable {
      ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
      factory.addMediaType("bar", new MediaType("foo", "bar"));
      factory.afterPropertiesSet();
      ContentNegotiationManager manager = factory.getObject();

      List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
      ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
//      handler.setServletContext(new MockServletContext());
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
//      handler.setServletContext(new MockServletContext());
      handler.setLocations(paths);
      handler.setContentNegotiationManager(manager);
      handler.afterPropertiesSet();

      this.request.addHeader("Accept", "application/json,text/plain,*/*");
      this.request.setRequestURI("foo.html");
      handler.handleRequest(requestContext);

      assertThat(this.response.getContentType()).isEqualTo("text/html");
    }

    @Test
    void getResourceWithMediaTypeResolvedThroughServletContext() throws Throwable {
      MockServletContext servletContext = new MockServletContext() {
        @Override
        public String getMimeType(String filePath) {
          return "foo/bar";
        }
      };

      List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
      ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
//      handler.setServletContext(servletContext);
      handler.setLocations(paths);
      handler.afterPropertiesSet();

      MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "");
      request.setRequestURI("foo.css");
      handler.handleRequest(new ServletRequestContext(null, request, response));

      assertThat(this.response.getContentType()).isEqualTo("foo/bar");
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
        this.request = new MockHttpServletRequest("GET", "");
        this.request.setRequestURI("not-there.css");
        this.request.setMethod(method.name());
        this.response = new MockHttpServletResponse();
        requestContext = new ServletRequestContext(null, request, response);

        assertThat(this.handler.handleRequest(requestContext)).isEqualTo(SimpleNotFoundHandler.NONE_RETURN_VALUE);
      }
    }

  }

  @Nested
  class RangeRequestTests {

    private ResourceHttpRequestHandler handler;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private ServletRequestContext requestContext;

    @BeforeEach
    void setup() throws Throwable {
      TestServletContext servletContext = new TestServletContext();
      this.handler = new ResourceHttpRequestHandler();
      this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
      this.handler.afterPropertiesSet();
      this.request = new MockHttpServletRequest(servletContext, "GET", "");
      this.response = new MockHttpServletResponse();
      requestContext = new ServletRequestContext(null, request, response);
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
      //handler.setServletContext(new MockServletContext());
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

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;
    private ServletRequestContext requestContext;

    @BeforeEach
    void setup() {
      TestServletContext servletContext = new TestServletContext();
      this.handler = new ResourceHttpRequestHandler();
      this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
      this.request = new MockHttpServletRequest(servletContext, "GET", "");
      this.response = new MockHttpServletResponse();
      requestContext = new ServletRequestContext(null, request, response);
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
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
      this.handler.handleRequest(requestContext);
      assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    void shouldRespondWithModifiedResource() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
      this.handler.handleRequest(requestContext);
      assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
      assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
    }

    @Test
    void shouldRespondWithNotModifiedWhenEtag() throws Throwable {
      this.handler.setEtagGenerator(resource -> "testEtag");
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-None-Match", "\"testEtag\"");
      this.handler.handleRequest(requestContext);
      assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    void shouldRespondWithModifiedResourceWhenEtagNoMatch() throws Throwable {
      this.handler.setEtagGenerator(resource -> "noMatch");
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");
      this.request.addHeader("If-None-Match", "\"testEtag\"");
      this.handler.handleRequest(requestContext);
      assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
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
      assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    void overwritesExistingCacheControlHeaders() throws Throwable {
      this.handler.setCacheSeconds(3600);
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("foo.css");

      this.response.setHeader("Cache-Control", "no-store");

      this.handler.handleRequest(requestContext);

      requestContext.requestCompleted();

      assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
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

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private ServletRequestContext requestContext;

    @BeforeEach
    void setup() throws Throwable {
      TestServletContext servletContext = new TestServletContext();
      this.handler = new ResourceHttpRequestHandler();
      this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
      this.request = new MockHttpServletRequest(servletContext, "GET", "");
      this.response = new MockHttpServletResponse();
      requestContext = new ServletRequestContext(null, request, response);
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
      testInvalidPath("%2F%2F%2E%2E%2F%2F%2E%2E" + secretPath);
    }

    private void testInvalidPath(String requestPath) {
      this.request.setRequestURI(requestPath);
      this.response = new MockHttpServletResponse();
      requestContext = new ServletRequestContext(null, request, response);
      assertNotFound();
    }

    @SneakyThrows
    private void assertNotFound() {
      assertThat(this.handler.handleRequest(requestContext)).isEqualTo(SimpleNotFoundHandler.NONE_RETURN_VALUE);
    }

    @Test
    void shouldRejectPathWithTraversal() throws Throwable {
      this.handler.afterPropertiesSet();
      for (HttpMethod method : HttpMethod.values()) {
        this.request = new MockHttpServletRequest("GET", "");
        this.response = new MockHttpServletResponse();
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
      this.response = new MockHttpServletResponse();
      requestContext = new ServletRequestContext(null, request, response);
      assertNotFound();
    }

    @Test
    void ignoreInvalidEscapeSequence() throws Throwable {
      this.handler.afterPropertiesSet();
      this.request.setRequestURI("/%foo%/bar.txt");
      this.response = new MockHttpServletResponse();
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
    void servletContextRootValidation() {
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
                      "resolved to a Resource of type: class cn.taketoday.core.io.FileSystemResource. " +
                      "If this is intentional, please pass it as a pre-configured Resource via setLocations.");
    }

  }

  private static class TestServletContext extends MockServletContext {

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
