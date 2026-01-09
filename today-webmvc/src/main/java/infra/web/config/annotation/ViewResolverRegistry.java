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

package infra.web.config.annotation;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.BeanInitializationException;
import infra.context.ApplicationContext;
import infra.core.Ordered;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.web.accept.ContentNegotiationManager;
import infra.web.view.BeanNameViewResolver;
import infra.web.view.ContentNegotiatingViewResolver;
import infra.web.view.View;
import infra.web.view.ViewResolver;
import infra.web.view.freemarker.FreeMarkerConfigurer;
import infra.web.view.freemarker.FreeMarkerViewResolver;
import infra.web.view.groovy.GroovyMarkupConfigurer;
import infra.web.view.groovy.GroovyMarkupViewResolver;
import infra.web.view.script.ScriptTemplateConfigurer;
import infra.web.view.script.ScriptTemplateViewResolver;

/**
 * Assist with the configuration of a chain of
 * {@link infra.web.view.ViewResolver ViewResolver} instances.
 * This class is expected to be used via {@link WebMvcConfigurer#configureViewResolvers}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 15:42
 */
public class ViewResolverRegistry {

  private final @Nullable ApplicationContext applicationContext;

  private final @Nullable ContentNegotiationManager contentNegotiationManager;

  private final ArrayList<ViewResolver> viewResolvers = new ArrayList<>(4);

  private @Nullable ContentNegotiatingViewResolver contentNegotiatingResolver;

  private @Nullable Integer order;

  /**
   * Class constructor with {@link ContentNegotiationManager} and {@link ApplicationContext}.
   */
  public ViewResolverRegistry(@Nullable ContentNegotiationManager contentNegotiationManager,
          @Nullable ApplicationContext context) {
    this.applicationContext = context;
    this.contentNegotiationManager = contentNegotiationManager;
  }

  /**
   * Whether any view resolvers have been registered.
   */
  public boolean hasRegistrations() {
    return contentNegotiatingResolver != null || !viewResolvers.isEmpty();
  }

  /**
   * Enable use of a {@link ContentNegotiatingViewResolver} to front all other
   * configured view resolvers and select among all selected Views based on
   * media types requested by the client (e.g. in the Accept header).
   * <p>If invoked multiple times the provided default views will be added to
   * any other default views that may have been configured already.
   *
   * @see ContentNegotiatingViewResolver#setDefaultViews
   */
  public void enableContentNegotiation(View... defaultViews) {
    initContentNegotiatingViewResolver(defaultViews);
  }

  /**
   * Enable use of a {@link ContentNegotiatingViewResolver} to front all other
   * configured view resolvers and select among all selected Views based on
   * media types requested by the client (e.g. in the Accept header).
   * <p>If invoked multiple times the provided default views will be added to
   * any other default views that may have been configured already.
   *
   * @see ContentNegotiatingViewResolver#setDefaultViews
   */
  public void enableContentNegotiation(boolean useNotAcceptableStatus, View... defaultViews) {
    ContentNegotiatingViewResolver vr = initContentNegotiatingViewResolver(defaultViews);
    vr.setUseNotAcceptableStatusCode(useNotAcceptableStatus);
  }

  private ContentNegotiatingViewResolver initContentNegotiatingViewResolver(View[] defaultViews) {
    // ContentNegotiatingResolver in the registry: elevate its precedence!
    this.order = order != null ? order : Ordered.HIGHEST_PRECEDENCE;

    if (contentNegotiatingResolver != null) {
      if (ObjectUtils.isNotEmpty(defaultViews)
              && CollectionUtils.isNotEmpty(contentNegotiatingResolver.getDefaultViews())) {
        ArrayList<View> views = new ArrayList<>(contentNegotiatingResolver.getDefaultViews());
        CollectionUtils.addAll(views, defaultViews);
        contentNegotiatingResolver.setDefaultViews(views);
      }
    }
    else {
      this.contentNegotiatingResolver = new ContentNegotiatingViewResolver();
      contentNegotiatingResolver.setDefaultViews(Arrays.asList(defaultViews));
      contentNegotiatingResolver.setViewResolvers(viewResolvers);
      if (contentNegotiationManager != null) {
        contentNegotiatingResolver.setContentNegotiationManager(contentNegotiationManager);
      }
    }
    return contentNegotiatingResolver;
  }

