/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.web.config;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.AbstractBeanFactory.Prototypes;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RootController;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.HandlerMethodRegistry;
import cn.taketoday.web.mapping.MethodParameter;

/**
 * @author TODAY <br>
 */
@Singleton(Constant.ACTION_CONFIG)
public class ActionConfiguration implements Ordered, WebApplicationInitializer {

    private static final Logger log = LoggerFactory.getLogger(ActionConfiguration.class);

    private final String contextPath;
    private Properties variables;

    @Autowired(Constant.HANDLER_METHOD_REGISTRY)
    public HandlerMethodRegistry handlerMethodRegistry;

    private final BeanDefinitionLoader beanDefinitionLoader;

    private Map<String, HandlerMethod> regexUrls = new HashMap<>();

    private final AbstractBeanFactory beanFactory;

    @Autowired
    public ActionConfiguration(WebApplicationContext applicationContext, AbstractBeanFactory beanFactory) {

        final String contextPath = applicationContext.getContextPath();
        this.contextPath = contextPath == null ? Constant.BLANK : contextPath;
        this.beanFactory = beanFactory;

        final Environment environment = applicationContext.getEnvironment();

        this.variables = environment.getProperties();
        this.beanDefinitionLoader = environment.getBeanDefinitionLoader();
    }

    /**
     * Start config
     * 
     * @throws Exception
     *             If any {@link Exception} occurred
     */
    protected void startConfiguration() throws Exception {

        // @since 2.3.3
        for (final Entry<String, BeanDefinition> entry : beanFactory.getBeanDefinitions().entrySet()) {

            final BeanDefinition def = entry.getValue();

            if (!def.isAbstract() && (def.isAnnotationPresent(RootController.class)
                                      || def.isAnnotationPresent(ActionMapping.class))) { // ActionMapping on the class is ok
                buildHandlerMapping(def.getBeanClass());
            }
        }
    }

    /**
     * Build {@link HandlerMethod}
     * 
     * @param beanClass
     *            Bean class
     * @throws Exception
     *             If any {@link Exception} occurred
     * @since 2.3.7
     */
    public void buildHandlerMapping(final Class<?> beanClass) throws Exception {

        final Set<String> namespaces = new HashSet<>(4, 1.0f); // name space
        final Set<RequestMethod> methodsOnClass = new HashSet<>(8, 1.0f); // method

        final AnnotationAttributes controllerMapping = // find mapping on class
                ClassUtils.getAnnotationAttributes(ActionMapping.class, beanClass);

        if (ObjectUtils.isNotEmpty(controllerMapping)) {
            for (final String value : controllerMapping.getStringArray(Constant.VALUE)) {
                namespaces.add(StringUtils.checkUrl(value));
            }
            Collections.addAll(methodsOnClass, controllerMapping.getAttribute("method", RequestMethod[].class));
        }

        for (final Method method : beanClass.getDeclaredMethods()) { 
            this.buildHandlerMapping(beanClass, method, namespaces, methodsOnClass);
        }
    }

    /**
     * Set Action Mapping
     * 
     * @param beanClass
     *            Controller
     * @param method
     *            Action or Handler
     * @param namespaces
     *            Name space is that path mapping on the class level
     * @param methodsOnClass
     *            request method on class
     * @throws Exception
     *             If any {@link Exception} occurred
     */
    protected void buildHandlerMapping(final Class<?> beanClass,
                                       final Method method,
                                       final Set<String> namespaces,
                                       final Set<RequestMethod> methodsOnClass) throws Exception //
    {
        final AnnotationAttributes[] annotationAttributes = // find mapping on method
                ClassUtils.getAnnotationAttributesArray(method, ActionMapping.class);

        if (ObjectUtils.isNotEmpty(annotationAttributes)) {
            // do mapping url
            this.mappingHandlerMapping(this.createHandlerMapping(beanClass, method), // create HandlerMapping
                                       namespaces, methodsOnClass, annotationAttributes);
        }
    }

