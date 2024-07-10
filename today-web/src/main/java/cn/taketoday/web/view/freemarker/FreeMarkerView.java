/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.Charset;
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
import freemarker.core.Environment;
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
 * <p>Exposes the following configuration properties:
 * <ul>
 * <li><b>{@link #setUrl(String) url}</b>: the location of the FreeMarker template
 * relative to the FreeMarker template context (directory).</li>
 * <li><b>{@link #setEncoding(String) encoding}</b>: the encoding used to decode
 * byte sequences to character sequences when reading the FreeMarker template file.
 * Default is determined by the FreeMarker {@link Configuration}.</li>
 * <li><b>{@link #setContentType(String) contentType}</b>: the content type of the
 * rendered response. Defaults to {@code "text/html;charset=ISO-8859-1"} but may
 * need to be set to a value that corresponds to the actual generated content
 * type (see note below).</li>
 * </ul>
 *
 * <p>Depends on a single {@link FreeMarkerConfig} object such as
 * {@link FreeMarkerConfigurer} being accessible in the current web application
 * context. Alternatively the FreeMarker {@link Configuration} can be set directly
 * via {@link #setConfiguration}.
 *
 * <p><b>Note:</b> To ensure that the correct encoding is used when rendering the
 * response, set the {@linkplain #setContentType(String) content type} with an
 * appropriate {@code charset} attribute &mdash; for example,
 * {@code "text/html;charset=UTF-8"}. When using {@link FreeMarkerViewResolver}
 * to create the view for you, set the
 * {@linkplain FreeMarkerViewResolver#setContentType(String) content type}
 * directly in the {@code FreeMarkerViewResolver}; however, as of Infra 5.0,
 * it is no longer necessary to explicitly set the content type in the
 * {@code FreeMarkerViewResolver} if you have set an explicit encoding via either
 * {@link #setEncoding(String)}, {@link FreeMarkerConfigurer#setDefaultEncoding(String)},
 * or {@link Configuration#setDefaultEncoding(String)}.
 *
 * <p>Note: Infra FreeMarker support requires FreeMarker 2.3.26 or higher.
 * As of Infra 5.0, FreeMarker templates are rendered in a minimal
 * fashion without JSP support, just exposing request attributes in addition
 * to the MVC-provided model map for alignment with common Servlet resources.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
   * Set the encoding used to decode byte sequences to character sequences when
   * reading the FreeMarker template file for this view.
   * <p>Defaults to {@code null} to signal that the FreeMarker
   * {@link Configuration} should be used to determine the encoding.
   * <p>A non-null encoding will override the default encoding determined by
   * the FreeMarker {@code Configuration}.
   * <p>If the encoding is not explicitly set here or in the FreeMarker
   * {@code Configuration}, FreeMarker will read template files using the platform
   * file encoding (defined by the JVM system property {@code file.encoding})
   * or UTF-8 if the platform file encoding is undefined.
   * <p>It's recommended to specify the encoding in the FreeMarker {@code Configuration}
   * rather than per template if all your templates share a common encoding.
   * <p>See the note in the {@linkplain FreeMarkerView class-level documentation}
   * for details regarding the encoding used to render the response.
   *
   * @see freemarker.template.Configuration#setDefaultEncoding
   * @see #setCharset(Charset)
   * @see #getEncoding()
   * @see #setContentType(String)
   */
  public void setEncoding(@Nullable String encoding) {
    this.encoding = encoding;
  }

  /**
   * Set the {@link Charset} used to decode byte sequences to character sequences
   * when reading the FreeMarker template file for this view.
   * <p>See {@link #setEncoding(String)} for details.
   *
   * @see java.nio.charset.StandardCharsets
   * @since 5.0
   */
  public void setCharset(@Nullable Charset charset) {
    this.encoding = (charset != null ? charset.name() : null);
  }

  /**
   * Get the encoding used to decode byte sequences to character sequences
   * when reading the FreeMarker template file for this view, or {@code null}
   * to signal that the FreeMarker {@link Configuration} should be used to
   * determine the encoding.
   *
   * @see #setEncoding(String)
   */
  @Nullable
  protected String getEncoding() {
    return this.encoding;
  }

  /**
   * Set the FreeMarker {@link Configuration} to be used by this view.
   * <p>If not set, the default lookup will occur: a single {@link FreeMarkerConfig}
   * is expected in the current web application context, with any bean name.
   */
  public void setConfiguration(@Nullable Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Return the FreeMarker {@link Configuration} used by this view.
   */
  @Nullable
  protected Configuration getConfiguration() {
    return this.configuration;
  }

  /**
   * Obtain the FreeMarker {@link Configuration} for actual use.
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
   * Invoked on startup. Looks for a single {@link FreeMarkerConfig} bean to
   * find the relevant {@link Configuration} for this view.
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
   * Autodetect a {@link FreeMarkerConfig} object via the {@code ApplicationContext}.
   *
   * @return the {@code FreeMarkerConfig} instance to use for FreeMarkerViews
   * @throws BeansException if no {@link FreeMarkerConfig} bean could be found
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
   * {@linkplain ObjectWrapper#DEFAULT_WRAPPER default wrapper} if none specified.
   *
   * @see freemarker.template.Configuration#getObjectWrapper()
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
      throw new ApplicationContextException("Failed to parse [%s]".formatted(url), ex);
    }
    catch (IOException ex) {
      throw new ApplicationContextException("Failed to load [%s]".formatted(url), ex);
    }
  }

  /**
   * Process the model map by merging it with the FreeMarker template.
   * <p>Output is directed to the response.
   * <p>This method can be overridden if custom behavior is needed.
   */
  @Override
  protected void renderMergedTemplateModel(Map<String, Object> model, RequestContext context) throws Exception {
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
  protected void exposeHelpers(Map<String, Object> model, RequestContext request) throws Exception {

  }

  /**
   * Render the FreeMarker view to the given response, using the given model
   * map which contains the complete template model to use.
   * <p>The default implementation renders the template specified by the "url"
   * bean property, retrieved via {@code getTemplate}. It delegates to the
   * {@code processTemplate} method to merge the template instance with
   * the given template model.
   * <p>Can be overridden to customize the behavior, for example to render
   * multiple templates into a single view.
   *
   * @param model the model to use for rendering
   * @param context current HTTP request context
   * @throws IOException if the template file could not be retrieved
   * @throws Exception if rendering failed
   * @see #setUrl
   * @see #getTemplate(java.util.Locale)
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
   * Retrieve the FreeMarker {@link Template} to be rendered by this view, for
   * the specified locale and using the {@linkplain #setEncoding(String) configured
   * encoding} if set.
   * <p>By default, the template specified by the "url" bean property will be retrieved.
   *
   * @param locale the current locale
   * @return the FreeMarker {@code Template} to render
   * @throws IOException if the template file could not be retrieved
   * @see #setUrl
   * @see #getTemplate(String, java.util.Locale)
   */
  protected Template getTemplate(Locale locale) throws IOException {
    String url = getUrl();
    Assert.state(url != null, "'url' not set");
    return getTemplate(url, locale);
  }

  /**
   * Retrieve the FreeMarker {@link Template} to be rendered by this view, for
   * the specified name and locale and using the {@linkplain #setEncoding(String)
   * configured encoding} if set.
   * <p>Can be called by subclasses to retrieve a specific template,
   * for example to render multiple templates into a single view.
   *
   * @param name the file name of the desired template
   * @param locale the current locale
   * @return the FreeMarker template
   * @throws IOException if the template file could not be retrieved
   * @see #setEncoding(String)
   */
  protected Template getTemplate(String name, Locale locale) throws IOException {
    String encoding = getEncoding();
    return encoding != null
            ? obtainConfiguration().getTemplate(name, locale, encoding)
            : obtainConfiguration().getTemplate(name, locale);
  }

  /**
   * Process the FreeMarker template and write the result to the response.
   * <p>Can be overridden to customize the behavior.
   *
   * @param template the template to process
   * @param model the model for the template
   * @param response servlet response (use this to get the OutputStream or Writer)
   * @throws IOException if the template file could not be retrieved
   * @throws TemplateException if thrown by FreeMarker
   * @see freemarker.template.Template#createProcessingEnvironment(Object, java.io.Writer)
   * @see freemarker.core.Environment#process()
   */
  protected void processTemplate(Template template, SimpleHash model, RequestContext response)
          throws IOException, TemplateException {

    String encoding = getEncoding();
    if (encoding != null) {
      Environment env = template.createProcessingEnvironment(model, response.getWriter());
      env.setOutputEncoding(encoding);
      env.process();
    }
    else {
      template.process(model, response.getWriter());
    }
  }

  /**
   * Extension of FreeMarker {@link SimpleHash}, adding a fallback to request attributes.
   * Similar to the formerly used {@link freemarker.ext.servlet.AllHttpScopesHashModel},
   * just limited to common request attribute exposure.
   */
  private static class RequestHashModel extends SimpleHash {

    @Serial
    private static final long serialVersionUID = 1L;

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
