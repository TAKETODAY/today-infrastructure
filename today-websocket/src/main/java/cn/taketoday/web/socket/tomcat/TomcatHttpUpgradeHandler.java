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

package cn.taketoday.web.socket.tomcat;

import org.apache.coyote.http11.upgrade.InternalHttpUpgradeHandler;
import org.apache.coyote.http11.upgrade.UpgradeInfo;
import org.apache.tomcat.util.net.AbstractEndpoint.Handler.SocketState;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SocketEvent;
import org.apache.tomcat.util.net.SocketWrapperBase;
import org.apache.tomcat.websocket.Transformation;
import org.apache.tomcat.websocket.WsIOException;
import org.apache.tomcat.websocket.WsSession;
import org.apache.tomcat.websocket.server.WsFrameServer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.reflect.MethodInvoker;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.WebConnection;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Servlet 3.1 HTTP upgrade handler for WebSocket connections.
 *
 * @author TODAY 2021/5/5 0:16
 * @since 3.0.1
 */
public final class TomcatHttpUpgradeHandler implements InternalHttpUpgradeHandler {
  private static final Logger log = LoggerFactory.getLogger(TomcatHttpUpgradeHandler.class);
  private static final MethodInvoker notifyDataAvailable = MethodInvoker.fromMethod(WsFrameServer.class, "notifyDataAvailable");

  private final ClassLoader applicationClassLoader;

  private SocketWrapperBase<?> socketWrapper;

  private Endpoint endpoint;
  private ServerEndpointConfig serverEndpointConfig;
  private TomcatServerContainer webSocketContainer;
  private TomcatHandshakeRequest handshakeRequest;
  private List<Extension> negotiatedExtensions;
  private String subProtocol;
  private Transformation transformation;
  private boolean secure;
  private WebConnection connection;

  private TomcatRemoteEndpointImplServer wsRemoteEndpointServer;
  private TomcatFrameServer wsFrame;
  private WsSession wsSession;

  // @since 4.0
  private final UpgradeInfo upgradeInfo = new UpgradeInfo();

  public TomcatHttpUpgradeHandler() {
    applicationClassLoader = Thread.currentThread().getContextClassLoader();
  }

  @Override
  public void setSocketWrapper(SocketWrapperBase<?> socketWrapper) {
    this.socketWrapper = socketWrapper;
  }

  @Override
  public void init(WebConnection connection) {
    String httpSessionId = null;
    Object session = handshakeRequest.getHttpSession();
    if (session != null) {
      httpSessionId = ((HttpSession) session).getId();
    }

    // Need to call onOpen using the web application's class loader
    // Create the frame using the application's class loader so it can pick
    // up application specific config from the ServerContainerImpl
    Thread t = Thread.currentThread();
    ClassLoader cl = t.getContextClassLoader();
    t.setContextClassLoader(applicationClassLoader);
    try {
      wsRemoteEndpointServer = new TomcatRemoteEndpointImplServer(socketWrapper, webSocketContainer);
      wsSession = new WsSession(endpoint, wsRemoteEndpointServer,
                                webSocketContainer, handshakeRequest.getRequestURI(),
                                handshakeRequest.getParameterMap(),
                                handshakeRequest.getQueryString(),
                                handshakeRequest.getUserPrincipal(), httpSessionId,
                                negotiatedExtensions,
                                subProtocol,
                                Collections.emptyMap(),
                                secure,
                                serverEndpointConfig);
      wsFrame = new TomcatFrameServer(socketWrapper, upgradeInfo, wsSession, transformation, applicationClassLoader);
      // WsFrame adds the necessary final transformations. Copy the
      // completed transformation chain to the remote end point.
      wsRemoteEndpointServer.setTransformation(wsFrame.getTransformation());
      endpoint.onOpen(wsSession, serverEndpointConfig);
      webSocketContainer.registerSession(serverEndpointConfig.getPath(), wsSession);
    }
    catch (DeploymentException e) {
      throw new IllegalArgumentException(e);
    }
    finally {
      t.setContextClassLoader(cl);
    }
  }

