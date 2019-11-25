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
package cn.taketoday.web.view;

import java.awt.image.RenderedImage;

import javax.imageio.ImageIO;

import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.HandlerMethod;

/**
 * @author TODAY <br>
 *         2019-07-14 15:15
 */
public class ImageViewResolver implements ViewResolver {

    @Override
    public boolean supports(HandlerMethod handlerMethod) {
        return handlerMethod.isAssignableFrom(RenderedImage.class);
    }

    @Override
    public void resolveView(final RequestContext requestContext, final Object result) throws Throwable {

        requestContext.contentType("image/png"); // sub classes can override this method to apply content type

        ImageIO.write((RenderedImage) result, Constant.IMAGE_PNG, requestContext.getOutputStream());
    }

}