    /**
     * create a hash set
     * 
     * @param elements
     *            Elements instance
     */
    @SafeVarargs
    public static <E> Set<E> newHashSet(E... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    /**
     * Mapping given HandlerMapping to {@link HandlerMethodRegistry}
     * 
     * @param handlerMapping
     *            current {@link HandlerMethod}
     * @param namespaces
     *            path on class
     * @param classRequestMethods
     *            methods on class
     * @param annotationAttributes
     *            {@link ActionMapping} Attributes
     */
    protected void mappingHandlerMapping(final HandlerMethod handlerMapping,
                                         final Set<String> namespaces,
                                         final Set<RequestMethod> classRequestMethods,
                                         final AnnotationAttributes[] annotationAttributes) // TODO 
    {
        final boolean emptyNamespaces = namespaces.isEmpty();
        final boolean emptyClassRequestMethods = classRequestMethods.isEmpty();

        for (final AnnotationAttributes handlerMethodMapping : annotationAttributes) {

            final boolean exclude = handlerMethodMapping.getBoolean("exclude"); // exclude name space on class ?
            final Set<RequestMethod> requestMethods = // http request method on method(action/handler)
                    newHashSet(handlerMethodMapping.getAttribute("method", RequestMethod[].class));

            if (!emptyClassRequestMethods) requestMethods.addAll(classRequestMethods);

            for (final String urlOnMethod : handlerMethodMapping.getStringArray("value")) { // url on method
                // splice urls and request methods
                // ---------------------------------
                for (final RequestMethod requestMethod : requestMethods) {

                    final String checkedUrl = StringUtils.checkUrl(urlOnMethod);
                    if (exclude || emptyNamespaces) {
                        doMapping(checkedUrl, requestMethod, handlerMapping);
                        continue;
                    }
                    for (final String namespace : namespaces) {
                        doMapping(namespace.concat(checkedUrl), requestMethod, handlerMapping);
                    }
                }
            }
        }
    }

    /**
     * Mapping to {@link HandlerMethodRegistry}
     * 
     * @param handlerMethod
     *            {@link HandlerMethod}
     * @param urlOnMethod
     *            Method url mapping
     * @param requestMethod
     *            HTTP request method
     * @see RequestMethod
     */
    protected void doMapping(final String urlOnMethod,
                             final RequestMethod requestMethod,
                             final HandlerMethod handlerMapping) //
    {
        final String key = new StringBuilder()
                .append(requestMethod.name())
                .append(getContextPath())
                // GET/blog/users/1 GET/blog/#{key}/1
                .append(ContextUtils.resolveValue(urlOnMethod, String.class, variables))
                .toString();

        if (!doMappingPathVariable(key, requestMethod.name(), handlerMapping)) {

            handlerMethodRegistry.getHandlerMappings().put(key, handlerMapping);

            log.info("Mapped [{}] -> [{}] interceptors -> {}",
                     key, handlerMapping.getMethod(),
                     Arrays.toString(handlerMapping.getInterceptors()));
        }
    }

    /**
     * Create path variable mapping.
     * 
     * @param regexUrl
     *            regex url
     * @param methodParameters
     *            {@link MethodParameter}s
     * @param method
     *            Target Action or Handler
     * @param index
     *            {@link HandlerMethod} index
     * @param requestMethod_
     *            Request method string
     * @return If mapped
     */
    protected boolean doMappingPathVariable(final String regexUrl,
                                            final String requestMethod_,
                                            final HandlerMethod handlerMapping) //
    {
        final Method method = handlerMapping.getMethod();
        final MethodParameter[] methodParameters = handlerMapping.getParameters();

        if (!(regexUrl.indexOf('*') > -1 || regexUrl.indexOf('{') > -1)) { //
            return false; // not a path variable
        }

        String methodUrl = regexUrl; // copy regex url

        String regexUrlToUse = // replace all (*, **)
                regexUrl.replaceAll(Constant.ANY_PATH, Constant.ANY_PATH_REGEXP)
                        .replaceAll(Constant.ONE_PATH, Constant.ONE_PATH_REGEXP);

        boolean hasSet = false;

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter methodParameter = methodParameters[i];
            if (!methodParameter.isAnnotationPresent(PathVariable.class)) {
                continue;
            }
            PathVariable pathVariable = methodParameter.getAnnotation(PathVariable.class);
            if (pathVariable == null) {
                throw new ConfigurationException("You must specify a @PathVariable Like this: [public String update(@PathVariable int id, ..) {...}]");
            }

            String regex = pathVariable.pattern(); // customize regex
            if (StringUtils.isEmpty(regex)) {
                regex = pathVariable.regex();
            }

            if (StringUtils.isEmpty(regex)) {
                Class<?> parameterClass = methodParameter.getParameterClass();
                regex = NumberUtils.isNumber(parameterClass) ? Constant.NUMBER_REGEXP : Constant.STRING_REGEXP;
            }

            String parameterName = methodParameter.getName();
            regexUrlToUse = regexUrlToUse.replace('{' + parameterName + '}', regex);

            String[] splitRegex = methodUrl.split(Constant.PATH_VARIABLE_REGEXP);
            String tempMethodUrl = methodUrl;

            for (String reg : splitRegex) {
                tempMethodUrl = tempMethodUrl.replaceFirst(reg, Constant.REPLACE_REGEXP);
            }

            String[] regexArr = tempMethodUrl.split(Constant.REPLACE_REGEXP);
            for (int j = 0; j < regexArr.length; j++) {
                if (regexArr[j].equals('{' + parameterName + '}')) {
                    methodParameter.setPathIndex(j);
                }
            }
            if (!hasSet) {
                methodParameter.setSplitMethodUrl(methodUrl.replace(requestMethod_, Constant.BLANK)//
                        .split(Constant.PATH_VARIABLE_REGEXP)//
                );
                hasSet = true;
            }
        }

        // fix
        if (regexUrlToUse.indexOf('{') > -1 && regexUrlToUse.indexOf('}') > -1) { // don't have a parameter name named ''
            throw new ConfigurationException("Check @PathVariable configuration on method: [" + method + "]");
        }

        this.regexUrls.put(regexUrlToUse, handlerMapping);
        log.info("Mapped [{}] -> [{}]", regexUrlToUse, method);
        return true;
    }

