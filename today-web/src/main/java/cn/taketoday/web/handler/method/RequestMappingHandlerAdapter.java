/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.handler.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.WebSession;
import cn.taketoday.util.ReflectionUtils.MethodFilter;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.InitBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.bind.support.DefaultSessionAttributeStore;
import cn.taketoday.web.bind.support.SessionAttributeStore;
import cn.taketoday.web.bind.support.WebBindingInitializer;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.context.async.CallableProcessingInterceptor;
import cn.taketoday.web.context.async.DeferredResultProcessingInterceptor;
import cn.taketoday.web.context.async.WebAsyncManager;
import cn.taketoday.web.context.async.WebAsyncTask;
import cn.taketoday.web.context.async.WebAsyncUtils;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
import cn.taketoday.web.view.RedirectModelManager;

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
 * @since 4.0 2022/4/8 22:46
 */
public class RequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter
        implements BeanFactoryAware, InitializingBean {

  /**
   * MethodFilter that matches {@link InitBinder @InitBinder} methods.
   */
  public static final MethodFilter INIT_BINDER_METHODS = method ->
          AnnotatedElementUtils.hasAnnotation(method, InitBinder.class);

  /**
   * MethodFilter that matches {@link ModelAttribute @ModelAttribute} methods.
   */
  public static final MethodFilter MODEL_ATTRIBUTE_METHODS = method ->
          !AnnotatedElementUtils.hasAnnotation(method, ActionMapping.class)
                  && AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class);

  private ParameterResolvingRegistry resolvingRegistry;
  private ResolvableParameterFactory resolvableParameterFactory;

  private ReturnValueHandlerManager returnValueHandlerManager;

  @Nullable
  private List<ModelAndViewResolver> modelAndViewResolvers;

  @Nullable
  private WebBindingInitializer webBindingInitializer;

  private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("MvcAsync");

  @Nullable
  private Long asyncRequestTimeout;

  private CallableProcessingInterceptor[] callableInterceptors = new CallableProcessingInterceptor[0];

  private DeferredResultProcessingInterceptor[] deferredResultInterceptors = new DeferredResultProcessingInterceptor[0];

  private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

  private int cacheSecondsForSessionAttributeHandlers = 0;

  private boolean synchronizeOnSession = false;

  private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

  private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache = new ConcurrentHashMap<>(64);

  private final Map<Class<?>, Set<Method>> initBinderCache = new ConcurrentHashMap<>(64);

  private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>();

  private final Map<Class<?>, Set<Method>> modelAttributeCache = new ConcurrentHashMap<>(64);

  private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>();

  private final Map<HandlerMethod, ActionMappingAnnotationHandler> annotationHandlerMap = new HashMap<>();
  private final Map<HandlerMethod, ResultableHandlerMethod> invocableHandlerMethodMap = new ConcurrentHashMap<>();

  @Nullable
  private SessionManager sessionManager;

  @Nullable
  private RedirectModelManager redirectModelManager;

  public void setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  public void setResolvingRegistry(ParameterResolvingRegistry resolvingRegistry) {
    this.resolvingRegistry = resolvingRegistry;
    this.resolvableParameterFactory =
            new ParameterResolvingRegistryResolvableParameterFactory(resolvingRegistry);
  }

  /**
   * Provide handlers for custom return value types. Custom handlers are
   * ordered after built-in ones. To override the built-in support for
   * return value handling use {@link #setReturnValueHandlerManager}.
   */
  public void setCustomReturnValueHandlers(List<ReturnValueHandler> returnValueHandlers) {
    returnValueHandlerManager.addHandlers(returnValueHandlers);
  }

  /**
   * Configure the complete list of supported return value types thus
   * overriding handlers that would otherwise be configured by default.
   */
  public void setReturnValueHandlerManager(@Nullable ReturnValueHandlerManager manager) {
    if (manager == null) {
      manager = new ReturnValueHandlerManager();
      manager.setApplicationContext(getApplicationContext());
      manager.registerDefaultHandlers();
    }
    this.returnValueHandlerManager = manager;
  }

  /**
   * Return the configured handlers, or possibly {@code null} if not
   * initialized yet via {@link #afterPropertiesSet()}.
   */
  public ReturnValueHandlerManager getReturnValueHandlerManager() {
    return this.returnValueHandlerManager;
  }

  /**
   * Provide custom {@link ModelAndViewResolver ModelAndViewResolvers}.
   * <p><strong>Note:</strong> This method is available for backwards
   * compatibility only. However, it is recommended to re-write a
   * {@code ModelAndViewResolver} as {@link HandlerMethodReturnValueHandler}.
   * An adapter between the two interfaces is not possible since the
   * {@link HandlerMethodReturnValueHandler#supportsHandlerMethod(HandlerMethod)} method
   * cannot be implemented. Hence {@code ModelAndViewResolver}s are limited
   * to always being invoked at the end after all other return value
   * handlers have been given a chance.
   * <p>A {@code HandlerMethodReturnValueHandler} provides better access to
   * the return type and controller method information and can be ordered
   * freely relative to other return value handlers.
   */
  public void setModelAndViewResolvers(@Nullable List<ModelAndViewResolver> modelAndViewResolvers) {
    this.modelAndViewResolvers = modelAndViewResolvers;
  }

  /**
   * Return the configured {@link ModelAndViewResolver ModelAndViewResolvers}, or {@code null}.
   */
  @Nullable
  public List<ModelAndViewResolver> getModelAndViewResolvers() {
    return this.modelAndViewResolvers;
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
   * Set the default {@link AsyncTaskExecutor} to use when a controller method
   * return a {@link Callable}. Controller methods can override this default on
   * a per-request basis by returning an {@link WebAsyncTask}.
   * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used.
   * It's recommended to change that default in production as the simple executor
   * does not re-use threads.
   */
  public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Specify the amount of time, in milliseconds, before concurrent handling
   * should time out. In Servlet 3, the timeout begins after the main request
   * processing thread has exited and ends when the request is dispatched again
   * for further processing of the concurrently produced result.
   * <p>If this value is not set, the default timeout of the underlying
   * implementation is used.
   *
   * @param timeout the timeout value in milliseconds
   */
  public void setAsyncRequestTimeout(long timeout) {
    this.asyncRequestTimeout = timeout;
  }

  /**
   * Configure {@code CallableProcessingInterceptor}'s to register on async requests.
   *
   * @param interceptors the interceptors to register
   */
  public void setCallableInterceptors(List<CallableProcessingInterceptor> interceptors) {
    this.callableInterceptors = interceptors.toArray(new CallableProcessingInterceptor[0]);
  }

  /**
   * Configure {@code DeferredResultProcessingInterceptor}'s to register on async requests.
   *
   * @param interceptors the interceptors to register
   */
  public void setDeferredResultInterceptors(List<DeferredResultProcessingInterceptor> interceptors) {
    this.deferredResultInterceptors = interceptors.toArray(new DeferredResultProcessingInterceptor[0]);
  }

  /**
   * Configure the registry for reactive library types to be supported as
   * return values from controller methods.
   */
  public void setReactiveAdapterRegistry(ReactiveAdapterRegistry reactiveAdapterRegistry) {
    this.reactiveAdapterRegistry = reactiveAdapterRegistry;
  }

  /**
   * Return the configured reactive type registry of adapters.
   */
  public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
    return this.reactiveAdapterRegistry;
  }

  /**
   * Specify the strategy to store session attributes with. The default is
   * {@link cn.taketoday.web.bind.support.DefaultSessionAttributeStore},
   * storing session attributes in the WebSession with the same attribute
   * name as in the model.
   */
  public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
    this.sessionAttributeStore = sessionAttributeStore;
  }

  /**
   * Cache content produced by {@code @SessionAttributes} annotated handlers
   * for the given number of seconds.
   * <p>Possible values are:
   * <ul>
   * <li>-1: no generation of cache-related headers</li>
   * <li>0 (default value): "Cache-Control: no-store" will prevent caching</li>
   * <li>1 or higher: "Cache-Control: max-age=seconds" will ask to cache content;
   * not advised when dealing with session attributes</li>
   * </ul>
   * <p>In contrast to the "cacheSeconds" property which will apply to all general
   * handlers (but not to {@code @SessionAttributes} annotated handlers),
   * this setting will apply to {@code @SessionAttributes} handlers only.
   *
   * @see #setCacheSeconds
   * @see cn.taketoday.web.bind.annotation.SessionAttributes
   */
  public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
    this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
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
    // Do this first, it may add ResponseBody advice beans
    initControllerAdviceCache();
    if (resolvingRegistry == null) {
      ParameterResolvingRegistry resolvingRegistry = new ParameterResolvingRegistry();
      resolvingRegistry.setApplicationContext(getApplicationContext());
      resolvingRegistry.registerDefaultStrategies();
      setResolvingRegistry(resolvingRegistry);
    }
    // prepare returnValueHandlerManager
    setReturnValueHandlerManager(returnValueHandlerManager);
  }

  private void initControllerAdviceCache() {
    if (getApplicationContext() == null) {
      return;
    }

    var adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
    for (ControllerAdviceBean adviceBean : adviceBeans) {
      Class<?> beanType = adviceBean.getBeanType();
      if (beanType == null) {
        throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
      }
      Set<Method> attrMethods = MethodIntrospector.filterMethods(beanType, MODEL_ATTRIBUTE_METHODS);
      if (!attrMethods.isEmpty()) {
        this.modelAttributeAdviceCache.put(adviceBean, attrMethods);
      }
      Set<Method> binderMethods = MethodIntrospector.filterMethods(beanType, INIT_BINDER_METHODS);
      if (!binderMethods.isEmpty()) {
        this.initBinderAdviceCache.put(adviceBean, binderMethods);
      }
    }

    if (log.isDebugEnabled()) {
      int binderSize = this.initBinderAdviceCache.size();
      int modelSize = this.modelAttributeAdviceCache.size();
      if (modelSize == 0 && binderSize == 0) {
        log.debug("ControllerAdvice beans: none");
      }
      else {
        log.debug("ControllerAdvice beans: {} @ModelAttribute, {} @InitBinder", modelSize, binderSize);
      }
    }
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
    checkRequest(request);

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

    HttpHeaders headers = request.getHeaders();
    if (!headers.containsKey(HEADER_CACHE_CONTROL)) {
      if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
        applyCacheSeconds(request, cacheSecondsForSessionAttributeHandlers);
      }
      else {
        prepareResponse(request);
      }
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
   * Return the {@link SessionAttributesHandler} instance for the given handler type
   * (never {@code null}).
   */
  private SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
    return this.sessionAttributesHandlerCache.computeIfAbsent(
            handlerMethod.getBeanType(),
            type -> new SessionAttributesHandler(type, this.sessionAttributeStore));
  }

  /**
   * Invoke the {@link RequestMapping} handler method preparing a {@link ModelAndView}
   * if view resolution is required.
   *
   * @see #createInvocableHandlerMethod(HandlerMethod)
   */
  @Nullable
  protected Object invokeHandlerMethod(
          RequestContext request, HandlerMethod handlerMethod) throws Throwable {

    BindingContext bindingContext = createBindingContext(handlerMethod);

    ModelFactory modelFactory = getModelFactory(handlerMethod);

    var invocableMethod = createInvocableHandlerMethod(handlerMethod);

    RedirectModel inputRedirectModel = RequestContextUtils.getInputRedirectModel(request, redirectModelManager);
    bindingContext.addAllAttributes(inputRedirectModel);

    modelFactory.initModel(request, bindingContext, invocableMethod);

    AsyncWebRequest asyncWebRequest = request.getAsyncWebRequest();
    asyncWebRequest.setTimeout(asyncRequestTimeout);

    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
    asyncManager.setTaskExecutor(taskExecutor);
    asyncManager.setAsyncRequest(asyncWebRequest);
    asyncManager.registerCallableInterceptors(callableInterceptors);
    asyncManager.registerDeferredResultInterceptors(deferredResultInterceptors);

    Object returnValue = invocableMethod.invokeAndHandle(request, bindingContext);

    if (request.isConcurrentHandlingStarted()) {
      return HttpRequestHandler.NONE_RETURN_VALUE;
    }

    return returnValue;
  }

  private InitBinderBindingContext createBindingContext(HandlerMethod handlerMethod) {
    List<InvocableHandlerMethod> binderMethods = getBinderMethods(handlerMethod);
    return new InitBinderBindingContext(getWebBindingInitializer(), binderMethods);
  }

  /**
   * Create a {@link ResultableHandlerMethod} from the given {@link HandlerMethod} definition.
   *
   * @param handlerMethod the {@link HandlerMethod} definition
   * @return the corresponding {@link ResultableHandlerMethod} (or custom subclass thereof)
   */
  protected ResultableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
    return invocableHandlerMethodMap.computeIfAbsent(handlerMethod,
            handler -> new ResultableHandlerMethod(
                    handler, returnValueHandlerManager, resolvableParameterFactory));
  }

  private ModelFactory getModelFactory(HandlerMethod handlerMethod) {
    SessionAttributesHandler sessionAttrHandler = getSessionAttributesHandler(handlerMethod);
    Class<?> handlerType = handlerMethod.getBeanType();

    ArrayList<InvocableHandlerMethod> attrMethods = new ArrayList<>();
    // Global methods first
    for (var entry : modelAttributeAdviceCache.entrySet()) {
      ControllerAdviceBean controllerAdviceBean = entry.getKey();
      if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
        Object bean = controllerAdviceBean.resolveBean();
        for (Method method : entry.getValue()) {
          attrMethods.add(createModelAttributeMethod(bean, method));
        }
      }
    }
    // controller local methods
    Set<Method> methods = modelAttributeCache.get(handlerType);
    if (methods == null) {
      methods = MethodIntrospector.filterMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
      modelAttributeCache.put(handlerType, methods);
    }
    for (Method method : methods) {
      Object bean = handlerMethod.getBean();
      attrMethods.add(createModelAttributeMethod(bean, method));
    }
    return new ModelFactory(attrMethods, sessionAttrHandler);
  }

  private InvocableHandlerMethod createModelAttributeMethod(Object bean, Method method) {
    return new InvocableHandlerMethod(bean, method, resolvableParameterFactory);
  }

  private List<InvocableHandlerMethod> getBinderMethods(HandlerMethod handlerMethod) {
    Class<?> handlerType = handlerMethod.getBeanType();
    Set<Method> methods = initBinderCache.get(handlerType);
    if (methods == null) {
      methods = MethodIntrospector.filterMethods(handlerType, INIT_BINDER_METHODS);
      initBinderCache.put(handlerType, methods);
    }

    var initBinderMethods = new ArrayList<InvocableHandlerMethod>();
    // Global methods first
    for (var entry : initBinderAdviceCache.entrySet()) {
      Set<Method> methodSet = entry.getValue();
      ControllerAdviceBean controllerAdviceBean = entry.getKey();
      if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
        Object bean = controllerAdviceBean.resolveBean();
        for (Method method : methodSet) {
          initBinderMethods.add(createInitBinderMethod(bean, method));
        }
      }
    }

    for (Method method : methods) {
      Object bean = handlerMethod.getBean();
      initBinderMethods.add(createInitBinderMethod(bean, method));
    }
    return initBinderMethods;
  }

  private InvocableHandlerMethod createInitBinderMethod(Object bean, Method method) {
    return new InvocableHandlerMethod(bean, method, resolvableParameterFactory);
  }

}