  @Override
  public SocketState upgradeDispatch(SocketEvent status) {
    switch (status) {
      case OPEN_READ:
        try {
          return (SocketState) notifyDataAvailable.invoke(wsFrame, null);
        }
        catch (Throwable io) {
          if (io instanceof WsIOException) {
            close(((WsIOException) io).getCloseReason());
          }
          else {
            onError(io);
            CloseReason cr = new CloseReason(CloseCodes.CLOSED_ABNORMALLY, io.getMessage());
            close(cr);
          }
        }
        return SocketState.CLOSED;
      case OPEN_WRITE:
        wsRemoteEndpointServer.onWritePossible(false);
        break;
      case STOP:
        CloseReason cr = new CloseReason(CloseCodes.GOING_AWAY, "The server is stopping");
        try {
          wsSession.close(cr);
        }
        catch (IOException ioe) {
          onError(ioe);
          cr = new CloseReason(CloseCodes.CLOSED_ABNORMALLY, ioe.getMessage());
          close(cr);
          return SocketState.CLOSED;
        }
        break;
      case ERROR:
        String msg = "Closing WebSocket connection due to an error";
        wsSession.doClose(new CloseReason(CloseCodes.GOING_AWAY, msg),
                          new CloseReason(CloseCodes.CLOSED_ABNORMALLY, msg));
        //$FALL-THROUGH$
      case DISCONNECT:
      case TIMEOUT:
      case CONNECT_FAIL:
        return SocketState.CLOSED;

    }
    if (wsFrame.isOpen()) {
      return SocketState.UPGRADED;
    }
    else {
      return SocketState.CLOSED;
    }
  }

  @Override
  public void timeoutAsync(long now) {
    // NO-OP
  }

  @Override
  public void pause() {
    // NO-OP
  }

  @Override
  public void destroy() {
    if (connection != null) {
      try {
        connection.close();
      }
      catch (Exception e) {
        log.error("Failed to close WebConnection while destroying the WebSocket HttpUpgradeHandler", e);
      }
    }
  }

  private void onError(Throwable throwable) {
    // Need to call onError using the web application's class loader
    Thread t = Thread.currentThread();
    ClassLoader cl = t.getContextClassLoader();
    t.setContextClassLoader(applicationClassLoader);
    try {
      endpoint.onError(wsSession, throwable);
    }
    finally {
      t.setContextClassLoader(cl);
    }
  }

  private void close(CloseReason cr) {
    /*
     * Any call to this method is a result of a problem reading from the
     * client. At this point that state of the connection is unknown.
     * Attempt to send a close frame to the client and then close the socket
     * immediately. There is no point in waiting for a close frame from the
     * client because there is no guarantee that we can recover from
     * whatever messed up state the client put the connection into.
     */
    wsSession.onClose(cr);
  }

  @Override
  public void setSslSupport(SSLSupport sslSupport) {
    // NO-OP. WebSocket has no requirement to access the TLS information
    // associated with the underlying connection.
  }

  // setter

  public ClassLoader getApplicationClassLoader() {
    return applicationClassLoader;
  }

  public SocketWrapperBase<?> getSocketWrapper() {
    return socketWrapper;
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(Endpoint endpoint) {
    this.endpoint = endpoint;
  }

  public ServerEndpointConfig getServerEndpointConfig() {
    return serverEndpointConfig;
  }

  public void setServerEndpointConfig(ServerEndpointConfig serverEndpointConfig) {
    this.serverEndpointConfig = serverEndpointConfig;
  }

  public TomcatServerContainer getWebSocketContainer() {
    return webSocketContainer;
  }

  public void setWebSocketContainer(TomcatServerContainer webSocketContainer) {
    this.webSocketContainer = webSocketContainer;
  }

  public TomcatHandshakeRequest getHandshakeRequest() {
    return handshakeRequest;
  }

  public void setHandshakeRequest(TomcatHandshakeRequest handshakeRequest) {
    this.handshakeRequest = handshakeRequest;
  }

  public List<Extension> getNegotiatedExtensions() {
    return negotiatedExtensions;
  }

  public void setNegotiatedExtensions(List<Extension> negotiatedExtensions) {
    this.negotiatedExtensions = negotiatedExtensions;
  }

  public String getSubProtocol() {
    return subProtocol;
  }

  public void setSubProtocol(String subProtocol) {
    this.subProtocol = subProtocol;
  }

  public Transformation getTransformation() {
    return transformation;
  }

  public void setTransformation(Transformation transformation) {
    this.transformation = transformation;
  }

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public WebConnection getConnection() {
    return connection;
  }

  public void setConnection(WebConnection connection) {
    this.connection = connection;
  }

  public TomcatRemoteEndpointImplServer getWsRemoteEndpointServer() {
    return wsRemoteEndpointServer;
  }

  public void setWsRemoteEndpointServer(TomcatRemoteEndpointImplServer wsRemoteEndpointServer) {
    this.wsRemoteEndpointServer = wsRemoteEndpointServer;
  }

  public TomcatFrameServer getWsFrame() {
    return wsFrame;
  }

  public void setWsFrame(TomcatFrameServer wsFrame) {
    this.wsFrame = wsFrame;
  }

  public WsSession getWsSession() {
    return wsSession;
  }

  public void setWsSession(WsSession wsSession) {
    this.wsSession = wsSession;
  }

  // @since 4.0
  @Override
  public UpgradeInfo getUpgradeInfo() {
    return upgradeInfo;
  }

}
