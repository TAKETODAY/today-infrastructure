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
package cn.taketoday.web.resolver.result;

import java.awt.image.RenderedImage;
import java.io.File;

import com.alibaba.fastjson.JSON;

import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.utils.ResultUtils;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * @author TODAY <br>
 *         2019-07-14 10:47
 */
public abstract class AbstractResultResolver implements ResultResolver {

    private final int downloadFileBuf;
    /** view resolver **/
    private final ViewResolver viewResolver;

    public AbstractResultResolver(ViewResolver viewResolver, int downloadFileBuf) {
        this.downloadFileBuf = downloadFileBuf;

        if (viewResolver instanceof AbstractViewResolver) {
            JSON.defaultLocale = ((AbstractViewResolver) viewResolver).getLocale();
        }
        this.viewResolver = viewResolver;
    }

    public void resolveObject(final RequestContext requestContext, final Object view) throws Throwable {

        if (view instanceof String) {
            ResultUtils.resolveView((String) view, viewResolver, requestContext);
        }
        else if (view instanceof File) {
            ResultUtils.downloadFile(requestContext, ResourceUtils.getResource((File) view), downloadFileBuf);
        }
        else if (view instanceof Resource) {
            ResultUtils.downloadFile(requestContext, (Resource) view, downloadFileBuf);
        }
        else if (view instanceof ModelAndView) {
            resolveModelAndView(requestContext, (ModelAndView) view);
        }
        else if (view instanceof RenderedImage) {
            ResultUtils.resolveImage(requestContext, (RenderedImage) view);
        }
        else {
            ResultUtils.responseBody(requestContext, view);
        }
    }

    /**
     * Resolve {@link ModelAndView} return type
     * 
     * @param modelAndView
     * @throws Throwable
     * @since 2.3.3
     */
    public void resolveModelAndView(final RequestContext requestContext, final ModelAndView modelAndView) throws Throwable {
        if (modelAndView.hasView()) {
            resolveObject(requestContext, modelAndView.getView());
        }
    }

}
