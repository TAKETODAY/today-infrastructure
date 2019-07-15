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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-12 22:23
 */
public class RequestBodyParameterResolver implements ParameterResolver {

    @Override
    public boolean supports(final MethodParameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {

        final Object requestBody = requestContext.requestBody();
        if (requestBody != null) {
            return toJavaObject(parameter, requestBody);
        }
        return toJavaObject(parameter, JSON.parse(requestBody(requestContext, parameter)));
    }

    protected String requestBody(final RequestContext requestContext, final MethodParameter parameter) throws IOException {

        final StringBuilder builder = new StringBuilder((int) (requestContext.contentLength() + 16));
        try {
            final BufferedReader reader = requestContext.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        catch (IOException e) {
            throw WebUtils.newBadRequest("Request body", parameter, e);
        }
        if (builder.length() == 0) {
            throw WebUtils.newBadRequest("Request body", parameter, null);
        }
        return builder.toString();
    }

    protected Object toJavaObject(final MethodParameter parameter, final Object parsedJson) {

        if (parsedJson instanceof JSONArray) {// array
            // style: [{'name':'today','age':21},{'name':"YHJ",'age':22}]

            if (List.class.isAssignableFrom(parameter.getParameterClass())) {
                return ((JSONArray) parsedJson).toJavaList((Class<?>) parameter.getGenericityClass(0));
            }
        }
        else if (parsedJson instanceof JSONObject) {
            final JSONObject requestBody = (JSONObject) parsedJson;

            final Class<?> parameterClass = parameter.getParameterClass();

            if (List.class.isAssignableFrom(parameterClass)) {
                final JSONArray array = requestBody.getJSONArray(parameter.getName());
                if (array == null) { // style: {'users':[{'name':'today','age':21},{'name':"YHJ",'age':22}],...}
                    throw WebUtils.newBadRequest("Request body", parameter, null);
                }
                return array.toJavaList((Class<?>) parameter.getGenericityClass(0));
            }
            else {

                final JSONObject obj = requestBody.getJSONObject(parameter.getName());
                if (obj == null) { // only one request body
                    return requestBody.toJavaObject(parameterClass); // style: {'name':'today','age':21}
                }
                return obj.toJavaObject(parameterClass); // style: {'user':{'name':'today','age':21},...}
            }
        }
        throw WebUtils.newBadRequest("Request body", parameter, null);
    }
}
