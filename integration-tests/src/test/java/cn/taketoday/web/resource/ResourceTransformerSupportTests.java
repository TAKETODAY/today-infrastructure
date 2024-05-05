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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@code ResourceTransformerSupport}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class ResourceTransformerSupportTests {

  private ResourceTransformerChain transformerChain;

  private TestResourceTransformerSupport transformer;

  private final HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");

  @BeforeEach
  public void setUp() {
    VersionResourceResolver versionResolver = new VersionResourceResolver();
    versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
    PathResourceResolver pathResolver = new PathResourceResolver();
    pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));
    List<ResourceResolver> resolvers = new ArrayList<>();
    resolvers.add(versionResolver);
    resolvers.add(pathResolver);
    this.transformerChain = new DefaultResourceTransformerChain(new DefaultResourceResolvingChain(resolvers), null);

    this.transformer = new TestResourceTransformerSupport();
    this.transformer.setResourceUrlProvider(createUrlProvider(resolvers));
  }

  private ResourceUrlProvider createUrlProvider(List<ResourceResolver> resolvers) {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
    handler.setResourceResolvers(resolvers);

    ResourceUrlProvider urlProvider = new ResourceUrlProvider();
    urlProvider.setHandlerMap(Collections.singletonMap("/resources/**", handler));
    return urlProvider;
  }

  @Test
  public void resolveUrlPath() {
    this.request.setRequestURI("/resources/main.css");
    String resourcePath = "/resources/bar.css";
    Resource resource = getResource("main.css");
    String actual = this.transformer.resolveUrlPath(
            resourcePath, new MockRequestContext(null, request, null), resource, this.transformerChain);

    assertThat(actual).isEqualTo("/resources/bar-11e16cf79faee7ac698c805cf28248d2.css");
  }

  @Test
  public void resolveUrlPathWithRelativePath() {
    Resource resource = getResource("main.css");
    String actual = this.transformer.resolveUrlPath(
            "bar.css", new MockRequestContext(null, request, null), resource, this.transformerChain);

    assertThat(actual).isEqualTo("bar-11e16cf79faee7ac698c805cf28248d2.css");
  }

  @Test
  public void resolveUrlPathWithRelativePathInParentDirectory() {
    Resource resource = getResource("images/image.png");

    String actual = this.transformer.resolveUrlPath("../bar.css",
            new MockRequestContext(null, request, null)
            , resource, this.transformerChain);

    assertThat(actual).isEqualTo("../bar-11e16cf79faee7ac698c805cf28248d2.css");
  }

  @Test
  public void toAbsolutePath() {
    String absolute = this.transformer.toAbsolutePath("img/image.png",
            new MockRequestContext(null, new HttpMockRequestImpl("GET", "/resources/style.css"), null));
    assertThat(absolute).isEqualTo("/resources/img/image.png");

    absolute = this.transformer.toAbsolutePath("/img/image.png",
            new MockRequestContext(null, new HttpMockRequestImpl("GET", "/resources/style.css"), null));
    assertThat(absolute).isEqualTo("/img/image.png");
  }

  private Resource getResource(String filePath) {
    return new ClassPathResource("test/" + filePath, getClass());
  }

  private static class TestResourceTransformerSupport extends ResourceTransformerSupport {

    @Override
    public Resource transform(RequestContext request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
      throw new IllegalStateException("Should never be called");
    }
  }

}
