/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.socket.undertow;

import cn.taketoday.beans.InitializingBean;
import cn.taketoday.core.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.HttpStatus;
import cn.taketoday.web.http.ResponseStatusException;
import cn.taketoday.web.socket.AbstractStandardWebSocketHandlerAdapter;
import cn.taketoday.web.socket.StandardEndpoint;
import cn.taketoday.web.socket.StandardWebSocketSession;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.server.session.Session;
import io.undertow.servlet.api.ClassIntrospecter;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import io.undertow.util.PathTemplate;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.protocol.Handshake;
import io.undertow.websockets.jsr.ConfiguredServerEndpoint;
import io.undertow.websockets.jsr.DefaultContainerConfigurator;
import io.undertow.websockets.jsr.EncodingFactory;
import io.undertow.websockets.jsr.EndpointSessionHandler;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.handshake.HandshakeUtil;
import io.undertow.websockets.jsr.handshake.JsrHybi07Handshake;
import io.undertow.websockets.jsr.handshake.JsrHybi08Handshake;
import io.undertow.websockets.jsr.handshake.JsrHybi13Handshake;
import org.xnio.ChannelListener;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author TODAY 2021/5/6 18:19
 * @since 3.0.1
 */
public class UndertowWebSocketHandlerAdapter
        extends AbstractStandardWebSocketHandlerAdapter implements InitializingBean {

  private static final boolean directBuffers = Boolean.getBoolean("io.undertow.websockets.direct-buffers");
  private static final boolean invokeInIoThread = Boolean.getBoolean("io.undertow.websockets.invoke-in-io-thread");
  private static final String SESSION_ATTRIBUTE = "io.undertow.websocket.current-connections";

  /**
   * dispatch to a thread pool
   */
  private boolean dispatchToWorker = !invokeInIoThread;
  private Supplier<XnioWorker> workerSupplier;

  private ClassLoader classLoader = UndertowWebSocketHandlerAdapter.class.getClassLoader();
  private ClassIntrospecter classIntrospector = DefaultClassIntrospector.INSTANCE;
  private ByteBufferPool buffers = new DefaultByteBufferPool(directBuffers, 1024, 100, 12);
  private List<ThreadSetupHandler> threadSetupHandlers = Collections.emptyList();

  /**
   * @see ServerWebSocketContainer#deploymentComplete()
   */
  private boolean invokeDeploymentComplete = true;

  protected ServerWebSocketContainer serverContainer;

  private WebSocketConnectionCallback callback;
  private final Set<WebSocketChannel> peerConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());

  @Override
  @SuppressWarnings("unchecked")
  protected void doUpgrade(
          RequestContext context, WebSocketSession session, WebSocketHandler handler,
          String subProtocol, List<WebSocketExtension> supportedExtensions) {
    {
      final UndertowWebSocketHttpExchange facade = new UndertowWebSocketHttpExchange(context, peerConnections);

      Handshake handshaking = getHandshake(context, session, facade, handler);
      if (handshaking != null) {
        if (obtainContainer().isClosed()) {
          throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }

        facade.putAttachment(HandshakeUtil.PATH_PARAMS, Collections.emptyMap());
//        facade.putAttachment(HandshakeUtil.PRINCIPAL, req.getUserPrincipal());
        ServletRequestContext src = ServletRequestContext.requireCurrent();
        final HttpSessionImpl httpSession = src.getCurrentServletContext().getSession(src.getExchange(), false);
        final class HttpUpgradeListener0 implements HttpUpgradeListener {
          final Handshake selected;

          HttpUpgradeListener0(Handshake selected) {
            this.selected = selected;
          }

          @Override
          public void handleUpgrade(StreamConnection streamConnection, HttpServerExchange exchange) {
            WebSocketChannel channel = selected.createChannel(facade, streamConnection, facade.getBufferPool());
            peerConnections.add(channel);
            if (httpSession != null) {
              final Session underlying;
              if (System.getSecurityManager() == null) {
                underlying = httpSession.getSession();
              }
              else {
                underlying = AccessController.doPrivileged(new HttpSessionImpl.UnwrapSessionAction(httpSession));
              }
              List<WebSocketChannel> connections;
              synchronized(underlying) {
                connections = (List<WebSocketChannel>) underlying.getAttribute(SESSION_ATTRIBUTE);
                if (connections == null) {
                  underlying.setAttribute(SESSION_ATTRIBUTE, connections = new ArrayList<>());
                }
                connections.add(channel);
              }

              final class CloseChannelListener implements ChannelListener<WebSocketChannel> {
                final List<WebSocketChannel> connections;

                CloseChannelListener(List<WebSocketChannel> connections) {
                  this.connections = connections;
                }

                @Override
                public void handleEvent(WebSocketChannel channel) {
                  synchronized(underlying) {
                    connections.remove(channel);
                  }
                }
              }
              channel.addCloseTask(new CloseChannelListener(connections));
            }
            callback.onConnect(facade, channel);
          }
        }

        facade.upgradeChannel(new HttpUpgradeListener0(handshaking));
        handshaking.handshake(facade);
      }
    }

  }

  protected Handshake getHandshake(
          RequestContext context, WebSocketSession session, UndertowWebSocketHttpExchange facade, WebSocketHandler handler) {
    final ServerEndpointConfig endpointConfig = getServerEndpointConfig(handler);
    // StandardEndpoint
    final class StandardEndpointInstanceFactory implements InstanceFactory<StandardEndpoint> {

      @Override
      public InstanceHandle<StandardEndpoint> createInstance() {
        return new ImmediateInstanceHandle<>(new StandardEndpoint((StandardWebSocketSession) session, handler));
      }
    }

    final ConfiguredServerEndpoint configured = new ConfiguredServerEndpoint(
            endpointConfig, new StandardEndpointInstanceFactory(),
            PathTemplate.create(context.getRequestPath()), EncodingFactory.DEFAULT);

    final List<Handshake> handshakes = getHandshakes(configured);
    for (Handshake method : handshakes) {
      if (method.matches(facade)) {
        return method;
      }
    }
    return null;
  }

  protected List<Handshake> getHandshakes(ConfiguredServerEndpoint configured) {
    final ArrayList<Handshake> handshakes = new ArrayList<>();
    handshakes.add(new JsrHybi13Handshake(configured));
    handshakes.add(new JsrHybi08Handshake(configured));
    handshakes.add(new JsrHybi07Handshake(configured));
    return handshakes;
  }

  private ServerWebSocketContainer obtainContainer() {
    ServerWebSocketContainer serverContainer = this.serverContainer;
    Assert.state(serverContainer != null, "serverContainer has not been initialized");
    return serverContainer;
  }

  @Override
  public void afterPropertiesSet() {
    this.configurator = DefaultContainerConfigurator.INSTANCE;
    if (serverContainer == null) {
      Supplier<XnioWorker> workerSupplier = this.workerSupplier;
      if (workerSupplier == null) {
        workerSupplier = new DefaultXnioWorkerSupplier();
      }
      serverContainer = new ServerWebSocketContainer(
              classIntrospector, classLoader, workerSupplier, buffers, threadSetupHandlers, dispatchToWorker);
      if (invokeDeploymentComplete) {
        serverContainer.deploymentComplete();
      }
    }

    this.callback = new EndpointSessionHandler(serverContainer);
  }

  public void setWorkerSupplier(Supplier<XnioWorker> workerSupplier) {
    this.workerSupplier = workerSupplier;
  }

  public Supplier<XnioWorker> getWorkerSupplier() {
    return workerSupplier;
  }

  public void setBuffers(ByteBufferPool buffers) {
    Assert.notNull(buffers, "ByteBufferPool must not be null");
    this.buffers = buffers;
  }

  public ByteBufferPool getBuffers() {
    return buffers;
  }

  public ClassIntrospecter getClassIntrospector() {
    return classIntrospector;
  }

  public void setClassIntrospector(ClassIntrospecter classIntrospector) {
    Assert.notNull(classIntrospector, "ClassIntrospecter must not be null");
    this.classIntrospector = classIntrospector;
  }

  public void setThreadSetupHandlers(List<ThreadSetupHandler> threadSetupHandlers) {
    Assert.notNull(threadSetupHandlers, "ThreadSetupHandlers must not be null");
    this.threadSetupHandlers = threadSetupHandlers;
  }

  public List<ThreadSetupHandler> getThreadSetupHandlers() {
    return threadSetupHandlers;
  }

  public void setDispatchToWorker(boolean dispatchToWorker) {
    this.dispatchToWorker = dispatchToWorker;
  }

  public boolean isDispatchToWorker() {
    return dispatchToWorker;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public void setInvokeDeploymentComplete(boolean invokeDeploymentComplete) {
    this.invokeDeploymentComplete = invokeDeploymentComplete;
  }

  /**
   * XnioWorker Supplier
   */
  static final class DefaultXnioWorkerSupplier implements Supplier<XnioWorker> {
    volatile XnioWorker worker;

    @Override
    public XnioWorker get() {
      if (worker == null) {
        synchronized(this) {
          if (worker == null) {
            try {
              worker = Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true));
            }
            catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
      return worker;
    }
  }

}
