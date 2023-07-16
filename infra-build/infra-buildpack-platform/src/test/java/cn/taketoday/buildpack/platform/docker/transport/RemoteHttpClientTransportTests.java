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

package cn.taketoday.buildpack.platform.docker.transport;

import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import cn.taketoday.buildpack.platform.docker.configuration.DockerHost;
import cn.taketoday.buildpack.platform.docker.configuration.ResolvedDockerHost;
import cn.taketoday.buildpack.platform.docker.ssl.SslContextFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RemoteHttpClientTransport}
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class RemoteHttpClientTransportTests {

  @Test
  void createIfPossibleWhenDockerHostIsNotSetReturnsNull() {
    ResolvedDockerHost dockerHost = ResolvedDockerHost.from(null);
    RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
    assertThat(transport).isNull();
  }

  @Test
  void createIfPossibleWhenDockerHostIsDefaultReturnsNull() {
    ResolvedDockerHost dockerHost = ResolvedDockerHost.from(new DockerHost(null));
    RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
    assertThat(transport).isNull();
  }

  @Test
  void createIfPossibleWhenDockerHostIsFileReturnsNull() {
    ResolvedDockerHost dockerHost = ResolvedDockerHost.from(new DockerHost("unix:///var/run/socket.sock"));
    RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
    assertThat(transport).isNull();
  }

  @Test
  void createIfPossibleWhenDockerHostIsAddressReturnsTransport() {
    ResolvedDockerHost dockerHost = ResolvedDockerHost.from(new DockerHost("tcp://192.168.1.2:2376"));
    RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
    assertThat(transport).isNotNull();
  }

  @Test
  void createIfPossibleWhenNoTlsVerifyUsesHttp() {
    ResolvedDockerHost dockerHost = ResolvedDockerHost.from(new DockerHost("tcp://192.168.1.2:2376"));
    RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost);
    assertThat(transport.getHost()).satisfies(hostOf("http", "192.168.1.2", 2376));
  }

  @Test
  void createIfPossibleWhenTlsVerifyUsesHttps() throws Exception {
    SslContextFactory sslContextFactory = mock(SslContextFactory.class);
    given(sslContextFactory.forDirectory("/test-cert-path")).willReturn(SSLContext.getDefault());
    ResolvedDockerHost dockerHost = ResolvedDockerHost
            .from(new DockerHost("tcp://192.168.1.2:2376", true, "/test-cert-path"));
    RemoteHttpClientTransport transport = RemoteHttpClientTransport.createIfPossible(dockerHost, sslContextFactory);
    assertThat(transport.getHost()).satisfies(hostOf("https", "192.168.1.2", 2376));
  }

  @Test
  void createIfPossibleWhenTlsVerifyWithMissingCertPathThrowsException() {
    ResolvedDockerHost dockerHost = ResolvedDockerHost.from(new DockerHost("tcp://192.168.1.2:2376", true, null));
    assertThatIllegalArgumentException().isThrownBy(() -> RemoteHttpClientTransport.createIfPossible(dockerHost))
            .withMessageContaining("Docker host TLS verification requires trust material");
  }

  private Consumer<HttpHost> hostOf(String scheme, String hostName, int port) {
    return (host) -> {
      assertThat(host).isNotNull();
      assertThat(host.getSchemeName()).isEqualTo(scheme);
      assertThat(host.getHostName()).isEqualTo(hostName);
      assertThat(host.getPort()).isEqualTo(port);
    };
  }

}
