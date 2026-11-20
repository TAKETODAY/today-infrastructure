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

package infra.messaging.tcp.reactor;

import org.junit.jupiter.api.Test;
import reactor.netty.tcp.TcpClient;

import java.util.concurrent.CompletableFuture;

import infra.logging.Logger;
import infra.messaging.Message;
import infra.messaging.tcp.TcpConnection;
import infra.messaging.tcp.TcpConnectionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReactorNettyTcpClientTests {

  private final ReactorNettyCodec<String> codec = mock();

  @Test
  void constructorsValidateRequiredArguments() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ReactorNettyTcpClient<>(null, 61613, this.codec));
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ReactorNettyTcpClient<>("localhost", 61613, null));
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ReactorNettyTcpClient<>((TcpClient) null, this.codec));
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ReactorNettyTcpClient<>(TcpClient.create(), null));
  }

  @Test
  void externalClientManagesOnlyItsOwnScheduler() {
    TcpClient tcpClient = TcpClient.create();
    ReactorNettyTcpClient<String> client = new ReactorNettyTcpClient<>(tcpClient, this.codec);
    Logger logger = mock();
    client.setLogger(logger);

    assertThat(client.getLogger()).isSameAs(logger);
    assertThat(client.toString()).startsWith("ReactorNettyTcpClient[");

    client.shutdownAsync().join();
    client.shutdownAsync().join();
    TcpConnectionHandler<String> handler = mock();
    CompletableFuture<Void> result = client.connectAsync(handler);

    assertThat(result).isCompletedExceptionally();
    verify(handler).afterConnectFailure(org.mockito.ArgumentMatchers.any(IllegalStateException.class));
  }

  @Test
  void clientConfigurerConstructorAppliesConfigurerAndCanShutdown() {
    boolean[] configured = { false };
    ReactorNettyTcpClient<String> client = new ReactorNettyTcpClient<>(tcpClient -> {
      configured[0] = true;
      return tcpClient;
    }, this.codec);

    assertThat(configured[0]).isTrue();
    client.shutdownAsync().join();
  }

  @Test
  void managedClientCanShutdownWithoutConnecting() {
    ReactorNettyTcpClient<String> client = new ReactorNettyTcpClient<>("localhost", 61613, this.codec);

    client.shutdownAsync().join();

    assertThat(client.connectAsync(mock(TcpConnectionHandler.class))).isCompletedExceptionally();
  }

  @SuppressWarnings("unused")
  private static final class NoOpHandler implements TcpConnectionHandler<String> {

    @Override
    public void afterConnected(TcpConnection<String> connection) { }

    @Override
    public void afterConnectFailure(Throwable ex) { }

    @Override
    public void handleMessage(Message<String> message) { }

    @Override
    public void handleFailure(Throwable ex) { }

    @Override
    public void afterConnectionClosed() { }
  }

}
