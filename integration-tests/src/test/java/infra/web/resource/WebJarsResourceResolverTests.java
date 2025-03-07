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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for
 * {@link infra.web.resource.WebJarsResourceResolver}.
 *
 * @author Brian Clozel
 */
public class WebJarsResourceResolverTests {

  private List<Resource> locations;

  private WebJarsResourceResolver resolver;

  private ResourceResolvingChain chain;

  private final HttpMockRequest request = new HttpMockRequestImpl();

  private MockRequestContext requestContext;

  @BeforeEach
  public void setup() {
    // for this to work, an actual WebJar must be on the test classpath
    this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars"));
    this.resolver = new WebJarsResourceResolver();
    this.chain = mock(ResourceResolvingChain.class);
    this.requestContext = new MockRequestContext(null, request, new MockHttpResponseImpl());
  }

  @Test
  public void resolveUrlExisting() {
    this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
    String file = "/foo/2.3/foo.txt";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(file);

    String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

    assertThat(actual).isEqualTo(file);
    verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
  }

  @Test
  public void resolveUrlExistingNotInJarFile() {
    this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
    String file = "foo/foo.txt";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

    String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

    assertThat(actual).isNull();
    verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
    verify(this.chain, never()).resolveUrlPath("foo/2.3/foo.txt", this.locations);
  }

  @Test
  public void resolveUrlWebJarResource() {
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
  public void resolveUrlWebJarResourceNotFound() {
    String file = "something/something.js";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

    String actual = this.resolver.resolveUrlPath(file, this.locations, this.chain);

    assertThat(actual).isNull();
    verify(this.chain, times(1)).resolveUrlPath(file, this.locations);
    verify(this.chain, never()).resolveUrlPath(null, this.locations);
  }

  @Test
  public void resolveResourceExisting() {
    Resource expected = mock(Resource.class);
    this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
    String file = "foo/2.3/foo.txt";
    given(this.chain.resolveResource(this.requestContext, file, this.locations)).willReturn(expected);

    Resource actual = this.resolver.resolveResource(this.requestContext, file, this.locations, this.chain);

    assertThat(actual).isEqualTo(expected);
    verify(this.chain, times(1)).resolveResource(this.requestContext, file, this.locations);
  }

  @Test
  public void resolveResourceNotFound() {
    String file = "something/something.js";
    given(this.chain.resolveUrlPath(file, this.locations)).willReturn(null);

    Resource actual = this.resolver.resolveResource(this.requestContext, file, this.locations, this.chain);

    assertThat(actual).isNull();
    verify(this.chain, times(1)).resolveResource(this.requestContext, file, this.locations);
    verify(this.chain, never()).resolveResource(this.requestContext, null, this.locations);
  }

  @Test
  public void resolveResourceWebJar() {
    Resource expected = mock(Resource.class);
    String file = "underscorejs/underscore.js";
    String expectedPath = "underscorejs/1.8.3/underscore.js";
    this.locations = Collections.singletonList(new ClassPathResource("/META-INF/resources/webjars/", getClass()));
    given(this.chain.resolveResource(this.requestContext, expectedPath, this.locations)).willReturn(expected);

    Resource actual = this.resolver.resolveResource(this.requestContext, file, this.locations, this.chain);

    assertThat(actual).isEqualTo(expected);
    verify(this.chain, times(1)).resolveResource(this.requestContext, file, this.locations);
  }

}
