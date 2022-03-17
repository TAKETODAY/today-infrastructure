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

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.websocket.WsSession;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.apache.tomcat.websocket.pojo.PojoMethodMapping;
import org.apache.tomcat.websocket.server.Constants;
import org.apache.tomcat.websocket.server.UriTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import jakarta.servlet.ServletContext;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Encoder;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;

/**
 * @author TODAY 2021/5/4 23:40
 * @since 3.0.1
 */
public class TomcatServerContainer extends WsWebSocketContainer implements ServerContainer {
  private static final CloseReason AUTHENTICATED_HTTP_SESSION_CLOSED =
          new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                          "This connection was established under an authenticated " +
                                  "HTTP session that has ended.");

  private final TomcatWriteTimeout wsWriteTimeout = new TomcatWriteTimeout();

  private final ServletContext servletContext;
  private final Map<String, ExactPathMatch> configExactMatchMap = new ConcurrentHashMap<>();
  private final Map<Integer, ConcurrentSkipListMap<String, TemplatePathMatch>>
          configTemplateMatchMap = new ConcurrentHashMap<>();

  private volatile boolean enforceNoAddAfterHandshake = org.apache.tomcat.websocket.Constants.STRICT_SPEC_COMPLIANCE;
  private volatile boolean addAllowed = true;
  private final Map<String, Set<WsSession>> authenticatedSessions = new ConcurrentHashMap<>();
  private volatile boolean endpointsRegistered = false;
  private volatile boolean deploymentFailed = false;

  public TomcatServerContainer(ServletContext servletContext) {
    this.servletContext = servletContext;
    setInstanceManager((InstanceManager) servletContext.getAttribute(InstanceManager.class.getName()));

    // Configure servlet context wide defaults
    String value = servletContext.getInitParameter(
            Constants.BINARY_BUFFER_SIZE_SERVLET_CONTEXT_INIT_PARAM);
    if (value != null) {
      setDefaultMaxBinaryMessageBufferSize(Integer.parseInt(value));
    }

    value = servletContext.getInitParameter(
            Constants.TEXT_BUFFER_SIZE_SERVLET_CONTEXT_INIT_PARAM);
    if (value != null) {
      setDefaultMaxTextMessageBufferSize(Integer.parseInt(value));
    }

    value = servletContext.getInitParameter(
            Constants.ENFORCE_NO_ADD_AFTER_HANDSHAKE_CONTEXT_INIT_PARAM);
    if (value != null) {
      setEnforceNoAddAfterHandshake(Boolean.parseBoolean(value));
    }
  }

  /**
   * Published the provided endpoint implementation at the specified path with
   * the specified configuration. {@link #TomcatServerContainer(ServletContext)}
   * must be called before calling this method.
   *
   * @param sec The configuration to use when creating endpoint instances
   * @throws DeploymentException if the endpoint cannot be published as
   * requested
   */
  @Override
  public void addEndpoint(ServerEndpointConfig sec) throws DeploymentException {
    addEndpoint(sec, false);
  }

  private void addEndpoint(ServerEndpointConfig sec, boolean fromAnnotatedPojo) throws DeploymentException {
    if (enforceNoAddAfterHandshake && !addAllowed) {
      //serverContainer.addNotAllowed=
      throw new DeploymentException(
              "No further Endpoints may be registered once an attempt has been made to use one of the previously registered endpoints");
    }

    if (servletContext == null) {
      throw new DeploymentException("No ServletContext was specified");
    }

    if (deploymentFailed) {
      //serverContainer.failedDeployment=
      throw new DeploymentException(String.format(
              "Deployment of WebSocket Endpoints to the web application with path [%s] in host [%s]" +
                      " is not permitted due to the failure of a previous deployment",
              servletContext.getContextPath(), servletContext.getVirtualServerName()));
    }
    try {
      String path = sec.getPath();

      // Add method mapping to user properties
      PojoMethodMapping methodMapping = new PojoMethodMapping(sec.getEndpointClass(), sec.getDecoders(), path, null);
      if (methodMapping.getOnClose() != null || methodMapping.getOnOpen() != null
              || methodMapping.getOnError() != null || methodMapping.hasMessageHandlers()) {
        sec.getUserProperties().put(org.apache.tomcat.websocket.pojo.Constants.POJO_METHOD_MAPPING_KEY,
                                    methodMapping);
      }

      UriTemplate uriTemplate = new UriTemplate(path);
      if (uriTemplate.hasParameters()) {
        Integer key = uriTemplate.getSegmentCount();
        ConcurrentSkipListMap<String, TemplatePathMatch> templateMatches = configTemplateMatchMap.get(key);
        if (templateMatches == null) {
          // Ensure that if concurrent threads execute this block they
          // all end up using the same ConcurrentSkipListMap instance
          templateMatches = new ConcurrentSkipListMap<>();
          configTemplateMatchMap.putIfAbsent(key, templateMatches);
          templateMatches = configTemplateMatchMap.get(key);
        }
        TemplatePathMatch newMatch = new TemplatePathMatch(sec, uriTemplate, fromAnnotatedPojo);
        TemplatePathMatch oldMatch = templateMatches.putIfAbsent(uriTemplate.getNormalizedPath(), newMatch);
        if (oldMatch != null) {
          // Note: This depends on Endpoint instances being added
          //       before POJOs in WsSci#onStartup()
          if (oldMatch.isFromAnnotatedPojo() && !newMatch.isFromAnnotatedPojo() &&
                  oldMatch.getConfig().getEndpointClass() == newMatch.getConfig().getEndpointClass()) {
            // The WebSocket spec says to ignore the new match in this case
            templateMatches.put(path, oldMatch);
          }
          else {
            // Duplicate uriTemplate;
            throw new DeploymentException(
                    String.format("Multiple Endpoints may not be deployed to the same path [%s] :" +
                                          " existing endpoint was [%s] and new endpoint is [%s]",
                                  path, sec.getEndpointClass(), sec.getEndpointClass()));
          }
        }
      }
      else {
        // Exact match
        ExactPathMatch newMatch = new ExactPathMatch(sec, fromAnnotatedPojo);
        ExactPathMatch oldMatch = configExactMatchMap.put(path, newMatch);
        if (oldMatch != null) {
          // Note: This depends on Endpoint instances being added
          //       before POJOs in WsSci#onStartup()
          if (oldMatch.isFromAnnotatedPojo() && !newMatch.isFromAnnotatedPojo() &&
                  oldMatch.getConfig().getEndpointClass() == newMatch.getConfig().getEndpointClass()) {
            // The WebSocket spec says to ignore the new match in this case
            configExactMatchMap.put(path, oldMatch);
          }
          else {
            // Duplicate path mappings
            throw new DeploymentException(String.format(
                    "Multiple Endpoints may not be deployed to the same path [%s] : " +
                            "existing endpoint was [%s] and new endpoint is [%s]",
                    path, oldMatch.getConfig().getEndpointClass(), sec.getEndpointClass()));
          }
        }
      }

      endpointsRegistered = true;
    }
    catch (DeploymentException de) {
      failDeployment();
      throw de;
    }
  }

  /**
   * Provides the equivalent of {@link #addEndpoint(ServerEndpointConfig)}
   * for publishing plain old java objects (POJOs) that have been annotated as
   * WebSocket endpoints.
   *
   * @param pojo The annotated POJO
   */
  @Override
  public void addEndpoint(Class<?> pojo) throws DeploymentException {
    if (deploymentFailed) {
      throw new DeploymentException(
              String.format(
                      "Deployment of WebSocket Endpoints to the web application with path" +
                              " [%s] in host [%s] is not permitted due to the failure of a previous deployment",
                      servletContext.getContextPath(), servletContext.getVirtualServerName()));
    }

    ServerEndpointConfig sec;
    try {
      ServerEndpoint annotation = pojo.getAnnotation(ServerEndpoint.class);
      if (annotation == null) {
        throw new DeploymentException(
                String.format("Cannot deploy POJO class [%s] as it is not annotated with @ServerEndpoint", pojo.getName()));
      }
      String path = annotation.value();

      // Validate encoders
      validateEncoders(annotation.encoders());

      // ServerEndpointConfig
      Class<? extends Configurator> configuratorClazz = annotation.configurator();
      Configurator configurator = null;
      if (!configuratorClazz.equals(Configurator.class)) {
        try {
          configurator = annotation.configurator().getConstructor().newInstance();
        }
        catch (ReflectiveOperationException e) {
          //serverContainer.configuratorFail=Failed to create configurator of type [{0}] for POJO of type [{1}]
          throw new DeploymentException(String.format(
                  "Failed to create configurator of type [%s] for POJO of type [%s]",
                  annotation.configurator().getName(), pojo.getName()), e);
        }
      }
      sec = ServerEndpointConfig.Builder.create(pojo, path)
              .decoders(Arrays.asList(annotation.decoders()))
              .encoders(Arrays.asList(annotation.encoders()))
              .subprotocols(Arrays.asList(annotation.subprotocols()))
              .configurator(configurator)
              .build();
    }
    catch (DeploymentException de) {
      failDeployment();
      throw de;
    }

    addEndpoint(sec, false);
  }

  void failDeployment() {
    deploymentFailed = true;

    // Clear all existing deployments
    endpointsRegistered = false;
    configExactMatchMap.clear();
    configTemplateMatchMap.clear();
  }

  boolean areEndpointsRegistered() {
    return endpointsRegistered;
  }

  record WebSocketMappingResult(ServerEndpointConfig config, Map<String, String> pathParams) {

    ServerEndpointConfig getConfig() {
      return config;
    }

    Map<String, String> getPathParams() {
      return pathParams;
    }
  }

  public WebSocketMappingResult findMapping(String path) {
    // Prevent registering additional endpoints once the first attempt has
    // been made to use one
    if (addAllowed) {
      addAllowed = false;
    }
    // Check an exact match. Simple case as there are no templates.
    ExactPathMatch match = configExactMatchMap.get(path);
    if (match != null) {
      return new WebSocketMappingResult(match.getConfig(), Collections.emptyMap());
    }

    // No exact match. Need to look for template matches.
    UriTemplate pathUriTemplate = null;
    try {
      pathUriTemplate = new UriTemplate(path);
    }
    catch (DeploymentException e) {
      // Path is not valid so can't be matched to a WebSocketEndpoint
      return null;
    }

    // Number of segments has to match
    Integer key = pathUriTemplate.getSegmentCount();
    ConcurrentSkipListMap<String, TemplatePathMatch> templateMatches = configTemplateMatchMap.get(key);

    if (templateMatches == null) {
      // No templates with an equal number of segments so there will be
      // no matches
      return null;
    }

    // List is in alphabetical order of normalised templates.
    // Correct match is the first one that matches.
    ServerEndpointConfig sec = null;
    Map<String, String> pathParams = null;
    for (TemplatePathMatch templateMatch : templateMatches.values()) {
      pathParams = templateMatch.getUriTemplate().match(pathUriTemplate);
      if (pathParams != null) {
        sec = templateMatch.getConfig();
        break;
      }
    }

    if (sec == null) {
      // No match
      return null;
    }

    return new WebSocketMappingResult(sec, pathParams);
  }

  public boolean isEnforceNoAddAfterHandshake() {
    return enforceNoAddAfterHandshake;
  }

  public void setEnforceNoAddAfterHandshake(
          boolean enforceNoAddAfterHandshake) {
    this.enforceNoAddAfterHandshake = enforceNoAddAfterHandshake;
  }

  public TomcatWriteTimeout getTimeout() {
    return wsWriteTimeout;
  }

  /**
   * {@inheritDoc}
   *
   * Overridden to make it visible to other classes in this package.
   */
  @Override
  public void registerSession(Object key, WsSession wsSession) {
    super.registerSession(key, wsSession);
    if (wsSession.isOpen() &&
            wsSession.getUserPrincipal() != null &&
            wsSession.getHttpSessionId() != null) {
      registerAuthenticatedSession(
              wsSession, wsSession.getHttpSessionId());
    }
  }

  /**
   * {@inheritDoc}
   *
   * Overridden to make it visible to other classes in this package.
   */
  @Override
  protected void unregisterSession(Object key, WsSession wsSession) {
    if (wsSession.getUserPrincipal() != null &&
            wsSession.getHttpSessionId() != null) {
      unregisterAuthenticatedSession(wsSession, wsSession.getHttpSessionId());
    }
    super.unregisterSession(key, wsSession);
  }

  private void registerAuthenticatedSession(
          WsSession wsSession, String httpSessionId) {
    Set<WsSession> wsSessions = authenticatedSessions.get(httpSessionId);
    if (wsSessions == null) {
      wsSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
      authenticatedSessions.putIfAbsent(httpSessionId, wsSessions);
      wsSessions = authenticatedSessions.get(httpSessionId);
    }
    wsSessions.add(wsSession);
  }

  private void unregisterAuthenticatedSession(
          WsSession wsSession, String httpSessionId) {
    Set<WsSession> wsSessions = authenticatedSessions.get(httpSessionId);
    // wsSessions will be null if the HTTP session has ended
    if (wsSessions != null) {
      wsSessions.remove(wsSession);
    }
  }

  public void closeAuthenticatedSession(String httpSessionId) {
    Set<WsSession> wsSessions = authenticatedSessions.remove(httpSessionId);

    if (wsSessions != null && !wsSessions.isEmpty()) {
      for (WsSession wsSession : wsSessions) {
        try {
          wsSession.close(AUTHENTICATED_HTTP_SESSION_CLOSED);
        }
        catch (IOException e) {
          // Any IOExceptions during close will have been caught and the
          // onError method called.
        }
      }
    }
  }

  private static void validateEncoders(Class<? extends Encoder>[] encoders)
          throws DeploymentException {

    for (Class<? extends Encoder> encoder : encoders) {
      // Need to instantiate decoder to ensure it is valid and that
      // deployment can be failed if it is not
      @SuppressWarnings("unused")
      Encoder instance;
      try {
        encoder.getConstructor().newInstance();
      }
      catch (ReflectiveOperationException e) {
        throw new DeploymentException(String.format("Unable to create encoder of type [%s]", encoder.getName()), e);
      }
    }
  }

  private record TemplatePathMatch(
          ServerEndpointConfig config, UriTemplate uriTemplate, boolean fromAnnotatedPojo) {

    public ServerEndpointConfig getConfig() {
      return config;
    }

    public UriTemplate getUriTemplate() {
      return uriTemplate;
    }

    public boolean isFromAnnotatedPojo() {
      return fromAnnotatedPojo;
    }
  }

  private record ExactPathMatch(ServerEndpointConfig config, boolean fromAnnotatedPojo) {

    public ServerEndpointConfig getConfig() {
      return config;
    }

    public boolean isFromAnnotatedPojo() {
      return fromAnnotatedPojo;
    }
  }
}
