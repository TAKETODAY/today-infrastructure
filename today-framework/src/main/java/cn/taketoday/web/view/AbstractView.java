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

package cn.taketoday.web.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.ContextExposingRequestContext;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;

/**
 * Abstract base class for {@link View}
 * implementations. Subclasses should be JavaBeans, to allow for
 * convenient configuration as managed bean instances.
 *
 * <p>Provides support for static attributes, to be made available to the view,
 * with a variety of ways to specify them. Static attributes will be merged
 * with the given dynamic attributes (the model that the controller returned)
 * for each render operation.
 *
 * <p>Extends {@link WebApplicationContextSupport}, which will be helpful to
 * some views. Subclasses just need to implement the actual rendering.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setAttributes
 * @see #setAttributesMap
 * @see #renderMergedOutputModel
 * @since 4.0
 */
public abstract class AbstractView extends WebApplicationContextSupport implements View, BeanNameAware {

  /** Default content type. Overridable as bean property. */
  public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";

  /** Initial size for the temporary output byte array (if any). */
  private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 4096;

  @Nullable
  private String contentType = DEFAULT_CONTENT_TYPE;

  @Nullable
  private String requestContextAttribute;

  @Nullable
  private LinkedHashMap<String, Object> staticAttributes;

  private boolean exposePathVariables = true;

  private boolean exposeContextBeansAsAttributes = false;

  @Nullable
  private Set<String> exposedContextBeanNames;

  @Nullable
  private String beanName;

  /**
   * Set the content type for this view.
   * Default is "text/html;charset=UTF-8".
   * <p>May be ignored by subclasses if the view itself is assumed
   * to set the content type, e.g. in case of JSPs.
   */
  public void setContentType(@Nullable String contentType) {
    this.contentType = contentType;
  }

  /**
   * Return the content type for this view.
   */
  @Override
  @Nullable
  public String getContentType() {
    return this.contentType;
  }