    /**
     * Create {@link HandlerMethod}.
     * 
     * @param beanClass
     *            Controller class
     * @param method
     *            Action or Handler
     * @return A new {@link HandlerMethod}
     * @throws Exception
     *             If any {@link Throwable} occurred
     */
    protected HandlerMethod createHandlerMapping(final Class<?> beanClass, final Method method) throws Exception {

        final BeanDefinition def = beanFactory.getBeanDefinition(beanClass);

        final Object bean = def.isSingleton()
                ? beanFactory.getBean(def)
                : Prototypes.newProxyInstance(beanClass, def, beanFactory);

        if (bean == null) {
            throw new ConfigurationException("An unexpected exception occurred: [Can't get bean with given type: ["
                    + beanClass.getName() + "]]"//
            );
        }

        final List<MethodParameter> methodParameters = createMethodParameters(method);

        return HandlerMethod.create(bean, method, getInterceptors(beanClass, method), methodParameters);
    }

    /***
     * Build method parameter list
     * 
     * @param method
     *            Target Action or Handler
     */
    public static List<MethodParameter> createMethodParameters(Method method) {

        final Parameter[] parameters = method.getParameters();

        final List<MethodParameter> methodParameters = new ArrayList<>();
        final String[] methodArgsNames = ClassUtils.getMethodArgsNames(method);

        for (int i = 0; i < parameters.length; i++) {
            methodParameters.add(createMethodParameter(parameters[i], methodArgsNames[i]));
        }
        return methodParameters;
    }

