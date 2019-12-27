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

import java.io.File;
import java.io.IOException;

import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-14 11:18
 */
public class ResourceResultHandler extends HandlerMethodResultHandler implements RuntimeResultHandler {

    public ResourceResultHandler(int downloadFileBuf) {
        setDownloadFileBufferSize(downloadFileBuf);
    }

    @Override
    public boolean supports(HandlerMethod handlerMethod) {
        return handlerMethod.isAssignableFrom(Resource.class)
               || handlerMethod.isAssignableFrom(File.class);
    }

    @Override
    public boolean supportsResult(Object result) {
        return result instanceof Resource
               || result instanceof File;
    }

    @Override
    public void handleResult(RequestContext context, Object result) throws IOException {
        if (result instanceof Resource) {
            WebUtils.downloadFile(context, (Resource) result, getDownloadFileBufferSize());
        }
        WebUtils.downloadFile(context, ResourceUtils.getResource((File) result), getDownloadFileBufferSize());
    }

}
