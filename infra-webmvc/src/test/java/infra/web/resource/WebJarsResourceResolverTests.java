/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.resource;

import org.junit.jupiter.api.Test;
import org.webjars.WebJarAssetLocator;
import org.webjars.WebJarVersionLocator;

import java.util.List;

import infra.core.io.ByteArrayResource;
import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class WebJarsResourceResolverTests {

  private static final String ASSET = "sample/script.js";

  private static final String VERSIONED = "sample/1.0/script.js";

  private final WebJarAssetLocator locator = mock(WebJarAssetLocator.class);

  private final WebJarsResourceResolver resolver = new WebJarsResourceResolver(locator);

  private final ResourceResolvingChain chain = mock(ResourceResolvingChain.class);

  private final List<Resource> locations = List.of();

  @Test
  void prefersDirectMatchesOverLocator() {
    Resource asset = new ByteArrayResource(new byte[] { 1 });
    given(chain.resolveResource(null, ASSET, locations)).willReturn(asset);
    given(chain.resolveUrlPath(ASSET, locations)).willReturn(ASSET);

    assertThat(resolver.resolveResource(null, ASSET, locations, chain)).isSameAs(asset);
    assertThat(resolver.resolveUrlPath(ASSET, locations, chain)).isEqualTo(ASSET);
    then(locator).shouldHaveNoInteractions();
  }

  @Test
  void resolvesVersionedResourcesAndUrlsThroughChain() {
    Resource asset = new ByteArrayResource(new byte[] { 1 });
    given(locator.getFullPathExact("sample", "script.js"))
            .willReturn("META-INF/resources/webjars/" + VERSIONED);
    given(chain.resolveResource(null, VERSIONED, locations)).willReturn(asset);
    given(chain.resolveUrlPath(VERSIONED, locations)).willReturn(VERSIONED);

    assertThat(resolver.resolveResource(null, ASSET, locations, chain)).isSameAs(asset);
    assertThat(resolver.resolveUrlPath("/" + ASSET, locations, chain)).isEqualTo(VERSIONED);
  }

  @Test
  void returnsNullForMissingAssetsAndPathsWithoutAssetName() {
    assertThat(resolver.resolveResource(null, ASSET, locations, chain)).isNull();
    then(locator).should().getFullPathExact("sample", "script.js");
    assertThat(resolver.resolveUrlPath("sample", locations, chain)).isNull();
    then(locator).shouldHaveNoMoreInteractions();
  }

  @Test
  void defaultResolverLeavesAlreadyVersionedAssetsUnchanged() {
    given(chain.resolveUrlPath(VERSIONED, locations)).willReturn(VERSIONED);

    assertThat(new WebJarsResourceResolver().resolveUrlPath(VERSIONED, locations, chain)).isEqualTo(VERSIONED);
  }

  @Test
  void liteResolverUsesSuppliedVersionLocator() {
    WebJarVersionLocator versionLocator = mock(WebJarVersionLocator.class);
    given(versionLocator.fullPath("sample", "script.js"))
            .willReturn("META-INF/resources/webjars/" + VERSIONED);
    given(chain.resolveUrlPath(VERSIONED, locations)).willReturn(VERSIONED);

    assertThat(new LiteWebJarsResourceResolver(versionLocator).resolveUrlPath(ASSET, locations, chain))
            .isEqualTo(VERSIONED);
  }

}