    /**
     * Create a method parameter
     * 
     * @param parameter
     *            Reflect parameter
     * @param methodArgsName
     *            method parameter names
     * @return {@link MethodParameter}
     */
    public static MethodParameter createMethodParameter(Parameter parameter, String methodArgsName) {

        Type[] genericityClass = null;
        String parameterName = Constant.BLANK;
        Class<?> parameterClass = parameter.getType();
        // annotation
        boolean required = false;
        String defaultValue = null;

        final Type parameterizedType = parameter.getParameterizedType();
        if (parameterizedType instanceof ParameterizedType) {
            genericityClass = ((ParameterizedType) parameterizedType).getActualTypeArguments();
        }

        Collection<RequestParam> requestParams = ClassUtils.getAnnotation(parameter, RequestParam.class);

        if (!requestParams.isEmpty()) {
            final RequestParam requestParam = requestParams.iterator().next();
            required = requestParam.required();
            parameterName = requestParam.value();
            defaultValue = requestParam.defaultValue();
        }

        if (StringUtils.isEmpty(defaultValue) && NumberUtils.isNumber(parameter.getType())) {
            defaultValue = "0"; // fix default value
        }

        if (StringUtils.isEmpty(parameterName)) {
            parameterName = methodArgsName; // use method parameter name
        }
        return new MethodParameter(parameterName, required, defaultValue, parameter, genericityClass, parameterClass);
    }

    /**
     * Add intercepter to handler .
     * 
     * @param controllerClass
     *            controller class
     * @param action
     *            method
     */
    protected List<HandlerInterceptor> getInterceptors(final Class<?> controllerClass, final Method action) {

        final List<HandlerInterceptor> ret = new ArrayList<>();

        // 设置类拦截器
        final Interceptor controllerInterceptors = controllerClass.getAnnotation(Interceptor.class);
        if (controllerInterceptors != null) {
            Collections.addAll(ret, addInterceptors(controllerInterceptors.value()));
        }
        // HandlerInterceptor on a method
        final Interceptor actionInterceptors = action.getAnnotation(Interceptor.class);

        if (actionInterceptors != null) {
            Collections.addAll(ret, addInterceptors(actionInterceptors.value()));
            for (Class<? extends HandlerInterceptor> interceptor : actionInterceptors.exclude()) {
                ret.remove(beanFactory.getBean(interceptor));
            }
        }

        return ret;
    }

    /***
     * Register intercepter object list
     * 
     * @param interceptors
     *            {@link HandlerInterceptor} class
     * @return A list of {@link HandlerInterceptor} objects
     */
    public HandlerInterceptor[] addInterceptors(Class<? extends HandlerInterceptor>[] interceptors) {

        if (ObjectUtils.isEmpty(interceptors)) {
            return Constant.EMPTY_HANDLER_INTERCEPTOR;
        }

        final AbstractBeanFactory beanFactory = this.beanFactory;

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
            ret[i++] = Objects.requireNonNull(instance, "Can't get target interceptor bean");
        }
        return ret;
    }

    /**
     * Initialize All Action or Handler
     */
    @Override
    public void onStartup(WebApplicationContext applicationContext) throws Throwable {

        log.info("Initializing Controllers");
        startConfiguration();

        handlerMethodRegistry.setRegexMappings(regexUrls);
    }

    /**
     * Rebuild Controllers
     * 
     * @throws Throwable
     *             If any {@link Throwable} occurred
     */
    public void reBuiltControllers() throws Throwable {

        log.info("Rebuilding Controllers");

        regexUrls.clear();
        handlerMethodRegistry.getHandlerMappings().clear();

        startConfiguration();
        handlerMethodRegistry.setRegexMappings(regexUrls);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE - 99;
    }

    /**
     * Get this application context path
     * 
     * @return This application context path
     */
    public String getContextPath() {
        return contextPath;
    }

}
