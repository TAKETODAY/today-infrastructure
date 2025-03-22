/*
 * Copyright 2017 - 2025 the original author or authors.
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