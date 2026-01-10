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

package infra.core.io;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 14:35
 */
class ResourceRegionTests {

  @Test
  void validResourceRegionIsCreatedSuccessfully() {
    Resource resource = mock(Resource.class);
    ResourceRegion region = new ResourceRegion(resource, 0, 100);

    assertThat(region.getResource()).isSameAs(resource);
    assertThat(region.getPosition()).isEqualTo(0);
    assertThat(region.getCount()).isEqualTo(100);
  }

  @Test
  void nullResourceThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ResourceRegion(null, 0, 100))
            .withMessage("Resource is required");
  }

  @Test
  void negativePositionThrowsException() {
    Resource resource = mock(Resource.class);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ResourceRegion(resource, -1, 100))
            .withMessage("'position' must be greater than or equal to 0");
  }

  @Test
  void negativeCountThrowsException() {
    Resource resource = mock(Resource.class);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ResourceRegion(resource, 0, -1))
            .withMessage("'count' must be greater than or equal to 0");
  }

  @Test
  void zeroCountIsAllowed() {
    Resource resource = mock(Resource.class);
    ResourceRegion region = new ResourceRegion(resource, 100, 0);
    assertThat(region.getCount()).isZero();
  }

  @Test
  void toStringContainsAllFields() {
    Resource resource = mock(Resource.class);
    ResourceRegion region = new ResourceRegion(resource, 50, 100);

    assertThat(region.toString())
            .contains("count = 100")
            .contains("position = 50")
            .contains("resource = " + resource);
  }

  @Test
  void largePositionAndCountValuesAreSupported() {
    Resource resource = mock(Resource.class);
    long maxLong = Long.MAX_VALUE;
    ResourceRegion region = new ResourceRegion(resource, maxLong, maxLong);

    assertThat(region.getPosition()).isEqualTo(maxLong);
    assertThat(region.getCount()).isEqualTo(maxLong);
  }
}