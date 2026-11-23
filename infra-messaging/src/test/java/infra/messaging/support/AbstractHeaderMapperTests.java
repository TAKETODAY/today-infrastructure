/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.messaging.support;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import infra.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractHeaderMapperTests {

  private final TestHeaderMapper mapper = new TestHeaderMapper();

  @Test
  void appliesInboundAndOutboundPrefixesExactlyOnce() {
    this.mapper.setInboundPrefix("in.");
    this.mapper.setOutboundPrefix("out.");

    MessageHeaders inbound = this.mapper.toHeaders(Map.of("count", 3, "in.trace", "abc"));
    assertThat(inbound).containsEntry("in.count", 3).containsEntry("in.trace", "abc").doesNotContainKey("in.in.trace");

    var outbound = new HashMap<String, Object>();
    this.mapper.fromHeaders(new MessageHeaders(Map.of("count", 3, "out.trace", "abc")), outbound);
    assertThat(outbound).containsExactlyInAnyOrderEntriesOf(Map.of("out.count", 3, "out.trace", "abc"));

    this.mapper.setInboundPrefix(null);
    this.mapper.setOutboundPrefix(null);
    assertThat(this.mapper.toHeaders(Map.of("count", 3))).containsEntry("count", 3);
    outbound.clear();
    this.mapper.fromHeaders(new MessageHeaders(Map.of("count", 3)), outbound);
    assertThat(outbound).containsExactlyEntriesOf(Map.of("count", 3));
  }

  @Test
  void omitsMissingOrIncompatibleHeadersButAcceptsSubtypes() {
    var outbound = new HashMap<String, Object>();
    this.mapper.fromHeaders(new MessageHeaders(Map.of("count", 3L, "trace", 17)), outbound);
    assertThat(outbound).containsExactlyEntriesOf(Map.of("count", 3L));
    outbound.clear();
    this.mapper.fromHeaders(new MessageHeaders(Map.of("count", "3", "trace", "abc")), outbound);
    assertThat(outbound).containsExactlyEntriesOf(Map.of("trace", "abc"));
  }

  private static class TestHeaderMapper extends AbstractHeaderMapper<Map<String, Object>> {

    @Override
    public void fromHeaders(MessageHeaders headers, Map<String, Object> target) {
      Number count = getHeaderIfAvailable(headers, "count", Number.class);
      if (count != null) {
        target.put(fromHeaderName("count"), count);
      }
      String name = headers.containsKey("out.trace") ? "out.trace" : "trace";
      String trace = getHeaderIfAvailable(headers, name, String.class);
      if (trace != null) {
        target.put(fromHeaderName(name), trace);
      }
    }

    @Override
    public MessageHeaders toHeaders(Map<String, Object> source) {
      var headers = new HashMap<String, Object>();
      source.forEach((key, value) -> headers.put(toHeaderName(key), value));
      return new MessageHeaders(headers);
    }
  }

}
