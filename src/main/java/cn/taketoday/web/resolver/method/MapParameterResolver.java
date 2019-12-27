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
package cn.taketoday.web.resolver.method;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 *         2019-07-09 22:49
 */
public class MapParameterResolver implements OrderedParameterResolver {

    @Override
    public boolean supports(final MethodParameter parameter) {
        return parameter.is(Map.class) && supportsInternal(parameter);
    }

    protected boolean supportsInternal(final MethodParameter parameter) {
        return true;
    }

    /**
     * Resolve {@link Map} parameter.
     */
    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {

        // parameter class
        final Class<?> clazz = (Class<?>) parameter.getGenericityClass()[1];

        final String parameterName = parameter.getName();
        final Enumeration<String> parameterNames = requestContext.parameterNames();// all parameter
        final Map<String, Object> map = new HashMap<>();
        final String mapParamRegexp = Constant.MAP_PARAM_REGEXP;
        while (parameterNames.hasMoreElements()) {
            // users%5B%27today_1%27%5D.userId=434&users%5B%27today%27%5D.age=43&users%5B%27today%27%5D.userName=434&users%5B%27today%27%5D.sex=%E7%94%B7&users%5B%27today%27%5D.passwd=4343
            final String requestParameter = parameterNames.nextElement();
            if (requestParameter.startsWith(parameterName)) { // users['today'].userName=TODAY&users['today'].age=20

                final String[] keyList = requestParameter.split(mapParamRegexp); // [users, today, , userName]

                final String key = keyList[1];// get key
                Object newInstance = map.get(key);// 没有就是空值
                if (newInstance == null) {
                    newInstance = clazz.getConstructor().newInstance();// default constructor
                }
                //                if (!resolvePojoParameter(request, //
                //                        requestParameter, newInstance, clazz.getDeclaredField(keyList[3]))) {// 得到Field准备注入
                //
                //                    return map;
                //                }
                map.put(key, newInstance);// put directly
            }
        }
        return map;

        //        throw WebUtils.newBadRequest("Collection variable", parameter.getParameterName(), null);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 90;
    }
}
