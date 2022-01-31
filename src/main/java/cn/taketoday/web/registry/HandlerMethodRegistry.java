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
package cn.taketoday.web.registry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.loader.AnnotatedBeanDefinitionReader;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.AnnotationHandlerFactory;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.PathVariableMethodParameter;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.util.UrlPathHelper;

/**
 * Store {@link HandlerMethod}
 *
 * @author TODAY <br>
 * 2018-07-1 20:47:06
 */
public class HandlerMethodRegistry
        extends MappedHandlerRegistry implements HandlerRegistry, WebApplicationInitializer {

  private ConfigurableBeanFactory beanFactory;

  /** @since 3.0 */
  private AnnotationHandlerFactory<ActionMappingAnnotationHandler> annotationHandlerFactory;

  // @since 4.0
  private AnnotatedBeanDefinitionReader definitionReader;

  private BeanDefinitionRegistry registry;

  private boolean detectHandlerMethodsInAncestorContexts = false;

  public HandlerMethodRegistry() {
    setOrder(HIGHEST_PRECEDENCE);
  }

  // MappedHandlerRegistry
  // --------------------------

  @Override
  protected String computeKey(RequestContext context) {
    return context.getMethodValue().concat(context.getRequestPath());
  }

  /**
   * Initialize All Action or Handler
   */
  @Override
  public void onStartup(WebApplicationContext context) {
    log.info("Initializing Annotation Controllers");
    this.registry = context.unwrapFactory(BeanDefinitionRegistry.class);
    initActions();
    CollectionUtils.trimToSize(patternHandlers); // @since 4.0 trimToSize
  }

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    setBeanFactory(context.unwrapFactory(ConfigurableBeanFactory.class));
    setAnnotationHandlerFactory(new AnnotationHandlerFactory<>(context));
    super.initApplicationContext(context);
  }

  /**
   * Scan beans in the ApplicationContext, detect and register ActionMappings.
   *
   * @see #getCandidateBeanNames()
   */
  protected void initActions() {
    for (String beanName : getCandidateBeanNames()) {
      // ActionMapping on the class is ok
      MergedAnnotation<Controller> rootController = beanFactory.findAnnotationOnBean(beanName, Controller.class);
      MergedAnnotation<ActionMapping> actionMapping = beanFactory.findAnnotationOnBean(beanName, ActionMapping.class);
      MergedAnnotation<ActionMapping> controllerMapping = null;
      if (actionMapping.isPresent()) {
        controllerMapping = actionMapping;
      }
      // build
      if (rootController.isPresent() || actionMapping.isPresent()) {
        Class<?> type = beanFactory.getType(beanName);
        buildHandlerMethod(beanName, type, controllerMapping);
      }
    }
  }

  /**
   * Determine the names of candidate beans in the application context.
   *
   * @see #setDetectHandlerMethodsInAncestorContexts
   * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors
   * @since 4.0
   */
  protected Set<String> getCandidateBeanNames() {
    return this.detectHandlerMethodsInAncestorContexts
           ? BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class)
           : obtainApplicationContext().getBeanNamesForType(Object.class);
  }

  /**
   * Start config
   */
  public void startConfiguration() {
    // @since 2.3.3
    initActions();
  }

  private void buildHandlerMethod(
          String beanName, Class<?> beanClass,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping) {

    ReflectionUtils.doWithMethods(beanClass, method -> {
      buildHandlerMethod(beanName, method, beanClass, controllerMapping);
    });
  }

  /**
   * Set Action Mapping
   *
   * @param beanName bean name
   * @param method Action or Handler
   * @param beanClass Controller
   * @param controllerMapping find mapping on class
   */
  protected void buildHandlerMethod(
          String beanName, Method method, Class<?> beanClass,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping) {
    MergedAnnotation<ActionMapping> annotation = MergedAnnotations.from(method).get(ActionMapping.class);
    if (annotation.isPresent()) {
      // build HandlerMethod
      ActionMappingAnnotationHandler handler = createHandler(beanName, beanClass, method);
      // do mapping url
      mappingHandlerMethod(handler, controllerMapping, annotation);
    }
  }

  /**
   * Create {@link ActionMappingAnnotationHandler}.
   *
   * @param beanName bean name
   * @param beanClass Controller class
   * @param method Action or Handler
   * @return A new {@link ActionMappingAnnotationHandler}
   */
  protected ActionMappingAnnotationHandler createHandler(String beanName, Class<?> beanClass, Method method) {
    BeanSupplier<Object> beanSupplier = BeanSupplier.from(beanFactory, beanName);
    List<HandlerInterceptor> interceptors = getInterceptors(beanClass, method);
    return annotationHandlerFactory.create(beanSupplier, method, interceptors);
  }

  /**
   * Mapping given HandlerMapping to {@link HandlerMethodRegistry}
   *
   * @param handler current {@link ActionMappingAnnotationHandler}
   * methods on class
   */
  protected void mappingHandlerMethod(
          ActionMappingAnnotationHandler handler,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping,
          MergedAnnotation<ActionMapping> handlerMethodMapping) {
    boolean emptyNamespaces = true;
    boolean addClassRequestMethods = false;
    Set<String> namespaces = Collections.emptySet();
    Set<HttpMethod> classRequestMethods = Collections.emptySet();
    if (controllerMapping != null) {
      namespaces = new LinkedHashSet<>(4, 1.0f); // name space
      classRequestMethods = new LinkedHashSet<>(8, 1.0f); // method
      for (String value : controllerMapping.getStringArray(MergedAnnotation.VALUE)) {
        namespaces.add(StringUtils.formatURL(value));
      }
      Collections.addAll(classRequestMethods, controllerMapping.getEnumArray("method", HttpMethod.class));
      emptyNamespaces = namespaces.isEmpty();
      addClassRequestMethods = !classRequestMethods.isEmpty();
    }

    boolean exclude = handlerMethodMapping.getBoolean("exclude"); // exclude name space on class ?
    Set<HttpMethod> requestMethods = // http request method on method(action/handler)
            CollectionUtils.newHashSet(handlerMethodMapping.getEnumArray("method", HttpMethod.class));

    if (addClassRequestMethods)
      requestMethods.addAll(classRequestMethods);

    for (String urlOnMethod : handlerMethodMapping.getStringArray("value")) { // url on method
      String checkedUrl = StringUtils.formatURL(urlOnMethod);
      // splice urls and request methods
      // ---------------------------------
      for (HttpMethod requestMethod : requestMethods) {
        if (exclude || emptyNamespaces) {
          mappingHandlerMethod(checkedUrl, requestMethod, handler);
        }
        else {
          for (String namespace : namespaces) {
            mappingHandlerMethod(namespace.concat(checkedUrl), requestMethod, handler);
          }
        }
      }
    }
  }

  /**
   * Mapping to {@link HandlerMethodRegistry}
   *
   * @param handler {@link ActionMappingAnnotationHandler}
   * @param path Request path
   * @param requestMethod HTTP request method
   * @see HttpMethod
   */
  private void mappingHandlerMethod(String path, HttpMethod requestMethod, ActionMappingAnnotationHandler handler) {
    // GET/blog/users/1 GET/blog/#{key}/1
    String pathPattern = getRequestPathPattern(path);
    ActionMappingAnnotationHandler transformed = transformHandler(pathPattern, handler);
    super.registerHandler(requestMethod.name().concat(pathPattern), transformed);
  }

  protected final String getRequestPathPattern(String path) {
    String contextPath = getContextPath();
    if (StringUtils.isNotEmpty(contextPath)) {
      path = contextPath.concat(path);
    }
    path = resolveVariables(path);
    return UrlPathHelper.getSanitizedPath(path);
  }

  /**
   * Transform {@link ActionMappingAnnotationHandler} if path contains {@link PathVariable}
   *
   * @param pathPattern path pattern
   * @param handler Target {@link ActionMappingAnnotationHandler}
   * @return Transformed {@link ActionMappingAnnotationHandler}
   */
  protected ActionMappingAnnotationHandler transformHandler(
          String pathPattern, ActionMappingAnnotationHandler handler) {
    if (containsPathVariable(pathPattern)) {
      mappingPathVariable(pathPattern, handler);
      return handler;
    }
    return handler;
  }

  /**
   * contains {@link PathVariable} char: '{' and '}'
   *
   * @param path handler key
   * @return If contains '{' and '}'
   */
  public static boolean containsPathVariable(String path) {
    return path.indexOf('{') > -1 && path.indexOf('}') > -1;
  }

  /**
   * Mapping path variable.
   */
  protected void mappingPathVariable(String pathPattern, ActionMappingAnnotationHandler handler) {
    HashMap<String, ResolvableMethodParameter> parameterMapping = new HashMap<>();
    ResolvableMethodParameter[] resolvableParameters = handler.getResolvableParameters();
    for (ResolvableMethodParameter methodParameter : resolvableParameters) {
      parameterMapping.put(methodParameter.getName(), methodParameter);
    }

    int i = 0;
    PathMatcher pathMatcher = getPathMatcher();
    for (String variable : pathMatcher.extractVariableNames(pathPattern)) {
      ResolvableMethodParameter parameter = parameterMapping.get(variable);
      if (parameter == null) {
        throw new ConfigurationException(
                "There isn't a variable named: [" + variable +
                        "] in the parameter list at method: [" + handler.getMethod() + "]");
      }
      resolvableParameters[parameter.getParameterIndex()] =
              new PathVariableMethodParameter(i++, pathPattern, parameter, pathMatcher);
    }
  }

  /**
   * Get list of intercepters.
   *
   * @param controllerClass controller class
   * @param action method
   * @return List of {@link HandlerInterceptor} objects
   */
  protected List<HandlerInterceptor> getInterceptors(Class<?> controllerClass, Method action) {
    ArrayList<HandlerInterceptor> ret = new ArrayList<>();
    Set<Interceptor> controllerInterceptors = AnnotatedElementUtils.getAllMergedAnnotations(controllerClass, Interceptor.class);
    // get interceptor on class
    if (CollectionUtils.isNotEmpty(controllerInterceptors)) {
      for (Interceptor controllerInterceptor : controllerInterceptors) {
        Collections.addAll(ret, getInterceptors(controllerInterceptor.value()));
      }
    }
    // HandlerInterceptor on a method
    Set<Interceptor> actionInterceptors = AnnotatedElementUtils.getAllMergedAnnotations(action, Interceptor.class);
    if (CollectionUtils.isNotEmpty(actionInterceptors)) {
      ApplicationContext beanFactory = obtainApplicationContext();
      for (Interceptor actionInterceptor : actionInterceptors) {
        Collections.addAll(ret, getInterceptors(actionInterceptor.value()));
        // exclude interceptors
        for (Class<? extends HandlerInterceptor> interceptor : actionInterceptor.exclude()) {
          ret.remove(beanFactory.getBean(interceptor));
        }
      }
    }
    return ret;
  }

  /***
   * Get {@link HandlerInterceptor} objects
   *
   * @param interceptors
   *            {@link HandlerInterceptor} class
   * @return Array of {@link HandlerInterceptor} objects
   */
  public HandlerInterceptor[] getInterceptors(Class<? extends HandlerInterceptor>[] interceptors) {
    if (ObjectUtils.isEmpty(interceptors)) {
      return HandlerInterceptor.EMPTY_ARRAY;
    }
    int i = 0;
    HandlerInterceptor[] ret = new HandlerInterceptor[interceptors.length];
    for (Class<? extends HandlerInterceptor> interceptor : interceptors) {
      if (!registry.containsBeanDefinition(interceptor, true)) {
        try {
          definitionReader().registerBean(interceptor);
        }
        catch (BeanDefinitionStoreException e) {
          throw new ConfigurationException("Interceptor: [" + interceptor.getName() + "] register error", e);
        }
      }
      HandlerInterceptor instance = this.beanFactory.getBean(interceptor);
      Assert.state(instance != null, "Can't get target interceptor bean");
      ret[i++] = instance;
    }
    return ret;
  }

  /**
   * Rebuild Controllers
   */
  public void rebuildControllers() {
    log.info("Rebuilding Controllers");
    clearHandlers();
    startConfiguration();
  }

  public void setBeanFactory(ConfigurableBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "ConfigurableBeanFactory cannot be null");
    this.beanFactory = beanFactory;
  }

  public ConfigurableBeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setAnnotationHandlerFactory(AnnotationHandlerFactory<ActionMappingAnnotationHandler> annotationHandlerFactory) {
    this.annotationHandlerFactory = annotationHandlerFactory;
  }

  public AnnotationHandlerFactory<ActionMappingAnnotationHandler> getAnnotationHandlerFactory() {
    return annotationHandlerFactory;
  }

  protected final AnnotatedBeanDefinitionReader definitionReader() {
    if (definitionReader == null) {
      definitionReader = new AnnotatedBeanDefinitionReader(obtainApplicationContext());
      definitionReader.setEnableConditionEvaluation(false);
    }
    return definitionReader;
  }

  /**
   * Whether to detect handler methods in beans in ancestor ApplicationContexts.
   * <p>Default is "false": Only beans in the current ApplicationContext are
   * considered, i.e. only in the context that this HandlerMapping itself
   * is defined in (typically the current DispatcherServlet's context).
   * <p>Switch this flag on to detect handler beans in ancestor contexts
   * (typically the Spring root WebApplicationContext) as well.
   *
   * @see #getCandidateBeanNames()
   * @since 4.0
   */
  public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
    this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
  }
}
