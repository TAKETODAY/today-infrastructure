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

package infra.web.socket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloseStatusTests {

  @Test
  void createsKnownStatusesWithoutReason() {
    assertThat(CloseStatus.create(1000, null)).isSameAs(CloseStatus.NORMAL);
    assertThat(CloseStatus.create(1001, "")).isSameAs(CloseStatus.GOING_AWAY);
    assertThat(CloseStatus.create(1002, " ")).isSameAs(CloseStatus.PROTOCOL_ERROR);
    assertThat(CloseStatus.create(1003, null)).isSameAs(CloseStatus.NOT_ACCEPTABLE);
    assertThat(CloseStatus.create(1005, null)).isSameAs(CloseStatus.NO_STATUS_CODE);
    assertThat(CloseStatus.create(-1, null)).isSameAs(CloseStatus.NO_STATUS_CODE);
    assertThat(CloseStatus.create(1006, null)).isSameAs(CloseStatus.NO_CLOSE_FRAME);
    assertThat(CloseStatus.create(1007, null)).isSameAs(CloseStatus.BAD_DATA);
    assertThat(CloseStatus.create(1008, null)).isSameAs(CloseStatus.POLICY_VIOLATION);
    assertThat(CloseStatus.create(1009, null)).isSameAs(CloseStatus.TOO_BIG_TO_PROCESS);
    assertThat(CloseStatus.create(1010, null)).isSameAs(CloseStatus.REQUIRED_EXTENSION);
    assertThat(CloseStatus.create(1011, null)).isSameAs(CloseStatus.SERVER_ERROR);
    assertThat(CloseStatus.create(1012, null)).isSameAs(CloseStatus.SERVICE_RESTARTED);
    assertThat(CloseStatus.create(1013, null)).isSameAs(CloseStatus.SERVICE_OVERLOAD);
    assertThat(CloseStatus.create(4000, null)).isEqualTo(new CloseStatus(4000));
  }

  @Test
  void preservesReasonAndValueMethods() {
    CloseStatus status = new CloseStatus(4000, "because");
    assertThat(status.getCode()).isEqualTo(4000);
    assertThat(status.getReason()).isEqualTo("because");
    assertThat(status.withReason("another")).isEqualTo(new CloseStatus(4000, "another"));
    assertThat(status.equalsCode(new CloseStatus(4000))).isTrue();
    assertThat(status).isNotEqualTo(new CloseStatus(4001, "because"));
    assertThat(status).isNotEqualTo("status");
    assertThat(status.hashCode()).isEqualTo(new CloseStatus(4000, "because").hashCode());
    assertThat(status.toString()).contains("4000", "because");
  }

  @Test
  void rejectsInvalidValues() {
    assertThatThrownBy(() -> new CloseStatus(999)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new CloseStatus(5000)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CloseStatus.NORMAL.withReason("")).isInstanceOf(IllegalArgumentException.class);
  }

}
