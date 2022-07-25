/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Constant;
import cn.taketoday.web.ResolvableMethod;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.MatrixParam;
import cn.taketoday.web.annotation.RequestAttribute;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.bind.annotation.ModelAttribute;

/**
 * Predicates for {@code @MVC} annotations.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ResolvableMethod#annot(Predicate[])
 * @see ResolvableMethod.Builder#annot(Predicate[])
 * @since 4.0 2022/5/12 10:45
 */
public class MvcAnnotationPredicates {

  // Method parameter predicates

  public static ModelAttributePredicate modelAttribute() {
    return new ModelAttributePredicate();
  }

  public static RequestBodyPredicate requestBody() {
    return new RequestBodyPredicate();
  }

  public static RequestParamPredicate requestParam() {
    return new RequestParamPredicate();
  }

  public static RequestPartPredicate requestPart() {
    return new RequestPartPredicate();
  }

  public static RequestAttributePredicate requestAttribute() {
    return new RequestAttributePredicate();
  }

  public static MatrixVariablePredicate matrixAttribute() {
    return new MatrixVariablePredicate();
  }

  // Method predicates

  public static ModelAttributeMethodPredicate modelMethod() {
    return new ModelAttributeMethodPredicate();
  }

  public static ResponseStatusPredicate responseStatus() {
    return new ResponseStatusPredicate();
  }

  public static ResponseStatusPredicate responseStatus(HttpStatus code) {
    return new ResponseStatusPredicate(code);
  }

  public static RequestMappingPredicate requestMapping(String... path) {
    return new RequestMappingPredicate(path);
  }

  public static RequestMappingPredicate getMapping(String... path) {
    return new RequestMappingPredicate(path).method(HttpMethod.GET);
  }

  public static RequestMappingPredicate postMapping(String... path) {
    return new RequestMappingPredicate(path).method(HttpMethod.POST);
  }

  public static RequestMappingPredicate putMapping(String... path) {
    return new RequestMappingPredicate(path).method(HttpMethod.PUT);
  }

  public static RequestMappingPredicate deleteMapping(String... path) {
    return new RequestMappingPredicate(path).method(HttpMethod.DELETE);
  }

  public static RequestMappingPredicate optionsMapping(String... path) {
    return new RequestMappingPredicate(path).method(HttpMethod.OPTIONS);
  }

  public static RequestMappingPredicate headMapping(String... path) {
    return new RequestMappingPredicate(path).method(HttpMethod.HEAD);
  }

  public static class ModelAttributePredicate implements Predicate<MethodParameter> {

    private String name;

    private boolean binding = true;

    public ModelAttributePredicate name(String name) {
      this.name = name;
      return this;
    }

    public ModelAttributePredicate noName() {
      this.name = "";
      return this;
    }

    public ModelAttributePredicate noBinding() {
      this.binding = false;
      return this;
    }

    @Override
    public boolean test(MethodParameter parameter) {
      ModelAttribute annotation = parameter.getParameterAnnotation(ModelAttribute.class);
      return annotation != null &&
              (this.name == null || annotation.name().equals(this.name)) &&
              annotation.binding() == this.binding;
    }
  }

  public static class RequestBodyPredicate implements Predicate<MethodParameter> {

    private boolean required = true;

    public RequestBodyPredicate notRequired() {
      this.required = false;
      return this;
    }

    @Override
    public boolean test(MethodParameter parameter) {
      RequestBody annotation = parameter.getParameterAnnotation(RequestBody.class);
      return annotation != null && annotation.required() == this.required;
    }
  }

  public static class RequestParamPredicate implements Predicate<MethodParameter> {

    private String name;

    private boolean required = true;

    private String defaultValue = Constant.DEFAULT_NONE;

    public RequestParamPredicate name(String name) {
      this.name = name;
      return this;
    }

    public RequestParamPredicate noName() {
      this.name = "";
      return this;
    }

    public RequestParamPredicate notRequired() {
      this.required = false;
      return this;
    }

