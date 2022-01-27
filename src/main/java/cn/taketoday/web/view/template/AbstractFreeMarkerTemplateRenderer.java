/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.view.template;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.SharedVariable;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.resolver.AnnotationParameterResolver;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.resolver.ParameterResolvingStrategies;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;
import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultMapAdapter;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.utility.ObjectWrapperWithAPISupport;

/**
 * Abstract FreeMarker template-renderer
 * <p>
 * this class registered FreemarkerConfigParameterResolver, SharedVariableParameterResolver
 * to supports freemarker SharedVariable and Configuration instance parameter resolving
 * </p>
 *
 * @author TODAY 2019-11-22 13:25
 * @see WebMvcConfiguration#configureParameterResolving(List)
 */
public abstract class AbstractFreeMarkerTemplateRenderer
        extends AbstractTemplateRenderer implements WebMvcConfiguration, InitializingBean {
  private static final Logger log = LoggerFactory.getLogger(AbstractFreeMarkerTemplateRenderer.class);

  public static final String KEY_REQUEST_PARAMETERS = "RequestParameters";

  protected int cacheSize = 1024;
  private ObjectWrapper objectWrapper;
  private Configuration configuration;

  // @since 4.0
  private ResourceLoader resourceLoader;

  public AbstractFreeMarkerTemplateRenderer() {
    super(LOWEST_PRECEDENCE - 100);
    setSuffix(".ftl");
  }

  @Override
  public void configureParameterResolving(
          ParameterResolvingRegistry registry, List<ParameterResolvingStrategy> customizedStrategies) {
    ParameterResolvingStrategies defaultStrategies = registry.getDefaultStrategies();
    defaultStrategies.add(new FreemarkerConfigParameterResolver());
    defaultStrategies.add(new SharedVariableParameterResolver());
  }

  @Override
  public <T> void configureTemplateLoader(List<T> loaders) {
    TemplateLoader loader = createTemplateLoader(loaders);
    getConfiguration().setTemplateLoader(loader);

    if (log.isInfoEnabled()) {
      log.info("FreeMarker use [{}] to load templates, prefix: [{}], suffix: [{}]", loader, prefix, suffix);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    configuration.setLocale(locale);
    configuration.setDefaultEncoding(encoding);
  }

  @SuppressWarnings("unchecked")
  protected <T> TemplateLoader createTemplateLoader(List<T> loaders) {
    if (CollectionUtils.isEmpty(loaders)) {
      DefaultResourceTemplateLoader templateLoader = new DefaultResourceTemplateLoader(prefix, suffix, cacheSize);
      templateLoader.setResourceLoader(getResourceLoader());
      return templateLoader;
    }
    return new CompositeTemplateLoader((Collection<TemplateLoader>) loaders, cacheSize);
  }

  /**
   * Create Model Attributes.
   *
   * @param context Current request context
   * @return {@link TemplateHashModel}
   */
  protected TemplateHashModel createModel(RequestContext context) {
    ObjectWrapper wrapper = getObjectWrapper();
    Map<String, Object> attributes = context.asMap();
    // Create hash model wrapper for request
    attributes.put(KEY_REQUEST_PARAMETERS, new RequestContextParametersHashModel(wrapper, context));
    return DefaultMapAdapter.adapt(attributes, (ObjectWrapperWithAPISupport) wrapper);
  }

  @Override
  public void render(String name, RequestContext context) throws IOException {
    Template template = getConfiguration().getTemplate(name, locale, encoding);
    TemplateHashModel model = createModel(context);

    // Give subclasses a chance to hook into preprocessing
    if (preTemplateProcess(template, model, context)) {
      try {
        // Process the template
        Environment env = template.createProcessingEnvironment(model, context.getWriter());
        env.setOutputEncoding(encoding);
        processEnvironment(env, context);
      }
      catch (TemplateException e) {
        throw new TemplateRenderingException("Freemarker template rendering failed", e);
      }
      finally {
        // Give subclasses a chance to hook into postprocessing
        postTemplateProcess(template, model, context);
      }
    }
  }

  /**
   * Called before the execution is passed to
   * {@link Template#process(Object, java.io.Writer)}. This is a generic hook you
   * might use in subclasses to perform a specific action before the template is
   * processed.
   *
   * @param context The HTTP response. The HTTP headers are already initialized here,
   * such as the {@code contentType} and the
   * {@code responseCharacterEncoding} are already set, but you can do
   * the final adjustments here. The response {@link Writer} isn't
   * created yet, so changing HTTP headers and buffering parameters
   * works.
   * @param template The template that will get executed
   * @param model The data model that will be passed to the template. By default
   * this will be an
   * {@link freemarker.ext.servlet.AllHttpScopesHashModel} (which is a
   * {@link freemarker.template.SimpleHash} subclass). Thus, you can
   * add new variables to the data-model with the
   * {@link freemarker.template.SimpleHash#put(String, Object)}
   * subclass) method. However, to adjust the data-model, overriding
   * {@link #createModel(RequestContext)} is probably a more
   * appropriate place.
   * @return true to process the template, false to suppress template processing.
   */
  protected boolean preTemplateProcess(
          Template template, TemplateModel model, RequestContext context) throws IOException {
    return true;
  }

  /**
   * This is the method that actually executes the template. The original
   * implementation coming from {@link freemarker.ext.servlet.FreemarkerServlet}
   * simply calls {@link Environment#process()}. Overriding this method allows you
   * to prepare the {@link Environment} before the execution, or extract
   * information from the {@link Environment} after the execution. It also allows
   * you to capture exceptions throw by the template.
   *
   * @param env The {@link Environment} object already set up to execute the
   * template. You only have to call {@link Environment#process()} and
   * the output will be produced by the template.
   * @since 2.3.7
   */
  protected void processEnvironment(
          Environment env, RequestContext context) throws TemplateException, IOException {
    env.process();
  }

  /**
   * Called after the execution returns from
   * {@link Template#process(Object, java.io.Writer)}. This is a generic hook you
   * might use in subclasses to perform a specific action after the template is
   * processed. It will be invoked even if the template processing throws an
   * exception. By default does nothing.
   *
   * @param context the actual HTTP request context
   * @param template the template that was executed
   * @param data the data that was passed to the template
   * @since 2.3.7
   */
  protected void postTemplateProcess(
          Template template, TemplateModel data, RequestContext context) throws IOException {

  }

  public int getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  public ObjectWrapper getObjectWrapper() {
    return objectWrapper;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setObjectWrapper(ObjectWrapper wrapper) {
    this.objectWrapper = wrapper;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  // @since 4.0
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  // @since 4.0
  public ResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  // ParameterResolver

  private final class SharedVariableParameterResolver extends AnnotationParameterResolver<SharedVariable> {
    SharedVariableParameterResolver() {
      super(SharedVariable.class);
    }

    @Override
    protected Object resolveInternal(SharedVariable target, RequestContext context, ResolvableMethodParameter parameter) {
      TemplateModel sharedVariable = getConfiguration().getSharedVariable(parameter.getName());

      if (parameter.isInstance(sharedVariable)) {
        return sharedVariable;
      }

      if (sharedVariable instanceof WrapperTemplateModel) {
        Object wrappedObject = ((WrapperTemplateModel) sharedVariable).getWrappedObject();
        if (parameter.isInstance(wrappedObject)) {
          return wrappedObject;
        }
        throw new IllegalStateException("Not a instance of: " + parameter.getParameterType());
      }
      if (parameter.isRequired()) {
        throw new IllegalStateException("There is no shared variable named: ".concat(parameter.getName()));
      }
      return DefaultConversionService.getSharedInstance()
              .convert(parameter.getDefaultValue(), parameter.getTypeDescriptor());
    }
  }

  private final class FreemarkerConfigParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      return resolvable.is(Configuration.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return getConfiguration();
    }
  }

}
