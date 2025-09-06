/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.bind;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import infra.beans.ConfigurablePropertyAccessor;
import infra.beans.PropertyValue;
import infra.beans.PropertyValues;
import infra.core.MethodParameter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.validation.BindException;
import infra.validation.DataBinder;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.multipart.MultipartFile;

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
 * WebDataBinder binder = new WebDataBinder(myBean);
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
public class WebDataBinder extends DataBinder {

  /**
   * Default prefix that field marker parameters start with, followed by the field
   * name: e.g. "_subscribeToNewsletter" for a field "subscribeToNewsletter".
   * <p>Such a marker parameter indicates that the field was visible, that is,
   * existed in the form that caused the submission. If no corresponding field
   * value parameter was found, the field will be reset. The value of the field
   * marker parameter does not matter in this case; an arbitrary value can be used.
   * This is particularly useful for HTML checkboxes and select options.
   *
   * @see #setFieldMarkerPrefix
   */
  public static final String DEFAULT_FIELD_MARKER_PREFIX = "_";

  /**
   * Default prefix that field default parameters start with, followed by the field
   * name: e.g. "!subscribeToNewsletter" for a field "subscribeToNewsletter".
   * <p>Default parameters differ from field markers in that they provide a default
   * value instead of an empty value.
   *
   * @see #setFieldDefaultPrefix
   */
  public static final String DEFAULT_FIELD_DEFAULT_PREFIX = "!";

  private static final Set<String> FILTERED_HEADER_NAMES = Set.of("accept", "authorization", "connection",
          "cookie", "from", "host", "origin", "priority", "range", "referer", "upgrade");

