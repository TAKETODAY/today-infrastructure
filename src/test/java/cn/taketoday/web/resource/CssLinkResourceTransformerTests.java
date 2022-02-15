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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.resource.EncodedResourceResolver.EncodedResource;
import cn.taketoday.web.resource.GzipSupport.GzippedFiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CssLinkResourceTransformer}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.1
 */
@ExtendWith(GzipSupport.class)
public class CssLinkResourceTransformerTests {

  private ResourceTransformerChain transformerChain;

  private MockHttpServletRequest request;

  @BeforeEach
  public void setUp() {
    VersionResourceResolver versionResolver = new VersionResourceResolver();
    versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
    PathResourceResolver pathResolver = new PathResourceResolver();
    pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));
    List<ResourceResolver> resolvers = new ArrayList<>();
    resolvers.add(versionResolver);
    resolvers.add(new PathResourceResolver());
    ResourceUrlProvider resourceUrlProvider = createUrlProvider(resolvers);

    CssLinkResourceTransformer cssLinkTransformer = new CssLinkResourceTransformer();
    cssLinkTransformer.setResourceUrlProvider(resourceUrlProvider);

    this.transformerChain = new DefaultResourceTransformerChain(
            new DefaultResourceResolverChain(resolvers), Collections.singletonList(cssLinkTransformer));
  }

  private ResourceUrlProvider createUrlProvider(List<ResourceResolver> resolvers) {
    ResourceHttpRequestHandler resourceHandler = new ResourceHttpRequestHandler();
    resourceHandler.setResourceResolvers(resolvers);
    resourceHandler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));

    ResourceUrlProvider resourceUrlProvider = new ResourceUrlProvider();
    resourceUrlProvider.setHandlerMap(Collections.singletonMap("/static/**", resourceHandler));
    return resourceUrlProvider;
  }

  @Test
  public void transform() throws Exception {
    this.request = new MockHttpServletRequest("GET", "/static/main.css");
    Resource css = getResource("main.css");
    String expected = "\n" +
            "@import url(\"/static/bar-11e16cf79faee7ac698c805cf28248d2.css?#iefix\");\n" +
            "@import url('/static/bar-11e16cf79faee7ac698c805cf28248d2.css#bla-normal');\n" +
            "@import url(/static/bar-11e16cf79faee7ac698c805cf28248d2.css);\n\n" +
            "@import \"/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css\";\n" +
            "@import '/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css';\n\n" +
            "body { background: url(\"/static/images/image-f448cd1d5dba82b774f3202c878230b3.png?#iefix\") }\n";

    TransformedResource actual = (TransformedResource) this.transformerChain.transform(this.request, css);
    String result = new String(actual.getByteArray(), StandardCharsets.UTF_8);
    result = StringUtils.deleteAny(result, "\r");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void transformNoLinks() throws Exception {
    this.request = new MockHttpServletRequest("GET", "/static/foo.css");
    Resource expected = getResource("foo.css");
    Resource actual = this.transformerChain.transform(this.request, expected);
    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void transformExtLinksNotAllowed() throws Exception {
    this.request = new MockHttpServletRequest("GET", "/static/external.css");

    List<ResourceTransformer> transformers = Collections.singletonList(new CssLinkResourceTransformer());
    ResourceResolverChain mockChain = Mockito.mock(DefaultResourceResolverChain.class);
    ResourceTransformerChain chain = new DefaultResourceTransformerChain(mockChain, transformers);

    Resource resource = getResource("external.css");
    String expected = "@import url(\"https://example.org/fonts/css\");\n" +
            "body { background: url(\"file:///home/spring/image.png\") }\n" +
            "figure { background: url(\"//example.org/style.css\")}";

    TransformedResource transformedResource = (TransformedResource) chain.transform(this.request, resource);
    String result = new String(transformedResource.getByteArray(), StandardCharsets.UTF_8);
    result = StringUtils.deleteAny(result, "\r");
    assertThat(result).isEqualTo(expected);

    List<Resource> locations = Collections.singletonList(resource);
    Mockito.verify(mockChain, Mockito.never()).resolveUrlPath("https://example.org/fonts/css", locations);
    Mockito.verify(mockChain, Mockito.never()).resolveUrlPath("file:///home/spring/image.png", locations);
    Mockito.verify(mockChain, Mockito.never()).resolveUrlPath("//example.org/style.css", locations);
  }

  @Test
  public void transformSkippedForNonCssResource() throws Exception {
    this.request = new MockHttpServletRequest("GET", "/static/images/image.png");
    Resource expected = getResource("images/image.png");
    Resource actual = this.transformerChain.transform(this.request, expected);

    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void transformSkippedForGzippedResource(GzippedFiles gzippedFiles) throws Exception {
    gzippedFiles.create("main.css");

    this.request = new MockHttpServletRequest("GET", "/static/main.css");
    Resource original = new ClassPathResource("test/main.css", getClass());
    EncodedResource gzipped = new EncodedResource(original, "gzip", ".gz");
    Resource actual = this.transformerChain.transform(this.request, gzipped);

    assertThat(actual).isSameAs(gzipped);
  }

  @Test // https://github.com/spring-projects/spring-framework/issues/22602
  public void transformEmptyUrlFunction() throws Exception {
    this.request = new MockHttpServletRequest("GET", "/static/empty_url_function.css");
    Resource css = getResource("empty_url_function.css");
    String expected =
            ".fooStyle {\n" +
                    "\tbackground: transparent url() no-repeat left top;\n" +
                    "}";

    TransformedResource actual = (TransformedResource) this.transformerChain.transform(this.request, css);
    String result = new String(actual.getByteArray(), StandardCharsets.UTF_8);
    result = StringUtils.deleteAny(result, "\r");
    assertThat(result).isEqualTo(expected);
  }

  private Resource getResource(String filePath) {
    return new ClassPathResource("test/" + filePath, getClass());
  }

}
