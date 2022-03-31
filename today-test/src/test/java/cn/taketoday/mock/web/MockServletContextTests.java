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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.mock.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import cn.taketoday.core.io.FileSystemResourceLoader;
import cn.taketoday.http.MediaType;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockRequestDispatcher;
import cn.taketoday.mock.web.MockServletContext;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRegistration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockServletContext}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 19.02.2006
 */
@DisplayName("MockServletContext unit tests")
class MockServletContextTests {

	@Nested
	@DisplayName("with DefaultResourceLoader")
	class MockServletContextWithDefaultResourceLoaderTests {

		private final MockServletContext servletContext = new MockServletContext("org/springframework/mock");

		@Test
		void getResourcePaths() {
			Set<String> paths = servletContext.getResourcePaths("/web");
			assertThat(paths).isNotNull();
			assertThat(paths.contains("/web/MockServletContextTests.class")).isTrue();
		}

		@Test
		void getResourcePathsWithSubdirectories() {
			Set<String> paths = servletContext.getResourcePaths("/");
			assertThat(paths).isNotNull();
			assertThat(paths.contains("/web/")).isTrue();
		}

		@Test
		void getResourcePathsWithNonDirectory() {
			Set<String> paths = servletContext.getResourcePaths("/web/MockServletContextTests.class");
			assertThat(paths).isNull();
		}

		@Test
		void getResourcePathsWithInvalidPath() {
			Set<String> paths = servletContext.getResourcePaths("/web/invalid");
			assertThat(paths).isNull();
		}

		@Test
		void registerContextAndGetContext() {
			MockServletContext sc2 = new MockServletContext();
			servletContext.setContextPath("/");
			servletContext.registerContext("/second", sc2);
			assertThat(servletContext.getContext("/")).isSameAs(servletContext);
			assertThat(servletContext.getContext("/second")).isSameAs(sc2);
		}

		@Test
		void getMimeType() {
			assertThat(servletContext.getMimeType("test.html")).isEqualTo("text/html");
			assertThat(servletContext.getMimeType("test.gif")).isEqualTo("image/gif");
			assertThat(servletContext.getMimeType("test.foobar")).isNull();
		}

		/**
		 * Introduced to dispel claims in a thread on Stack Overflow:
		 * <a href="https://stackoverflow.com/questions/22986109/testing-spring-managed-servlet">Testing Spring managed servlet</a>
		 */
		@Test
		void getMimeTypeWithCustomConfiguredType() {
			servletContext.addMimeType("enigma", new MediaType("text", "enigma"));
			assertThat(servletContext.getMimeType("filename.enigma")).isEqualTo("text/enigma");
		}

		@Test
		void servletVersion() {
			assertThat(servletContext.getMajorVersion()).isEqualTo(3);
			assertThat(servletContext.getMinorVersion()).isEqualTo(1);
			assertThat(servletContext.getEffectiveMajorVersion()).isEqualTo(3);
			assertThat(servletContext.getEffectiveMinorVersion()).isEqualTo(1);

			servletContext.setMajorVersion(4);
			servletContext.setMinorVersion(0);
			servletContext.setEffectiveMajorVersion(4);
			servletContext.setEffectiveMinorVersion(0);
			assertThat(servletContext.getMajorVersion()).isEqualTo(4);
			assertThat(servletContext.getMinorVersion()).isEqualTo(0);
			assertThat(servletContext.getEffectiveMajorVersion()).isEqualTo(4);
			assertThat(servletContext.getEffectiveMinorVersion()).isEqualTo(0);
		}

