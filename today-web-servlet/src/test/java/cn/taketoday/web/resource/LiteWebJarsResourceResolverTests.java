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

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.mock.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/15 16:17
 */
class LiteWebJarsResourceResolverTests {

  private final List<Resource> locations = List.of(new ClassPathResource("/META-INF/resources/webjars"));

  // for this to work, an actual WebJar must be on the test classpath
  private final LiteWebJarsResourceResolver resolver = new LiteWebJarsResourceResolver();

  private final ResourceResolvingChain chain = mock();

  private final HttpServletRequest request = new MockHttpServletRequest();

  private final ServletRequestContext requestContext = new ServletRequestContext(null, request, new MockHttpServletResponse());

  @Test
  void resolveUrlExisting() {
    String file = "/foo/2.3/foo.txt";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(file);

    String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

    assertThat(actual).isEqualTo(file);
    verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
  }

  @Test
  void resolveUrlExistingNotInJarFile() {
    String file = "foo/foo.txt";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

    String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

    assertThat(actual).isNull();
    verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
    verify(this.chain, never()).resolveUrlPath("foo/2.3/foo.txt", this.locations);
  }

  @Test
  void resolveUrlWebJarResource() {
    String file = "underscorejs/underscore.js";
    String expected = "underscorejs/1.8.3/underscore.js";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);
    given(this.chain.resolveUrlPath(expected, this.locations)).willReturn(expected);

    String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

    assertThat(actual).isEqualTo(expected);
    verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
    verify(this.chain, times(1)).resolveUrlPath(expected, this.locations);
  }

  @Test
  void resolveUrlWebJarResourceNotFound() {
    String file = "something/something.js";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

    String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

    assertThat(actual).isNull();
    verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
    verify(this.chain, never()).resolveUrlPath(null, this.locations);
  }

  @Test
  void resolveResourceExisting() {
    Resource expected = mock();
    String file = "foo/2.3/foo.txt";
    given(this.chain.resolveResource(this.requestContext, file, this.locations)).willReturn(expected);

    Resource actual = this.resolver.resolveResource(this.requestContext, file, this.locations, this.chain);

    assertThat(actual).isEqualTo(expected);
    verify(this.chain, times(1)).resolveResource(this.requestContext, file, this.locations);
  }

  @Test
  void resolveResourceNotFound() {
    String file = "something/something.js";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

    Resource actual = this.resolver.resolveResource(this.requestContext, file, this.locations, this.chain);

    assertThat(actual).isNull();
    verify(this.chain, times(1)).resolveResource(this.requestContext, file, this.locations);
    verify(this.chain, never()).resolveResource(this.requestContext, null, this.locations);
  }

  @Test
  void resolveResourceWebJar() {
    Resource expected = mock();
    String file = "underscorejs/underscore.js";
    String expectedPath = "underscorejs/1.8.3/underscore.js";
    given(this.chain.resolveResource(this.requestContext, expectedPath, this.locations)).willReturn(expected);

    Resource actual = this.resolver.resolveResource(this.requestContext, file, this.locations, this.chain);

    assertThat(actual).isEqualTo(expected);
    verify(this.chain, times(1)).resolveResource(this.requestContext, file, this.locations);
  }

}