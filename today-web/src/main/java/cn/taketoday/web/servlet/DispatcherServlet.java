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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet;

import java.io.Serial;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.context.ConfigurableWebEnvironment;
import cn.taketoday.web.context.ConfigurableWebServletApplicationContext;
import cn.taketoday.web.context.ContextLoader;
import cn.taketoday.web.context.support.StandardServletEnvironment;
import cn.taketoday.web.context.support.WebApplicationContextUtils;
import cn.taketoday.web.handler.DispatcherHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Central dispatcher for HTTP request handlers/controllers in Servlet
 *
 * @author TODAY 2018-06-25 19:47:14
 * @since 2.0
 */
public class DispatcherServlet
        extends DispatcherHandler implements Servlet, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Prefix for the ServletContext attribute for the ApplicationContext.
   * The completion is the servlet name.
   */
  public static final String SERVLET_CONTEXT_PREFIX = DispatcherServlet.class.getName() + ".CONTEXT.";

  private transient ServletConfig servletConfig;

  /** ServletContext attribute to find the WebApplicationContext in. */
  @Nullable
  private String contextAttribute;

  /** Should we publish the context as a ServletContext attribute?. */
  private boolean publishContext = true;

  public DispatcherServlet() { }

  /**
   * Create a new {@code InfraHandler} with the given application context. This
   * constructor is useful in Servlet environments where instance-based registration
   * of servlets is possible through the {@link ServletContext#addServlet} API.
   * <p>Using this constructor indicates that the following properties / init-params
   * will be ignored:
   * <ul>
   * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
   * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
   * </ul>
   * <p>The given application context may or may not yet be {@linkplain
   * ConfigurableApplicationContext#refresh() refreshed}. If it (a) is an implementation
   * of {@link ConfigurableApplicationContext} and (b) has <strong>not</strong>
   * already been refreshed (the recommended approach), then the following will occur:
   * <ul>
   * <li>If the given context does not already have a {@linkplain
   * ConfigurableApplicationContext#setParent parent}, the root application context
   * will be set as the parent.</li>
   * <li>If the given context has not already been assigned an {@linkplain
   * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
   * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
   * the application context</li>
   * <li>{@link #postProcessApplicationContext} will be called</li>
   * <li>Any {@link ApplicationContextInitializer ApplicationContextInitializers} specified through the
   * "contextInitializerClasses" init-param or through the {@link
   * #setContextInitializers} property will be applied.</li>
   * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called</li>
   * </ul>
   * If the context has already been refreshed or does not implement
   * {@code ConfigurableApplicationContext}, none of the above will occur under the
   * assumption that the user has performed these actions (or not) per his or her
   * specific needs.
   *
   * @param applicationContext the context to use
   * @see #initApplicationContext
   * @see #configureAndRefreshApplicationContext
   */
  public DispatcherServlet(ApplicationContext context) {
    super(context);
  }

  /**
   * Set the name of the ServletContext attribute which should be used to retrieve the
   * {@link WebApplicationContext} that this servlet is supposed to use.
   *
   * @since 4.0
   */
  public void setContextAttribute(@Nullable String contextAttribute) {
    this.contextAttribute = contextAttribute;
  }

  /**
   * Return the name of the ServletContext attribute which should be used to retrieve the
   * {@link WebApplicationContext} that this servlet is supposed to use.
   *
   * @since 4.0
   */
  @Nullable
  public String getContextAttribute() {
    return this.contextAttribute;
  }

  /**
   * Set whether to publish this servlet's context as a ServletContext attribute,
   * available to all objects in the web container. Default is "true".
   * <p>This is especially handy during testing, although it is debatable whether
   * it's good practice to let other application objects access the context this way.
   *
   * @since 4.0
   */
  public void setPublishContext(boolean publishContext) {
    this.publishContext = publishContext;
  }

  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardServletEnvironment();
  }

  @Override
  public void init(ServletConfig servletConfig) {
    this.servletConfig = servletConfig;
    String servletName = servletConfig.getServletName();
    servletConfig.getServletContext().log(
            "Initializing Infra " + getClass().getSimpleName() + " '" + servletName + "'");
    log.info("Initializing Servlet '{}'", servletName);

    init();
  }

  @Override
  protected void afterApplicationContextInit() {
    if (publishContext) {
      // Publish the context as a servlet context attribute.
      String attrName = getServletContextAttributeName();
      getServletContext().setAttribute(attrName, getApplicationContext());
    }
  }

  @Override
  protected void applyInitializers(ConfigurableApplicationContext context, List<ApplicationContextInitializer> initializers) {
    String globalClassNames = getServletContext().getInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM);
    if (globalClassNames != null) {
      for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
        initializers.add(loadInitializer(className, context));
      }
    }

    super.applyInitializers(context, initializers);
  }

  @Override
  protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
    super.postProcessApplicationContext(context);

    if (context instanceof ConfigurableWebServletApplicationContext wac) {
      wac.setServletContext(getServletContext());
      wac.setServletConfig(getServletConfig());
    }

    // The wac environment's #initPropertySources will be called in any case when the context
    // is refreshed; do it eagerly here to ensure servlet property sources are in place for
    // use in any post-processing or initialization that occurs below prior to #refresh
    ConfigurableEnvironment env = context.getEnvironment();
    if (env instanceof ConfigurableWebEnvironment cwe) {
      cwe.initPropertySources(getServletContext(), getServletConfig());
    }
  }

  @Override
  protected void applyDefaultContextId(ConfigurableApplicationContext context) {
    // Generate default id...
    context.setId(APPLICATION_CONTEXT_ID_PREFIX +
            ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
  }

  @Override
  protected ApplicationContext getRootApplicationContext() {
    return WebApplicationContextUtils.getWebApplicationContext(getServletContext());
  }

  /**
   * Returns a reference to the {@link ServletContext} in which this
   * servlet is running. See {@link ServletConfig#getServletContext}.
   *
   * <p>
   * This method is supplied for convenience. It gets the context
   * from the servlet's <code>ServletConfig</code> object.
   *
   * @return ServletContext the <code>ServletContext</code>
   * object passed to this servlet by the <code>init</code> method
   */
  public ServletContext getServletContext() {
    return getServletConfig().getServletContext();
  }

  /**
   * Retrieve a {@code ApplicationContext} from the {@code ServletContext}
   * attribute with the {@link #setContextAttribute configured name}. The
   * {@code ApplicationContext} must have already been loaded and stored in the
   * {@code ServletContext} before this servlet gets initialized (or invoked).
   * <p>Subclasses may override this method to provide a different
   * {@code ApplicationContext} retrieval strategy.
   *
   * @return the ApplicationContext for this servlet, or {@code null} if not found
   * @see #getContextAttribute()
   */
  @Override
  @Nullable
  protected ApplicationContext findApplicationContext() {
    String attrName = getContextAttribute();
    if (attrName == null) {
      return null;
    }
    WebApplicationContext wac =
            WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
    if (wac == null) {
      throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
    }
    return null;
  }

  /**
   * Return the ServletContext attribute name for this servlet's WebApplicationContext.
   * <p>The default implementation returns
   * {@code SERVLET_CONTEXT_PREFIX + servlet name}.
   *
   * @see #SERVLET_CONTEXT_PREFIX
   * @see #getServletName
   */
  public String getServletContextAttributeName() {
    return SERVLET_CONTEXT_PREFIX + getServletName();
  }

  @Override
  public void service(ServletRequest request, ServletResponse response) throws ServletException {
    HttpServletRequest servletRequest = (HttpServletRequest) request;
    if (log.isDebugEnabled()) {
      logRequest(servletRequest);
    }
    RequestContext context = RequestContextHolder.get();

    boolean reset = false;
    if (context == null) {
      ApplicationContext ctx = getApplicationContext();
      context = new ServletRequestContext(ctx, servletRequest, (HttpServletResponse) response);
      RequestContextHolder.set(context);
      reset = true;
    }

    try {
      dispatch(context);
    }
    catch (final Throwable e) {
      throw new ServletException(e);
    }
    finally {
      if (reset) {
        RequestContextHolder.remove();
      }
    }
  }

  @Override
  public ServletConfig getServletConfig() {
    Assert.state(servletConfig != null, "DispatcherServlet has not been initialized");
    return servletConfig;
  }

  @Override
  public final String getServletInfo() {
    return "DispatcherServlet, Copyright © TODAY & 2017 - 2022 All Rights Reserved";
  }

  /**
   * Returns the name of this servlet instance. See {@link ServletConfig#getServletName}.
   *
   * @return the name of this servlet instance
   */
  public String getServletName() {
    return getServletConfig().getServletName();
  }

  @Override
  protected void logInfo(String msg) {
    super.logInfo(msg);
    getServletContext().log(msg);
  }

  // @since 4.0
  private void logRequest(HttpServletRequest request) {
    String params;
    String contentType = request.getContentType();
    if (StringUtils.startsWithIgnoreCase(contentType, "multipart/")) {
      params = "multipart";
    }
    else if (isEnableLoggingRequestDetails()) {
      params = request.getParameterMap().entrySet().stream()
              .map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue()))
              .collect(Collectors.joining(", "));
    }
    else {
      // Avoid request body parsing for form data
      params = StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                       || !request.getParameterMap().isEmpty() ? "masked" : "";
    }

    String queryString = request.getQueryString();
    String queryClause = StringUtils.isNotEmpty(queryString) ? "?" + queryString : "";
    String dispatchType = !DispatcherType.REQUEST.equals(request.getDispatcherType())
                          ? "\"" + request.getDispatcherType() + "\" dispatch for "
                          : "";
    String message = dispatchType + request.getMethod() + " " +
            request.getRequestURL() + queryClause + ", parameters={" + params + "}";
    message = URLDecoder.decode(message, StandardCharsets.UTF_8);
    if (log.isTraceEnabled()) {
      StringBuilder headers = new StringBuilder();
      Enumeration<String> headerNames = request.getHeaderNames();
      if (headerNames.hasMoreElements()) {
        if (isEnableLoggingRequestDetails()) {
          // first
          String name = headerNames.nextElement();
          headers.append(name)
                  .append(':')
                  .append(Collections.list(request.getHeaders(name)));

          while (headerNames.hasMoreElements()) {
            name = headerNames.nextElement();
            headers.append(", ");
            headers.append(name);
            headers.append(':');
            headers.append(Collections.list(request.getHeaders(name)));
          }
        }
        else {
          headers.append("masked");
        }
      }

      log.trace(message + ", headers={" + headers + "} in DispatcherServlet '" +
              getServletConfig().getServletName() + "'");
    }
    else {
      log.debug(message);
    }
  }

}
