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
import java.util.Map.Entry;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.Prototypes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.loader.BeanDefinitionReader;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RootController;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.HandlerMethodBuilder;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.handler.PathVariableMethodParameter;
import cn.taketoday.web.http.HttpMethod;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * Store {@link HandlerMethod}
 *
 * @author TODAY <br>
 * 2018-07-1 20:47:06
 */
public class HandlerMethodRegistry
        extends AbstractUrlHandlerRegistry implements HandlerRegistry, WebApplicationInitializer {

  private ConfigurableBeanFactory beanFactory;

  /** @since 3.0 */
  private HandlerMethodBuilder<HandlerMethod> handlerBuilder;

  // @since 4.0
  private BeanDefinitionReader definitionReader;

  private BeanDefinitionRegistry registry;

  public HandlerMethodRegistry() {
    setOrder(HIGHEST_PRECEDENCE);
  }

  // MappedHandlerRegistry
  // --------------------------

  @Override
  protected String computeKey(RequestContext context) {
    return context.getMethod().concat(context.getRequestPath());
  }

  /**
   * Initialize All Action or Handler
   */
  @Override
  public void onStartup(WebApplicationContext context) {
    log.info("Initializing Annotation Controllers");
    this.registry = context.unwrapFactory(BeanDefinitionRegistry.class);
    startConfiguration();
    CollectionUtils.trimToSize(patternHandlers); // @since 4.0 trimToSize
  }

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    setBeanFactory(context.unwrapFactory(ConfigurableBeanFactory.class));
    setHandlerBuilder(new HandlerMethodBuilder<>(context));
    super.initApplicationContext(context);
  }

  /**
   * Start config
   */
  public void startConfiguration() {
    ApplicationContext beanFactory = obtainApplicationContext();
    // @since 2.3.3
    for (Entry<String, BeanDefinition> entry : beanFactory.getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();

      // ActionMapping on the class is ok
      MergedAnnotation<ActionMapping> actionMapping = beanFactory.getMergedAnnotationOnBean(def.getName(), ActionMapping.class);
      MergedAnnotation<RootController> rootController = beanFactory.getMergedAnnotationOnBean(def.getName(), RootController.class);
      MergedAnnotation<ActionMapping> controllerMapping = null;
      if (actionMapping.isPresent()) {
        controllerMapping = actionMapping;
      }
      // build
      if (rootController.isPresent() || actionMapping.isPresent()) {
        if (def.hasBeanClass()) {
          buildHandlerMethod(def.getBeanClass(), controllerMapping);
        }
        else {
          Class<?> type = beanFactory.getType(def.getName());
          buildHandlerMethod(type, controllerMapping);
        }
      }
    }
  }

  private void buildHandlerMethod(
          Class<?> beanClass, @Nullable MergedAnnotation<ActionMapping> controllerMapping) {
    for (Method method : ReflectionUtils.getDeclaredMethods(beanClass)) {
      buildHandlerMethod(method, beanClass, controllerMapping);
    }
  }

  /**
   * Set Action Mapping
   *
   * @param beanClass Controller
   * @param method Action or Handler
   * @param controllerMapping find mapping on class
   */
  protected void buildHandlerMethod(
          Method method, Class<?> beanClass,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping) {
    MergedAnnotation<ActionMapping> annotation = MergedAnnotations.from(method).get(ActionMapping.class);
    if (annotation.isPresent()) {
      // build HandlerMethod
      HandlerMethod handler = createHandlerMethod(beanClass, method);
      // do mapping url
      mappingHandlerMethod(handler, controllerMapping, annotation);
    }
  }

  /**
   * Mapping given HandlerMapping to {@link HandlerMethodRegistry}
   *
   * @param handler current {@link HandlerMethod}
   * methods on class
   */
  protected void mappingHandlerMethod(
          HandlerMethod handler,
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
      Collections.addAll(classRequestMethods, controllerMapping.getEnum("method", HttpMethod.class));
      emptyNamespaces = namespaces.isEmpty();
      addClassRequestMethods = !classRequestMethods.isEmpty();
    }

    boolean exclude = handlerMethodMapping.getBoolean("exclude"); // exclude name space on class ?
    Set<HttpMethod> requestMethods = // http request method on method(action/handler)
            CollectionUtils.newHashSet(handlerMethodMapping.getEnum("method", HttpMethod.class));

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
   * @param handlerMethod {@link HandlerMethod}
   * @param path Request path
   * @param requestMethod HTTP request method
   * @see HttpMethod
   */
  private void mappingHandlerMethod(String path, HttpMethod requestMethod, HandlerMethod handlerMethod) {
    // GET/blog/users/1 GET/blog/#{key}/1
    String pathPattern = getRequestPathPattern(path);
    HandlerMethod transformed = transformHandlerMethod(pathPattern, handlerMethod);
    super.registerHandler(requestMethod.name().concat(pathPattern), transformed);
  }

  protected final String getRequestPathPattern(String path) {
    String contextPath = getContextPath();
    if (StringUtils.isNotEmpty(contextPath)) {
      path = contextPath.concat(path);
    }
    path = resolveVariables(path);
    return sanitizedPath(path);
  }

  /**
   * Sanitize the given path. Uses the following rules:
   * <ul>
   * <li>replace all "//" by "/"</li>
   * </ul>
   */
  static String sanitizedPath(String path) {
    int index = path.indexOf("//");
    if (index >= 0) {
      StringBuilder sanitized = new StringBuilder(path);
      while (index != -1) {
        sanitized.deleteCharAt(index);
        index = sanitized.indexOf("//", index);
      }
      return sanitized.toString();
    }
    return path;
  }

  /**
   * Transform {@link HandlerMethod} if path contains {@link PathVariable}
   *
   * @param pathPattern path pattern
   * @param handler Target {@link HandlerMethod}
   * @return Transformed {@link HandlerMethod}
   */
  protected HandlerMethod transformHandlerMethod(String pathPattern, HandlerMethod handler) {
    if (containsPathVariable(pathPattern)) {
      HandlerMethod transformed = new HandlerMethod(handler);
      mappingPathVariable(pathPattern, transformed);
      return transformed;
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
  protected void mappingPathVariable(String pathPattern, HandlerMethod handler) {
    HashMap<String, MethodParameter> parameterMapping = new HashMap<>();
    MethodParameter[] methodParameters = handler.getParameters();
    for (MethodParameter methodParameter : methodParameters) {
      parameterMapping.put(methodParameter.getName(), methodParameter);
    }

    int i = 0;
    PathMatcher pathMatcher = getPathMatcher();
    for (String variable : pathMatcher.extractVariableNames(pathPattern)) {
      MethodParameter parameter = parameterMapping.get(variable);
      if (parameter == null) {
        throw new ConfigurationException(
                "There isn't a variable named: [" + variable +
                        "] in the parameter list at method: [" + handler.getMethod() + "]");
      }
      methodParameters[parameter.getParameterIndex()] = //
              new PathVariableMethodParameter(i++, pathPattern, handler, parameter, pathMatcher);
    }
  }

  /**
   * Create {@link HandlerMethod}.
   *
   * @param beanClass Controller class
   * @param method Action or Handler
   * @return A new {@link HandlerMethod}
   */
  protected HandlerMethod createHandlerMethod(Class<?> beanClass, Method method) {
    Object handlerBean = createHandler(beanClass, this.beanFactory);
    if (handlerBean == null) {
      throw new ConfigurationException(
              "An unexpected exception occurred: [Can't get bean with given type: [" + beanClass.getName() + "]]");
    }
    List<HandlerInterceptor> interceptors = getInterceptors(beanClass, method);
    return handlerBuilder.build(handlerBean, method, interceptors);
  }

  /**
   * Create a handler bean instance
   *
   * @param beanClass Target bean class
   * @param beanFactory {@link ConfigurableBeanFactory}
   * @return Returns a handler bean of target beanClass
   */
  protected Object createHandler(Class<?> beanClass, ConfigurableBeanFactory beanFactory) {
    BeanDefinition def = registry.getBeanDefinition(beanClass);
    return def.isSingleton()
           ? beanFactory.getBean(def)
           : Prototypes.newProxyInstance(beanClass, def, beanFactory);
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

  public void setHandlerBuilder(HandlerMethodBuilder<HandlerMethod> handlerBuilder) {
    this.handlerBuilder = handlerBuilder;
  }

  public HandlerMethodBuilder<HandlerMethod> getHandlerBuilder() {
    return handlerBuilder;
  }

  protected final BeanDefinitionReader definitionReader() {
    if (definitionReader == null) {
      definitionReader = new BeanDefinitionReader(obtainApplicationContext());
      definitionReader.setEnableConditionEvaluation(false);
    }
    return definitionReader;
  }
}
