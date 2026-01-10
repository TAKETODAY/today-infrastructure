/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 2.0
 */
class ResourceOverridingShadowingClassLoaderTests {

  private static final String EXISTING_RESOURCE = "infra/instrument/classloading/testResource.xml";

  private ClassLoader thisClassLoader = getClass().getClassLoader();

  private ResourceOverridingShadowingClassLoader overridingLoader = new ResourceOverridingShadowingClassLoader(thisClassLoader);

  @Test
  public void testFindsExistingResourceWithGetResourceAndNoOverrides() {
    assertThat(thisClassLoader.getResource(EXISTING_RESOURCE)).isNotNull();
    assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNotNull();
  }

  @Test
  public void testDoesNotFindExistingResourceWithGetResourceAndNullOverride() {
    assertThat(thisClassLoader.getResource(EXISTING_RESOURCE)).isNotNull();
    overridingLoader.override(EXISTING_RESOURCE, null);
    assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNull();
  }

  @Test
  public void testFindsExistingResourceWithGetResourceAsStreamAndNoOverrides() {
    assertThat(thisClassLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
    assertThat(overridingLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
  }

  @Test
  public void testDoesNotFindExistingResourceWithGetResourceAsStreamAndNullOverride() {
    assertThat(thisClassLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
    overridingLoader.override(EXISTING_RESOURCE, null);
    assertThat(overridingLoader.getResourceAsStream(EXISTING_RESOURCE)).isNull();
  }

  @Test
  public void testFindsExistingResourceWithGetResourcesAndNoOverrides() throws IOException {
    assertThat(thisClassLoader.getResources(EXISTING_RESOURCE)).isNotNull();
    assertThat(overridingLoader.getResources(EXISTING_RESOURCE)).isNotNull();
    assertThat(countElements(overridingLoader.getResources(EXISTING_RESOURCE))).isEqualTo(1);
  }

  @Test
  public void testDoesNotFindExistingResourceWithGetResourcesAndNullOverride() throws IOException {
    assertThat(thisClassLoader.getResources(EXISTING_RESOURCE)).isNotNull();
    overridingLoader.override(EXISTING_RESOURCE, null);
    assertThat(countElements(overridingLoader.getResources(EXISTING_RESOURCE))).isEqualTo(0);
  }

  @Test
  void overrideExistingResourceWithDifferentResource() {
    String newPath = "infra/instrument/classloading/other.xml";
    overridingLoader.override(EXISTING_RESOURCE, newPath);
    assertThat(overridingLoader.getResource(EXISTING_RESOURCE))
            .isEqualTo(thisClassLoader.getResource(newPath));
  }

  @Test
  void suppressExistingResourceUsingSuppress() {
    overridingLoader.suppress(EXISTING_RESOURCE);
    assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNull();
    assertThat(overridingLoader.getResourceAsStream(EXISTING_RESOURCE)).isNull();
  }

  @Test
  void overrideNonExistentResource() {
    String nonExistent = "does/not/exist.xml";
    overridingLoader.override(nonExistent, EXISTING_RESOURCE);
    assertThat(overridingLoader.getResource(nonExistent))
            .isEqualTo(thisClassLoader.getResource(EXISTING_RESOURCE));
  }

  @Test
  void copyOverridesFromAnotherLoader() {
    ResourceOverridingShadowingClassLoader other = new ResourceOverridingShadowingClassLoader(thisClassLoader);
    other.override(EXISTING_RESOURCE, "other.xml");

    overridingLoader.copyOverrides(other);
    assertThat(overridingLoader.getResource(EXISTING_RESOURCE))
            .isEqualTo(thisClassLoader.getResource("other.xml"));
  }

  @Test
  void overrideMultipleResources() throws IOException {
    String otherResource = "infra/instrument/classloading/other.xml";
    overridingLoader.override(EXISTING_RESOURCE, otherResource);
    overridingLoader.override(otherResource, EXISTING_RESOURCE);

    assertThat(overridingLoader.getResource(EXISTING_RESOURCE))
            .isEqualTo(thisClassLoader.getResource(otherResource));
    assertThat(overridingLoader.getResource(otherResource))
            .isEqualTo(thisClassLoader.getResource(EXISTING_RESOURCE));
  }

  @Test
  void nullClassLoaderNotAllowed() {
    assertThatThrownBy(() -> new ResourceOverridingShadowingClassLoader(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void overrideToNonExistentResource() {
    overridingLoader.override(EXISTING_RESOURCE, "does/not/exist.xml");
    assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNull();
  }

  @Test
  void multipleOverridesRetainCorrectMappings() {
    String resource1 = "path/one.xml";
    String resource2 = "path/two.xml";
    String resource3 = "path/three.xml";

    overridingLoader.override(resource1, resource2);
    overridingLoader.override(resource2, resource3);

    assertThat(overridingLoader.getResource(resource1))
            .isEqualTo(thisClassLoader.getResource(resource2));
    assertThat(overridingLoader.getResource(resource2))
            .isEqualTo(thisClassLoader.getResource(resource3));
  }

  @Test
  void enumGetResourcesReturnsEmptyEnumerationWhenSuppressed() throws IOException {
    overridingLoader.suppress(EXISTING_RESOURCE);
    Enumeration<URL> resources = overridingLoader.getResources(EXISTING_RESOURCE);

    assertThat(resources.hasMoreElements()).isFalse();
    assertThatThrownBy(resources::nextElement)
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Should not be called. I am empty.");
  }

  @Test
  void overriddenResourcesRetainOriginalBehaviorForUnmappedPaths() throws IOException {
    String unmappedPath = "path/unmapped.xml";
    assertThat(overridingLoader.getResource(unmappedPath))
            .isEqualTo(thisClassLoader.getResource(unmappedPath));
    assertThat(overridingLoader.getResourceAsStream(unmappedPath))
            .isEqualTo(thisClassLoader.getResourceAsStream(unmappedPath));
    assertThat(countElements(overridingLoader.getResources(unmappedPath)))
            .isEqualTo(countElements(thisClassLoader.getResources(unmappedPath)));
  }

  @Test
  void overrideCanBeOverwrittenWithNewMapping() {
    String path = "resource.xml";
    String override1 = "override1.xml";
    String override2 = "override2.xml";

    overridingLoader.override(path, override1);
    overridingLoader.override(path, override2);

    assertThat(overridingLoader.getResource(path))
            .isEqualTo(thisClassLoader.getResource(override2));
  }

  @Test
  void suppressedResourceCanBeOverriddenLater() {
    overridingLoader.suppress(EXISTING_RESOURCE);
    assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNull();

    String newPath = "new/path.xml";
    overridingLoader.override(EXISTING_RESOURCE, newPath);
    assertThat(overridingLoader.getResource(EXISTING_RESOURCE))
            .isEqualTo(thisClassLoader.getResource(newPath));
  }

  @Test
  void copyOverridesWithNullLoaderThrowsException() {
    assertThatThrownBy(() -> overridingLoader.copyOverrides(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Other ClassLoader is required");
  }

  private int countElements(Enumeration<?> e) {
    int elts = 0;
    while (e.hasMoreElements()) {
      e.nextElement();
      ++elts;
    }
    return elts;
  }
}
