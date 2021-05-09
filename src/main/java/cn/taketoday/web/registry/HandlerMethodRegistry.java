/**
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

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.factory.Prototypes;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestMethod;
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
import cn.taketoday.web.interceptor.HandlerInterceptor;

import static cn.taketoday.context.utils.CollectionUtils.newHashSet;
import static cn.taketoday.context.utils.StringUtils.checkUrl;

/**
 * Store {@link HandlerMethod}
 *
 * @author TODAY <br>
 * 2018-07-1 20:47:06
 */
@MissingBean
public class HandlerMethodRegistry
        extends AbstractUrlHandlerRegistry implements HandlerRegistry, WebApplicationInitializer {

  private ConfigurableBeanFactory beanFactory;
  private BeanDefinitionLoader beanDefinitionLoader;

  /** @since 3.0 */
  private HandlerMethodBuilder<HandlerMethod> handlerBuilder;

  public HandlerMethodRegistry() {
    setOrder(HIGHEST_PRECEDENCE);
  }

  // MappedHandlerRegistry
  // --------------------------

  @Override
  protected String computeKey(final RequestContext context) {
    return context.getMethod().concat(context.getRequestPath());
  }

  /**
   * Initialize All Action or Handler
   */
  @Override
  public void onStartup(WebApplicationContext context) {
    log.info("Initializing Annotation Controllers");
    startConfiguration();
  }

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    setBeanFactory(context.getBean(ConfigurableBeanFactory.class));

    final Environment environment = context.getEnvironment();
    BeanDefinitionLoader beanDefinitionLoader = environment.getBeanDefinitionLoader();
    if (beanDefinitionLoader == null) {
      final BeanFactory beanFactory = context.getBeanFactory();
      if (beanFactory instanceof BeanDefinitionLoader) {
        beanDefinitionLoader = (BeanDefinitionLoader) beanFactory;
      }
      else {
        throw new IllegalStateException("No BeanDefinitionLoader");
      }
    }
    setBeanDefinitionLoader(beanDefinitionLoader);

    setHandlerBuilder(new HandlerMethodBuilder<>(context));
    super.initApplicationContext(context);
  }

  /**
   * Start config
   */
  public void startConfiguration() {
    final ApplicationContext beanFactory = obtainApplicationContext();
    // @since 2.3.3
    for (final Entry<String, BeanDefinition> entry : beanFactory.getBeanDefinitions().entrySet()) {
      final BeanDefinition def = entry.getValue();
      if (!def.isAbstract() && isController(def)) { // ActionMapping on the class is ok
        buildHandlerMethod(def.getBeanClass());
      }
    }
  }

  /**
   * Whether the given type is a handler with handler methods.
   *
   * @param def
   *         the definition of the bean being checked
   *
   * @return "true" if this a handler type, "false" otherwise.
   */
  protected boolean isController(final BeanDefinition def) {
    return def.isAnnotationPresent(RootController.class)
            || def.isAnnotationPresent(ActionMapping.class);
  }

  /**
   * Build {@link HandlerMethod}
   *
   * @param beanClass
   *         Bean class
   *
   * @since 2.3.7
   */
  public void buildHandlerMethod(final Class<?> beanClass) {
    // find mapping on class
    final AnnotationAttributes controllerMapping
            = ClassUtils.getAnnotationAttributes(ActionMapping.class, beanClass);
    for (final Method method : ReflectionUtils.getDeclaredMethods(beanClass)) {
      buildHandlerMethod(method, beanClass, controllerMapping);
    }
  }

  /**
   * Set Action Mapping
   *
   * @param beanClass
   *         Controller
   * @param method
   *         Action or Handler
   * @param controllerMapping
   *         find mapping on class
   */
  protected void buildHandlerMethod(final Method method,
                                    final Class<?> beanClass,
                                    final AnnotationAttributes controllerMapping) {

    final AnnotationAttributes[] actionMapping = // find mapping on method
            ClassUtils.getAnnotationAttributesArray(method, ActionMapping.class);

    if (ObjectUtils.isNotEmpty(actionMapping)) {
      // build HandlerMethod
      final HandlerMethod handler = createHandlerMethod(beanClass, method);
      // do mapping url
      mappingHandlerMethod(handler, controllerMapping, actionMapping);
    }
  }

  /**
   * Mapping given HandlerMapping to {@link HandlerMethodRegistry}
   *
   * @param handler
   *         current {@link HandlerMethod}
   *         methods on class
   * @param annotationAttributes
   *         {@link ActionMapping} Attributes, never be null
   */
  protected void mappingHandlerMethod(final HandlerMethod handler,
                                      final AnnotationAttributes controllerMapping,
                                      final AnnotationAttributes[] annotationAttributes) {
    boolean emptyNamespaces = true;
    boolean addClassRequestMethods = false;
    Set<String> namespaces = Collections.emptySet();
    Set<RequestMethod> classRequestMethods = Collections.emptySet();
    if (ObjectUtils.isNotEmpty(controllerMapping)) {
      namespaces = new LinkedHashSet<>(4, 1.0f); // name space
      classRequestMethods = new LinkedHashSet<>(8, 1.0f); // method
      for (final String value : controllerMapping.getStringArray(Constant.VALUE)) {
        namespaces.add(checkUrl(value));
      }
      Collections.addAll(classRequestMethods, controllerMapping.getAttribute("method", RequestMethod[].class));
      emptyNamespaces = namespaces.isEmpty();
      addClassRequestMethods = !classRequestMethods.isEmpty();
    }

    for (final AnnotationAttributes handlerMethodMapping : annotationAttributes) {
      final boolean exclude = handlerMethodMapping.getBoolean("exclude"); // exclude name space on class ?
      final Set<RequestMethod> requestMethods = // http request method on method(action/handler)
              newHashSet(handlerMethodMapping.getAttribute("method", RequestMethod[].class));

      if (addClassRequestMethods) requestMethods.addAll(classRequestMethods);

      for (final String urlOnMethod : handlerMethodMapping.getStringArray("value")) { // url on method
        final String checkedUrl = checkUrl(urlOnMethod);
        // splice urls and request methods
        // ---------------------------------
        for (final RequestMethod requestMethod : requestMethods) {
          if (exclude || emptyNamespaces) {
            mappingHandlerMethod(checkedUrl, requestMethod, handler);
          }
          else {
            for (final String namespace : namespaces) {
              mappingHandlerMethod(namespace.concat(checkedUrl), requestMethod, handler);
            }
          }
        }
      }
    }

  }

  /**
   * Mapping to {@link HandlerMethodRegistry}
   *
   * @param handlerMethod
   *         {@link HandlerMethod}
   * @param path
   *         Request path
   * @param requestMethod
   *         HTTP request method
   *
   * @see RequestMethod
   */
  private void mappingHandlerMethod(String path, RequestMethod requestMethod, HandlerMethod handlerMethod) {
    // GET/blog/users/1 GET/blog/#{key}/1
    final String pathPattern = getRequestPathPattern(path);
    super.registerHandler(requestMethod.name().concat(pathPattern),
                          transformHandlerMethod(pathPattern, handlerMethod));
  }

  protected final String getRequestPathPattern(String path) {
    final String contextPath = getContextPath();
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
  static String sanitizedPath(final String path) {
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
   * @param pathPattern
   *         path pattern
   * @param handler
   *         Target {@link HandlerMethod}
   *
   * @return Transformed {@link HandlerMethod}
   */
  protected HandlerMethod transformHandlerMethod(final String pathPattern, final HandlerMethod handler) {
    if (containsPathVariable(pathPattern)) {
      final HandlerMethod transformed = new HandlerMethod(handler);
      mappingPathVariable(pathPattern, transformed);
      return transformed;
    }
    return handler;
  }

  /**
   * contains {@link PathVariable} char: '{' and '}'
   *
   * @param path
   *         handler key
   *
   * @return If contains '{' and '}'
   */
  protected boolean containsPathVariable(final String path) {
    return path.indexOf('{') > -1 && path.indexOf('}') > -1;
  }

  /**
   * Mapping path variable.
   */
  protected void mappingPathVariable(final String pathPattern, final HandlerMethod handler) {
    final HashMap<String, MethodParameter> parameterMapping = new HashMap<>();
    final MethodParameter[] methodParameters = handler.getParameters();
    for (MethodParameter methodParameter : methodParameters) {
      parameterMapping.put(methodParameter.getName(), methodParameter);
    }

    int i = 0;
    final PathMatcher pathMatcher = getPathMatcher();
    for (final String variable : pathMatcher.extractVariableNames(pathPattern)) {
      final MethodParameter parameter = parameterMapping.get(variable);
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
   * @param beanClass
   *         Controller class
   * @param method
   *         Action or Handler
   *
   * @return A new {@link HandlerMethod}
   */
  protected HandlerMethod createHandlerMethod(final Class<?> beanClass, final Method method) {
    final Object handlerBean = createHandler(beanClass, this.beanFactory);
    if (handlerBean == null) {
      throw new ConfigurationException(
              "An unexpected exception occurred: [Can't get bean with given type: [" + beanClass.getName() + "]]");
    }
    final List<HandlerInterceptor> interceptors = getInterceptors(beanClass, method);
    return handlerBuilder.build(handlerBean, method, interceptors);
  }

  /**
   * Create a handler bean instance
   *
   * @param beanClass
   *         Target bean class
   * @param beanFactory
   *         {@link ConfigurableBeanFactory}
   *
   * @return Returns a handler bean of target beanClass
   */
  protected Object createHandler(final Class<?> beanClass, final ConfigurableBeanFactory beanFactory) {
    final BeanDefinition def = beanFactory.getBeanDefinition(beanClass);
    return def.isSingleton()
           ? beanFactory.getBean(def)
           : Prototypes.newProxyInstance(beanClass, def, beanFactory);
  }

  /**
   * Get list of intercepters.
   *
   * @param controllerClass
   *         controller class
   * @param action
   *         method
   *
   * @return List of {@link HandlerInterceptor} objects
   */
  protected List<HandlerInterceptor> getInterceptors(final Class<?> controllerClass, final Method action) {
    final ArrayList<HandlerInterceptor> ret = new ArrayList<>();

    // 设置类拦截器
    final Interceptor[] controllerInterceptors = ClassUtils.getAnnotationArray(controllerClass, Interceptor.class);
    if (controllerInterceptors != null) {
      for (final Interceptor controllerInterceptor : controllerInterceptors) {
        Collections.addAll(ret, getInterceptors(controllerInterceptor.value()));
      }
    }
    // HandlerInterceptor on a method
    final Interceptor[] actionInterceptors = ClassUtils.getAnnotationArray(action, Interceptor.class);
    if (actionInterceptors != null) {
      for (final Interceptor actionInterceptor : actionInterceptors) {
        Collections.addAll(ret, getInterceptors(actionInterceptor.value()));
        // exclude interceptors
        final ApplicationContext beanFactory = obtainApplicationContext();
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
      return Constant.EMPTY_HANDLER_INTERCEPTOR;
    }
    final ApplicationContext beanFactory = obtainApplicationContext();

    int i = 0;
    final HandlerInterceptor[] ret = new HandlerInterceptor[interceptors.length];
    for (Class<? extends HandlerInterceptor> interceptor : interceptors) {

      if (!beanFactory.containsBeanDefinition(interceptor, true)) {
        try {
          beanFactory.registerBean(beanDefinitionLoader.createBeanDefinition(interceptor));
        }
        catch (BeanDefinitionStoreException e) {
          throw new ConfigurationException("Interceptor: [" + interceptor.getName() + "] register error", e);
        }
      }
      final HandlerInterceptor instance = beanFactory.getBean(interceptor);
      Assert.state(instance != null, "Can't get target interceptor bean");
      ret[i++] = instance;
    }
    return ret;
  }

  /**
   * Rebuild Controllers
   */
  public void rebuiltControllers() {
    log.info("Rebuilding Controllers");
    clearHandlers();
    startConfiguration();
  }

  public void setBeanFactory(ConfigurableBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "ConfigurableBeanFactory cannot be null");
    this.beanFactory = beanFactory;
  }

  public void setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader) {
    this.beanDefinitionLoader = beanDefinitionLoader;
  }

  public ConfigurableBeanFactory getBeanFactory() {
    return beanFactory;
  }

  public BeanDefinitionLoader getBeanDefinitionLoader() {
    return beanDefinitionLoader;
  }

  public void setHandlerBuilder(HandlerMethodBuilder<HandlerMethod> handlerBuilder) {
    this.handlerBuilder = handlerBuilder;
  }

  public HandlerMethodBuilder<HandlerMethod> getHandlerBuilder() {
    return handlerBuilder;
  }
}
