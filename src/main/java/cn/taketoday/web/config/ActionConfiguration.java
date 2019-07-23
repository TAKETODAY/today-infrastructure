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
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.mapping.HandlerInterceptorRegistry;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMappingRegistry;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 */
@Slf4j
@Singleton(Constant.ACTION_CONFIG)
public class ActionConfiguration implements Ordered, DisposableBean, WebApplicationContextAware, WebApplicationInitializer {

    @Getter
    private String contextPath;

    private Map<String, Integer> regexUrls = new HashMap<>();
    private Map<String, Integer> requestMappings = new HashMap<>();

    @Autowired(Constant.HANDLER_MAPPING_REGISTRY)
    public HandlerMappingRegistry handlerMappingRegistry;

    @Autowired(Constant.HANDLER_INTERCEPTOR_REGISTRY)
    public HandlerInterceptorRegistry handlerInterceptorRegistry;

    private WebApplicationContext applicationContext;

    private BeanDefinitionLoader beanDefinitionLoader;

    private Properties variables;

    /**
     * Build {@link HandlerMapping}
     * 
     * @param beanClass
     *            bean class
     * @throws Exception
     *             if any {@link Exception} occurred
     * @since 2.3.7
     */
    public void buildHandlerMapping(final Class<?> beanClass) throws Exception {

        final Collection<AnnotationAttributes> controllerAttributes = //
                ClassUtils.getAnnotationAttributes(beanClass, Controller.class);

        if (controllerAttributes.isEmpty()) {
            return;
        }

        final Collection<ActionMapping> controllerMappings = //
                ClassUtils.getAnnotation(beanClass, ActionMapping.class); // find mapping on class

        final Set<String> namespaces = new HashSet<>(4, 1.0f); // name space
        final Set<RequestMethod> methodsOnClass = new HashSet<>(8, 1.0f); // method

        if (!controllerMappings.isEmpty()) {
            ActionMapping controllerMapping = controllerMappings.iterator().next(); // get first mapping on class
            for (String value : controllerMapping.value()) {
                namespaces.add(StringUtils.checkUrl(value));
            }
            Collections.addAll(methodsOnClass, controllerMapping.method());
        }

        for (Method method : beanClass.getDeclaredMethods()) {
            this.setActionMapping(beanClass, method, namespaces, methodsOnClass);
        }
    }

    /**
     * start config
     * 
     * @throws Exception
     */
    protected void startConfiguration() throws Exception {
        // @since 2.3.3
        for (Entry<String, BeanDefinition> entry : applicationContext.getBeanDefinitions().entrySet()) {
            final BeanDefinition beanDefinition = entry.getValue();
            if (!beanDefinition.isAbstract()) {
                buildHandlerMapping(beanDefinition.getBeanClass());
            }
        }
    }

    /**
     * Set Action Mapping
     * 
     * @param beanClass
     * @param method
     * @param namespaces
     * @param methodsOnClass
     * @throws Exception
     */
    private void setActionMapping(Class<?> beanClass, Method method, //
            Set<String> namespaces, Set<RequestMethod> methodsOnClass) throws Exception //
    {
        final Collection<AnnotationAttributes> annotationAttributes = //
                ClassUtils.getAnnotationAttributes(method, ActionMapping.class);

        if (!annotationAttributes.isEmpty()) {
            // do mapping url
            this.mappingHandlerMapping(this.createHandlerMapping(beanClass, method), // create HandlerMapping
                    namespaces, methodsOnClass, annotationAttributes);
        }
    }

    /**
     * create a hash set
     * 
     * @param elements
     */
    @SafeVarargs
    private static <E> Set<E> newHashSet(E... elements) {
        return Stream.of(elements).collect(Collectors.toSet());
    }

