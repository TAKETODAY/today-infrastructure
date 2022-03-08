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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.ContentNegotiationManagerFactoryBean;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;

/**
 * Implementation of {@link ViewResolver} that resolves a view based on the request file name
 * or {@code Accept} header.
 *
 * <p>The {@code ContentNegotiatingViewResolver} does not resolve views itself, but delegates to
 * other {@link ViewResolver ViewResolvers}. By default, these other view resolvers are picked up automatically
 * from the application context, though they can also be set explicitly by using the
 * {@link #setViewResolvers viewResolvers} property. <strong>Note</strong> that in order for this
 * view resolver to work properly, the {@link #setOrder order} property needs to be set to a higher
 * precedence than the others (the default is {@link Ordered#HIGHEST_PRECEDENCE}).
 *
 * <p>This view resolver uses the requested {@linkplain MediaType media type} to select a suitable
 * {@link View} for a request. The requested media type is determined through the configured
 * {@link ContentNegotiationManager}. Once the requested media type has been determined, this resolver
 * queries each delegate view resolver for a {@link View} and determines if the requested media type
 * is {@linkplain MediaType#includes(MediaType) compatible} with the view's
 * {@linkplain View#getContentType() content type}). The most compatible view is returned.
 *
 * <p>Additionally, this view resolver exposes the {@link #setDefaultViews(List) defaultViews} property,
 * allowing you to override the views provided by the view resolvers. Note that these default views are
 * offered as candidates, and still need have the content type requested (via file extension, parameter,
 * or {@code Accept} header, described above).
 *
 * <p>For example, if the request path is {@code /view.html}, this view resolver will look for a view
 * that has the {@code text/html} content type (based on the {@code html} file extension). A request
 * for {@code /view} with a {@code text/html} request {@code Accept} header has the same result.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see ViewResolver
 * @see InternalResourceViewResolver
 * @see BeanNameViewResolver
 * @since 4.0
 */