    public RequestParamPredicate notRequired(String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    @Override
    public boolean test(MethodParameter parameter) {
      RequestParam annotation = parameter.getParameterAnnotation(RequestParam.class);
      return annotation != null &&
              (this.name == null || annotation.name().equals(this.name)) &&
              annotation.required() == this.required &&
              annotation.defaultValue().equals(this.defaultValue);
    }
  }

  public static class RequestPartPredicate implements Predicate<MethodParameter> {

    private String name;

    private boolean required = true;

    public RequestPartPredicate name(String name) {
      this.name = name;
      return this;
    }

    public RequestPartPredicate noName() {
      this.name = "";
      return this;
    }

    public RequestPartPredicate notRequired() {
      this.required = false;
      return this;
    }

    @Override
    public boolean test(MethodParameter parameter) {
      RequestPart annotation = parameter.getParameterAnnotation(RequestPart.class);
      return annotation != null &&
              (this.name == null || annotation.name().equals(this.name)) &&
              annotation.required() == this.required;
    }
  }

  public static class ModelAttributeMethodPredicate implements Predicate<Method> {

    private String name;

    public ModelAttributeMethodPredicate name(String name) {
      this.name = name;
      return this;
    }

    public ModelAttributeMethodPredicate noName() {
      this.name = "";
      return this;
    }

    @Override
    public boolean test(Method method) {
      ModelAttribute annot = AnnotatedElementUtils.findMergedAnnotation(method, ModelAttribute.class);
      return annot != null && (this.name == null || annot.name().equals(this.name));
    }
  }

  public static class RequestAttributePredicate implements Predicate<MethodParameter> {

    private String name;

    private boolean required = true;

    public RequestAttributePredicate name(String name) {
      this.name = name;
      return this;
    }

    public RequestAttributePredicate noName() {
      this.name = "";
      return this;
    }

    public RequestAttributePredicate notRequired() {
      this.required = false;
      return this;
    }

    @Override
    public boolean test(MethodParameter parameter) {
      RequestAttribute annotation = parameter.getParameterAnnotation(RequestAttribute.class);
      return annotation != null &&
              (this.name == null || annotation.name().equals(this.name)) &&
              annotation.required() == this.required;
    }
  }

  public static class ResponseStatusPredicate implements Predicate<Method> {

    private HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;

    private ResponseStatusPredicate() {
    }

    private ResponseStatusPredicate(HttpStatus code) {
      this.code = code;
    }

    @Override
    public boolean test(Method method) {
      ResponseStatus annot = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
      return annot != null && annot.code().equals(this.code);
    }
  }

  public static class RequestMappingPredicate implements Predicate<Method> {

    private String[] path;

    private HttpMethod[] method = {};

    private String[] params;

    private RequestMappingPredicate(String... path) {
      this.path = path;
    }

    public RequestMappingPredicate method(HttpMethod... methods) {
      this.method = methods;
      return this;
    }

    public RequestMappingPredicate params(String... params) {
      this.params = params;
      return this;
    }

    @Override
    public boolean test(Method method) {
      ActionMapping annot = AnnotatedElementUtils.findMergedAnnotation(method, ActionMapping.class);
      return annot != null &&
              Arrays.equals(this.path, annot.path()) &&
              Arrays.equals(this.method, annot.method()) &&
              (this.params == null || Arrays.equals(this.params, annot.params()));
    }
  }

  public static class MatrixVariablePredicate implements Predicate<MethodParameter> {

    private String name;

    private String pathVar;

    public MatrixVariablePredicate name(String name) {
      this.name = name;
      return this;
    }

    public MatrixVariablePredicate noName() {
      this.name = "";
      return this;
    }

    public MatrixVariablePredicate pathVar(String name) {
      this.pathVar = name;
      return this;
    }

    public MatrixVariablePredicate noPathVar() {
      this.pathVar = Constant.DEFAULT_NONE;
      return this;
    }

    @Override
    public boolean test(MethodParameter parameter) {
      MatrixParam annotation = parameter.getParameterAnnotation(MatrixParam.class);
      return annotation != null &&
              (this.name == null || this.name.equalsIgnoreCase(annotation.name())) &&
              (this.pathVar == null || this.pathVar.equalsIgnoreCase(annotation.pathVar()));
    }
  }

}
