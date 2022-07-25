/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.socket.client.jetty;

/**
 * Tests for {@link JettyWebSocketClient}.
 *
 * @author Rossen Stoyanchev
 */
public class JettyWebSocketClientTests {

	/* TODO: complete upgrade to Jetty 11
	private JettyWebSocketClient client;

	private TestJettyWebSocketServer server;

	private String wsUrl;

	private WebSocketSession wsSession;


	@BeforeEach
	public void setup() throws Exception {

		this.server = new TestJettyWebSocketServer(new TextWebSocketHandler());
		this.server.start();

		this.client = new JettyWebSocketClient();
		this.client.start();

		this.wsUrl = "ws://localhost:" + this.server.getPort() + "/test";
	}

	@AfterEach
	public void teardown() throws Exception {
		this.wsSession.close();
		this.client.stop();
		this.server.stop();
	}


	@Test
	public void doHandshake() throws Exception {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.setSecWebSocketProtocol(Arrays.asList("echo"));

		this.wsSession = this.client.doHandshake(new TextWebSocketHandler(), headers, new URI(this.wsUrl)).get();

		assertThat(this.wsSession.getUri().toString()).isEqualTo(this.wsUrl);
		assertThat(this.wsSession.getAcceptedProtocol()).isEqualTo("echo");
	}

	@Test
	public void doHandshakeWithTaskExecutor() throws Exception {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.setSecWebSocketProtocol(Arrays.asList("echo"));

		this.client.setTaskExecutor(new SimpleAsyncTaskExecutor());
		this.wsSession = this.client.doHandshake(new TextWebSocketHandler(), headers, new URI(this.wsUrl)).get();

		assertThat(this.wsSession.getUri().toString()).isEqualTo(this.wsUrl);
		assertThat(this.wsSession.getAcceptedProtocol()).isEqualTo("echo");
	}


	private static class TestJettyWebSocketServer {

		private final Server server;


		public TestJettyWebSocketServer(final WebSocketHandler webSocketHandler) {

			this.server = new Server();
			ServerConnector connector = new ServerConnector(this.server);
			connector.setPort(0);

			this.server.addConnector(connector);
			this.server.setHandler(new WebSocketUpgradeHandler() {
				@Override
				public void configure(JettyWebSocketServletFactory factory) {
					factory.setCreator(new JettyWebSocketCreator() {
						@Override
						public Object createWebSocket(JettyServerUpgradeRequest req, JettyServerUpgradeResponse resp) {
							if (CollectionUtils.isNotEmpty(req.getSubProtocols())) {
								resp.setAcceptedSubProtocol(req.getSubProtocols().get(0));
							}
							JettyWebSocketSession session = new JettyWebSocketSession(null, null);
							return new JettyWebSocketHandlerAdapter(webSocketHandler, session);
						}
					});
				}
			});
		}

		public void start() throws Exception {
			this.server.start();
		}

		public void stop() throws Exception {
			this.server.stop();
		}

		public int getPort() {
			return ((ServerConnector) this.server.getConnectors()[0]).getLocalPort();
		}
	}
	*/

}