public class ContentNegotiatingViewResolver
        extends ApplicationContextSupport implements ViewResolver, Ordered, InitializingBean {

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  private final ContentNegotiationManagerFactoryBean negotiationManagerFactoryBean = new ContentNegotiationManagerFactoryBean();

  private boolean useNotAcceptableStatusCode = false;

  @Nullable
  private List<View> defaultViews;

  @Nullable
  private List<ViewResolver> viewResolvers;

  private int order = Ordered.HIGHEST_PRECEDENCE;

  /**
   * Set the {@link cn.taketoday.web.accept.ContentNegotiationManager} to use to determine requested media types.
   * <p>If not set, ContentNegotiationManager's default constructor will be used,
   * applying a {@link cn.taketoday.web.accept.HeaderContentNegotiationStrategy}.
   *
   * @see cn.taketoday.web.accept.ContentNegotiationManager#ContentNegotiationManager()
   */
  public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager;
  }

  /**
   * Return the {@link cn.taketoday.web.accept.ContentNegotiationManager} to use to determine requested media types.
   */
  @Nullable
  public ContentNegotiationManager getContentNegotiationManager() {
    return this.contentNegotiationManager;
  }

  /**
   * Indicate whether a {@link jakarta.servlet.http.HttpServletResponse#SC_NOT_ACCEPTABLE 406 Not Acceptable}
   * status code should be returned if no suitable view can be found.
   * <p>Default is {@code false}, meaning that this view resolver returns {@code null} for
   * {@link #resolveViewName(String, Locale)} when an acceptable view cannot be found.
   * This will allow for view resolvers chaining. When this property is set to {@code true},
   * {@link #resolveViewName(String, Locale)} will respond with a view that sets the
   * response status to {@code 406 Not Acceptable} instead.
   */
  public void setUseNotAcceptableStatusCode(boolean useNotAcceptableStatusCode) {
    this.useNotAcceptableStatusCode = useNotAcceptableStatusCode;
  }

  /**
   * Whether to return HTTP Status 406 if no suitable is found.
   */
  public boolean isUseNotAcceptableStatusCode() {
    return this.useNotAcceptableStatusCode;
  }

  /**
   * Set the default views to use when a more specific view can not be obtained
   * from the {@link ViewResolver} chain.
   */
  public void setDefaultViews(@Nullable List<View> defaultViews) {
    this.defaultViews = defaultViews;
  }

  @Nullable
  public List<View> getDefaultViews() {
    return this.defaultViews;
  }

  /**
   * Sets the view resolvers to be wrapped by this view resolver.
   * <p>If this property is not set, view resolvers will be detected automatically.
   */
  public void setViewResolvers(@Nullable List<ViewResolver> viewResolvers) {
    this.viewResolvers = viewResolvers;
  }

  @Nullable
  public List<ViewResolver> getViewResolvers() {
    return viewResolvers;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  protected void initApplicationContext() {
    ApplicationContext context = obtainApplicationContext();
    Collection<ViewResolver> matchingBeans =
            BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class).values();
    if (viewResolvers == null) {
      this.viewResolvers = new ArrayList<>(matchingBeans.size());
      for (ViewResolver viewResolver : matchingBeans) {
        if (this != viewResolver) {
          viewResolvers.add(viewResolver);
        }
      }
    }
    else {
      AutowireCapableBeanFactory autowireCapableBeanFactory = context.getAutowireCapableBeanFactory();
      for (int i = 0; i < viewResolvers.size(); i++) {
        ViewResolver vr = viewResolvers.get(i);
        if (matchingBeans.contains(vr)) {
          continue;
        }
        String name = vr.getClass().getName() + i;
        autowireCapableBeanFactory.initializeBean(vr, name);
      }
    }
    AnnotationAwareOrderComparator.sort(this.viewResolvers);
  }

  @Override
  public void afterPropertiesSet() {
    if (this.contentNegotiationManager == null) {
      this.contentNegotiationManager = negotiationManagerFactoryBean.build();
    }
    if (CollectionUtils.isEmpty(viewResolvers)) {
      log.warn("No ViewResolvers configured");
    }
  }

  @Override
  @Nullable
  public View resolveViewName(String viewName, Locale locale) throws Exception {
    RequestContext context = RequestContextHolder.currentContext();
    List<MediaType> requestedMediaTypes = getMediaTypes(context);
    if (requestedMediaTypes != null) {
      List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);
      View bestView = getBestView(candidateViews, requestedMediaTypes, context);
      if (bestView != null) {
        return bestView;
      }
    }

    String mediaTypeInfo = log.isDebugEnabled() && requestedMediaTypes != null ? " given " + requestedMediaTypes : "";
    if (useNotAcceptableStatusCode) {
      if (log.isDebugEnabled()) {
        log.debug("Using 406 NOT_ACCEPTABLE {}", mediaTypeInfo);
      }
      return NOT_ACCEPTABLE_VIEW;
    }
    else {
      log.debug("View remains unresolved {}", mediaTypeInfo);
      return null;
    }
  }

  /**
   * Determines the list of {@link MediaType} for the given {@link jakarta.servlet.http.HttpServletRequest}.
   *
   * @param context the current request
   * @return the list of media types requested, if any
   */
  @Nullable
  protected List<MediaType> getMediaTypes(RequestContext context) {
    ContentNegotiationManager manager = getContentNegotiationManager();
    Assert.state(manager != null, "No ContentNegotiationManager set");
    try {
      List<MediaType> acceptableMediaTypes = manager.resolveMediaTypes(context);
      MediaType[] producibleMediaTypes = getProducibleMediaTypes(context);
      LinkedHashSet<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
      for (MediaType acceptable : acceptableMediaTypes) {
        for (MediaType producible : producibleMediaTypes) {
          if (acceptable.isCompatibleWith(producible)) {
            compatibleMediaTypes.add(getMostSpecificMediaType(acceptable, producible));
          }
        }
      }
      ArrayList<MediaType> selectedMediaTypes = new ArrayList<>(compatibleMediaTypes);
      MimeTypeUtils.sortBySpecificity(selectedMediaTypes);
      return selectedMediaTypes;
    }
    catch (HttpMediaTypeNotAcceptableException ex) {
      if (log.isDebugEnabled()) {
        log.debug(ex.getMessage());
      }
      return null;
    }
  }

  private MediaType[] getProducibleMediaTypes(RequestContext context) {
    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    if (matchingMetadata != null) {
      MediaType[] mediaTypes = matchingMetadata.getProducibleMediaTypes();
      if (ObjectUtils.isNotEmpty(mediaTypes)) {
        return mediaTypes;
      }
    }
    return new MediaType[] { MediaType.ALL };
  }

  /**
   * Return the more specific of the acceptable and the producible media types
   * with the q-value of the former.
   */
  private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
    produceType = produceType.copyQualityValue(acceptType);
    if (acceptType.isLessSpecific(produceType)) {
      return produceType;
    }
    else {
      return acceptType;
    }
  }

  private List<View> getCandidateViews(
          String viewName, Locale locale, List<MediaType> requestedMediaTypes) throws Exception {

    ArrayList<View> candidateViews = new ArrayList<>();
    List<ViewResolver> viewResolvers = this.viewResolvers;
    if (viewResolvers != null) {
      ContentNegotiationManager negotiationManager = getContentNegotiationManager();
      Assert.state(negotiationManager != null, "No ContentNegotiationManager set");
      for (ViewResolver viewResolver : viewResolvers) {
        View view = viewResolver.resolveViewName(viewName, locale);
        if (view != null) {
          candidateViews.add(view);
        }
        for (MediaType requestedMediaType : requestedMediaTypes) {
          List<String> extensions = negotiationManager.resolveFileExtensions(requestedMediaType);
          for (String extension : extensions) {
            String viewNameWithExtension = viewName + '.' + extension;
            view = viewResolver.resolveViewName(viewNameWithExtension, locale);
            if (view != null) {
              candidateViews.add(view);
            }
          }
        }
      }
    }
    if (CollectionUtils.isNotEmpty(defaultViews)) {
      candidateViews.addAll(defaultViews);
    }
    return candidateViews;
  }

  @Nullable
  private View getBestView(
          List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestContext context) {
    for (View candidateView : candidateViews) {
      if (candidateView instanceof SmartView smartView) {
        if (smartView.isRedirectView()) {
          return candidateView;
        }
      }
    }
    for (MediaType mediaType : requestedMediaTypes) {
      for (View candidateView : candidateViews) {
        if (StringUtils.hasText(candidateView.getContentType())) {
          MediaType candidateContentType = MediaType.parseMediaType(candidateView.getContentType());
          if (mediaType.isCompatibleWith(candidateContentType)) {
            mediaType = mediaType.removeQualityValue();
            if (log.isDebugEnabled()) {
              log.debug("Selected '{}' given {}", mediaType, requestedMediaTypes);
            }
            context.setAttribute(View.SELECTED_CONTENT_TYPE, mediaType);
            return candidateView;
          }
        }
      }
    }
    return null;
  }

  private static final View NOT_ACCEPTABLE_VIEW
          = (model, context) -> context.setStatus(HttpStatus.NOT_ACCEPTABLE);

}
