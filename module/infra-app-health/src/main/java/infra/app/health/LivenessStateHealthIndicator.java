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

import infra.app.availability.ApplicationAvailability;
import infra.app.availability.AvailabilityState;
import infra.app.availability.LivenessState;
import infra.app.health.contributor.HealthIndicator;
import infra.app.health.contributor.Status;

/**
 * A {@link HealthIndicator} that checks the {@link LivenessState} of the application.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class LivenessStateHealthIndicator extends AvailabilityStateHealthIndicator {

  public LivenessStateHealthIndicator(ApplicationAvailability availability) {
    super(availability, LivenessState.class, (statusMappings) -> {
      statusMappings.add(LivenessState.CORRECT, Status.UP);
      statusMappings.add(LivenessState.BROKEN, Status.DOWN);
    });
  }

  @Override
  protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
    return applicationAvailability.getLivenessState();
  }

}