    /**
     * Mapping given HandlerMapping to {@link HandlerMappingRegistry}
     * 
     * @param handlerMapping
     *            current {@link HandlerMapping}
     * @param namespaces
     *            path on class
     * @param classRequestMethods
     *            methods on class
     * @param annotationAttributes
     *            {@link ActionMapping} Attributes
     */
    private void mappingHandlerMapping(HandlerMapping handlerMapping, Set<String> namespaces, //
            Set<RequestMethod> classRequestMethods, Collection<AnnotationAttributes> annotationAttributes) //
    {
        HandlerMethod handlerMethod = handlerMapping.getHandlerMethod();

        // add the mapping
        final int handlerMappingIndex = handlerMappingRegistry.add(handlerMapping); // index of handler method

        for (AnnotationAttributes handlerMethodMapping : annotationAttributes) {
            boolean exclude = handlerMethodMapping.getBoolean("exclude"); // exclude name space on class ?

            Set<RequestMethod> requestMethods = //
                    newHashSet(handlerMethodMapping.getAttribute("method", RequestMethod[].class));

            requestMethods.addAll(classRequestMethods);

            for (String urlOnMethod : handlerMethodMapping.getStringArray("value")) { // url on method
                // splice urls and request methods
                for (RequestMethod requestMethod : requestMethods) {
                    if (exclude || namespaces.isEmpty()) {
                        doMapping(handlerMappingIndex, handlerMethod, StringUtils.checkUrl(urlOnMethod), requestMethod);
                        continue;
                    }
                    for (String namespace : namespaces) {
                        doMapping(handlerMappingIndex, handlerMethod, namespace + StringUtils.checkUrl(urlOnMethod), requestMethod);
                    }
                }
            }
        }
    }

    /**
     * Mapping to {@link HandlerMappingRegistry}
     * 
     * @param handlerMappingIndex
     *            index of the {@link HandlerMapping} array
     * @param handlerMethod
     * @param urlOnMethod
     * @param requestMethod
     */
    private void doMapping(final int handlerMappingIndex, HandlerMethod handlerMethod, String urlOnMethod, RequestMethod requestMethod) {

        final String url = requestMethod.name() //
                + contextPath //
                + ContextUtils.resolveValue(urlOnMethod, String.class, variables); // GET/blog/users/1 GET/blog/#{key}/1

        if (!doMappingPathVariable(url, //
                handlerMethod.getParameters(), handlerMethod.getMethod(), handlerMappingIndex, requestMethod.name())) {

            this.requestMappings.put(url, Integer.valueOf(handlerMappingIndex));
            log.info(//
                    "Mapped [{}] -> [{}] interceptors -> {}", //
                    url, handlerMethod.getMethod(), //
                    Arrays.toString(handlerMappingRegistry.get(handlerMappingIndex).getInterceptors())//
            );
        }
    }

    /**
     * Create path variable mapping.
     * 
     * @param regexUrl
     *            regex url
     * @param methodParameters
     */
    private boolean doMappingPathVariable(String regexUrl, //
            MethodParameter[] methodParameters, Method method, int index, String requestMethod_) //
    {
        if (!regexUrl.contains("*") && !regexUrl.contains("{")) { //
            return false; // not a path variable
        }

        String methodUrl = regexUrl; // copy regex url

        regexUrl = regexUrl.replaceAll(Constant.ANY_PATH, Constant.ANY_PATH_REGEXP);
        regexUrl = regexUrl.replaceAll(Constant.ONE_PATH, Constant.ONE_PATH_REGEXP);
        boolean hasSet = false;

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            MethodParameter methodParameter = methodParameters[i];
            if (!methodParameter.isAnnotationPresent(PathVariable.class)) {
                continue;
            }
            Class<?> parameterClass = methodParameter.getParameterClass();

            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable == null) {
                throw new ConfigurationException(//
                        "You must specify a @PathVariable Like this: [public String update(@PathVariable int id, ..) {...}]"//
                );
            }
            String regex = pathVariable.pattern(); // customize regex
            if (StringUtils.isEmpty(regex)) {
                regex = pathVariable.regex();
            }
            if (StringUtils.isEmpty(regex)) {
                if (parameterClass == String.class) {
                    regex = Constant.STRING_REGEXP;
                }
                else {
                    regex = Constant.NUMBER_REGEXP;
                }
            }

