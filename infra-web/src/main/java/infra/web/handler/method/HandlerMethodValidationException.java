/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import infra.context.MessageSource;
import infra.context.MessageSourceResolvable;
import infra.core.MethodParameter;
import infra.http.HttpStatus;
import infra.lang.Assert;
import infra.validation.method.MethodValidationResult;
import infra.validation.method.ParameterErrors;
import infra.validation.method.ParameterValidationResult;
import infra.web.annotation.CookieValue;
import infra.web.annotation.MatrixVariable;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestHeader;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.server.ResponseStatusException;
import infra.web.util.BindErrorUtils;

/**
 * {@link ResponseStatusException} that is also {@link MethodValidationResult}.
 * Raised by {@link HandlerMethodValidator} in case of method validation errors
 * on a web controller method.
 *
 * <p>The {@link #getStatusCode()} is 400 for input validation errors, and 500
 * for validation errors on a return value.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class HandlerMethodValidationException extends ResponseStatusException implements MethodValidationResult {

  private final MethodValidationResult validationResult;

  private final Predicate<MethodParameter> modelAttributePredicate;

  private final Predicate<MethodParameter> requestParamPredicate;

  public HandlerMethodValidationException(MethodValidationResult validationResult) {
    this(validationResult, param -> param.hasParameterAnnotation(ModelAttribute.class),
            param -> param.hasParameterAnnotation(RequestParam.class));
  }

  public HandlerMethodValidationException(MethodValidationResult validationResult,
          Predicate<MethodParameter> modelAttributePredicate, Predicate<MethodParameter> requestParamPredicate) {

    super(initHttpStatus(validationResult), "Validation failure", null, null, null);
    this.validationResult = validationResult;
    this.modelAttributePredicate = modelAttributePredicate;
    this.requestParamPredicate = requestParamPredicate;
  }

  private static HttpStatus initHttpStatus(MethodValidationResult validationResult) {
    return validationResult.isForReturnValue() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
  }

  @Override
  public Object[] getDetailMessageArguments(MessageSource messageSource, Locale locale) {
    return new Object[] { BindErrorUtils.resolveAndJoin(getAllErrors(), messageSource, locale) };
  }

  @Override
  public Object[] getDetailMessageArguments() {
    return new Object[] { BindErrorUtils.resolveAndJoin(getAllErrors()) };
  }

  @Override
  public Object getTarget() {
    return this.validationResult.getTarget();
  }

  @Override
  public Method getMethod() {
    return this.validationResult.getMethod();
  }

  @Override
  public boolean isForReturnValue() {
    return this.validationResult.isForReturnValue();
  }

  @Override
  public List<ParameterValidationResult> getParameterValidationResults() {
    return this.validationResult.getParameterValidationResults();
  }

  @Override
  public List<MessageSourceResolvable> getCrossParameterValidationResults() {
    return this.validationResult.getCrossParameterValidationResults();
  }

  /**
   * Provide a {@link Visitor Visitor} to handle {@link ParameterValidationResult}s
   * through callback methods organized by controller method parameter type.
   */
  public void visitResults(Visitor visitor) {
    for (ParameterValidationResult result : getParameterValidationResults()) {
      MethodParameter param = result.getMethodParameter();
      CookieValue cookieValue = param.getParameterAnnotation(CookieValue.class);
      if (cookieValue != null) {
        visitor.cookieValue(cookieValue, result);
        continue;
      }
      MatrixVariable matrixVariable = param.getParameterAnnotation(MatrixVariable.class);
      if (matrixVariable != null) {
        visitor.matrixVariable(matrixVariable, result);
        continue;
      }
      if (this.modelAttributePredicate.test(param)) {
        ModelAttribute modelAttribute = param.getParameterAnnotation(ModelAttribute.class);
        visitor.modelAttribute(modelAttribute, asErrors(result));
        continue;
      }
      PathVariable pathVariable = param.getParameterAnnotation(PathVariable.class);
      if (pathVariable != null) {
        visitor.pathVariable(pathVariable, result);
        continue;
      }
      RequestBody requestBody = param.getParameterAnnotation(RequestBody.class);
      if (requestBody != null) {
        if (result instanceof ParameterErrors errors) {
          visitor.requestBody(requestBody, errors);
        }
        else {
          visitor.requestBodyValidationResult(requestBody, result);
        }
        continue;
      }
      RequestHeader requestHeader = param.getParameterAnnotation(RequestHeader.class);
      if (requestHeader != null) {
        visitor.requestHeader(requestHeader, result);
        continue;
      }
      if (this.requestParamPredicate.test(param)) {
        RequestParam requestParam = param.getParameterAnnotation(RequestParam.class);
        visitor.requestParam(requestParam, result);
        continue;
      }
      RequestPart requestPart = param.getParameterAnnotation(RequestPart.class);
      if (requestPart != null) {
        visitor.requestPart(requestPart, asErrors(result));
        continue;
      }
      visitor.other(result);
    }
  }

  private static ParameterErrors asErrors(ParameterValidationResult result) {
    Assert.state(result instanceof ParameterErrors, "Expected ParameterErrors");
    return (ParameterErrors) result;
  }

  /**
   * Contract to handle validation results with callbacks by controller method
   * parameter type, with {@link #other} serving as the fallthrough.
   */
  public interface Visitor {

    /**
     * Handle results for {@code @CookieValue} method parameters.
     *
     * @param cookieValue the annotation declared on the parameter
     * @param result the validation result
     */
    void cookieValue(CookieValue cookieValue, ParameterValidationResult result);

    /**
     * Handle results for {@code @MatrixVariable} method parameters.
     *
     * @param matrixVariable the annotation declared on the parameter
     * @param result the validation result
     */
    void matrixVariable(MatrixVariable matrixVariable, ParameterValidationResult result);

    /**
     * Handle results for {@code @ModelAttribute} method parameters.
     *
     * @param modelAttribute the optional {@code ModelAttribute} annotation,
     * possibly {@code null} if the method parameter is declared without it.
     * @param errors the validation errors
     */
    void modelAttribute(@Nullable ModelAttribute modelAttribute, ParameterErrors errors);

    /**
     * Handle results for {@code @PathVariable} method parameters.
     *
     * @param pathVariable the annotation declared on the parameter
     * @param result the validation result
     */
    void pathVariable(PathVariable pathVariable, ParameterValidationResult result);

    /**
     * Handle results for {@code @RequestBody} method parameters.
     *
     * @param requestBody the annotation declared on the parameter
     * @param errors the validation error
     */
    void requestBody(RequestBody requestBody, ParameterErrors errors);

    /**
     * An additional {@code @RequestBody} callback for validation failures
     * for constraints on the method parameter. For example:
     * <pre class="code">
     * &#064;RequestBody List&lt;&#064;NotEmpty String&gt; ids
     * </pre>
     * Handle results for {@code @RequestBody} method parameters.
     *
     * @param requestBody the annotation declared on the parameter
     * @param result the validation result
     */
    default void requestBodyValidationResult(RequestBody requestBody, ParameterValidationResult result) {
    }

    /**
     * Handle results for {@code @RequestHeader} method parameters.
     *
     * @param requestHeader the annotation declared on the parameter
     * @param result the validation result
     */
    void requestHeader(RequestHeader requestHeader, ParameterValidationResult result);

    /**
     * Handle results for {@code @RequestParam} method parameters.
     *
     * @param requestParam the optional {@code RequestParam} annotation,
     * possibly {@code null} if the method parameter is declared without it.
     * @param result the validation result
     */
    void requestParam(@Nullable RequestParam requestParam, ParameterValidationResult result);

    /**
     * Handle results for {@code @RequestPart} method parameters.
     *
     * @param requestPart the annotation declared on the parameter
     * @param errors the validation errors
     */
    void requestPart(RequestPart requestPart, ParameterErrors errors);

    /**
     * Handle other results that aren't any of the above.
     *
     * @param result the validation result
     */
    void other(ParameterValidationResult result);

  }

}
