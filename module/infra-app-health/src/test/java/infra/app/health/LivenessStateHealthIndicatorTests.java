/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.app.availability.ApplicationAvailability;
import infra.app.availability.LivenessState;
import infra.app.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LivenessStateHealthIndicator}
 *
 * @author Brian Clozel
 */
class LivenessStateHealthIndicatorTests {

  private ApplicationAvailability availability;

  private LivenessStateHealthIndicator healthIndicator;

  @BeforeEach
  void setUp() {
    this.availability = mock(ApplicationAvailability.class);
    this.healthIndicator = new LivenessStateHealthIndicator(this.availability);
  }

  @Test
  void livenessIsLive() {
    given(this.availability.getLivenessState()).willReturn(LivenessState.CORRECT);
    assertThat(this.healthIndicator.health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void livenessIsBroken() {
    given(this.availability.getLivenessState()).willReturn(LivenessState.BROKEN);
    assertThat(this.healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

}