            String parameterName = methodParameter.getName();
            regexUrl = regexUrl.replace('{' + parameterName + '}', regex);

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
                methodParameter.setSplitMethodUrl(//
                        methodUrl.replace(requestMethod_, Constant.BLANK).split(Constant.PATH_VARIABLE_REGEXP)//
                );
                hasSet = true;
            }
        }
        this.regexUrls.put(regexUrl, index);
        log.info("Mapped [{}] -> [{}]", regexUrl, method);
        return true;
    }

    /**
     * Create {@link HandlerMapping}.
     * 
     * @param beanClass
     * @param method
     */
    private HandlerMapping createHandlerMapping(Class<?> beanClass, Method method) throws Exception {

        final List<MethodParameter> methodParameters = createMethodParameters(method);

        final Object bean = applicationContext.getBean(beanClass);
        if (bean == null) {
            throw new ConfigurationException(//
                    "An unexpected exception occurred: [Can't get bean with given type: [" + beanClass.getName() + "]]"//
            );
        }

        final HandlerMethod handlerMethod = HandlerMethod.create(method, methodParameters);

        return new HandlerMapping(bean, handlerMethod, getInterceptor(beanClass, method));
    }

    /***
     * Build method parameter list
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
     *            method parameter namesM
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
     * @param requestMapping
     *            mapping of request
     */
    protected List<Integer> getInterceptor(Class<?> controllerClass, Method action) {

        final List<Integer> ids = new ArrayList<>();

        // 设置类拦截器
        final Interceptor controllerInterceptors = controllerClass.getAnnotation(Interceptor.class);
        if (controllerInterceptors != null) {
            ids.addAll(addInterceptors(controllerInterceptors.value()));
        }
        // HandlerInterceptor on a method
        final Interceptor actionInterceptors = action.getAnnotation(Interceptor.class);

        if (actionInterceptors != null) {
            ids.addAll(addInterceptors(actionInterceptors.value()));

            for (Class<? extends HandlerInterceptor> interceptor : actionInterceptors.exclude()) {
                final int index = handlerInterceptorRegistry.indexOf(interceptor);
                if (index >= 0) {
                    ids.remove(Integer.valueOf(index));
                }
            }
        }

        return ids;
    }

    /***
     * Register intercepter id into intercepter registry
     * 
     * @param interceptors
     * @return
     */
    public List<Integer> addInterceptors(Class<? extends HandlerInterceptor>[] interceptors) {

        if (interceptors == null || interceptors.length == 0) {
            return Collections.emptyList();
        }

        final HandlerInterceptorRegistry handlerInterceptorRegistry = this.handlerInterceptorRegistry;

        final List<Integer> ids = new ArrayList<>(interceptors.length);

        for (Class<? extends HandlerInterceptor> interceptor : interceptors) {
            try {
                final int index = handlerInterceptorRegistry.indexOf(interceptor); // 获得对象的位置
                if (index >= 0) {
                    ids.add(Integer.valueOf(index));
                }
                else {
                    final HandlerInterceptor newInstance;
                    if (applicationContext.containsBeanDefinition(interceptor)) {
                        newInstance = applicationContext.getBean(interceptor);
                    }
                    else {
                        newInstance = (HandlerInterceptor) applicationContext//
                                .refresh(beanDefinitionLoader.createBeanDefinition(interceptor));
                    }
                    ids.add(Integer.valueOf(handlerInterceptorRegistry.add(newInstance)));
                }
            }
            catch (Exception e) {
                throw new ConfigurationException("Interceptor: [" + interceptor.getName() + "] register error", e);
            }
        }
        return ids;
    }

    @Override
    public void destroy() throws Exception {
        if (regexUrls != null) {
            this.regexUrls.clear();
        }
        if (requestMappings != null) {
            this.requestMappings.clear();
        }
    }

    @Override
    public void onStartup(WebApplicationContext applicationContext) throws Throwable {

        log.info("Initializing Controllers");
        startConfiguration();
        handlerMappingRegistry.setRegexMappings(new HashMap<>(regexUrls))//
                .setRequestMappings(new HashMap<>(requestMappings));
    }

    public void reBuiltControllers() throws Throwable {

        log.info("Rebuilding Controllers");

        regexUrls.clear();
        requestMappings.clear();

        startConfiguration();
        handlerMappingRegistry.setRegexMappings(new HashMap<>(regexUrls))//
                .setRequestMappings(new HashMap<>(requestMappings));
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE - 99;
    }

    @Override
    public void setWebApplicationContext(WebApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.contextPath = applicationContext.getContextPath();
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        this.variables = environment.getProperties();
        this.beanDefinitionLoader = environment.getBeanDefinitionLoader();
    }

}
