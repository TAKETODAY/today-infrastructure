/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.IOException;
import java.util.List;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.ui.JsonSequence;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-17 20:17
 */
@MissingBean(type = MessageConverter.class)
public class FastJsonMessageConverter implements MessageConverter {

  private SerializerFeature[] serializeFeatures;

  @Autowired
  public FastJsonMessageConverter(@Env(Constant.FAST_JSON_SERIALIZE_FEATURES) SerializerFeature[] feature) {

    this.serializeFeatures = feature == null ? new SerializerFeature[] { //
            SerializerFeature.WriteMapNullValue, //
            SerializerFeature.WriteNullListAsEmpty, // 
            SerializerFeature.DisableCircularReferenceDetect//
    } : feature;
  }

  @Override
  public void write(final RequestContext context, final Object message) throws IOException {

    if (message instanceof CharSequence) {
      try {
        context.getOutputStream()
                .write(message.toString().getBytes(Constant.DEFAULT_CHARSET));
      }
      catch (RuntimeException e) {
        context.getWriter().write(message.toString());
      }
    }
    else {
      context.contentType(Constant.CONTENT_TYPE_JSON);
      if (message instanceof JsonSequence) {
        try {
          context.getOutputStream()
                  .write(((JsonSequence) message).toJson().getBytes(Constant.DEFAULT_CHARSET));
        }
        catch (RuntimeException e) {
          context.getWriter().write(((JsonSequence) message).toJson());
        }
      }
      else {
        try {
          JSON.writeJSONString(context.getOutputStream(), message, getSerializeFeatures());
        }
        catch (RuntimeException e) {
          JSON.writeJSONString(context.getWriter(), message, getSerializeFeatures());
        }
      }
    }
  }

  @Override
  public Object read(final RequestContext requestContext, final MethodParameter parameter) throws IOException {

    final Object requestBody = requestContext.requestBody();
    if (requestBody != null) {
      return toJavaObject(parameter, requestBody);
    }

    final StringBuilder builder = new StringBuilder((int) (requestContext.contentLength() + 16));
    try {
      StringUtils.appendLine(requestContext.getReader(), builder);
    }
    catch (IOException e) {
      throw WebUtils.newBadRequest("Request body", parameter, e);
    }
    if (builder.length() == 0) {
      throw WebUtils.newBadRequest("Request body", parameter, null);
    }
    return toJavaObject(parameter, requestContext.requestBody(JSON.parse(builder.toString())));
  }

  protected Object toJavaObject(final MethodParameter parameter, final Object parsedJson) {

    if (parsedJson instanceof JSONArray) { // array
      return fromJSONArray(parameter, (JSONArray) parsedJson);
    }

    if (parsedJson instanceof JSONObject) {
      return fromJSONObject(parameter, (JSONObject) parsedJson);
    }
    throw WebUtils.newBadRequest("Request body", parameter, null);
  }

  protected Object fromJSONArray(final MethodParameter parameter, final JSONArray requestBody) {
    // style: [{"name":"today","age":21},{"name":"YHJ","age":22}]

    if (parameter.is(List.class)) {
      return requestBody.toJavaList((Class<?>) parameter.getGenericityClass(0));
    }

    if (parameter.isArray()) {
      return requestBody.toJavaList(parameter.getParameterClass())
              .toArray();
    }

    if (parameter.is(JSONArray.class)) {
      return requestBody;
    }

    try {

      final List<?> list = requestBody.toJavaList(parameter.getParameterClass());
      if (!list.isEmpty()) {
        return list.get(0);
      }

      throw WebUtils.newBadRequest("Request body", parameter, null);
    }
    catch (JSONException e) {
      throw new BadRequestException(e);
    }
  }

  protected Object fromJSONObject(final MethodParameter parameter, final JSONObject requestBody) {

    if (parameter.is(List.class)) {
      return getJSONArray(parameter, requestBody)
              .toJavaList((Class<?>) parameter.getGenericityClass(0));
    }

    if (parameter.isArray()) {
      return getJSONArray(parameter, requestBody)
              .toJavaList(parameter.getParameterClass())
              .toArray();
    }

    if (parameter.is(JSONObject.class)) {
      return getJSONObject(parameter, requestBody);
    }

    if (parameter.is(JSONArray.class)) {
      return getJSONArray(parameter, requestBody);
    }

    return getJSONObject(parameter, requestBody)
            .toJavaObject(parameter.getParameterClass());
  }

  protected JSONObject getJSONObject(final MethodParameter parameter, final JSONObject requestBody) {
    final JSONObject obj = requestBody.getJSONObject(parameter.getName());
    if (obj == null) { // only one request body
      return requestBody; // style: {'name':'today','age':21}
    }
    return obj; // style: {'user':{'name':'today','age':21},...}
  }

  protected JSONArray getJSONArray(final MethodParameter parameter, final JSONObject requestBody) {
    final JSONArray array = requestBody.getJSONArray(parameter.getName());
    if (array == null) { // style: {"users":[{"name":"today","age":21},{"name":"YHJ","age":22}],...}
      throw WebUtils.newBadRequest("Request body", parameter, null);
    }
    return array;
  }

  public SerializerFeature[] getSerializeFeatures() {
    return serializeFeatures;
  }

  public void setSerializeFeatures(SerializerFeature... serializeFeatures) {
    this.serializeFeatures = serializeFeatures;
  }
}
