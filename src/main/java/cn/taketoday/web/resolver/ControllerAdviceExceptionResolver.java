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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.resolver;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.WebDebugMode;
import cn.taketoday.web.config.ActionConfiguration;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.mapping.WebMapping;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ViewResolver;

/**
 * @author TODAY <br>
 *         2019-06-22 19:17
 * @since 2.3.7
 */
@WebDebugMode
@Singleton(Constant.EXCEPTION_RESOLVER)
public class ControllerAdviceExceptionResolver extends DefaultExceptionResolver implements BeanFactoryAware {

    /** context path */
    private final String contextPath;
    /** download file buffer */
    private final int downloadFileBuf;

    private final ViewResolver viewResolver;

    private final ParameterResolver parameterResolver;

    private final Map<Class<? extends Throwable>, ExceptionHandlerMapping> exceptionHandlers = new HashMap<>();

    @Autowired
    public ControllerAdviceExceptionResolver(ViewResolver viewResolver, //
            ParameterResolver parameterResolver, WebApplicationContext applicationContext) //
    {
        this.viewResolver = viewResolver;
        this.parameterResolver = parameterResolver;

        // @since 2.3.7
        final String downloadBuff = applicationContext.getEnvironment().getProperty("download.buff.size");
        if (StringUtils.isEmpty(downloadBuff)) {
            this.downloadFileBuf = 10240;
        }
        else {
            this.downloadFileBuf = Integer.parseInt(downloadBuff);
        }
        this.contextPath = applicationContext.getServletContext().getContextPath();
    }

    @Override
    public void resolveException(HttpServletRequest request, //
            HttpServletResponse response, Throwable exception, WebMapping mvcMapping) throws Throwable //
    {

        if (mvcMapping instanceof HandlerMapping) {
            final ExceptionHandlerMapping exceptionHandler = exceptionHandlers.get(exception.getClass());
            if (exceptionHandler != null) {
                final Object result = invokeExceptionHandler(request, response, exceptionHandler);
                resolveResult(request, response, exceptionHandler, result);
                return;
            }
        }
        super.resolveException(request, response, exception, mvcMapping);
    }

    protected void resolveResult(//
            final HttpServletRequest request, //
            final HttpServletResponse response, //
            final HandlerMethod handlerMethod, final Object result) throws Throwable //
    {
        switch (handlerMethod.getReutrnType())
        {
            case Constant.RETURN_VIEW : {
                WebUtils.resolveView(request, response, (String) result, contextPath, viewResolver);
                break;
            }
            case Constant.RETURN_STRING : {
                response.getWriter().print(result);
                break;
            }
            case Constant.RETURN_FILE : {
                WebUtils.downloadFile(request, response, (File) result, downloadFileBuf);
                break;
            }
            case Constant.RETURN_IMAGE : {
                // need set content type
                ImageIO.write((RenderedImage) result, Constant.IMAGE_PNG, response.getOutputStream());
                break;
            }
            case Constant.RETURN_JSON : {
                WebUtils.resolveJsonView(response, result);
                break;
            }
            case Constant.RETURN_MODEL_AND_VIEW : {
                resolveModelAndView(request, response, (ModelAndView) result);
                break;
            }
            case Constant.RETURN_VOID : {
                final Object attribute = request.getAttribute(Constant.KEY_MODEL_AND_VIEW);
                if (attribute != null) {
                    resolveModelAndView(request, response, (ModelAndView) attribute);
                }
                break;
            }
            case Constant.RETURN_OBJECT : {
                WebUtils.resolveObject(request, response, result, viewResolver, downloadFileBuf);
                break;
            }
            default:
        }
    }

    /**
     * Resolve {@link ModelAndView} return type
     * 
     * @param request
     *            current request
     * @param response
     *            current response
     * @param modelAndView
     * @throws Throwable
     */
    public void resolveModelAndView(final HttpServletRequest request, //
            final HttpServletResponse response, final ModelAndView modelAndView) throws Throwable //
    {
        if (modelAndView.noView()) {
            return;
        }
        final String contentType = modelAndView.getContentType();
        if (StringUtils.isNotEmpty(contentType)) {
            response.setContentType(contentType);
        }
        final Object view = modelAndView.getView();
        if (view instanceof String) {
            WebUtils.resolveView(request, response, (String) view, contextPath, viewResolver, modelAndView.getDataModel());
        }
        else if (view instanceof StringBuilder || view instanceof StringBuffer) {
            response.getWriter().print(view.toString());
        }
        else if (view instanceof File) {
            WebUtils.downloadFile(request, response, (File) view, downloadFileBuf);
        }
        else if (view instanceof RenderedImage) {
            WebUtils.resolveImage(response, (RenderedImage) view);
        }
        else
            WebUtils.resolveJsonView(response, view);
    }

    protected Object invokeExceptionHandler(//
            final HttpServletRequest request, //
            final HttpServletResponse response, //
            final ExceptionHandlerMapping exceptionHandlerMapping) throws Throwable //
    {
        // method parameter
        final MethodParameter[] methodParameters = exceptionHandlerMapping.getParameter();
        // Handler Method parameter list
        final Object[] args = new Object[methodParameters.length];

        parameterResolver.resolveParameter(args, methodParameters, request, response);

        return exceptionHandlerMapping.getMethod().invoke(exceptionHandlerMapping.getHandler(), args); // invoke
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {

        final List<Object> handlers = beanFactory.getAnnotatedBeans(ControllerAdvice.class);

        for (final Object handler : handlers) {

            final Class<? extends Object> handlerClass = handler.getClass();

            final Collection<AnnotationAttributes> annotationAttributes = //
                    ClassUtils.getAnnotationAttributes(handlerClass, ControllerAdvice.class);
            final boolean restful = annotationAttributes.iterator().next().getBoolean("restful");

            for (final Method method : handlerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ExceptionHandler.class)) {

                    final HandlerMethod handlerMethod = //
                            ActionConfiguration.createHandlerMethod(method, restful, //
                                    ActionConfiguration.createMethodParameters(method));

                    final Object bean = beanFactory.getBean(handlerClass);

                    final ExceptionHandlerMapping mapping = new ExceptionHandlerMapping(bean, handlerMethod);

                    for (Class<? extends Throwable> value : method.getAnnotation(ExceptionHandler.class).value()) {
                        exceptionHandlers.put(value, mapping);
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    private static class ExceptionHandlerMapping extends HandlerMethod implements Serializable {

        private final Object handler;

        public ExceptionHandlerMapping(Object handler, HandlerMethod handlerMethod) {
            super(handlerMethod.getMethod(), handlerMethod.getReutrnType(), handlerMethod.getParameter());
            this.handler = handler;
        }

        public Object getHandler() {
            return handler;
        }
    }
}
