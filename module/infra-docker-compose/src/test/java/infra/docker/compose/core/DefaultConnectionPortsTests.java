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

package infra.docker.compose.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.core.io.ClassPathResource;
import infra.docker.compose.core.DefaultConnectionPorts.ContainerPort;
import infra.docker.compose.core.DockerCliInspectResponse.Config;
import infra.docker.compose.core.DockerCliInspectResponse.HostPort;
import infra.docker.compose.core.DockerCliInspectResponse.NetworkSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DefaultConnectionPorts}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultConnectionPortsTests {

  @Test
  void createWhenBridgeNetwork() throws IOException {
    DefaultConnectionPorts ports = createForJson("docker-inspect-bridge-network.json");
    assertThat(ports.getMappings()).containsExactly(entry(new ContainerPort(6379, "tcp"), 32770));
  }

  @Test
  void createWhenHostNetwork() throws Exception {
    DefaultConnectionPorts ports = createForJson("docker-inspect-host-network.json");
    assertThat(ports.getMappings()).containsExactly(entry(new ContainerPort(6379, "tcp"), 6379));
  }

  @Test
  void getPrefersTcpWhenPortIsPublishedForBothProtocols() {
    Map<String, List<HostPort>> mappings = new LinkedHashMap<>();
    mappings.put("53/tcp", List.of(new HostPort("127.0.0.1", "10053")));
    mappings.put("53/udp", List.of(new HostPort("127.0.0.1", "20053")));
    DefaultConnectionPorts ports = createForPorts(mappings);
    assertThat(ports.get(53)).isEqualTo(10053);
    assertThat(ports.getAll("udp")).containsExactly(20053);
  }

  @Test
  void getReturnsUdpPortWhenNoTcpPortIsPublished() {
    DefaultConnectionPorts ports = createForPorts(
            Map.of("53/udp", List.of(new HostPort("127.0.0.1", "20053"))));
    assertThat(ports.get(53)).isEqualTo(20053);
  }

  private DefaultConnectionPorts createForPorts(Map<String, List<HostPort>> mappings) {
    DockerCliInspectResponse response = new DockerCliInspectResponse("id",
            new Config("test", Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList()),
            new NetworkSettings(mappings), null);
    return new DefaultConnectionPorts(response);
  }

  private DefaultConnectionPorts createForJson(String path) throws IOException {
    String json = new ClassPathResource(path, getClass()).getContentAsString(StandardCharsets.UTF_8);
    DockerCliInspectResponse inspectResponse = DockerJson.deserialize(json, DockerCliInspectResponse.class);
    return new DefaultConnectionPorts(inspectResponse);
  }

  @Nested
  class ContainerPortTests {

    @Test
    void parse() {
      ContainerPort port = ContainerPort.parse("123/tcp");
      assertThat(port).isEqualTo(new ContainerPort(123, "tcp"));
    }

    @Test
    void parseWhenNoSlashThrowsException() {
      assertThatIllegalStateException().isThrownBy(() -> ContainerPort.parse("123"))
              .withMessage("Unable to parse container port '123'");
    }

    @Test
    void parseWhenMultipleSlashesThrowsException() {
      assertThatIllegalStateException().isThrownBy(() -> ContainerPort.parse("123/tcp/ip"))
              .withMessage("Unable to parse container port '123/tcp/ip'");
    }

    @Test
    void parseWhenNotNumberThrowsException() {
      assertThatIllegalStateException().isThrownBy(() -> ContainerPort.parse("tcp/123"))
              .withMessage("Unable to parse container port 'tcp/123'");
    }

  }

}
