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
package cn.taketoday.web.servlet;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.mapping.HandlerInterceptorRegistry;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMappingRegistry;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.mapping.RegexMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * @author TODAY <br>
 *         2018-06-25 19:47:14
 * @version 2.3.7
 */
public class DispatcherServlet implements Servlet {

    private static final Logger log = LoggerFactory.getLogger(DispatcherServlet.class);

    /** view resolver **/
    private final ViewResolver viewResolver;
    /** parameter resolver */
    private final ParameterResolver parameterResolver;
    /** exception resolver */
    private final ExceptionResolver exceptionResolver;
    /** context path */
    private final String contextPath;
    /** download file buffer */
    @Value(value = "#{download.buff.size}", required = false)
    private int downloadFileBuf = 10240;
    /** Action mapping registry */
    private final HandlerMappingRegistry handlerMappingRegistry;
    /** intercepter registry */
    private final HandlerInterceptorRegistry handlerInterceptorRegistry;

    private final WebApplicationContext applicationContext;

    private ServletConfig servletConfig;

    @Autowired
    public DispatcherServlet(//
            ViewResolver viewResolver, //
            ParameterResolver parameterResolver, //
            ExceptionResolver exceptionResolver, //
            HandlerMappingRegistry handlerMappingRegistry, //
            HandlerInterceptorRegistry handlerInterceptorRegistry) //
    {
        this.viewResolver = viewResolver;

        if (exceptionResolver == null) {
            throw new ConfigurationException("You must provide an 'exceptionResolver'");
        }
        this.exceptionResolver = exceptionResolver;

        if (parameterResolver == null) {
            throw new ConfigurationException("You must provide a 'parameterResolver'");
        }
        this.parameterResolver = parameterResolver;

        if (viewResolver instanceof AbstractViewResolver) {
            JSON.defaultLocale = ((AbstractViewResolver) viewResolver).getLocale();
        }
        this.handlerMappingRegistry = handlerMappingRegistry;
        this.handlerInterceptorRegistry = handlerInterceptorRegistry;
        this.applicationContext = WebUtils.getWebApplicationContext();
        this.contextPath = this.applicationContext.getServletContext().getContextPath();

        // @since 2.3.7
        final String property = applicationContext.getEnvironment()//
                .getProperty("fastjson.serialize.features");

        if (StringUtils.isNotEmpty(property)) {

            WebUtils.SERIALIZE_FEATURES = //
                    (SerializerFeature[]) ConvertUtils.convert(property, SerializerFeature[].class);
        }
    }

