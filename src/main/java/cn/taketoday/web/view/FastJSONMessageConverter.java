/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.RequestBodyParsingException;

/**
 * support {@link JSONArray}, {@link JSONObject},
 * {@link Collection}, POJO, Array
 *
 * @author TODAY 2019-07-17 20:17
 */
public class FastJSONMessageConverter extends MessageConverter {

  private SerializerFeature[] serializeFeatures = new SerializerFeature[] {
          SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullListAsEmpty,
          SerializerFeature.DisableCircularReferenceDetect
  };

  /** @since 3.0 */
  private int parserFeature = JSON.DEFAULT_PARSER_FEATURE;
  /** fast-json Parser Config @since 3.0 */
  private ParserConfig parserConfig = ParserConfig.getGlobalInstance();

  public FastJSONMessageConverter() { }

  public FastJSONMessageConverter(SerializerFeature... feature) {
    this.serializeFeatures = feature;
  }

  @Override
  protected void writeInternal(final RequestContext context, Object noneNullMessage) throws IOException {
    try {
      JSON.writeJSONString(context.getOutputStream(), noneNullMessage, getSerializeFeatures());
    }
    catch (RuntimeException e) {
      JSON.writeJSONString(context.getWriter(), noneNullMessage, getSerializeFeatures());
    }
  }

  @Override
  public Object read(final RequestContext context, final MethodParameter parameter) throws IOException {
    final Object body = getBody(context);
    if (body instanceof JSONArray) { // array
      return fromJSONArray(parameter, (JSONArray) body);
    }
    else if (body instanceof JSONObject) {
      return fromJSONObject(parameter, (JSONObject) body);
    }
    throw new RequestBodyParsingException("Cannot determine request-body");
  }

  protected Object getBody(RequestContext context) {
    Object requestBody = context.requestBody();
    if (requestBody == null) {
      final StringBuilder builder = new StringBuilder((int) (context.getContentLength() + 16));
      try {
        StringUtils.appendLine(context.getReader(), builder);
      }
      catch (IOException e) {
        throw new RequestBodyParsingException("Request body read failed", e);
      }
      if (builder.length() == 0) {
        return null;
      }
      requestBody = parseRequestBody(builder.toString());
      // cache request body
      context.setRequestBody(requestBody);
    }
    return requestBody;
  }

  /**
   * do parse operation
   *
   * @param body
   *         request body text
   *
   * @return {@link JSONObject} or {@link JSONArray}
   */
  protected Object parseRequestBody(String body) {
    return JSON.parse(body, parserConfig, parserFeature);
  }

  protected Object fromJSONArray(final MethodParameter parameter, final JSONArray requestBody) {
    // style: [{"name":"today","age":21},{"name":"YHJ","age":22}]

    if (parameter.is(List.class)) {
      return requestBody.toJavaList((Class<?>) parameter.getGeneric(0));
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
      if (!CollectionUtils.isEmpty(list)) {
        return list.get(0);
      }
      return null;
    }
    catch (JSONException e) {
      throw new RequestBodyParsingException("Request body read failed", e);
    }
  }

  protected Object fromJSONObject(final MethodParameter parameter, final JSONObject requestBody) {

    if (parameter.is(List.class)) {
      return getJSONArray(parameter, requestBody)
              .toJavaList((Class<?>) parameter.getGeneric(0));
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
    // style: {"users":[{"name":"today","age":21},{"name":"YHJ","age":22}],...}
    return requestBody.getJSONArray(parameter.getName());
  }

  //

  /**
   * Add new features to {@link #parserFeature}
   */
  public void addParserFeatures(Feature... parserFeature) {
    int featureValues = this.parserFeature;
    for (Feature feature : parserFeature) {
      featureValues = Feature.config(featureValues, feature, true);
    }
    setParserFeatures(featureValues);
  }

  /**
   * override {@link #parserFeature}
   *
   * @param parserFeature
   *         new {@link Feature}s
   */
  public void setParserFeature(Feature... parserFeature) {
    setParserFeatures(Feature.of(parserFeature));
  }

  protected void setParserFeatures(int features) {
    this.parserFeature = features;
  }

  public SerializerFeature[] getSerializeFeatures() {
    return serializeFeatures;
  }

  public void setSerializeFeatures(SerializerFeature... serializeFeatures) {
    this.serializeFeatures = serializeFeatures;
  }

  public ParserConfig getParserConfig() {
    return parserConfig;
  }

  /**
   * Set FastJSON parser config
   *
   * @param parserConfig
   *         config object
   */
  public void setParserConfig(ParserConfig parserConfig) {
    this.parserConfig = parserConfig;
  }
}

