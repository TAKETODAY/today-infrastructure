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

package cn.taketoday.mock.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import cn.taketoday.core.io.FileSystemResourceLoader;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.api.RequestDispatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockContextImpl}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
@DisplayName("MockContext unit tests")
class MockContextTests {

  @Nested
  @DisplayName("with DefaultResourceLoader")
  class MockContextWithDefaultResourceLoaderTests {

    private final MockContextImpl mockContext = new MockContextImpl("cn/taketoday/mock");

    @Test
    void getResourcePaths() {
      Set<String> paths = mockContext.getResourcePaths("/web");
      assertThat(paths).isNotNull();
      assertThat(paths.contains("/web/MockContextTests.class")).isTrue();
    }

    @Test
    void getResourcePathsWithSubdirectories() {
      Set<String> paths = mockContext.getResourcePaths("/");
      assertThat(paths).isNotNull();
      assertThat(paths.contains("/web/")).isTrue();
    }

    @Test
    void getResourcePathsWithNonDirectory() {
      Set<String> paths = mockContext.getResourcePaths("/web/MockContextTests.class");
      assertThat(paths).isNull();
    }

    @Test
    void getResourcePathsWithInvalidPath() {
      Set<String> paths = mockContext.getResourcePaths("/web/invalid");
      assertThat(paths).isNull();
    }

    @Test
    void getMimeType() {
      assertThat(mockContext.getMimeType("test.html")).isEqualTo("text/html");
      assertThat(mockContext.getMimeType("test.gif")).isEqualTo("image/gif");
      assertThat(mockContext.getMimeType("test.foobar")).isNull();
    }

    /**
     * Introduced to dispel claims in a thread on Stack Overflow:
     * <a href="https://stackoverflow.com/questions/22986109/testing-spring-managed-servlet">Testing Infra managed servlet</a>
     */
    @Test
    void getMimeTypeWithCustomConfiguredType() {
      mockContext.addMimeType("enigma", new MediaType("text", "enigma"));
      assertThat(mockContext.getMimeType("filename.enigma")).isEqualTo("text/enigma");
    }

    @Test
    void mockVersion() {
      assertThat(mockContext.getMajorVersion()).isEqualTo(3);
      assertThat(mockContext.getMinorVersion()).isEqualTo(1);
      assertThat(mockContext.getEffectiveMajorVersion()).isEqualTo(3);
      assertThat(mockContext.getEffectiveMinorVersion()).isEqualTo(1);

      mockContext.setMajorVersion(4);
      mockContext.setMinorVersion(0);
      mockContext.setEffectiveMajorVersion(4);
      mockContext.setEffectiveMinorVersion(0);
      assertThat(mockContext.getMajorVersion()).isEqualTo(4);
      assertThat(mockContext.getMinorVersion()).isEqualTo(0);
      assertThat(mockContext.getEffectiveMajorVersion()).isEqualTo(4);
      assertThat(mockContext.getEffectiveMinorVersion()).isEqualTo(0);
    }

    @Test
    void registerAndUnregisterNamedDispatcher() throws Exception {
      final String name = "test-servlet";
      final String url = "/test";
      assertThat(mockContext.getNamedDispatcher(name)).isNull();

      mockContext.registerNamedDispatcher(name, new MockRequestDispatcher(url));
      RequestDispatcher namedDispatcher = mockContext.getNamedDispatcher(name);
      assertThat(namedDispatcher).isNotNull();
      MockHttpResponseImpl response = new MockHttpResponseImpl();
      namedDispatcher.forward(new HttpMockRequestImpl(mockContext), response);
      assertThat(response.getForwardedUrl()).isEqualTo(url);

      mockContext.unregisterNamedDispatcher(name);
      assertThat(mockContext.getNamedDispatcher(name)).isNull();
    }

    @Test
    void getNamedDispatcherForDefault() throws Exception {
      final String name = "default";
      RequestDispatcher namedDispatcher = mockContext.getNamedDispatcher(name);
      assertThat(namedDispatcher).isNotNull();

      MockHttpResponseImpl response = new MockHttpResponseImpl();
      namedDispatcher.forward(new HttpMockRequestImpl(mockContext), response);
      assertThat(response.getForwardedUrl()).isEqualTo(name);
    }

    @Test
    void setDefaultServletName() throws Exception {
      final String originalDefault = "default";
      final String newDefault = "test";
      assertThat(mockContext.getNamedDispatcher(originalDefault)).isNotNull();

      mockContext.setDefaultMockName(newDefault);
      assertThat(mockContext.getDefaultMockName()).isEqualTo(newDefault);
      assertThat(mockContext.getNamedDispatcher(originalDefault)).isNull();

      RequestDispatcher namedDispatcher = mockContext.getNamedDispatcher(newDefault);
      assertThat(namedDispatcher).isNotNull();
      MockHttpResponseImpl response = new MockHttpResponseImpl();
      namedDispatcher.forward(new HttpMockRequestImpl(mockContext), response);
      assertThat(response.getForwardedUrl()).isEqualTo(newDefault);
    }

  }

  /**
   * @since 4.0
   */
  @Nested
  @DisplayName("with FileSystemResourceLoader")
  class MockContextWithFileSystemResourceLoaderTests {

    private final MockContextImpl mockContext =
            new MockContextImpl("cn/taketoday/mock", new FileSystemResourceLoader());

    @Test
    void getResourcePathsWithRelativePathToWindowsCDrive() {
      Set<String> paths = mockContext.getResourcePaths("C:\\temp");
      assertThat(paths).isNull();
    }

    @Test
    void getResourceWithRelativePathToWindowsCDrive() throws Exception {
      URL resource = mockContext.getResource("C:\\temp");
      assertThat(resource).isNull();
    }

    @Test
    void getResourceAsStreamWithRelativePathToWindowsCDrive() {
      InputStream inputStream = mockContext.getResourceAsStream("C:\\temp");
      assertThat(inputStream).isNull();
    }

    @Test
    void getRealPathWithRelativePathToWindowsCDrive() {
      String realPath = mockContext.getRealPath("C:\\temp");

      if (OS.WINDOWS.isCurrentOs()) {
        assertThat(realPath).isNull();
      }
      else {
        assertThat(realPath).isNotNull();
      }
    }

  }

}
