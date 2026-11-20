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

package infra.test.web.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ExpectedCountTests {

  @Test
  void factoriesExposeInclusiveBounds() {
    bounds(ExpectedCount.once(), 1, 1);
    bounds(ExpectedCount.twice(), 2, 2);
    bounds(ExpectedCount.times(3), 3, 3);
    bounds(ExpectedCount.manyTimes(), 1, Integer.MAX_VALUE);
    bounds(ExpectedCount.min(3), 3, Integer.MAX_VALUE);
    bounds(ExpectedCount.max(3), 1, 3);
    bounds(ExpectedCount.never(), 0, 0);
    bounds(ExpectedCount.between(0, 3), 0, 3);
    bounds(ExpectedCount.between(0, 0), 0, 0);
  }

  @Test
  void invalidCountsAreRejectedAtFactoryBoundary() {
    assertThatIllegalArgumentException().isThrownBy(() -> ExpectedCount.times(0)).withMessageContaining("count");
    assertThatIllegalArgumentException().isThrownBy(() -> ExpectedCount.min(0)).withMessageContaining("min");
    assertThatIllegalArgumentException().isThrownBy(() -> ExpectedCount.max(0)).withMessageContaining("max");
    assertThatIllegalArgumentException().isThrownBy(() -> ExpectedCount.between(-1, 1)).withMessageContaining("minCount");
    assertThatIllegalArgumentException().isThrownBy(() -> ExpectedCount.between(2, 1)).withMessageContaining("maxCount");
  }

  private void bounds(ExpectedCount count, int min, int max) {
    assertThat(count.getMinCount()).isEqualTo(min);
    assertThat(count.getMaxCount()).isEqualTo(max);
  }

}
