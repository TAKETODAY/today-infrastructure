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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.UrlBasedResource;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.ResourceHandlerRegistry;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.mock.MockHttpServletResponse;
import cn.taketoday.web.mock.MockServletContext;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.StandardWebServletApplicationContext;
import cn.taketoday.web.util.UriUtils;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Integration tests for static resource handling.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceHttpRequestHandlerIntegrationTests {

  private final MockServletContext servletContext = new MockServletContext();

  private final MockServletConfig servletConfig = new MockServletConfig(this.servletContext);

  public static Stream<Arguments> argumentSource() {
    return Stream.of(
            arguments(true, "/cp"),
            arguments(true, "/fs"),
            arguments(true, "/url")
    );
  }

  @ParameterizedTest
  @MethodSource("argumentSource")
  void cssFile(boolean usePathPatterns, String pathPrefix) throws Exception {
    MockHttpServletRequest request = initRequest(pathPrefix + "/test/foo.css");
    MockHttpServletResponse response = new MockHttpServletResponse();

    DispatcherServlet servlet = initDispatcherServlet(WebConfig.class);
    servlet.service(request, response);

    String description = "usePathPattern=" + usePathPatterns + ", prefix=" + pathPrefix;
    assertThat(response.getStatus()).as(description).isEqualTo(200);
    assertThat(response.getContentType()).as(description).isEqualTo("text/css");
    assertThat(response.getContentAsString()).as(description).isEqualTo("h1 { color:red; }");
  }

  @ParameterizedTest
  @MethodSource("argumentSource")
  void classpathLocationWithEncodedPath(boolean usePathPatterns, String pathPrefix) throws Exception {
    MockHttpServletRequest request = initRequest(pathPrefix + "/test/foo with spaces.css");
    MockHttpServletResponse response = new MockHttpServletResponse();

    DispatcherServlet servlet = initDispatcherServlet(WebConfig.class);
    servlet.service(request, response);

    String description = "usePathPattern=" + usePathPatterns + ", prefix=" + pathPrefix;
    assertThat(response.getStatus()).as(description).isEqualTo(200);
    assertThat(response.getContentType()).as(description).isEqualTo("text/css");
    assertThat(response.getContentAsString()).as(description).isEqualTo("h1 { color:red; }");
  }

  private DispatcherServlet initDispatcherServlet(Class<?>... configClasses)
          throws ServletException {

    StandardWebServletApplicationContext context = new StandardWebServletApplicationContext();
    context.register(configClasses);
    context.refresh();

    DispatcherServlet servlet = new DispatcherServlet(context);
//    servlet.setApplicationContext(context);
    servlet.init(this.servletConfig);
    return servlet;
  }

  private MockHttpServletRequest initRequest(String path) {
    path = UriUtils.encodePath(path, StandardCharsets.UTF_8);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    return request;
  }

  @EnableWebMvc
  static class WebConfig implements WebMvcConfiguration {

    @Override
    public void configureResourceHandler(ResourceHandlerRegistry registry) {
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
      FileBasedResource fileSystemLocation = new FileBasedResource(path);
      registry.addResourceHandler(pattern).addResourceLocations(fileSystemLocation);
    }

    protected void registerUrlLocation(String pattern, String path, ResourceHandlerRegistry registry) {
      try {
        UrlBasedResource urlLocation = new UrlBasedResource(path);
        registry.addResourceHandler(pattern).addResourceLocations(urlLocation);
      }
      catch (MalformedURLException ex) {
        throw new IllegalStateException(ex);
      }
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

}
