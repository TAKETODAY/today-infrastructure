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

package infra.web.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.support.ApplicationObjectSupport;
import infra.core.Ordered;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.MimeTypeUtils;
import infra.util.StringUtils;
import infra.web.HandlerMatchingMetadata;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.ContentNegotiationManagerFactoryBean;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ViewResolver
 * @see BeanNameViewResolver
 * @since 4.0
 */
public class ContentNegotiatingViewResolver extends ApplicationObjectSupport implements ViewResolver, Ordered, InitializingBean {

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  private boolean useNotAcceptableStatusCode = false;

  @Nullable
  private List<View> defaultViews;

  @Nullable
  private List<ViewResolver> viewResolvers;

  private int order = Ordered.HIGHEST_PRECEDENCE;

  /**
   * Set the {@link infra.web.accept.ContentNegotiationManager} to use to determine requested media types.
   * <p>If not set, ContentNegotiationManager's default constructor will be used,
   * applying a {@link infra.web.accept.HeaderContentNegotiationStrategy}.
   *
   * @see infra.web.accept.ContentNegotiationManager#ContentNegotiationManager()
   */
  public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager;
  }

  /**
   * Return the {@link infra.web.accept.ContentNegotiationManager} to use to determine requested media types.
   */
  @Nullable
  public ContentNegotiationManager getContentNegotiationManager() {
    return this.contentNegotiationManager;
  }

  /**
   * Indicate whether a {@link HttpStatus#NOT_ACCEPTABLE 406 Not Acceptable}
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
      this.contentNegotiationManager = new ContentNegotiationManagerFactoryBean().build();
    }
    if (CollectionUtils.isEmpty(viewResolvers)) {
      logger.warn("No ViewResolvers configured");
    }
  }

  @Override
  @Nullable
  public View resolveViewName(String viewName, Locale locale) throws Exception {
    RequestContext context = RequestContextHolder.getRequired();
    List<MediaType> requestedMediaTypes = getMediaTypes(context);
    if (requestedMediaTypes != null) {
      List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);
      View bestView = getBestView(candidateViews, requestedMediaTypes, context);
      if (bestView != null) {
        return bestView;
      }
    }

    String mediaTypeInfo = logger.isDebugEnabled() && requestedMediaTypes != null ? " given " + requestedMediaTypes : "";
    if (useNotAcceptableStatusCode) {
      if (logger.isDebugEnabled()) {
        logger.debug("Using 406 NOT_ACCEPTABLE {}", mediaTypeInfo);
      }
      return NOT_ACCEPTABLE_VIEW;
    }
    else {
      logger.debug("View remains unresolved {}", mediaTypeInfo);
      return null;
    }
  }

  /**
   * Determines the list of {@link MediaType} for the given {@link RequestContext}.
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
      Collection<MediaType> producibleMediaTypes = getProducibleMediaTypes(context);
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
      if (logger.isDebugEnabled()) {
        logger.debug(ex.getMessage());
      }
      return null;
    }
  }

  private Collection<MediaType> getProducibleMediaTypes(RequestContext context) {
    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    if (matchingMetadata != null) {
      var mediaTypes = matchingMetadata.getProducibleMediaTypes();
      if (CollectionUtils.isNotEmpty(mediaTypes)) {
        return mediaTypes;
      }
    }
    return List.of(MediaType.ALL);
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

  private List<View> getCandidateViews(String viewName, Locale locale, List<MediaType> requestedMediaTypes) throws Exception {
    ArrayList<View> candidateViews = new ArrayList<>();
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
  private View getBestView(List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestContext context) {
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
            if (logger.isDebugEnabled()) {
              logger.debug("Selected '{}' given {}", mediaType, requestedMediaTypes);
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
