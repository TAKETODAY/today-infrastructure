/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 2.0
 */
public class ResourceOverridingShadowingClassLoaderTests {

  private static final String EXISTING_RESOURCE = "cn/taketoday/instrument/classloading/testResource.xml";

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

  private int countElements(Enumeration<?> e) {
    int elts = 0;
    while (e.hasMoreElements()) {
      e.nextElement();
      ++elts;
    }
    return elts;
  }
}
