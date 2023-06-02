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
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.BindException;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Special {@link cn.taketoday.validation.DataBinder} to perform data binding
 * from servlet request parameters to JavaBeans, including support for multipart files.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore the design and use of data binding should be considered
 * carefully with regard to security. For more details, please refer to the dedicated
 * sections on data binding for
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Infra Web MVC</a> and
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Infra WebFlux</a>
 * in the reference manual.
 *
 * <p>See the DataBinder/WebDataBinder superclasses for customization options,
 * which include specifying allowed/required fields, and registering custom
 * property editors.
 *
 * <p>Can also be used for manual data binding in custom web controllers:
 * for example, in a plain Controller implementation or in a MultiActionController
 * handler method. Simply instantiate a ServletRequestDataBinder for each binding
 * process, and invoke {@code bind} with the current ServletRequest as argument:
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * ServletRequestDataBinder binder = new ServletRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #bind(jakarta.servlet.ServletRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 * @since 4.0 2022/3/2 16:30
 */
public class ServletRequestDataBinder extends WebDataBinder {

  /**
   * Create a new ServletRequestDataBinder instance, with default object name.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @see #DEFAULT_OBJECT_NAME
   */
  public ServletRequestDataBinder(@Nullable Object target) {
    super(target);
  }

  /**
   * Create a new ServletRequestDataBinder instance.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @param objectName the name of the target object
   */
  public ServletRequestDataBinder(@Nullable Object target, String objectName) {
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
   * <p>The type of the target property for a multipart file can be MultipartFile,
   * byte[], or String. The latter two receive the contents of the uploaded file;
   * all metadata like original file name, content type, etc are lost in those cases.
   *
   * @param request the request with parameters to bind (can be multipart)
   * @see cn.taketoday.web.multipart.MultipartFile
   * @see #bind(cn.taketoday.beans.PropertyValues)
   */
  public void bind(ServletRequest request) {
    PropertyValues mpvs = new ServletRequestParameterPropertyValues(request);
    if (StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.MULTIPART_FORM_DATA_VALUE)) {
      HttpServletRequest httpServletRequest = ServletUtils.getNativeRequest(request, HttpServletRequest.class);
      if (httpServletRequest != null && HttpMethod.POST.matches(httpServletRequest.getMethod())) {
        ServletUtils.bindParts(httpServletRequest, mpvs, isBindEmptyMultipartFiles());
      }
    }
    addBindValues(mpvs, request);
    doBind(mpvs);
  }

  /**
   * Extension point that subclasses can use to add extra bind values for a
   * request. Invoked before {@link #doBind(PropertyValues)}.
   * The default implementation is empty.
   *
   * @param mpvs the property values that will be used for data binding
   * @param request the current request
   */
  protected void addBindValues(PropertyValues mpvs, ServletRequest request) { }

  /**
   * Treats errors as fatal.
   * <p>Use this method only if it's an error if the input isn't valid.
   * This might be appropriate if all input is from dropdowns, for example.
   *
   * @throws RequestBindingException subclass of ServletException on any binding problem
   */
  public void closeNoCatch() throws RequestBindingException {
    if (getBindingResult().hasErrors()) {
      throw new RequestBindingException(
              "Errors binding onto object '" + getBindingResult().getObjectName() + "'",
              new BindException(getBindingResult()));
    }
  }

}
