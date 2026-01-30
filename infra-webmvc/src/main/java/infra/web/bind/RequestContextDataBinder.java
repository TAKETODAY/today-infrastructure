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

package infra.web.bind;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import infra.beans.PropertyValues;
import infra.core.MethodParameter;
import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.validation.BindException;
import infra.validation.DataBinder;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.multipart.Part;

/**
 * Special {@link DataBinder} for data binding from web request parameters
 * to JavaBean objects.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore the design and use of data binding should be considered
 * carefully with regard to security. For more details, please refer to the dedicated
 * sections on data binding for
 * <a href="https://docs.today-tech.cn/today-infrastructure/web/webmvc.html#mvc">Infra Web MVC</a>
 * in the reference manual.
 *
 * <p>Includes support for field markers which address a common problem with
 * HTML checkboxes and select options: detecting that a field was part of
 * the form, but did not generate a request parameter because it was empty.
 * A field marker allows to detect that state and reset the corresponding
 * bean property accordingly. Default values, for parameters that are otherwise
 * not present, can specify a value for the field other then empty.
 *
 * <p>Can also used for manual data binding in custom web controllers or interceptors
 * that build on Infra {@link RequestContext} implementation. Simply instantiate
 * a WebDataBinder for each binding process, and invoke {@code bind} with
 * the current RequestContext as argument:
 *
 * <pre> {@code
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * RequestContextDataBinder binder = new RequestContextDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * // ...
 * }</pre>
 *
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 * @see #setFieldDefaultPrefix
 * @since 4.0 2022/3/2 16:28
 */
public class RequestContextDataBinder extends WebDataBinder {

  private static final Set<String> FILTERED_HEADER_NAMES = Set.of("accept", "authorization", "connection",
          "cookie", "from", "host", "origin", "priority", "range", "referer", "upgrade");

