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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlBasedResource;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.ContentNegotiationManagerFactoryBean;
import cn.taketoday.web.context.support.StaticWebApplicationContext;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.mock.MockHttpServletResponse;
import cn.taketoday.web.mock.MockServletContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
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

  private ResourceHttpRequestHandler handler;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  ServletRequestContext requestContext;

  @BeforeEach
  public void setup() throws Exception {
    List<Resource> paths = new ArrayList<>(2);
    paths.add(new ClassPathResource("test/", getClass()));
    paths.add(new ClassPathResource("testalternatepath/", getClass()));
    paths.add(new ClassPathResource("META-INF/resources/webjars/"));

    TestServletContext servletContext = new TestServletContext();

    this.handler = new ResourceHttpRequestHandler();
    this.handler.setLocations(paths);
    this.handler.setCacheSeconds(3600);
    this.handler.afterPropertiesSet();

    this.request = new MockHttpServletRequest(servletContext, "GET", "");
    this.response = new MockHttpServletResponse();
    requestContext = new ServletRequestContext(null, request, response);
  }

  @Test
  public void getResource() throws Exception {
    request.setRequestURI("foo.css");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getContentType()).isEqualTo("text/css");
    assertThat(this.response.getContentLength()).isEqualTo(17);
    assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
    assertThat(this.response.containsHeader("Last-Modified")).isTrue();
    assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
    assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
  }

  @Test
  public void getResourceHttpHeader() throws Exception {
    this.request.setMethod("HEAD");
    request.setRequestURI("foo.css");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(200);
    assertThat(this.response.getContentType()).isEqualTo("text/css");
    assertThat(this.response.getContentLength()).isEqualTo(17);
    assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
    assertThat(this.response.containsHeader("Last-Modified")).isTrue();
    assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void getResourceHttpOptions() throws Exception {
    this.request.setMethod("OPTIONS");
    request.setRequestURI("foo.css");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(200);
    assertThat(this.response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
  }

  @Test
  public void getResourceNoCache() throws Exception {
    request.setRequestURI("foo.css");
    this.handler.setCacheSeconds(0);
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-store");
    assertThat(this.response.containsHeader("Last-Modified")).isTrue();
    assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void getVersionedResource() throws Exception {
    VersionResourceResolver versionResolver = new VersionResourceResolver()
            .addFixedVersionStrategy("versionString", "/**");
    this.handler.setResourceResolvers(Arrays.asList(versionResolver, new PathResourceResolver()));
    this.handler.afterPropertiesSet();

    request.setRequestURI("versionString/foo.css");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getHeader("ETag")).isEqualTo("W/\"versionString\"");
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void getResourceWithHtmlMediaType() throws Throwable {
    request.setRequestURI("foo.html");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getContentType()).isEqualTo("text/html");
    assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
    assertThat(this.response.containsHeader("Last-Modified")).isTrue();
    assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.html") / 1000);
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void getResourceFromAlternatePath() throws Exception {
    request.setRequestURI("baz.css");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getContentType()).isEqualTo("text/css");
    assertThat(this.response.getContentLength()).isEqualTo(17);
    assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
    assertThat(this.response.containsHeader("Last-Modified")).isTrue();
    assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("testalternatepath/baz.css") / 1000);
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
    assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
  }

  @Test
  public void getResourceFromSubDirectory() throws Exception {
    request.setRequestURI("js/foo.js");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getContentType()).isEqualTo("text/javascript");
    assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
  }

  @Test
  public void getResourceFromSubDirectoryOfAlternatePath() throws Exception {
    request.setRequestURI("js/baz.js");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getContentType()).isEqualTo("text/javascript");
    assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
  }

  @Test  // SPR-13658
  public void getResourceWithRegisteredMediaType() throws Exception {
    ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
    factory.addMediaType("bar", new MediaType("foo", "bar"));
    factory.afterPropertiesSet();
    ContentNegotiationManager manager = factory.getObject();

    List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

    handler.setLocations(paths);
    handler.setContentNegotiationManager(manager);
    handler.afterPropertiesSet();

    request.setRequestURI("foo.bar");
    handler.handleRequest(requestContext);

    assertThat(this.response.getContentType()).isEqualTo("foo/bar");
    assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
  }

  @Test  // SPR-14577
  public void getMediaTypeWithFavorPathExtensionOff() throws Exception {
    ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
    factory.afterPropertiesSet();
    ContentNegotiationManager manager = factory.getObject();

    List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

    handler.setLocations(paths);
    handler.setContentNegotiationManager(manager);
    handler.afterPropertiesSet();

    this.request.addHeader("Accept", "application/json,text/plain,*/*");
    request.setRequestURI("foo.html");
    handler.handleRequest(requestContext);

    assertThat(this.response.getContentType()).isEqualTo("text/html");
  }

  @Test  // SPR-14368
  public void getResourceWithMediaTypeResolvedThroughServletContext() throws Exception {
    MockServletContext servletContext = new MockServletContext() {
      @Override
      public String getMimeType(String filePath) {
        return "foo/bar";
      }
    };

    List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.setLocations(paths);
    handler.afterPropertiesSet();

    MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "");
    request.setRequestURI("foo.css");
    handler.handleRequest(new ServletRequestContext(null, request, response));

    assertThat(this.response.getContentType()).isEqualTo("foo/bar");
    assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
  }

  @Test  // gh-27538, gh-27624
  public void filterNonExistingLocations() throws Exception {
    List<Resource> inputLocations = Arrays.asList(
            new ClassPathResource("test/", getClass()),
            new ClassPathResource("testalternatepath/", getClass()),
            new ClassPathResource("nosuchpath/", getClass()));

    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

    handler.setLocations(inputLocations);
    handler.setOptimizeLocations(true);
    handler.afterPropertiesSet();

    List<Resource> actual = handler.getLocations();
    assertThat(actual).hasSize(2);
    assertThat(actual.get(0).getLocation().toString()).endsWith("test/");
    assertThat(actual.get(1).getLocation().toString()).endsWith("testalternatepath/");
  }

  @Test
  public void testInvalidPath() throws Exception {
    // Use mock ResourceResolver: i.e. we're only testing upfront validations...

    Resource resource = mock(Resource.class);
    given(resource.getName()).willThrow(new AssertionError("Resource should not be resolved"));
    given(resource.getInputStream()).willThrow(new AssertionError("Resource should not be resolved"));
    ResourceResolver resolver = mock(ResourceResolver.class);
    given(resolver.resolveResource(any(), any(), any(), any())).willReturn(resource);

    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
    handler.setResourceResolvers(Collections.singletonList(resolver));
    handler.afterPropertiesSet();

    testInvalidPath("../testsecret/secret.txt", handler);
    testInvalidPath("test/../../testsecret/secret.txt", handler);
    testInvalidPath(":/../../testsecret/secret.txt", handler);

    Resource location = new UrlBasedResource(getClass().getResource("test/"));
    this.handler.setLocations(Collections.singletonList(location));
    Resource secretResource = new UrlBasedResource(getClass().getResource("testsecret/secret.txt"));
    String secretPath = secretResource.getLocation().getPath();

    testInvalidPath("file:" + secretPath, handler);
    testInvalidPath("/file:" + secretPath, handler);
    testInvalidPath("url:" + secretPath, handler);
    testInvalidPath("/url:" + secretPath, handler);
    testInvalidPath("/../.." + secretPath, handler);
    testInvalidPath("/%2E%2E/testsecret/secret.txt", handler);
    testInvalidPath("/%2E%2E/testsecret/secret.txt", handler);
    testInvalidPath("%2F%2F%2E%2E%2F%2F%2E%2E" + secretPath, handler);
  }

  private void testInvalidPath(String requestPath, ResourceHttpRequestHandler handler) throws Exception {
    request.setRequestURI(requestPath);
    this.response = new MockHttpServletResponse();
    requestContext = new ServletRequestContext(null, request, response);

    handler.handleRequest(requestContext);
    assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void resolvePathWithTraversal() throws Exception {
    for (HttpMethod method : HttpMethod.values()) {
      this.request = new MockHttpServletRequest("GET", "");
      this.response = new MockHttpServletResponse();
      requestContext = new ServletRequestContext(null, request, response);

      testResolvePathWithTraversal(method);
    }
  }

  private void testResolvePathWithTraversal(HttpMethod httpMethod) throws Exception {
    this.request.setMethod(httpMethod.name());

    Resource location = new ClassPathResource("test/", getClass());
    this.handler.setLocations(Collections.singletonList(location));

    testResolvePathWithTraversal(location, "../testsecret/secret.txt");
    testResolvePathWithTraversal(location, "test/../../testsecret/secret.txt");
    testResolvePathWithTraversal(location, ":/../../testsecret/secret.txt");

    location = new UrlBasedResource(getClass().getResource("test/"));
    this.handler.setLocations(Collections.singletonList(location));
    Resource secretResource = new UrlBasedResource(getClass().getResource("testsecret/secret.txt"));
    String secretPath = secretResource.getLocation().getPath();

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

  private void testResolvePathWithTraversal(Resource location, String requestPath) throws Exception {
    request.setRequestURI(requestPath);
    this.response = new MockHttpServletResponse();
    requestContext = new ServletRequestContext(null, request, response);

    this.handler.handleRequest(requestContext);
    if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
      fail(requestPath + " doesn't actually exist as a relative path");
    }
    assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

//  @Test
//  public void ignoreInvalidEscapeSequence() throws Exception {
//    request.setRequestURI("/%foo%/bar.txt");
//    this.response = new MockHttpServletResponse();
//    requestContext = new ServletRequestContext(null, request, response);
//
//    this.handler.handleRequest(requestContext);
//    assertThat(this.response.getStatus()).isEqualTo(404);
//  }

  @Test
  public void processPath() {
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
    assertThat(this.handler.processPath("   ")).isEqualTo("");
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
  public void initAllowedLocations() {
    PathResourceResolver resolver = (PathResourceResolver) this.handler.getResourceResolvers().get(0);
    Resource[] locations = resolver.getAllowedLocations();

    assertThat(locations.length).isEqualTo(3);
    assertThat(((ClassPathResource) locations[0]).getPath()).isEqualTo("test/");
    assertThat(((ClassPathResource) locations[1]).getPath()).isEqualTo("testalternatepath/");
    assertThat(((ClassPathResource) locations[2]).getPath()).isEqualTo("META-INF/resources/webjars/");
  }

  @Test
  public void initAllowedLocationsWithExplicitConfiguration() throws Exception {
    ClassPathResource location1 = new ClassPathResource("test/", getClass());
    ClassPathResource location2 = new ClassPathResource("testalternatepath/", getClass());

    PathResourceResolver pathResolver = new PathResourceResolver();
    pathResolver.setAllowedLocations(location1);

    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.setResourceResolvers(Collections.singletonList(pathResolver));

    handler.setLocations(Arrays.asList(location1, location2));
    handler.afterPropertiesSet();

    Resource[] locations = pathResolver.getAllowedLocations();
    assertThat(locations.length).isEqualTo(1);
    assertThat(((ClassPathResource) locations[0]).getPath()).isEqualTo("test/");
  }

  @Test
  public void notModified() throws Exception {
    request.setRequestURI("foo.css");
    this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
    requestContext = new ServletRequestContext(null, request, response);

    this.handler.handleRequest(requestContext);
    assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
  }

  @Test
  public void modified() throws Exception {
    request.setRequestURI("foo.css");
    this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
    this.handler.handleRequest(requestContext);
    assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
  }

  @Test
  public void directory() throws Exception {
    request.setRequestURI("js/");
    this.handler.handleRequest(requestContext);
    assertThat(this.response.getStatus()).isEqualTo(404);
  }

  @Test
  public void directoryInJarFile() throws Exception {
    request.setRequestURI("underscorejs/");
    this.handler.handleRequest(requestContext);
    assertThat(this.response.getStatus()).isEqualTo(404);
  }

  @Test
  public void missingResourcePath() throws Exception {
    request.setRequestURI("");
    this.handler.handleRequest(requestContext);
    assertThat(this.response.getStatus()).isEqualTo(404);
  }

  @Test
  public void unsupportedHttpMethod() throws Exception {
    request.setRequestURI("foo.css");
    this.request.setMethod("POST");
    assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class).isThrownBy(() ->
            this.handler.handleRequest(requestContext));
  }

  @Test
  public void resourceNotFound() throws Exception {
    for (HttpMethod method : HttpMethod.values()) {
      this.request = new MockHttpServletRequest("GET", "");
      this.response = new MockHttpServletResponse();
      resourceNotFound(method);
    }
  }

  private void resourceNotFound(HttpMethod httpMethod) throws Exception {
    this.request.setMethod(httpMethod.name());
    request.setRequestURI("not-there.css");
    requestContext = new ServletRequestContext(null, request, response);

    this.handler.handleRequest(requestContext);
    assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  public void partialContentByteRange() throws Exception {
    this.request.addHeader("Range", "bytes=0-1");
    request.setRequestURI("foo.txt");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(206);
    assertThat(this.response.getContentType()).isEqualTo("text/plain");
    assertThat(this.response.getContentLength()).isEqualTo(2);
    assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
    assertThat(this.response.getContentAsString()).isEqualTo("So");
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void partialContentByteRangeNoEnd() throws Exception {
    this.request.addHeader("Range", "bytes=9-");
    request.setRequestURI("foo.txt");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(206);
    assertThat(this.response.getContentType()).isEqualTo("text/plain");
    assertThat(this.response.getContentLength()).isEqualTo(1);
    assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
    assertThat(this.response.getContentAsString()).isEqualTo(".");
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void partialContentByteRangeLargeEnd() throws Exception {
    this.request.addHeader("Range", "bytes=9-10000");
    request.setRequestURI("foo.txt");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(206);
    assertThat(this.response.getContentType()).isEqualTo("text/plain");
    assertThat(this.response.getContentLength()).isEqualTo(1);
    assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
    assertThat(this.response.getContentAsString()).isEqualTo(".");
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void partialContentSuffixRange() throws Exception {
    this.request.addHeader("Range", "bytes=-1");
    request.setRequestURI("foo.txt");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(206);
    assertThat(this.response.getContentType()).isEqualTo("text/plain");
    assertThat(this.response.getContentLength()).isEqualTo(1);
    assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
    assertThat(this.response.getContentAsString()).isEqualTo(".");
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void partialContentSuffixRangeLargeSuffix() throws Exception {
    this.request.addHeader("Range", "bytes=-11");
    request.setRequestURI("foo.txt");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(206);
    assertThat(this.response.getContentType()).isEqualTo("text/plain");
    assertThat(this.response.getContentLength()).isEqualTo(10);
    assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-9/10");
    assertThat(this.response.getContentAsString()).isEqualTo("Some text.");
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void partialContentInvalidRangeHeader() throws Exception {
    this.request.addHeader("Range", "bytes= foo bar");
    request.setRequestURI("foo.txt");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(416);
    assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes */10");
    assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
    assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
  }

  @Test
  public void partialContentMultipleByteRanges() throws Exception {
    this.request.addHeader("Range", "bytes=0-1, 4-5, 8-9");
    request.setRequestURI("foo.txt");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(206);
    assertThat(this.response.getContentType().startsWith("multipart/byteranges; boundary=")).isTrue();

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

  @Test  // gh-25976
  public void partialContentByteRangeWithEncodedResource(GzipSupport.GzippedFiles gzippedFiles) throws Exception {
    String path = "js/foo.js";
    gzippedFiles.create(path);

    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.setResourceResolvers(Arrays.asList(new EncodedResourceResolver(), new PathResourceResolver()));
    handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));

    handler.afterPropertiesSet();

    this.request.addHeader("Accept-Encoding", "gzip");
    this.request.addHeader("Range", "bytes=0-1");
    request.setRequestURI(path);
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

  @Test  // gh-25976
  public void partialContentWithHttpHead() throws Exception {
    this.request.setMethod("HEAD");
    this.request.addHeader("Range", "bytes=0-1");
    request.setRequestURI("foo.txt");
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getStatus()).isEqualTo(206);
    assertThat(this.response.getContentType()).isEqualTo("text/plain");
    assertThat(this.response.getContentLength()).isEqualTo(2);
    assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
    assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
  }

  @Test  // SPR-14005
  public void doOverwriteExistingCacheControlHeaders() throws Exception {
    request.setRequestURI("foo.css");
    this.response.setHeader("Cache-Control", "no-store");

    this.handler.handleRequest(requestContext);

    assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
  }

  @Test
  public void ignoreLastModified() throws Exception {
    request.setRequestURI("foo.css");
    this.handler.setUseLastModified(false);
    this.handler.handleRequest(requestContext);

    assertThat(this.response.getContentType()).isEqualTo("text/css");
    assertThat(this.response.getContentLength()).isEqualTo(17);
    assertThat(this.response.containsHeader("Last-Modified")).isFalse();
    assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
  }

  @Test
  public void servletContextRootValidation() {
    StaticWebApplicationContext context = new StaticWebApplicationContext() {
      @Override
      public Resource getResource(String location) {
        return new FileBasedResource("/");
      }
    };

    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.setLocationValues(Collections.singletonList("/"));
    handler.setApplicationContext(context);

    assertThatIllegalStateException().isThrownBy(handler::afterPropertiesSet)
            .withMessage("The String-based location \"/\" should be relative to the web application root but " +
                    "resolved to a Resource of type: class cn.taketoday.core.io.FileBasedResource. " +
                    "If this is intentional, please pass it as a pre-configured Resource via setLocations.");
  }

  private long resourceLastModified(String resourceName) throws IOException {
    return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
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