  /**
   * Set the name of the RequestContext attribute for this view.
   * Default is none.
   */
  public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
    this.requestContextAttribute = requestContextAttribute;
  }

  /**
   * Return the name of the RequestContext attribute, if any.
   */
  @Nullable
  public String getRequestContextAttribute() {
    return this.requestContextAttribute;
  }

  /**
   * Set static attributes as a CSV string.
   * Format is: attname0={value1},attname1={value1}
   * <p>"Static" attributes are fixed attributes that are specified in
   * the View instance configuration. "Dynamic" attributes, on the other hand,
   * are values passed in as part of the model.
   */
  public void setAttributesCSV(@Nullable String propString) throws IllegalArgumentException {
    if (propString != null) {
      StringTokenizer st = new StringTokenizer(propString, ",");
      while (st.hasMoreTokens()) {
        String tok = st.nextToken();
        int eqIdx = tok.indexOf('=');
        if (eqIdx == -1) {
          throw new IllegalArgumentException(
                  "Expected '=' in attributes CSV string '" + propString + "'");
        }
        if (eqIdx >= tok.length() - 2) {
          throw new IllegalArgumentException(
                  "At least 2 characters ([]) required in attributes CSV string '" + propString + "'");
        }
        String name = tok.substring(0, eqIdx);
        // Delete first and last characters of value: { and }
        int beginIndex = eqIdx + 2;
        int endIndex = tok.length() - 1;
        String value = tok.substring(beginIndex, endIndex);

        addStaticAttribute(name, value);
      }
    }
  }

  /**
   * Set static attributes for this view from a
   * {@code java.util.Properties} object.
   * <p>"Static" attributes are fixed attributes that are specified in
   * the View instance configuration. "Dynamic" attributes, on the other hand,
   * are values passed in as part of the model.
   * <p>This is the most convenient way to set static attributes. Note that
   * static attributes can be overridden by dynamic attributes, if a value
   * with the same name is included in the model.
   * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
   * or a "props" element in XML bean definitions.
   */
  public void setAttributes(Properties attributes) {
    CollectionUtils.mergePropertiesIntoMap(attributes, staticAttributes());
  }

  private LinkedHashMap<String, Object> staticAttributes() {
    LinkedHashMap<String, Object> staticAttributes = this.staticAttributes;
    if (staticAttributes == null) {
      staticAttributes = new LinkedHashMap<>();
      this.staticAttributes = staticAttributes;
    }
    return staticAttributes;
  }

  /**
   * Set static attributes for this view from a Map. This allows to set
   * any kind of attribute values, for example bean references.
   * <p>"Static" attributes are fixed attributes that are specified in
   * the View instance configuration. "Dynamic" attributes, on the other hand,
   * are values passed in as part of the model.
   * <p>Can be populated with a "map" or "props" element in XML bean definitions.
   *
   * @param attributes a Map with name Strings as keys and attribute objects as values
   */
  public void setAttributesMap(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      for (Map.Entry<String, ?> entry : attributes.entrySet()) {
        addStaticAttribute(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Allow Map access to the static attributes of this view,
   * with the option to add or override specific entries.
   * <p>Useful for specifying entries directly, for example via
   * "attributesMap[myKey]". This is particularly useful for
   * adding or overriding entries in child view definitions.
   */
  public Map<String, Object> getAttributesMap() {
    return staticAttributes();
  }

  /**
   * Add static data to this view, exposed in each view.
   * <p>"Static" attributes are fixed attributes that are specified in
   * the View instance configuration. "Dynamic" attributes, on the other hand,
   * are values passed in as part of the model.
   * <p>Must be invoked before any calls to {@code render}.
   *
   * @param name the name of the attribute to expose
   * @param value the attribute value to expose
   * @see #render
   */
  public void addStaticAttribute(String name, Object value) {
    staticAttributes().put(name, value);
  }

  /**
   * Return the static attributes for this view. Handy for testing.
   * <p>Returns an unmodifiable Map, as this is not intended for
   * manipulating the Map but rather just for checking the contents.
   *
   * @return the static attributes in this view
   */
  @Nullable
  public Map<String, Object> getStaticAttributes() {
    return staticAttributes;
  }

  /**
   * Specify whether to add path variables to the model or not.
   * <p>Path variables are commonly bound to URI template variables through the {@code @PathVariable}
   * annotation. They're are effectively URI template variables with type conversion applied to
   * them to derive typed Object values. Such values are frequently needed in views for
   * constructing links to the same and other URLs.
   * <p>Path variables added to the model override static attributes (see {@link #setAttributes(Properties)})
   * but not attributes already present in the model.
   * <p>By default this flag is set to {@code true}. Concrete view types can override this.
   *
   * @param exposePathVariables {@code true} to expose path variables, and {@code false} otherwise
   */
  public void setExposePathVariables(boolean exposePathVariables) {
    this.exposePathVariables = exposePathVariables;
  }

  /**
   * Return whether to add path variables to the model or not.
   */
  public boolean isExposePathVariables() {
    return this.exposePathVariables;
  }

  /**
   * Set whether to make all beans in the application context accessible
   * as request attributes, through lazy checking once an attribute gets accessed.
   * <p>This will make all such beans accessible in plain {@code ${...}}
   * expressions in a JSP 2.0 page, as well as in JSTL's {@code c:out}
   * value expressions.
   * <p>Default is "false". Switch this flag on to transparently expose all
   * Framework beans in the request attribute namespace.
   * <p><b>NOTE:</b> Context beans will override any custom request or session
   * attributes of the same name that have been manually added. However, model
   * attributes (as explicitly exposed to this view) of the same name will
   * always override context beans.
   *
   * @see #getRequestContextToExpose
   */
  public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
    this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
  }

  /**
   * Specify the names of beans in the context which are supposed to be exposed.
   * If this is non-null, only the specified beans are eligible for exposure as
   * attributes.
   * <p>If you'd like to expose all Framework beans in the application context, switch
   * the {@link #setExposeContextBeansAsAttributes "exposeContextBeansAsAttributes"}
   * flag on but do not list specific bean names for this property.
   */
  public void setExposedContextBeanNames(String... exposedContextBeanNames) {
    this.exposedContextBeanNames = new HashSet<>(Arrays.asList(exposedContextBeanNames));
  }

  /**
   * Set the view's name. Helpful for traceability.
   * <p>Framework code must call this when constructing views.
   */
  @Override
  public void setBeanName(@Nullable String beanName) {
    this.beanName = beanName;
  }

  /**
   * Return the view's name. Should never be {@code null},
   * if the view was correctly configured.
   */
  @Nullable
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Prepares the view given the specified model, merging it with static
   * attributes and a RequestContext attribute, if necessary.
   * Delegates to renderMergedOutputModel for the actual rendering.
   *
   * @see #renderMergedOutputModel
   */
  @Override
  public void render(@Nullable Map<String, ?> model, RequestContext context) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("View {}, model {} {}",
              formatViewName(),
              model != null ? model : Collections.emptyMap(),
              CollectionUtils.isEmpty(staticAttributes) ? "" : ", static attributes " + staticAttributes);
    }

    Map<String, Object> mergedModel = createMergedOutputModel(model, context);
    prepareResponse(context);
    renderMergedOutputModel(mergedModel, getRequestContextToExpose(context));
  }

  /**
   * Creates a combined output Map (never {@code null}) that includes dynamic values and static attributes.
   * Dynamic values take precedence over static attributes.
   */
  protected Map<String, Object> createMergedOutputModel(
          @Nullable Map<String, ?> model, RequestContext context) {

    // Consolidate static and dynamic model attributes.
    Map<String, Object> staticAttributes = getStaticAttributes();
    int size = model != null ? model.size() : 0;
    if (staticAttributes != null) {
      size += staticAttributes.size();
    }

    Map<String, String> pathVars = null;
    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    if (exposePathVariables && matchingMetadata != null) {
      pathVars = matchingMetadata.getUriVariables();
      size += pathVars.size();
    }

    Map<String, Object> mergedModel = CollectionUtils.newLinkedHashMap(size);
    if (CollectionUtils.isNotEmpty(staticAttributes)) {
      mergedModel.putAll(staticAttributes);
    }
    if (CollectionUtils.isNotEmpty(pathVars)) {
      mergedModel.putAll(pathVars);
    }
    if (CollectionUtils.isNotEmpty(model)) {
      mergedModel.putAll(model);
    }

    // Expose RequestContext?
    if (requestContextAttribute != null) {
      mergedModel.put(requestContextAttribute, context);
    }

    return mergedModel;
  }

  /**
   * Prepare the given response for rendering.
   * <p>The default implementation applies a workaround for an IE bug
   * when sending download content via HTTPS.
   *
   * @param context current HTTP request context
   */
  protected void prepareResponse(RequestContext context) {
    if (generatesDownloadContent()) {
      HttpHeaders headers = context.responseHeaders();
      headers.setPragma("private");
      headers.setCacheControl("private, must-revalidate");
    }
  }

  /**
   * Return whether this view generates download content
   * (typically binary content like PDF or Excel files).
   * <p>The default implementation returns {@code false}. Subclasses are
   * encouraged to return {@code true} here if they know that they are
   * generating download content that requires temporary caching on the
   * client side, typically via the response OutputStream.
   *
   * @see #prepareResponse
   * @see RequestContext#getOutputStream()
   */
  protected boolean generatesDownloadContent() {
    return false;
  }

  /**
   * Get the request handle to expose to {@link #renderMergedOutputModel}, i.e. to the view.
   * <p>The default implementation wraps the original request for exposure of Framework beans
   * as request attributes (if demanded).
   *
   * @param original the original request as provided by the engine
   * @return the wrapped request, or the original request if no wrapping is necessary
   * @see #setExposeContextBeansAsAttributes
   * @see #setExposedContextBeanNames
   */
  protected RequestContext getRequestContextToExpose(RequestContext original) {
    if (this.exposeContextBeansAsAttributes || this.exposedContextBeanNames != null) {
      WebApplicationContext wac = getWebApplicationContext();
      Assert.state(wac != null, "No WebApplicationContext");
      return new ContextExposingRequestContext(original, wac, this.exposedContextBeanNames);
    }
    return original;
  }

  /**
   * Subclasses must implement this method to actually render the view.
   * <p>The first step will be preparing the request: In the JSP case,
   * this would mean setting model objects as request attributes.
   * The second step will be the actual rendering of the view,
   * for example including the JSP via a RequestDispatcher.
   *
   * @param model combined output Map (never {@code null}),
   * with dynamic values taking precedence over static attributes
   * @param context current HTTP request context
   * @throws Exception if rendering failed
   */
  protected abstract void renderMergedOutputModel(
          Map<String, Object> model, RequestContext context) throws Exception;

  /**
   * Expose the model objects in the given map as request attributes.
   * Names will be taken from the model Map.
   * This method is suitable for all resources reachable by {@link jakarta.servlet.RequestDispatcher}.
   *
   * @param model a Map of model objects to expose
   * @param request current HTTP request
   */
  protected void exposeModelAsRequestAttributes(
          Map<String, Object> model, RequestContext request) throws Exception {

    for (Map.Entry<String, Object> entry : model.entrySet()) {
      String name = entry.getKey();
      Object value = entry.getValue();
      if (value != null) {
        request.setAttribute(name, value);
      }
      else {
        request.removeAttribute(name);
      }
    }
  }

  /**
   * Create a temporary OutputStream for this view.
   * <p>This is typically used as IE workaround, for setting the content length header
   * from the temporary stream before actually writing the content to the HTTP response.
   */
  protected ByteArrayOutputStream createTemporaryOutputStream() {
    return new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
  }

  /**
   * Write the given temporary OutputStream to the HTTP response.
   *
   * @param response current HTTP response
   * @param baos the temporary OutputStream to write
   * @throws IOException if writing/flushing failed
   */
  protected void writeToResponse(RequestContext response, ByteArrayOutputStream baos) throws IOException {
    // Write content type and also length (determined via byte array).
    response.setContentType(getContentType());
    response.setContentLength(baos.size());

    // Flush byte array to servlet output stream.
    OutputStream out = response.getOutputStream();
    baos.writeTo(out);
    out.flush();
  }

  /**
   * Set the content type of the response to the configured
   * {@link #setContentType(String) content type} unless the
   * {@link View#SELECTED_CONTENT_TYPE} request attribute is present and set
   * to a concrete media type.
   */
  protected void setResponseContentType(RequestContext context) {
    MediaType mediaType = (MediaType) context.getAttribute(View.SELECTED_CONTENT_TYPE);
    if (mediaType != null && mediaType.isConcrete()) {
      context.setContentType(mediaType.toString());
    }
    else {
      context.setContentType(getContentType());
    }
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + formatViewName();
  }

  protected String formatViewName() {
    return getBeanName() != null
           ? "name '" + getBeanName() + "'"
           : "[" + getClass().getSimpleName() + "]";
  }

}
