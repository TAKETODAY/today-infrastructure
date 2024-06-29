/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.WebSession;
import cn.taketoday.web.RedirectModelManager;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.bind.support.WebBindingInitializer;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;

/**
 * Extension of {@link AbstractHandlerMethodAdapter} that supports
 * {@link RequestMapping @RequestMapping} annotated {@link HandlerMethod HandlerMethods}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ParameterResolvingStrategy
 * @see HandlerMethodReturnValueHandler
 * @see RequestMappingHandlerMapping
 * @since 4.0 2022/4/8 22:46
 */
public class RequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter
        implements BeanFactoryAware, InitializingBean {

  @Nullable
  private ParameterResolvingRegistry resolvingRegistry;

  @Nullable
  private WebBindingInitializer webBindingInitializer;

  private boolean synchronizeOnSession = false;

  private ParameterNameDiscoverer parameterNameDiscoverer = ParameterNameDiscoverer.getSharedInstance();

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  @Nullable
  private SessionManager sessionManager;

  @Nullable
  private RedirectModelManager redirectModelManager;

  private ControllerMethodResolver methodResolver;

  private ModelHandler modelHandler;

  public void setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  public void setResolvingRegistry(@Nullable ParameterResolvingRegistry resolvingRegistry) {
    this.resolvingRegistry = resolvingRegistry;
  }

  /**
   * Provide a WebBindingInitializer with "global" initialization to apply
   * to every DataBinder instance.
   */
  public void setWebBindingInitializer(@Nullable WebBindingInitializer webBindingInitializer) {
    this.webBindingInitializer = webBindingInitializer;
  }

  /**
   * Return the configured WebBindingInitializer, or {@code null} if none.
   */
  @Nullable
  public WebBindingInitializer getWebBindingInitializer() {
    return this.webBindingInitializer;
  }

  /**
   * Set if controller execution should be synchronized on the session,
   * to serialize parallel invocations from the same client.
   * <p>More specifically, the execution of the {@code handleRequestInternal}
   * method will get synchronized if this flag is "true". The best available
   * session mutex will be used for the synchronization; ideally, this will
   * be a mutex exposed by HttpSessionMutexListener.
   * <p>The session mutex is guaranteed to be the same object during
   * the entire lifetime of the session, available under the key defined
   * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
   * safe reference to synchronize on for locking on the current session.
   * <p>In many cases, the HttpSession reference itself is a safe mutex
   * as well, since it will always be the same object reference for the
   * same active logical session. However, this is not guaranteed across
   * different servlet containers; the only 100% safe way is a session mutex.
   *
   * @see cn.taketoday.web.util.WebSessionMutexListener
   * @see cn.taketoday.web.util.WebUtils#getSessionMutex(WebSession)
   */
  public void setSynchronizeOnSession(boolean synchronizeOnSession) {
    this.synchronizeOnSession = synchronizeOnSession;
  }

  /**
   * Set the ParameterNameDiscoverer to use for resolving method parameter names if needed
   * (e.g. for default attribute names).
   * <p>Default is a {@link cn.taketoday.core.DefaultParameterNameDiscoverer}.
   */
  public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * A {@link ConfigurableBeanFactory} is expected for resolving expressions
   * in method argument default values.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (beanFactory instanceof ConfigurableBeanFactory) {
      this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }
  }

  /**
   * Return the owning factory of this bean instance, or {@code null} if none.
   */
  @Nullable
  protected ConfigurableBeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  public void setSessionManager(@Nullable SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Override
  public void afterPropertiesSet() {
    ApplicationContext context = obtainApplicationContext();
    if (resolvingRegistry == null) {
      resolvingRegistry = ParameterResolvingRegistry.get(context);
    }

    this.methodResolver = new ControllerMethodResolver(context,
            new RegistryResolvableParameterFactory(resolvingRegistry, parameterNameDiscoverer));

    this.modelHandler = new ModelHandler(methodResolver);
  }

  /**
   * Always return {@code true} since any method argument and return value
   * type will be processed in some way. A method argument not recognized
   * by any ParameterResolvingStrategy is interpreted as a request parameter
   * if it is a simple type, or as a model attribute otherwise. A return value
   * not recognized by any HandlerMethodReturnValueHandler will be interpreted
   * as a model attribute.
   */
  @Override
  protected boolean supportsInternal(HandlerMethod handlerMethod) {
    return true;
  }

  @Override
  protected Object handleInternal(RequestContext request, HandlerMethod handlerMethod) throws Throwable {
    Object returnValue;

    // Execute invokeHandlerMethod in synchronized block if required.
    if (synchronizeOnSession) {
      WebSession session = getSession(request);
      if (session != null) {
        Object mutex = WebUtils.getSessionMutex(session);
        synchronized(mutex) {
          returnValue = invokeHandlerMethod(request, handlerMethod);
        }
      }
      else {
        // No Session available -> no mutex necessary
        returnValue = invokeHandlerMethod(request, handlerMethod);
      }
    }
    else {
      // No synchronization on session demanded at all...
      returnValue = invokeHandlerMethod(request, handlerMethod);
    }

    if (!request.containsResponseHeader(HttpHeaders.CACHE_CONTROL)) {
      prepareResponse(request);
    }

    return returnValue;
  }

  @Nullable
  private WebSession getSession(RequestContext request) {
    WebSession session = null;
    if (sessionManager != null) {
      session = sessionManager.getSession(request, false);
    }
    if (session == null) {
      session = RequestContextUtils.getSession(request, false);
    }
    return session;
  }

  /**
   * Invoke the {@link RequestMapping} handler method preparing a {@link ModelAndView}
   * if view resolution is required.
   *
   * @see ControllerMethodResolver#createHandlerMethod(HandlerMethod)
   */
  @Nullable
  protected Object invokeHandlerMethod(RequestContext request, HandlerMethod handlerMethod) throws Throwable {
    var binding = new InitBinderBindingContext(modelHandler, webBindingInitializer, methodResolver, handlerMethod);
    request.setBinding(binding);

    // add last RedirectModel to this request
    var inputRedirectModel = request.getInputRedirectModel(redirectModelManager);
    if (inputRedirectModel != null) {
      binding.addAllAttributes(inputRedirectModel);
    }

    binding.initModel(request);

    if (handlerMethod instanceof InvocableHandlerMethod invocableMethod) {
      return invocableMethod.invokeAndHandle(request);
    }

    return methodResolver.createHandlerMethod(handlerMethod)
            .invokeAndHandle(request);
  }

}
