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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import infra.core.io.ClassPathResource;
import infra.core.io.FileSystemResource;
import infra.core.io.UrlResource;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockMockConfig;
import infra.web.annotation.ControllerAdvice;
import infra.web.config.EnableWebMvc;
import infra.web.config.ResourceHandlerRegistry;
import infra.web.config.WebMvcConfigurer;
import infra.web.handler.ResponseEntityExceptionHandler;
import infra.web.mock.MockDispatcher;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;
import infra.web.util.UriUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for static resource handling.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceHttpRequestHandlerIntegrationTests {

  private final MockContextImpl mockContext = new MockContextImpl();

  private final MockMockConfig mockConfig = new MockMockConfig(this.mockContext);

  public static Stream<Arguments> argumentSource() {
    return Stream.of(
            Arguments.arguments(true, "/cp"),
            Arguments.arguments(true, "/fs"),
            Arguments.arguments(true, "/url")
    );
  }

  @ParameterizedTest
  @MethodSource("argumentSource")
  void cssFile(boolean usePathPatterns, String pathPrefix) throws Exception {
    HttpMockRequestImpl request = initRequest(pathPrefix + "/test/foo.css");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    MockDispatcher servlet = initDispatcher(WebConfig.class);
    servlet.service(request, response);

    String description = "usePathPattern=" + usePathPatterns + ", prefix=" + pathPrefix;
    assertThat(response.getStatus()).as(description).isEqualTo(200);
    assertThat(response.getContentType()).as(description).isEqualTo("text/css");
    assertThat(response.getContentAsString()).as(description).isEqualTo("h1 { color:red; }");
  }

  @ParameterizedTest
  @MethodSource("argumentSource")
  void classpathLocationWithEncodedPath(boolean usePathPatterns, String pathPrefix) throws Exception {
    HttpMockRequestImpl request = initRequest(pathPrefix + "/test/foo with spaces.css");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    MockDispatcher servlet = initDispatcher(WebConfig.class);
    servlet.service(request, response);

    String description = "usePathPattern=" + usePathPatterns + ", prefix=" + pathPrefix;
    assertThat(response.getStatus()).as(description).isEqualTo(200);
    assertThat(response.getContentType()).as(description).isEqualTo("text/css");
    assertThat(response.getContentAsString()).as(description).isEqualTo("h1 { color:red; }");
  }

  @Test
  void testNoResourceFoundException() throws Exception {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setMockConfig(this.mockConfig);
    context.register(WebConfig.class);
    context.register(GlobalExceptionHandler.class);
    context.refresh();

    MockDispatcher servlet = new MockDispatcher();
    servlet.setApplicationContext(context);
    servlet.init(this.mockConfig);

    HttpMockRequestImpl request = initRequest("/cp/non-existing");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    servlet.service(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
//    assertThat(response.getContentType()).isEqualTo("application/problem+json");
//    assertThat(response.getContentAsString()).isEqualTo("""
//        {"type":"about:blank",\
//        "title":"Not Found",\
//        "status":404,\
//        "detail":"No static resource non-existing.",\
//        "instance":"/cp/non-existing"}\
//        """);
  }

  private MockDispatcher initDispatcher(Class<?>... configClasses) {
    var context = new AnnotationConfigWebApplicationContext();
    context.setMockContext(this.mockContext);
    context.register(configClasses);
    context.refresh();

    MockDispatcher servlet = new MockDispatcher(context);
    servlet.init(this.mockConfig);
    return servlet;
  }

  private HttpMockRequestImpl initRequest(String path) {
    path = UriUtils.encodePath(path, StandardCharsets.UTF_8);
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", path);
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    return request;
  }

  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      ClassPathResource classPathLocation = new ClassPathResource("", getClass());
      String path = getPath(classPathLocation);

      registerClasspathLocation("/cp/**", classPathLocation, registry);
      registerFileSystemLocation("/fs/**", path, registry);
      registerUrlLocation("/url/**", "file:" + path, registry);
    }

    protected void registerClasspathLocation(String pattern, ClassPathResource resource, ResourceHandlerRegistry registry) {
      registry.addResourceHandler(pattern).addResourceLocations(resource);
    }

    protected void registerFileSystemLocation(String pattern, String path, ResourceHandlerRegistry registry) {
      FileSystemResource fileSystemLocation = new FileSystemResource(path);
      registry.addResourceHandler(pattern).addResourceLocations(fileSystemLocation);
    }

    protected void registerUrlLocation(String pattern, String path, ResourceHandlerRegistry registry) {
      try {
        UrlResource urlLocation = new UrlResource(path);
        registry.addResourceHandler(pattern).addResourceLocations(urlLocation);
      }
      catch (MalformedURLException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
      converters.add(new MappingJackson2HttpMessageConverter());
    }

    private String getPath(ClassPathResource resource) {
      try {
        return resource.getFile().getCanonicalPath().replace('\\', '/').replace("classes/java", "resources") + "/";
      }
      catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  @ControllerAdvice
  private static class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  }

}
