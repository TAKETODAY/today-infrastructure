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

package cn.taketoday.web.config;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 15:42
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.view.BeanNameViewResolver;
import cn.taketoday.web.view.ContentNegotiatingViewResolver;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewResolver;
import cn.taketoday.web.view.freemarker.FreeMarkerConfigurer;
import cn.taketoday.web.view.freemarker.FreeMarkerViewResolver;
import cn.taketoday.web.view.groovy.GroovyMarkupConfigurer;
import cn.taketoday.web.view.groovy.GroovyMarkupViewResolver;
import cn.taketoday.web.view.script.ScriptTemplateConfigurer;
import cn.taketoday.web.view.script.ScriptTemplateViewResolver;

/**
 * Assist with the configuration of a chain of
 * {@link cn.taketoday.web.view.ViewResolver ViewResolver} instances.
 * This class is expected to be used via {@link WebMvcConfiguration#configureViewResolvers}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ViewResolverRegistry {

  @Nullable
  private final ContentNegotiationManager contentNegotiationManager;

  @Nullable
  private final ApplicationContext applicationContext;

  @Nullable
  private ContentNegotiatingViewResolver contentNegotiatingResolver;

  private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

  @Nullable
  private Integer order;

  /**
   * Class constructor with {@link ContentNegotiationManager} and {@link ApplicationContext}.
   */
  public ViewResolverRegistry(
          @Nullable ContentNegotiationManager contentNegotiationManager, @Nullable ApplicationContext context) {
    this.applicationContext = context;
    this.contentNegotiationManager = contentNegotiationManager;
  }

  /**
   * Whether any view resolvers have been registered.
   */
  public boolean hasRegistrations() {
    return (this.contentNegotiatingResolver != null || !this.viewResolvers.isEmpty());
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
    this.order = this.order != null ? this.order : Ordered.HIGHEST_PRECEDENCE;

    if (this.contentNegotiatingResolver != null) {
      if (ObjectUtils.isNotEmpty(defaultViews)
              && CollectionUtils.isNotEmpty(this.contentNegotiatingResolver.getDefaultViews())) {
        ArrayList<View> views = new ArrayList<>(this.contentNegotiatingResolver.getDefaultViews());
        CollectionUtils.addAll(views, defaultViews);
        this.contentNegotiatingResolver.setDefaultViews(views);
      }
    }
    else {
      this.contentNegotiatingResolver = new ContentNegotiatingViewResolver();
      this.contentNegotiatingResolver.setDefaultViews(Arrays.asList(defaultViews));
      this.contentNegotiatingResolver.setViewResolvers(this.viewResolvers);
      if (this.contentNegotiationManager != null) {
        this.contentNegotiatingResolver.setContentNegotiationManager(this.contentNegotiationManager);
      }
    }
    return this.contentNegotiatingResolver;
  }

  /**
   * Register JSP view resolver using a default view name prefix of "/WEB-INF/"
   * and a default suffix of ".jsp".
   * <p>When this method is invoked more than once, each call will register a
   * new ViewResolver instance. Note that since it's not easy to determine
   * if a JSP exists without forwarding to it, using multiple JSP-based view
   * resolvers only makes sense in combination with the "viewNames" property
   * on the resolver indicating which view names are handled by which resolver.
   */
  public UrlBasedViewResolverRegistration jsp() {
    return jsp("/WEB-INF/", ".jsp");
  }

  /**
   * Register JSP view resolver with the specified prefix and suffix.
   * <p>When this method is invoked more than once, each call will register a
   * new ViewResolver instance. Note that since it's not easy to determine
   * if a JSP exists without forwarding to it, using multiple JSP-based view
   * resolvers only makes sense in combination with the "viewNames" property
   * on the resolver indicating which view names are handled by which resolver.
   */
  public UrlBasedViewResolverRegistration jsp(String prefix, String suffix) {
    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
    resolver.setPrefix(prefix);
    resolver.setSuffix(suffix);
    this.viewResolvers.add(resolver);
    return new UrlBasedViewResolverRegistration(resolver);
  }

  /**
   * Register a FreeMarker view resolver with an empty default view name
   * prefix and a default suffix of ".ftl".
   * <p><strong>Note</strong> that you must also configure FreeMarker by adding a
   * {@link cn.taketoday.web.view.freemarker.FreeMarkerConfigurer} bean.
   */
  public UrlBasedViewResolverRegistration freeMarker() {
    if (notFoundBeanOfType(FreeMarkerConfigurer.class)) {
      throw new BeanInitializationException("In addition to a FreeMarker view resolver " +
              "there must also be a single FreeMarkerConfig bean in this web application context " +
              "(or its parent): FreeMarkerConfigurer is the usual implementation. " +
              "This bean may be given any name.");
    }
    FreeMarkerRegistration registration = new FreeMarkerRegistration();
    this.viewResolvers.add(registration.getViewResolver());
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
    this.viewResolvers.add(registration.getViewResolver());
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
    this.viewResolvers.add(registration.getViewResolver());
    return registration;
  }

  /**
   * Register a bean name view resolver that interprets view names as the names
   * of {@link cn.taketoday.web.view.View} beans.
   */
  public void beanName() {
    BeanNameViewResolver resolver = new BeanNameViewResolver();
    this.viewResolvers.add(resolver);
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
    this.viewResolvers.add(viewResolver);
  }

  /**
   * ViewResolver's registered through this registry are encapsulated in an
   * instance of {@link cn.taketoday.web.view.ViewResolverComposite
   * ViewResolverComposite} and follow the order of registration.
   * This property determines the order of the ViewResolverComposite itself
   * relative to any additional ViewResolver's (not registered here) present in
   * the Spring configuration
   * <p>By default this property is not set, which means the resolver is ordered
   * at {@link Ordered#LOWEST_PRECEDENCE} unless content negotiation is enabled
   * in which case the order (if not set explicitly) is changed to
   * {@link Ordered#HIGHEST_PRECEDENCE}.
   */
  public void order(int order) {
    this.order = order;
  }

  private boolean notFoundBeanOfType(Class<?> beanType) {
    return this.applicationContext != null
            && CollectionUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
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
