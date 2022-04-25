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
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.ReflectionUtils.MethodFilter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.InitBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.bind.support.DefaultDataBinderFactory;
import cn.taketoday.web.bind.support.DefaultSessionAttributeStore;
import cn.taketoday.web.bind.support.SessionAttributeStore;
import cn.taketoday.web.bind.support.WebBindingInitializer;
import cn.taketoday.web.bind.support.WebDataBinderFactory;
import cn.taketoday.web.context.async.AsyncWebRequest;
import cn.taketoday.web.context.async.CallableProcessingInterceptor;
import cn.taketoday.web.context.async.DeferredResultProcessingInterceptor;
import cn.taketoday.web.context.async.WebAsyncManager;
import cn.taketoday.web.context.async.WebAsyncTask;
import cn.taketoday.web.context.async.WebAsyncUtils;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.method.support.ModelAndViewContainer;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.session.WebSession;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
import cn.taketoday.web.view.View;

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
          !AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)
                  && AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class);

  private ParameterResolvingRegistry resolvingRegistry;

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

  private boolean ignoreDefaultModelOnRedirect = false;

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

  public ParameterResolvingRegistry getResolvingRegistry() {
    return resolvingRegistry;
  }

  public void setResolvingRegistry(ParameterResolvingRegistry resolvingRegistry) {
    this.resolvingRegistry = resolvingRegistry;
  }

  /**
   * Provide handlers for custom return value types. Custom handlers are
   * ordered after built-in ones. To override the built-in support for
   * return value handling use {@link #setReturnValueHandlerManager}.
   */
  public void setCustomReturnValueHandlers(@Nullable List<ReturnValueHandler> returnValueHandlers) {
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
   * By default the content of the "default" model is used both during
   * rendering and redirect scenarios. Alternatively a controller method
   * can declare a {@link cn.taketoday.web.view.RedirectModel} argument and use it to provide
   * attributes for a redirect.
   * <p>Setting this flag to {@code true} guarantees the "default" model is
   * never used in a redirect scenario even if a RedirectAttributes argument
   * is not declared. Setting it to {@code false} means the "default" model
   * may be used in a redirect if the controller method doesn't declare a
   * RedirectAttributes argument.
   * <p>The default setting is {@code false} but new applications should
   * consider setting it to {@code true}.
   *
   * @see RedirectModel
   */
  public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
    this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
  }

  /**
   * Specify the strategy to store session attributes with. The default is
   * {@link cn.taketoday.web.bind.support.DefaultSessionAttributeStore},
   * storing session attributes in the HttpSession with the same attribute
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

  @Override
  public void afterPropertiesSet() {
    // Do this first, it may add ResponseBody advice beans
    initControllerAdviceCache();
    if (resolvingRegistry == null) {
      this.resolvingRegistry = new ParameterResolvingRegistry();
      resolvingRegistry.setApplicationContext(getApplicationContext());
      resolvingRegistry.registerDefaultStrategies();
    }
    // prepare returnValueHandlerManager
    setReturnValueHandlerManager(returnValueHandlerManager);
  }

  private void initControllerAdviceCache() {
    if (getApplicationContext() == null) {
      return;
    }

    List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
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
  protected ModelAndView handleInternal(RequestContext request, HandlerMethod handlerMethod) throws Throwable {

    ModelAndView mav;
    checkRequest(request);

    // Execute invokeHandlerMethod in synchronized block if required.
    if (this.synchronizeOnSession) {
      WebSession session = RequestContextUtils.getSession(request, false);
      if (session != null) {
        Object mutex = WebUtils.getSessionMutex(session);
        synchronized(mutex) {
          mav = invokeHandlerMethod(request, handlerMethod);
        }
      }
      else {
        // No Session available -> no mutex necessary
        mav = invokeHandlerMethod(request, handlerMethod);
      }
    }
    else {
      // No synchronization on session demanded at all...
      mav = invokeHandlerMethod(request, handlerMethod);
    }

    HttpHeaders headers = request.getHeaders();
    if (!headers.containsKey(HEADER_CACHE_CONTROL)) {
      if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
        applyCacheSeconds(request, this.cacheSecondsForSessionAttributeHandlers);
      }
      else {
        prepareResponse(request);
      }
    }

    return mav;
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
   * @see #createInvocableHandlerMethod(RequestContext, HandlerMethod)
   */
  @Nullable
  protected ModelAndView invokeHandlerMethod(
          RequestContext request, HandlerMethod handlerMethod) throws Throwable {

    WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
    ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);

    ActionMappingAnnotationHandler handler = annotationHandlerMap.get(handlerMethod);

    ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(request, handlerMethod);

    if (this.returnValueHandlerManager != null) {
      invocableMethod.setReturnValueHandlerManager(this.returnValueHandlerManager);
    }

    ModelAndViewContainer mavContainer = new ModelAndViewContainer();
    mavContainer.setWebDataBinderFactory(binderFactory);
    mavContainer.addAllAttributes(RequestContextUtils.getInputRedirectModel(request));
    modelFactory.initModel(request, mavContainer, invocableMethod);
    mavContainer.setIgnoreDefaultModelOnRedirect(ignoreDefaultModelOnRedirect);

    AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request);
    asyncWebRequest.setTimeout(asyncRequestTimeout);

    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
    asyncManager.setTaskExecutor(taskExecutor);
    asyncManager.setAsyncRequest(asyncWebRequest);
    asyncManager.registerCallableInterceptors(callableInterceptors);
    asyncManager.registerDeferredResultInterceptors(deferredResultInterceptors);

    if (asyncManager.hasConcurrentResult()) {
      Object result = asyncManager.getConcurrentResult();
      mavContainer = asyncManager.getModelContainer();
      asyncManager.clearConcurrentResult();
      LogFormatUtils.traceDebug(log, traceOn -> {
        String formatted = LogFormatUtils.formatValue(result, !traceOn);
        return "Resume with async result [" + formatted + "]";
      });
      invocableMethod = invocableMethod.wrapConcurrentResult(result);
    }

    invocableMethod.invokeAndHandle(request, mavContainer);

    if (asyncManager.isConcurrentHandlingStarted()) {
      return null;
    }

    return getModelAndView(mavContainer, modelFactory, request);
  }

  /**
   * Create a {@link ServletInvocableHandlerMethod} from the given {@link HandlerMethod} definition.
   *
   * @param request current HTTP request
   * @param handlerMethod the {@link HandlerMethod} definition
   * @return the corresponding {@link ServletInvocableHandlerMethod} (or custom subclass thereof)
   */
  protected ServletInvocableHandlerMethod createInvocableHandlerMethod(
          RequestContext request, HandlerMethod handlerMethod) {
    return new ServletInvocableHandlerMethod(handlerMethod);
  }

  private ModelFactory getModelFactory(HandlerMethod handlerMethod, WebDataBinderFactory binderFactory) {
    SessionAttributesHandler sessionAttrHandler = getSessionAttributesHandler(handlerMethod);
    Class<?> handlerType = handlerMethod.getBeanType();
    Set<Method> methods = this.modelAttributeCache.get(handlerType);
    if (methods == null) {
      methods = MethodIntrospector.filterMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
      this.modelAttributeCache.put(handlerType, methods);
    }
    ArrayList<InvocableHandlerMethod> attrMethods = new ArrayList<>();
    // Global methods first
    for (Map.Entry<ControllerAdviceBean, Set<Method>> entry : modelAttributeAdviceCache.entrySet()) {
      Set<Method> methodSet = entry.getValue();
      ControllerAdviceBean controllerAdviceBean = entry.getKey();
      if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
        Object bean = controllerAdviceBean.resolveBean();
        for (Method method : methodSet) {
          attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
        }
      }
    }

    for (Method method : methods) {
      Object bean = handlerMethod.getBean();
      attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
    }
    return new ModelFactory(attrMethods, binderFactory, sessionAttrHandler);
  }

  private InvocableHandlerMethod createModelAttributeMethod(WebDataBinderFactory factory, Object bean, Method method) {
    InvocableHandlerMethod attrMethod = new InvocableHandlerMethod(bean, method);
    attrMethod.setResolvingRegistry(resolvingRegistry);
    attrMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
    attrMethod.setDataBinderFactory(factory);
    return attrMethod;
  }

  private WebDataBinderFactory getDataBinderFactory(HandlerMethod handlerMethod) throws Exception {
    Class<?> handlerType = handlerMethod.getBeanType();
    Set<Method> methods = initBinderCache.get(handlerType);
    if (methods == null) {
      methods = MethodIntrospector.filterMethods(handlerType, INIT_BINDER_METHODS);
      initBinderCache.put(handlerType, methods);
    }

    var initBinderMethods = new ArrayList<InvocableHandlerMethod>();
    // Global methods first
    for (Map.Entry<ControllerAdviceBean, Set<Method>> entry : initBinderAdviceCache.entrySet()) {
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
    return createDataBinderFactory(initBinderMethods);
  }

  private InvocableHandlerMethod createInitBinderMethod(Object bean, Method method) {
    InvocableHandlerMethod binderMethod = new InvocableHandlerMethod(bean, method);
    binderMethod.setResolvingRegistry(resolvingRegistry);
    binderMethod.setDataBinderFactory(new DefaultDataBinderFactory(this.webBindingInitializer));
    binderMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
    return binderMethod;
  }

  /**
   * Template method to create a new InitBinderDataBinderFactory instance.
   * <p>The default implementation creates a ServletRequestDataBinderFactory.
   * This can be overridden for custom ServletRequestDataBinder subclasses.
   *
   * @param binderMethods {@code @InitBinder} methods
   * @return the InitBinderDataBinderFactory instance to use
   * @throws Exception in case of invalid state or arguments
   */
  protected InitBinderDataBinderFactory createDataBinderFactory(List<InvocableHandlerMethod> binderMethods)
          throws Exception {

    return new ServletRequestDataBinderFactory(binderMethods, getWebBindingInitializer());
  }

  @Nullable
  private ModelAndView getModelAndView(ModelAndViewContainer mavContainer,
          ModelFactory modelFactory, RequestContext context) throws Throwable {

    modelFactory.updateModel(context, mavContainer);
    if (mavContainer.isRequestHandled()) {
      return null;
    }

    Model model = mavContainer.getModel();

    ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model.asMap(), mavContainer.getStatus());
    if (!mavContainer.isViewReference()) {
      mav.setView((View) mavContainer.getView());
    }

    RedirectModel redirectModel = mavContainer.getRedirectModel();
    if (redirectModel != null) {
      Map<String, ?> flashAttributes = redirectModel.asMap();
      RedirectModel outputRedirectModel = RequestContextUtils.getOutputRedirectModel(context);
      outputRedirectModel.addAllAttributes(flashAttributes);
    }
    return mav;
  }

}