		@Test
		void registerAndUnregisterNamedDispatcher() throws Exception {
			final String name = "test-servlet";
			final String url = "/test";
			assertThat(servletContext.getNamedDispatcher(name)).isNull();

			servletContext.registerNamedDispatcher(name, new MockRequestDispatcher(url));
			RequestDispatcher namedDispatcher = servletContext.getNamedDispatcher(name);
			assertThat(namedDispatcher).isNotNull();
			MockHttpServletResponse response = new MockHttpServletResponse();
			namedDispatcher.forward(new MockHttpServletRequest(servletContext), response);
			assertThat(response.getForwardedUrl()).isEqualTo(url);

			servletContext.unregisterNamedDispatcher(name);
			assertThat(servletContext.getNamedDispatcher(name)).isNull();
		}

		@Test
		void getNamedDispatcherForDefaultServlet() throws Exception {
			final String name = "default";
			RequestDispatcher namedDispatcher = servletContext.getNamedDispatcher(name);
			assertThat(namedDispatcher).isNotNull();

			MockHttpServletResponse response = new MockHttpServletResponse();
			namedDispatcher.forward(new MockHttpServletRequest(servletContext), response);
			assertThat(response.getForwardedUrl()).isEqualTo(name);
		}

		@Test
		void setDefaultServletName() throws Exception {
			final String originalDefault = "default";
			final String newDefault = "test";
			assertThat(servletContext.getNamedDispatcher(originalDefault)).isNotNull();

			servletContext.setDefaultServletName(newDefault);
			assertThat(servletContext.getDefaultServletName()).isEqualTo(newDefault);
			assertThat(servletContext.getNamedDispatcher(originalDefault)).isNull();

			RequestDispatcher namedDispatcher = servletContext.getNamedDispatcher(newDefault);
			assertThat(namedDispatcher).isNotNull();
			MockHttpServletResponse response = new MockHttpServletResponse();
			namedDispatcher.forward(new MockHttpServletRequest(servletContext), response);
			assertThat(response.getForwardedUrl()).isEqualTo(newDefault);
		}

		/**
		 * @since 4.0
		 */
		@Test
		void getServletRegistration() {
			assertThat(servletContext.getServletRegistration("servlet")).isNull();
		}

		/**
		 * @since 4.0
		 */
		@Test
		void getServletRegistrations() {
			Map<String, ? extends ServletRegistration> servletRegistrations = servletContext.getServletRegistrations();
			assertThat(servletRegistrations).isNotNull();
			assertThat(servletRegistrations.size()).isEqualTo(0);
		}

		/**
		 * @since 4.0
		 */
		@Test
		void getFilterRegistration() {
			assertThat(servletContext.getFilterRegistration("filter")).isNull();
		}

		/**
		 * @since 4.0
		 */
		@Test
		void getFilterRegistrations() {
			Map<String, ? extends FilterRegistration> filterRegistrations = servletContext.getFilterRegistrations();
			assertThat(filterRegistrations).isNotNull();
			assertThat(filterRegistrations.size()).isEqualTo(0);
		}

	}

	/**
	 * @since 4.0
	 */
	@Nested
	@DisplayName("with FileSystemResourceLoader")
	class MockServletContextWithFileSystemResourceLoaderTests {

		private final MockServletContext servletContext =
				new MockServletContext( "org/springframework/mock", new FileSystemResourceLoader());

		@Test
		void getResourcePathsWithRelativePathToWindowsCDrive() {
			Set<String> paths = servletContext.getResourcePaths("C:\\temp");
			assertThat(paths).isNull();
		}

		@Test
		void getResourceWithRelativePathToWindowsCDrive() throws Exception {
			URL resource = servletContext.getResource("C:\\temp");
			assertThat(resource).isNull();
		}

		@Test
		void getResourceAsStreamWithRelativePathToWindowsCDrive() {
			InputStream inputStream = servletContext.getResourceAsStream("C:\\temp");
			assertThat(inputStream).isNull();
		}

		@Test
		void getRealPathWithRelativePathToWindowsCDrive() {
			String realPath = servletContext.getRealPath("C:\\temp");

			if (OS.WINDOWS.isCurrentOs()) {
				assertThat(realPath).isNull();
			}
			else {
				assertThat(realPath).isNotNull();
			}
		}

	}

}
