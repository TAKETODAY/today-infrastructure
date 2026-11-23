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

package infra.messaging.simp.stomp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StompSessionHandlerAdapterTests {

  @Test
  void providesNoOpCallbacksAndStringErrorPayloadType() {
    StompSessionHandlerAdapter handler = new StompSessionHandlerAdapter() { };
    StompHeaders headers = new StompHeaders();

    assertThat(handler.getPayloadType(headers)).isEqualTo(String.class);
    handler.handleFrame(headers, null);
    handler.afterConnected(null, headers);
    handler.handleException(null, null, headers, new byte[0], new RuntimeException());
    handler.handleTransportError(null, new RuntimeException());
  }

}