  /**
   * Register a FreeMarker view resolver with an empty default view name
   * prefix and a default suffix of ".ftl".
   * <p><strong>Note</strong> that you must also configure FreeMarker by adding a
   * {@link infra.web.view.freemarker.FreeMarkerConfigurer} bean.
   */
  public UrlBasedViewResolverRegistration freeMarker() {
    if (notFoundBeanOfType(FreeMarkerConfigurer.class)) {
      throw new BeanInitializationException("In addition to a FreeMarker view resolver " +
              "there must also be a single FreeMarkerConfig bean in this web application context " +
              "(or its parent): FreeMarkerConfigurer is the usual implementation. " +
              "This bean may be given any name.");
    }
    FreeMarkerRegistration registration = new FreeMarkerRegistration();
    viewResolvers.add(registration.getViewResolver());
    return registration;
  }

  /**
   * Register a Groovy markup view resolver with an empty default view name
   * prefix and a default suffix of ".tpl".
   */
  public UrlBasedViewResolverRegistration groovy() {
    if (notFoundBeanOfType(GroovyMarkupConfigurer.class)) {
      throw new BeanInitializationException("In addition to a Groovy markup view resolver " +
              "there must also be a single GroovyMarkupConfig bean in this web application context " +
              "(or its parent): GroovyMarkupConfigurer is the usual implementation. " +
              "This bean may be given any name.");
    }
    GroovyMarkupRegistration registration = new GroovyMarkupRegistration();
    viewResolvers.add(registration.getViewResolver());
    return registration;
  }

  /**
   * Register a script template view resolver with an empty default view name prefix and suffix.
   */
  public UrlBasedViewResolverRegistration scriptTemplate() {
    if (notFoundBeanOfType(ScriptTemplateConfigurer.class)) {
      throw new BeanInitializationException("In addition to a script template view resolver " +
              "there must also be a single ScriptTemplateConfig bean in this web application context " +
              "(or its parent): ScriptTemplateConfigurer is the usual implementation. " +
              "This bean may be given any name.");
    }
    ScriptRegistration registration = new ScriptRegistration();
    viewResolvers.add(registration.getViewResolver());
    return registration;
  }

  /**
   * Register a bean name view resolver that interprets view names as the names
   * of {@link infra.web.view.View} beans.
   */
  public void beanName() {
    BeanNameViewResolver resolver = new BeanNameViewResolver();
    viewResolvers.add(resolver);
  }

  /**
   * Register a {@link ViewResolver} bean instance. This may be useful to
   * configure a custom (or 3rd party) resolver implementation. It may also be
   * used as an alternative to other registration methods in this class when
   * they don't expose some more advanced property that needs to be set.
   */
  public void viewResolver(ViewResolver viewResolver) {
    if (viewResolver instanceof ContentNegotiatingViewResolver) {
      throw new BeanInitializationException(
              "addViewResolver cannot be used to configure a ContentNegotiatingViewResolver. " +
                      "Please use the method enableContentNegotiation instead.");
    }
    viewResolvers.add(viewResolver);
  }

  /**
   * ViewResolver's registered through this registry are encapsulated in an
   * instance of {@link infra.web.view.ViewResolverComposite
   * ViewResolverComposite} and follow the order of registration.
   * This property determines the order of the ViewResolverComposite itself
   * relative to any additional ViewResolver's (not registered here) present in
   * the Framework configuration
   * <p>By default this property is not set, which means the resolver is ordered
   * at {@link Ordered#LOWEST_PRECEDENCE} unless content negotiation is enabled
   * in which case the order (if not set explicitly) is changed to
   * {@link Ordered#HIGHEST_PRECEDENCE}.
   */
  public void order(int order) {
    this.order = order;
  }

  boolean notFoundBeanOfType(Class<?> beanType) {
    return this.applicationContext != null
            && ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
            this.applicationContext, beanType, false, false));
  }

  protected int getOrder() {
    return this.order != null ? this.order : Ordered.LOWEST_PRECEDENCE;
  }

  protected List<ViewResolver> getViewResolvers() {
    if (this.contentNegotiatingResolver != null) {
      return Collections.singletonList(this.contentNegotiatingResolver);
    }
    else {
      return this.viewResolvers;
    }
  }

  private static class FreeMarkerRegistration extends UrlBasedViewResolverRegistration {

    public FreeMarkerRegistration() {
      super(new FreeMarkerViewResolver());
      getViewResolver().setSuffix(".ftl");
    }
  }

  private static class GroovyMarkupRegistration extends UrlBasedViewResolverRegistration {

    public GroovyMarkupRegistration() {
      super(new GroovyMarkupViewResolver());
      getViewResolver().setSuffix(".tpl");
    }
  }

  private static class ScriptRegistration extends UrlBasedViewResolverRegistration {

    public ScriptRegistration() {
      super(new ScriptTemplateViewResolver());
      getViewResolver();
    }
  }

}
