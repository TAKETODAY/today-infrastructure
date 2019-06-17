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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.mapping.ViewMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ViewResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-25 19:48:28
 * @version 2.0.0
 */
@Slf4j
@SuppressWarnings("serial")
public class ViewDispatcher extends GenericServlet {

    @Autowired(Constant.VIEW_RESOLVER)
    protected ViewResolver viewResolver;
    /** exception Resolver */
    @Autowired(Constant.EXCEPTION_RESOLVER)
    private ExceptionResolver exceptionResolver;

    @Value(value = "#{download.buff.size}", required = false)
    private int downloadFileBuf = 10240;

    /** view 视图映射池 */
    private static final Map<String, ViewMapping> VIEW_REQUEST_MAPPING = new HashMap<>(16, 1f);

    public static final Map<String, ViewMapping> getMappings() {
        return VIEW_REQUEST_MAPPING;
    }

    public static final void register(String name, ViewMapping viewMapping) {
        VIEW_REQUEST_MAPPING.put(name, viewMapping);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        final ViewMapping mapping = VIEW_REQUEST_MAPPING.get(request.getRequestURI());

        try {

            if (mapping == null) {
                response.sendError(404);
                log.debug("NOT FOUND -> [{}]", request.getRequestURI());
                return;
            }

            Object result = null;
            if (mapping.hasAction()) {
                result = mapping.getAction().invoke(mapping.getController(), request, response);
            }
            if (response.isCommitted()) {
                return;
            }
            if (mapping.getStatus() != 0) {
                response.setStatus(mapping.getStatus());
            }
            final String contentType = mapping.getContentType();
            if (StringUtils.isNotEmpty(contentType)) {
                response.setContentType(contentType);
            }
            switch (mapping.getReturnType())
            {
                case Constant.TYPE_FORWARD : {
                    viewResolver.resolveView(mapping.getAssetsPath(), request, response);
                    return;
                }
                case Constant.TYPE_REDIRECT : {
                    response.sendRedirect(mapping.getAssetsPath());
                    return;
                }
                case Constant.RETURN_IMAGE : {
                    WebUtils.resolveImage(response, (RenderedImage) result);
                    return;
                }
                case Constant.RETURN_STRING : {
                    response.getWriter().print(result);
                    break;
                }
                case Constant.RETURN_OBJECT : {
                    WebUtils.resolveObject(request, response, result, viewResolver, downloadFileBuf);
                    break;
                }
                default:
                    response.sendError(500);
                    break;
            }
        }
        catch (Throwable exception) {
            WebUtils.resolveException(request, response, //
                    getServletConfig().getServletContext(), exceptionResolver, exception);
        }
    }

    @Override
    public String getServletInfo() {
        return "ViewDispatcher, Copyright © Today & 2017 - 2018 All Rights Reserved";
    }

}
