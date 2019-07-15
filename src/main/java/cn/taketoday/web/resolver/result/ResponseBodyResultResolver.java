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

import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.utils.ResultUtils;

/**
 * @author TODAY <br>
 *         2019-07-14 01:19
 */
public class ResponseBodyResultResolver implements OrderedResultResolver {

    @Autowired
    public ResponseBodyResultResolver(@Env(value = Constant.FAST_JSON_SERIALIZE_FEATURES) SerializerFeature[] serializerFeature) {

        if (serializerFeature != null) {
            ResultUtils.SERIALIZE_FEATURES = serializerFeature;
        }
    }

    @Override
    public boolean supports(HandlerMethod handlerMethod) {
        return true;
    }

    @Override
    public void resolveResult(RequestContext requestContext, Object result) throws Throwable {
        ResultUtils.responseBody(requestContext, result);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 100;
    }

}