  // @since 5.0
  private Predicate<String> headerPredicate = name -> !FILTERED_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT));

  @Nullable
  private String fieldMarkerPrefix = DEFAULT_FIELD_MARKER_PREFIX;

  @Nullable
  private String fieldDefaultPrefix = DEFAULT_FIELD_DEFAULT_PREFIX;

  private boolean bindEmptyMultipartFiles = true;

  /**
   * Create a new WebDataBinder instance, with default object name.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @see #DEFAULT_OBJECT_NAME
   */
  public WebDataBinder(@Nullable Object target) {
    super(target);
  }

  /**
   * Create a new WebDataBinder instance.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @param objectName the name of the target object
   */
  public WebDataBinder(@Nullable Object target, String objectName) {
    super(target, objectName);
  }

  /**
   * Specify a prefix that can be used for parameters that mark potentially
   * empty fields, having "prefix + field" as name. Such a marker parameter is
   * checked by existence: You can send any value for it, for example "visible".
   * This is particularly useful for HTML checkboxes and select options.
   * <p>Default is "_", for "_FIELD" parameters (e.g. "_subscribeToNewsletter").
   * Set this to null if you want to turn off the empty field check completely.
   * <p>HTML checkboxes only send a value when they're checked, so it is not
   * possible to detect that a formerly checked box has just been unchecked,
   * at least not with standard HTML means.
   * <p>One way to address this is to look for a checkbox parameter value if
   * you know that the checkbox has been visible in the form, resetting the
   * checkbox if no value found. In Framework web MVC, this typically happens
   * in a custom {@code onBind} implementation.
   * <p>This auto-reset mechanism addresses this deficiency, provided
   * that a marker parameter is sent for each checkbox field, like
   * "_subscribeToNewsletter" for a "subscribeToNewsletter" field.
   * As the marker parameter is sent in any case, the data binder can
   * detect an empty field and automatically reset its value.
   *
   * @see #DEFAULT_FIELD_MARKER_PREFIX
   */
  public void setFieldMarkerPrefix(@Nullable String fieldMarkerPrefix) {
    this.fieldMarkerPrefix = fieldMarkerPrefix;
  }

  /**
   * Return the prefix for parameters that mark potentially empty fields.
   */
  @Nullable
  public String getFieldMarkerPrefix() {
    return this.fieldMarkerPrefix;
  }

  /**
   * Specify a prefix that can be used for parameters that indicate default
   * value fields, having "prefix + field" as name. The value of the default
   * field is used when the field is not provided.
   * <p>Default is "!", for "!FIELD" parameters (e.g. "!subscribeToNewsletter").
   * Set this to null if you want to turn off the field defaults completely.
   * <p>HTML checkboxes only send a value when they're checked, so it is not
   * possible to detect that a formerly checked box has just been unchecked,
   * at least not with standard HTML means.  A default field is especially
   * useful when a checkbox represents a non-boolean value.
   * <p>The presence of a default parameter preempts the behavior of a field
   * marker for the given field.
   *
   * @see #DEFAULT_FIELD_DEFAULT_PREFIX
   */
  public void setFieldDefaultPrefix(@Nullable String fieldDefaultPrefix) {
    this.fieldDefaultPrefix = fieldDefaultPrefix;
  }

  /**
   * Return the prefix for parameters that mark default fields.
   */
  @Nullable
  public String getFieldDefaultPrefix() {
    return this.fieldDefaultPrefix;
  }

  /**
   * Set whether to bind empty MultipartFile parameters. Default is "true".
   * <p>Turn this off if you want to keep an already bound MultipartFile
   * when the user resubmits the form without choosing a different file.
   * Else, the already bound MultipartFile will be replaced by an empty
   * MultipartFile holder.
   *
   * @see infra.web.multipart.MultipartFile
   */
  public void setBindEmptyMultipartFiles(boolean bindEmptyMultipartFiles) {
    this.bindEmptyMultipartFiles = bindEmptyMultipartFiles;
  }

  /**
   * Return whether to bind empty MultipartFile parameters.
   */
  public boolean isBindEmptyMultipartFiles() {
    return this.bindEmptyMultipartFiles;
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
   * Check if a value can be resolved if {@link #getFieldDefaultPrefix()}
   * or {@link #getFieldMarkerPrefix()} is prepended.
   *
   * @param name the name of the value to resolve
   * @param type the type of value expected
   * @param resolver delegate resolver to use for the checks
   * @return the resolved value, or {@code null}
   */
  @Nullable
  protected Object resolvePrefixValue(String name, Class<?> type, BiFunction<String, Class<?>, Object> resolver) {
    Object value = resolver.apply(name, type);
    if (value == null) {
      String prefix = getFieldDefaultPrefix();
      if (prefix != null) {
        value = resolver.apply(prefix + name, type);
      }
      if (value == null) {
        prefix = getFieldMarkerPrefix();
        if (prefix != null && resolver.apply(prefix + name, type) != null) {
          value = getEmptyValue(type);
        }
      }
    }
    return value;
  }

  /**
   * This implementation performs a field default and marker check
   * before delegating to the superclass binding process.
   *
   * @see #checkFieldDefaults
   * @see #checkFieldMarkers
   */
  @Override
  protected void doBind(PropertyValues values) {
    checkFieldDefaults(values);
    checkFieldMarkers(values);
    adaptEmptyArrayIndices(values);
    super.doBind(values);
  }

  /**
   * Check the given property values for field defaults,
   * i.e. for fields that start with the field default prefix.
   * <p>The existence of a field defaults indicates that the specified
   * value should be used if the field is otherwise not present.
   *
   * @param values the property values to be bound (can be modified)
   * @see #getFieldDefaultPrefix
   */
  protected void checkFieldDefaults(PropertyValues values) {
    String fieldDefaultPrefix = getFieldDefaultPrefix();
    if (fieldDefaultPrefix != null) {
      ConfigurablePropertyAccessor propertyAccessor = getPropertyAccessor();
      for (PropertyValue pv : values.toArray()) {
        if (pv.getName().startsWith(fieldDefaultPrefix)) {
          String field = pv.getName().substring(fieldDefaultPrefix.length());
          if (propertyAccessor.isWritableProperty(field) && !values.contains(field)) {
            values.add(field, pv.getValue());
          }
          values.remove(pv);
        }
      }
    }
  }

  /**
   * Check the given property values for field markers,
   * i.e. for fields that start with the field marker prefix.
   * <p>The existence of a field marker indicates that the specified
   * field existed in the form. If the property values do not contain
   * a corresponding field value, the field will be considered as empty
   * and will be reset appropriately.
   *
   * @param values the property values to be bound (can be modified)
   * @see #getFieldMarkerPrefix
   * @see #getEmptyValue(String, Class)
   */
  protected void checkFieldMarkers(PropertyValues values) {
    String fieldMarkerPrefix = getFieldMarkerPrefix();
    if (fieldMarkerPrefix != null) {
      ConfigurablePropertyAccessor propertyAccessor = getPropertyAccessor();
      for (PropertyValue pv : values.toArray()) {
        if (pv.getName().startsWith(fieldMarkerPrefix)) {
          String field = pv.getName().substring(fieldMarkerPrefix.length());
          if (propertyAccessor.isWritableProperty(field) && !values.contains(field)) {
            Class<?> fieldType = propertyAccessor.getPropertyType(field);
            values.add(field, getEmptyValue(field, fieldType));
          }
          values.remove(pv);
        }
      }
    }
  }

  /**
   * Check for property values with names that end on {@code "[]"}. This is
   * used by some clients for array syntax without an explicit index value.
   * If such values are found, drop the brackets to adapt to the expected way
   * of expressing the same for data binding purposes.
   *
   * @param values the property values to be bound (can be modified)
   */
  protected void adaptEmptyArrayIndices(PropertyValues values) {
    ConfigurablePropertyAccessor propertyAccessor = getPropertyAccessor();
    for (PropertyValue pv : values.toArray()) {
      String name = pv.getName();
      if (name.endsWith("[]")) {
        String field = name.substring(0, name.length() - 2);
        if (propertyAccessor.isWritableProperty(field) && !values.contains(field)) {
          values.add(field, pv.getValue());
        }
        values.remove(pv);
      }
    }
  }

  /**
   * Determine an empty value for the specified field.
   * <p>The default implementation delegates to {@link #getEmptyValue(Class)}
   * if the field type is known, otherwise falls back to {@code null}.
   *
   * @param field the name of the field
   * @param fieldType the type of the field
   * @return the empty value (for most fields: {@code null})
   */
  @Nullable
  protected Object getEmptyValue(String field, @Nullable Class<?> fieldType) {
    return fieldType != null ? getEmptyValue(fieldType) : null;
  }

  /**
   * Determine an empty value for the specified field.
   * <p>The default implementation returns:
   * <ul>
   * <li>{@code Boolean.FALSE} for boolean fields
   * <li>an empty array for array types
   * <li>Collection implementations for Collection types
   * <li>Map implementations for Map types
   * <li>else, {@code null} is used as default
   * </ul>
   *
   * @param fieldType the type of the field
   * @return the empty value (for most fields: {@code null})
   */
  @Nullable
  public Object getEmptyValue(Class<?> fieldType) {
    try {
      if (boolean.class == fieldType || Boolean.class == fieldType) {
        // Special handling of boolean property.
        return Boolean.FALSE;
      }
      else if (fieldType.isArray()) {
        // Special handling of array property.
        return Array.newInstance(fieldType.getComponentType(), 0);
      }
      else if (Collection.class.isAssignableFrom(fieldType)) {
        return CollectionUtils.createCollection(fieldType, 0);
      }
      else if (Map.class.isAssignableFrom(fieldType)) {
        return CollectionUtils.createMap(fieldType, 0);
      }
    }
    catch (IllegalArgumentException ex) {
      logger.debug("Failed to create default value - falling back to null: {}", ex.getMessage());
    }
    // Default value: null.
    return null;
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
            && !MultipartFile.class.isAssignableFrom(type);
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
   * @see infra.web.multipart.MultipartFile
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
      var multipartFiles = request.multipartRequest().getMultipartFiles();
      if (!multipartFiles.isEmpty()) {
        bindMultipart(multipartFiles, pv);
      }
    }

    addBindValues(pv, request);
    return pv;
  }

  /**
   * Bind all multipart files contained in the given request, if any
   * (in case of a multipart request). To be called by subclasses.
   * <p>Multipart files will only be added to the property values if they
   * are not empty or if we're configured to bind empty multipart files too.
   *
   * @param multipartFiles a Map of field name String to MultipartFile object
   * @param mpvs the property values to be bound (can be modified)
   * @see infra.web.multipart.MultipartFile
   * @see #setBindEmptyMultipartFiles
   */
  protected void bindMultipart(Map<String, List<MultipartFile>> multipartFiles, PropertyValues mpvs) {
    for (Map.Entry<String, List<MultipartFile>> entry : multipartFiles.entrySet()) {
      List<MultipartFile> values = entry.getValue();
      String key = entry.getKey();
      if (values.size() == 1) {
        MultipartFile value = values.get(0);
        if (isBindEmptyMultipartFiles() || !value.isEmpty()) {
          mpvs.add(key, value);
        }
      }
      else {
        mpvs.add(key, values);
      }
    }
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

  @Nullable
  private static Map<String, String> getUriVars(RequestContext request) {
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

  @Nullable
  private Object getHeaderValue(RequestContext request, String name) {
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

    private final WebDataBinder dataBinder;

    @Nullable
    private Set<String> parameterNames;

    protected RequestValueResolver(RequestContext request, WebDataBinder dataBinder) {
      this.request = request;
      this.dataBinder = dataBinder;
    }

    protected RequestContext getRequest() {
      return this.request;
    }

    @Nullable
    @Override
    public final Object resolveValue(String name, Class<?> paramType) {
      Object value = getRequestParameter(name, paramType);
      if (value == null) {
        value = this.dataBinder.resolvePrefixValue(name, paramType, this::getRequestParameter);
      }
      if (value == null) {
        value = getMultipartValue(name, paramType);
      }
      return value;
    }

    @Nullable
    protected Object getRequestParameter(String name, Class<?> type) {
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

    @Nullable
    private Object getMultipartValue(String name, Class<?> paramType) {
      if (request.isMultipart()) {
        List<MultipartFile> files = request.multipartRequest().getFiles(name);
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
