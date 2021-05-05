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

package cn.taketoday.web.socket.tomcat;

import org.apache.tomcat.websocket.Transformation;
import org.apache.tomcat.websocket.TransformationFactory;
import org.apache.tomcat.websocket.WsHandshakeResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletContextAware;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.socket.AbstractStandardWebSocketHandlerAdapter;
import cn.taketoday.web.socket.DefaultWebSocketSession;
import cn.taketoday.web.socket.HandshakeFailedException;
import cn.taketoday.web.socket.StandardEndpoint;
import cn.taketoday.web.socket.StandardExtension;
import cn.taketoday.web.socket.UpgradeUtils;
import cn.taketoday.web.socket.WebSocketHandler;

/**
 * Tomcat WebSocket HandlerAdapter
 *
 * @author TODAY 2021/4/5 14:20
 * @since 3.0
 */
public class TomcatWebSocketHandlerAdapter extends AbstractStandardWebSocketHandlerAdapter
        implements ServletContextAware {

  static final boolean DISABLE_BUILTIN_EXTENSIONS = Boolean.getBoolean("websocket.DISABLE_BUILTIN_EXTENSIONS");

  public static final List<Extension> INSTALLED_EXTENSIONS;

  static {
    if (DISABLE_BUILTIN_EXTENSIONS) {
      INSTALLED_EXTENSIONS = Collections.unmodifiableList(new ArrayList<>());
    }
    else {
      List<Extension> installed = new ArrayList<>(1);
      installed.add(new StandardExtension("permessage-deflate"));
      INSTALLED_EXTENSIONS = Collections.unmodifiableList(installed);
    }
  }

  @Override
  protected TomcatServerContainer getServerContainer() {
    return (TomcatServerContainer) super.getServerContainer();
  }

  @Override
  protected void doUpgrade(RequestContext context,
                           DefaultWebSocketSession session, WebSocketHandler handler, String subProtocol) {

    final ServerEndpointConfig endpointConfig = handler.getEndpointConfig();
    // Negotiation phase 1. By default this simply filters out the
    // extensions that the server does not support but applications could
    // use a custom configurator to do more than this.
    LinkedList<Extension> installedExtensions = new LinkedList<>(INSTALLED_EXTENSIONS);
    if (!endpointConfig.getExtensions().isEmpty()) {
      installedExtensions.addAll(endpointConfig.getExtensions());
    }

    // Extensions
    // Should normally only be one header but handle the case of multiple
    // headers
    final HttpHeaders requestHeaders = context.requestHeaders();
    List<Extension> extensionsRequested = new ArrayList<>();
    List<String> extHeaders = requestHeaders.get(HttpHeaders.SEC_WEBSOCKET_EXTENSIONS);
    if (extHeaders != null) {
      for (final String extHeader : extHeaders) {
        UpgradeUtils.parseExtensionHeader(extensionsRequested, extHeader);
      }
    }
    List<Extension> negotiatedExtensionsPhase1
            = endpointConfig.getConfigurator().getNegotiatedExtensions(installedExtensions, extensionsRequested);

    // Negotiation phase 2. Create the Transformations that will be applied
    // to this connection. Note than an extension may be dropped at this
    // point if the client has requested a configuration that the server is
    // unable to support.
    List<Transformation> transformations = createTransformations(negotiatedExtensionsPhase1);
    List<Extension> negotiatedExtensionsPhase2;
    if (transformations.isEmpty()) {
      negotiatedExtensionsPhase2 = Collections.emptyList();
    }
    else {
      negotiatedExtensionsPhase2 = new ArrayList<>(transformations.size());
      for (Transformation t : transformations) {
        negotiatedExtensionsPhase2.add(t.getExtensionResponse());
      }
    }

    // Build the transformation pipeline
    Transformation transformation = null;
    StringBuilder responseHeaderExtensions = new StringBuilder();
    boolean first = true;
    for (Transformation t : transformations) {
      if (first) {
        first = false;
      }
      else {
        responseHeaderExtensions.append(',');
      }
      append(responseHeaderExtensions, t.getExtensionResponse());
      if (transformation == null) {
        transformation = t;
      }
      else {
        transformation.setNext(t);
      }
    }

    // Now we have the full pipeline, validate the use of the RSV bits.
    if (transformation != null && !transformation.validateRsvBits(0)) {
      // Extensions were specified that have incompatible RSV bit usage
      // TODO ex
    }
    if (!transformations.isEmpty()) {
      context.responseHeaders().set(HttpHeaders.SEC_WEBSOCKET_EXTENSIONS, responseHeaderExtensions.toString());
    }

    StandardEndpoint endpoint = new StandardEndpoint(handler, session);

    Assert.isInstanceOf(ServletRequestContext.class, context, "Not in tomcat servlet");
    final HttpServletRequest request = ((ServletRequestContext) context).getRequest();

    WsHandshakeResponse handshakeResponse = new WsHandshakeResponse();
    TomcatHandshakeRequest handshakeRequest = new TomcatHandshakeRequest(request, requestHeaders);

    endpointConfig.getConfigurator().modifyHandshake(endpointConfig, handshakeRequest, handshakeResponse);
    handshakeRequest.finished();

    final HttpHeaders responseHeaders = context.responseHeaders();
    // Add any additional headers
    for (Map.Entry<String, List<String>> entry : handshakeResponse.getHeaders().entrySet()) {
      for (String headerValue : entry.getValue()) {
        responseHeaders.add(entry.getKey(), headerValue);
      }
    }

    try {
      TomcatHttpUpgradeHandler wsHandler = request.upgrade(TomcatHttpUpgradeHandler.class);

      TomcatServerContainer webSocketContainer = getServerContainer();
      wsHandler.setEndpoint(endpoint);
      wsHandler.setSecure(request.isSecure());
      wsHandler.setServerEndpointConfig(endpointConfig);
      wsHandler.setWebSocketContainer(webSocketContainer);
      wsHandler.setHandshakeRequest(handshakeRequest);
      wsHandler.setNegotiatedExtensions(negotiatedExtensionsPhase2);
      wsHandler.setSubProtocol(subProtocol);
      wsHandler.setTransformation(transformation);
    }
    catch (Exception e) {
      throw new HandshakeFailedException(e);
    }
  }

  private static List<Transformation> createTransformations(List<Extension> negotiatedExtensions) {
    TransformationFactory factory = TransformationFactory.getInstance();
    LinkedHashMap<String, List<List<Extension.Parameter>>> extensionPreferences =
            new LinkedHashMap<>();

    // Result will likely be smaller than this
    List<Transformation> result = new ArrayList<>(negotiatedExtensions.size());
    for (Extension extension : negotiatedExtensions) {
      List<List<Extension.Parameter>> preferences =
              extensionPreferences.get(extension.getName());

      if (preferences == null) {
        preferences = new ArrayList<>();
        extensionPreferences.put(extension.getName(), preferences);
      }

      preferences.add(extension.getParameters());
    }

    for (Map.Entry<String, List<List<Extension.Parameter>>> entry :
            extensionPreferences.entrySet()) {
      Transformation transformation = factory.create(entry.getKey(), entry.getValue(), true);
      if (transformation != null) {
        result.add(transformation);
      }
    }
    return result;
  }

  private static void append(StringBuilder sb, Extension extension) {
    if (extension == null || extension.getName() == null || extension.getName().length() == 0) {
      return;
    }

    sb.append(extension.getName());

    for (Extension.Parameter p : extension.getParameters()) {
      sb.append(';');
      sb.append(p.getName());
      if (p.getValue() != null) {
        sb.append('=');
        sb.append(p.getValue());
      }
    }
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.serverContainer = new TomcatServerContainer(servletContext);
  }
}