  // @since 5.0
  private Predicate<String> headerPredicate = name -> !FILTERED_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT));

  /**
   * Create a new WebDataBinder instance, with default object name.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @see #DEFAULT_OBJECT_NAME
   */
  public RequestContextDataBinder(@Nullable Object target) {
    super(target);
  }

  /**
   * Create a new WebDataBinder instance.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @param objectName the name of the target object
   */
  public RequestContextDataBinder(@Nullable Object target, String objectName) {
    super(target, objectName);
  }

  /**
   * Add a Predicate that filters the header names to use for data binding.
   * Multiple predicates are combined with {@code AND}.
   *
   * @param predicate the predicate to add
   * @since 5.0
   */
  public void addHeaderPredicate(Predicate<String> predicate) {
    this.headerPredicate = this.headerPredicate.and(predicate);
  }

  /**
   * Set the Predicate that filters the header names to use for data binding.
   * <p>Note that this method resets any previous predicates that may have been
   * set, including headers excluded by default such as the RFC 9218 defined
   * "Priority" header.
   *
   * @param predicate the predicate to add
   * @since 5.0
   */
  public void setHeaderPredicate(Predicate<String> predicate) {
    Assert.notNull(predicate, "header predicate is required");
    this.headerPredicate = predicate;
  }

  /**
   * Use a default or single data constructor to create the target by
   * binding request parameters, multipart files, or parts to constructor args.
   * <p>After the call, use {@link #getBindingResult()} to check for bind errors.
   * If there are none, the target is set, and {@link #bind(RequestContext)}
   * can be called for further initialization via setters.
   *
   * @param request the request to bind
   */
  public void construct(RequestContext request) {
    construct(new RequestValueResolver(request, this));
  }

  @Override
  protected boolean shouldConstructArgument(MethodParameter param) {
    Class<?> type = param.nestedIfOptional().getNestedParameterType();
    return super.shouldConstructArgument(param)
            && !Part.class.isAssignableFrom(type);
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
   * <p>The type of the target property for a multipart file can be Part, Multipart,
   * byte[], or String. The latter two receive the contents of the uploaded file;
   * all metadata like original file name, content type, etc are lost in those cases.
   *
   * @param request the request with parameters to bind (can be multipart)
   * @see infra.web.multipart.Part
   * @see #bind(PropertyValues)
   */
  public void bind(RequestContext request) {
    if (shouldNotBindPropertyValues()) {
      return;
    }
    doBind(getValuesToBind(request));
  }

  /**
   * method to obtain the values for data binding.
   *
   * @param request the current exchange
   * @return a map of bind values
   */
  public PropertyValues getValuesToBind(RequestContext request) {
    PropertyValues pv = new PropertyValues(request.getParameters().toArrayMap(String[]::new));
    if (request.isMultipart()) {
      var multipartFiles = request.asMultipartRequest().getParts();
      if (!multipartFiles.isEmpty()) {
        bindMultipart(multipartFiles, pv);
      }
    }

    addBindValues(pv, request);
    return pv;
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

  /**
   * Merge URI variables into the property values to use for data binding.
   */
  protected void addBindValues(PropertyValues pv, RequestContext request) {
    Map<String, String> uriVars = getUriVars(request);
    if (uriVars != null) {
      for (var entry : uriVars.entrySet()) {
        addValueIfNotPresent(pv, "URI variable", entry.getKey(), entry.getValue());
      }
    }

    for (String name : request.getHeaders().keySet()) {
      Object value = getHeaderValue(request, name);
      if (value != null) {
        name = normalizeHeaderName(name);
        addValueIfNotPresent(pv, "Header", name, value);
      }
    }
  }

  private static String normalizeHeaderName(String name) {
    return StringUtils.uncapitalize(name.replace("-", ""));
  }

  private static @Nullable Map<String, String> getUriVars(RequestContext request) {
    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      return matchingMetadata.getUriVariables();
    }
    return null;
  }

  private static void addValueIfNotPresent(PropertyValues pv, String label, String name, Object value) {
    if (pv.contains(name)) {
      if (logger.isDebugEnabled()) {
        logger.debug("{} '{}' overridden by request bind value.", label, name);
      }
    }
    else {
      pv.add(name, value);
    }
  }

  private @Nullable Object getHeaderValue(RequestContext request, String name) {
    if (!this.headerPredicate.test(name)) {
      return null;
    }

    List<String> values = request.getHeaders().getValuesAsList(name);
    if (values.isEmpty()) {
      return null;
    }

    if (values.size() == 1) {
      return values.get(0);
    }

    return values;
  }

  /**
   * Resolver that looks up values to bind in a {@link RequestContext}.
   */
  protected class RequestValueResolver implements ValueResolver {

    private final RequestContext request;

    private final RequestContextDataBinder dataBinder;

    private @Nullable Set<String> parameterNames;

    protected RequestValueResolver(RequestContext request, RequestContextDataBinder dataBinder) {
      this.request = request;
      this.dataBinder = dataBinder;
    }

    protected RequestContext getRequest() {
      return this.request;
    }

    @Override
    public final @Nullable Object resolveValue(String name, Class<?> paramType) {
      Object value = getRequestParameter(name, paramType);
      if (value == null) {
        value = this.dataBinder.resolvePrefixValue(name, paramType, this::getRequestParameter);
      }
      if (value == null) {
        value = getMultipartValue(name, paramType);
      }
      return value;
    }

    protected @Nullable Object getRequestParameter(String name, Class<?> type) {
      Object value = request.getParameters(name);

      if (value == null && !name.endsWith("[]") && (List.class.isAssignableFrom(type) || type.isArray())) {
        value = this.request.getParameters(name + "[]");
      }
      if (value == null) {
        Map<String, String> uriVars = getUriVars(getRequest());
        if (uriVars != null) {
          value = uriVars.get(name);
        }
        if (value == null) {
          value = getHeaderValue(request, name);
        }
      }
      else if (((String[]) value).length == 1) {
        return ((String[]) value)[0];
      }
      return value;
    }

    private @Nullable Object getMultipartValue(String name, Class<?> paramType) {
      if (request.isMultipart()) {
        List<Part> files = request.asMultipartRequest().getParts(name);
        if (CollectionUtils.isNotEmpty(files)) {
          return files.size() == 1 ? files.get(0) : files;
        }
      }
      return null;
    }

    @Override
    public Set<String> getNames() {
      if (this.parameterNames == null) {
        this.parameterNames = initParameterNames(this.request);
      }
      return this.parameterNames;
    }

    private Set<String> initParameterNames(RequestContext request) {
      Set<String> set = request.getParameterNames();
      Map<String, String> uriVars = getUriVars(getRequest());
      if (uriVars != null) {
        set.addAll(uriVars.keySet());
      }

      for (String name : request.getHeaders().keySet()) {
        if (headerPredicate.test(name)) {
          set.add(normalizeHeaderName(name));
        }
      }

      return set;
    }

  }

}
