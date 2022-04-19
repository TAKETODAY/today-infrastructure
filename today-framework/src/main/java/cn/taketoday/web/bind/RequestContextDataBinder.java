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

package cn.taketoday.web.bind;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Nullable;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.DataBinder;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * Special {@link DataBinder} to perform data binding from web request parameters
 * to JavaBeans, including support for multipart files.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore the design and use of data binding should be considered
 * carefully with regard to security. For more details, please refer to the dedicated
 * sections on data binding for
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> and
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * in the reference manual.
 *
 * <p>See the DataBinder/WebDataBinder superclasses for customization options,
 * which include specifying allowed/required fields, and registering custom
 * property editors.
 *
 * <p>Can also used for manual data binding in custom web controllers or interceptors
 * that build on Framework's {@link RequestContext}
 * implementation. Simply instantiate a RequestContextDataBinder for each binding
 * process, and invoke {@code bind} with the current RequestContext as argument:
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * RequestContextDataBinder binder = new RequestContextDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #bind(RequestContext)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 * @since 4.0 2022/3/2 16:40
 */
public class RequestContextDataBinder extends WebDataBinder {

  /**
   * Create a new RequestContextDataBinder instance, with default object name.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @see #DEFAULT_OBJECT_NAME
   */
  public RequestContextDataBinder(@Nullable Object target) {
    super(target);
  }

  /**
   * Create a new RequestContextDataBinder instance.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @param objectName the name of the target object
   */
  public RequestContextDataBinder(@Nullable Object target, String objectName) {
    super(target, objectName);
  }

  /**
   * Bind the parameters of the given request to this binder's target,
   * also binding multipart files in case of a multipart request.
   * <p>This call can create field errors, representing basic binding
   * errors like a required field (code "required"), or type mismatch
   * between value and bean property (code "typeMismatch").
   * <p>Multipart files are bound via their parameter name, just like normal
   * HTTP parameters: i.e. "uploadedFile" to an "uploadedFile" bean property,
   * invoking a "setUploadedFile" setter method.
   * <p>The type of the target property for a multipart file can be Part, MultipartFile,
   * byte[], or String. The latter two receive the contents of the uploaded file;
   * all metadata like original file name, content type, etc are lost in those cases.
   *
   * @param request the request with parameters to bind (can be multipart)
   * @see cn.taketoday.web.multipart.MultipartFile
   * @see jakarta.servlet.http.Part
   * @see #bind(PropertyValues)
   */
  public void bind(RequestContext request) {
    PropertyValues propertyValues = new PropertyValues(request.getParameters());
    if (request.isMultipart()) {
      MultiValueMap<String, MultipartFile> multipartFiles = request.multipartFiles();
      if (multipartFiles != null) {
        bindMultipart(multipartFiles, propertyValues);
      }
    }
    doBind(propertyValues);
  }

  /**
   * Treats errors as fatal.
   * <p>Use this method only if it's an error if the input isn't valid.
   * This might be appropriate if all input is from dropdowns, for example.
   *
   * @throws BindException if binding errors have been encountered
   */
  public void closeNoCatch() throws BindException {
    if (getBindingResult().hasErrors()) {
      throw new BindException(getBindingResult());
    }
  }

}
