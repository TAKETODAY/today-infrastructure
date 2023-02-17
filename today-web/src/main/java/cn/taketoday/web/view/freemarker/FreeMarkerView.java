/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.view.AbstractTemplateView;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * View using the FreeMarker template engine.
 *
 * <p>Exposes the following JavaBean properties:
 * <ul>
 * <li><b>url</b>: the location of the FreeMarker template to be wrapped,
 * relative to the FreeMarker template context (directory).
 * <li><b>encoding</b> (optional, default is determined by FreeMarker configuration):
 * the encoding of the FreeMarker template file
 * </ul>
 *
 * <p>Depends on a single {@link FreeMarkerConfig} object such as {@link FreeMarkerConfigurer}
 * being accessible in the current web application context, with any bean name.
 * Alternatively, you can set the FreeMarker {@link Configuration} object as a
 * bean property. See {@link #setConfiguration} for more details on the impacts
 * of this approach.
 *
 * <p>Note: Infra FreeMarker support requires FreeMarker 2.3 or higher.
 *  FreeMarker templates are rendered in a minimal
 * fashion without JSP support, just exposing request attributes in addition
 * to the MVC-provided model map for alignment with common Servlet resources.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @see #setUrl
 * @see #setEncoding
 * @see #setConfiguration
 * @see FreeMarkerConfig
 * @see FreeMarkerConfigurer
 * @since 4.0
 */
public class FreeMarkerView extends AbstractTemplateView {

  @Nullable
  private String encoding;

  @Nullable
  private Configuration configuration;

  /**
   * Set the encoding of the FreeMarker template file. Default is determined
   * by the FreeMarker Configuration: "UTF-8" if not specified otherwise.
   * <p>Specify the encoding in the FreeMarker Configuration rather than per
   * template if all your templates share a common encoding.
   */
  public void setEncoding(@Nullable String encoding) {
    this.encoding = encoding;
  }

  /**
   * Return the encoding for the FreeMarker template.
   */
  @Nullable
  protected String getEncoding() {
    return this.encoding;
  }

  /**
   * Set the FreeMarker Configuration to be used by this view.
   * <p>If this is not set, the default lookup will occur: a single {@link FreeMarkerConfig}
   * is expected in the current web application context, with any bean name.
   */
  public void setConfiguration(@Nullable Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Return the FreeMarker configuration used by this view.
   */
  @Nullable
  protected Configuration getConfiguration() {
    return this.configuration;
  }

  /**
   * Obtain the FreeMarker configuration for actual use.
   *
   * @return the FreeMarker configuration (never {@code null})
   * @throws IllegalStateException in case of no Configuration object set
   */
  protected Configuration obtainConfiguration() {
    Configuration configuration = getConfiguration();
    Assert.state(configuration != null, "No Configuration set");
    return configuration;
  }

  /**
   * Invoked on startup. Looks for a single FreeMarkerConfig bean to
   * find the relevant Configuration for this factory.
   * <p>Checks that the template for the default Locale can be found:
   * FreeMarker will check non-Locale-specific templates if a
   * locale-specific one is not found.
   *
   * @see freemarker.cache.TemplateCache#getTemplate
   */
  @Override
  protected void initApplicationContext() {
    if (getConfiguration() == null) {
      FreeMarkerConfig config = autodetectConfiguration();
      setConfiguration(config.getConfiguration());
    }
  }

  /**
   * Autodetect a {@link FreeMarkerConfig} object via the ApplicationContext.
   *
   * @return the Configuration instance to use for FreeMarkerViews
   * @throws BeansException if no Configuration instance could be found
   * @see #getApplicationContext
   * @see #setConfiguration
   */
  protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
    try {
      return BeanFactoryUtils.beanOfTypeIncludingAncestors(
              obtainApplicationContext(), FreeMarkerConfig.class, true, false);
    }
    catch (NoSuchBeanDefinitionException ex) {
      throw new ApplicationContextException(
              "Must define a single FreeMarkerConfig bean in this web application context " +
                      "(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
                      "This bean may be given any name.", ex);
    }
  }

  /**
   * Return the configured FreeMarker {@link ObjectWrapper}, or the
   * {@link ObjectWrapper#DEFAULT_WRAPPER default wrapper} if none specified.
   *
   * @see Configuration#getObjectWrapper()
   */
  protected ObjectWrapper getObjectWrapper() {
    ObjectWrapper ow = obtainConfiguration().getObjectWrapper();
    return ow != null ? ow :
           new DefaultObjectWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build();
  }

  /**
   * Check that the FreeMarker template used for this view exists and is valid.
   * <p>Can be overridden to customize the behavior, for example in case of
   * multiple templates to be rendered into a single view.
   */
  @Override
  public boolean checkResource(Locale locale) throws Exception {
    String url = getUrl();
    Assert.state(url != null, "'url' not set");

    try {
      // Check that we can get the template, even if we might subsequently get it again.
      getTemplate(url, locale);
      return true;
    }
    catch (FileNotFoundException ex) {
      // Allow for ViewResolver chaining...
      return false;
    }
    catch (ParseException ex) {
      throw new ApplicationContextException("Failed to parse [" + url + "]", ex);
    }
    catch (IOException ex) {
      throw new ApplicationContextException("Failed to load [" + url + "]", ex);
    }
  }

  /**
   * Process the model map by merging it with the FreeMarker template.
   * Output is directed to the servlet response.
   * <p>This method can be overridden if custom behavior is needed.
   */
  @Override
  protected void renderMergedTemplateModel(
          Map<String, Object> model, RequestContext context) throws Exception {

    exposeHelpers(model, context);
    doRender(model, context);
  }

  /**
   * Expose helpers unique to each rendering operation. This is necessary so that
   * different rendering operations can't overwrite each other's formats etc.
   * <p>Called by {@code renderMergedTemplateModel}. The default implementation
   * is empty. This method can be overridden to add custom helpers to the model.
   *
   * @param model the model that will be passed to the template at merge time
   * @param request current HTTP request
   * @throws Exception if there's a fatal error while we're adding information to the context
   * @see #renderMergedTemplateModel
   */
  protected void exposeHelpers(Map<String, Object> model, RequestContext request) throws Exception { }

  /**
   * Render the FreeMarker view to the given response, using the given model
   * map which contains the complete template model to use.
   * <p>The default implementation renders the template specified by the "url"
   * bean property, retrieved via {@code getTemplate}. It delegates to the
   * {@code processTemplate} method to merge the template instance with
   * the given template model.
   * <p>Adds the standard Freemarker hash models to the model: request parameters,
   * request, session and application (ServletContext), as well as the JSP tag
   * library hash model.
   * <p>Can be overridden to customize the behavior, for example to render
   * multiple templates into a single view.
   *
   * @param model the model to use for rendering
   * @param context current HTTP request context
   * @throws IOException if the template file could not be retrieved
   * @throws Exception if rendering failed
   * @see #setUrl
   * @see cn.taketoday.web.RequestContextUtils#getLocale
   * @see #getTemplate(Locale)
   * @see #processTemplate
   * @see freemarker.ext.servlet.FreemarkerServlet
   */
  protected void doRender(Map<String, Object> model, RequestContext context) throws Exception {
    // Expose model to JSP tags (as request attributes).
    exposeModelAsRequestAttributes(model, context);
    // Expose FreeMarker hash model.
    SimpleHash fmModel = buildTemplateModel(model, context);

    // Grab the locale-specific version of the template.
    Locale locale = RequestContextUtils.getLocale(context);
    processTemplate(getTemplate(locale), fmModel, context);
  }

  /**
   * Build a FreeMarker template model for the given model Map.
   * <p>The default implementation builds a {@link SimpleHash} for the
   * given MVC model with an additional fallback to request attributes.
   *
   * @param model the model to use for rendering
   * @param request current HTTP request
   * @return the FreeMarker template model, as a {@link SimpleHash} or subclass thereof
   */
  protected SimpleHash buildTemplateModel(Map<String, Object> model, RequestContext request) {

    SimpleHash fmModel = new RequestHashModel(getObjectWrapper(), request);
    fmModel.putAll(model);
    return fmModel;
  }

  /**
   * Retrieve the FreeMarker template for the given locale,
   * to be rendering by this view.
   * <p>By default, the template specified by the "url" bean property
   * will be retrieved.
   *
   * @param locale the current locale
   * @return the FreeMarker template to render
   * @throws IOException if the template file could not be retrieved
   * @see #setUrl
   * @see #getTemplate(String, Locale)
   */
  protected Template getTemplate(Locale locale) throws IOException {
    String url = getUrl();
    Assert.state(url != null, "'url' not set");
    return getTemplate(url, locale);
  }

  /**
   * Retrieve the FreeMarker template specified by the given name,
   * using the encoding specified by the "encoding" bean property.
   * <p>Can be called by subclasses to retrieve a specific template,
   * for example to render multiple templates into a single view.
   *
   * @param name the file name of the desired template
   * @param locale the current locale
   * @return the FreeMarker template
   * @throws IOException if the template file could not be retrieved
   */
  protected Template getTemplate(String name, Locale locale) throws IOException {
    String encoding = getEncoding();
    return encoding != null
           ? obtainConfiguration().getTemplate(name, locale, encoding)
           : obtainConfiguration().getTemplate(name, locale);
  }

  /**
   * Process the FreeMarker template to the servlet response.
   * <p>Can be overridden to customize the behavior.
   *
   * @param template the template to process
   * @param model the model for the template
   * @param response servlet response (use this to get the OutputStream or Writer)
   * @throws IOException if the template file could not be retrieved
   * @throws TemplateException if thrown by FreeMarker
   * @see Template#process(Object, java.io.Writer)
   */
  protected void processTemplate(Template template, SimpleHash model, RequestContext response)
          throws IOException, TemplateException {
    template.process(model, response.getWriter());
  }

  /**
   * Extension of FreeMarker {@link SimpleHash}, adding a fallback to request attributes.
   * Similar to the formerly used {@link freemarker.ext.servlet.AllHttpScopesHashModel},
   * just limited to common request attribute exposure.
   */
  private static class RequestHashModel extends SimpleHash {

    private final RequestContext request;

    public RequestHashModel(ObjectWrapper wrapper, RequestContext request) {
      super(wrapper);
      this.request = request;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
      TemplateModel model = super.get(key);
      if (model != null) {
        return model;
      }
      Object obj = this.request.getAttribute(key);
      if (obj != null) {
        return wrap(obj);
      }
      return wrap(null);
    }
  }
}