    /**
     * 
     * TODO return value resolver <br>
     * method arguments resolver <br>
     * path matcher <br>
     * handler mapping
     */
    @Override
    public void service(final ServletRequest servletRequest, final ServletResponse servletResponse) //
            throws ServletException //
    {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Find handler mapping
        final HandlerMapping requestMapping = lookupHandlerMapping(request);
        try {

            if (requestMapping == null) {
                response.sendError(404);
                return;
            }

            final Object result;
            // Handler Method
            final HandlerMethod handlerMethod = requestMapping.getHandlerMethod();
            if (requestMapping.hasInterceptor()) {
                // get intercepter s
                final int[] interceptors = requestMapping.getInterceptors();
                // invoke intercepter
                final HandlerInterceptorRegistry handlerInterceptorRegistry = getHandlerInterceptorRegistry();
                for (final int interceptor : interceptors) {
                    if (!handlerInterceptorRegistry.get(interceptor).beforeProcess(request, response, requestMapping)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Interceptor: [{}] return false", handlerInterceptorRegistry.get(interceptor));
                        }
                        return;
                    }
                }
                result = invokeHandler(request, response, handlerMethod, requestMapping);
                for (final int interceptor : interceptors) {
                    handlerInterceptorRegistry.get(interceptor).afterProcess(result, request, response);
                }
            }
            else {
                result = invokeHandler(request, response, handlerMethod, requestMapping);
            }

            resolveResult(request, response, handlerMethod, result);
        }
        catch (Throwable exception) {
            WebUtils.resolveException(request, response, //
                    applicationContext.getServletContext(), exceptionResolver, requestMapping, exception);
        }
    }

    /**
     * Invoke Handler
     * 
     * @param request
     *            current request
     * @param response
     *            current response
     * @param handlerMethod
     * @param requestMapping
     * @return the result of handler
     * @throws Throwable
     * @since 2.3.7
     */
    protected Object invokeHandler(final HttpServletRequest request, final HttpServletResponse response,
            final HandlerMethod handlerMethod, final HandlerMapping requestMapping) throws Throwable //
    {
        // method parameter
        final MethodParameter[] methodParameters = handlerMethod.getParameter();
        // Handler Method parameter list
        final Object[] args = new Object[methodParameters.length];

        parameterResolver.resolveParameter(args, methodParameters, request, response);

        // log.debug("parameter list -> {}", Arrays.toString(args));
        return handlerMethod.getMethod().invoke(requestMapping.getAction(), args); // invoke
    }

    /**
     * Looking for {@link HandlerMapping}
     * 
     * @param request
     *            current request
     * @return mapped {@link HandlerMapping}
     * @since 2.3.7
     */
    protected HandlerMapping lookupHandlerMapping(final HttpServletRequest request) {
        // The key of handler
        String requestURI = request.getMethod() + request.getRequestURI();

        final HandlerMappingRegistry handlerMappingRegistry = getHandlerMappingRegistry();
        final Integer index = handlerMappingRegistry.getIndex(requestURI);
        if (index == null) {
            // path variable
            requestURI = StringUtils.decodeUrl(requestURI);// decode
            for (final RegexMapping regexMapping : handlerMappingRegistry.getRegexMappings()) {
                // TODO path matcher pathMatcher.match(requestURI, requestURI)
                if (regexMapping.pattern.matcher(requestURI).matches()) {
                    return handlerMappingRegistry.get(regexMapping.index);
                }
            }
            log.debug("NOT FOUND -> [{}]", requestURI);
            return null;
        }
        return handlerMappingRegistry.get(index.intValue());
    }

    /**
     * 
     * @param request
     * @param response
     * @param handlerMethod
     * @param result
     * @throws Throwable
     * @throws IOException
     * @since 2.3.7
     */
    protected void resolveResult(//
            final HttpServletRequest request, //
            final HttpServletResponse response, //
            final HandlerMethod handlerMethod,
            final Object result) throws Throwable //
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
     * @since 2.3.3
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

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
    }

    @Override
    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    /**
     * @return
     */
    public String getServletName() {
        return "DispatcherServlet";
    }

    @Override
    public String getServletInfo() {
        return "DispatcherServlet, Copyright © TODAY & 2017 - 2019 All Rights Reserved";
    }

    @Override
    public void destroy() {

        if (applicationContext != null) {
            final State state = applicationContext.getState();

            if (state != State.CLOSING && state != State.CLOSED) {

                applicationContext.close();

                final DateFormat dateFormat = new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT);//
                final String msg = new StringBuffer()//
                        .append("Your application destroyed at: [")//
                        .append(dateFormat.format(new Date()))//
                        .append("] on startup date: [")//
                        .append(dateFormat.format(applicationContext.getStartupDate()))//
                        .append("]")//
                        .toString();

                log.info(msg);
                applicationContext.getServletContext().log(msg);
            }
        }
    }

    public final HandlerInterceptorRegistry getHandlerInterceptorRegistry() {
        return this.handlerInterceptorRegistry;
    }

    public final HandlerMappingRegistry getHandlerMappingRegistry() {
        return this.handlerMappingRegistry;
    }

    public final String getContextPath() {
        return this.contextPath;
    }

    public final int getDownloadFileBuf() {
        return this.downloadFileBuf;
    }

    public final ViewResolver getViewResolver() {
        return this.viewResolver;
    }

    public final ExceptionResolver getExceptionResolver() {
        return this.exceptionResolver;
    }

    public final ParameterResolver getParameterResolver() {
        return this.parameterResolver;
    }

}
