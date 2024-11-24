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

package infra.web.handler;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import infra.lang.Nullable;
import infra.web.HandlerExceptionHandler;
import infra.web.RequestContext;
import infra.web.view.ModelAndView;

/**
 * {@link HandlerExceptionHandler} implementation that allows for mapping exception
 * class names to view names, either for a set of given handlers or for all handlers
 * in the DispatcherHandler.
 *
 * <p>Error views are analogous to error page JSPs, but can be used with any kind of
 * exception including any checked one, with fine-granular mappings for specific handlers.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/3 9:41
 */
public class SimpleMappingExceptionHandler extends AbstractHandlerExceptionHandler {
  /** The default name of the exception attribute: "exception". */
  public static final String DEFAULT_EXCEPTION_ATTRIBUTE = "exception";

  @Nullable
  private Properties exceptionMappings;

  @Nullable
  private Class<?>[] excludedExceptions;

  @Nullable
  private String defaultErrorView;

  @Nullable
  private Integer defaultStatusCode;

  private final HashMap<String, Integer> statusCodes = new HashMap<>();

  @Nullable
  private String exceptionAttribute = DEFAULT_EXCEPTION_ATTRIBUTE;

  /**
   * Set the mappings between exception class names and error view names.
   * The exception class name can be a substring, with no wildcard support at present.
   * A value of "Exception" would match {@code Exception}
   * and subclasses, for example.
   * <p><b>NB:</b> Consider carefully how
   * specific the pattern is, and whether to include package information (which isn't mandatory).
   * For example, "Exception" will match nearly anything, and will probably hide other rules.
   * "java.lang.Exception" would be correct if "Exception" was meant to define a rule for all
   * checked exceptions. With more unusual exception names such as "BaseBusinessException"
   * there's no need to use a FQN.
   *
   * @param mappings exception patterns (can also be fully qualified class names) as keys,
   * and error view names as values
   */
  public void setExceptionMappings(@Nullable Properties mappings) {
    this.exceptionMappings = mappings;
  }

  /**
   * Set one or more exceptions to be excluded from the exception mappings.
   * Excluded exceptions are checked first and if one of them equals the actual
   * exception, the exception will remain unresolved.
   *
   * @param excludedExceptions one or more excluded exception types
   */
  public void setExcludedExceptions(Class<?>... excludedExceptions) {
    this.excludedExceptions = excludedExceptions;
  }

  /**
   * Set the name of the default error view.
   * This view will be returned if no specific mapping was found.
   * <p>Default is none.
   */
  public void setDefaultErrorView(@Nullable String defaultErrorView) {
    this.defaultErrorView = defaultErrorView;
  }

  /**
   * Set the HTTP status code that this exception handler will apply for a given
   * resolved error view. Keys are view names; values are status codes.
   * <p>Note that this error code will only get applied in case of a top-level request.
   * It will not be set for an include request, since the HTTP status cannot be modified
   * from within an include.
   * <p>If not specified, the default status code will be applied.
   *
   * @see #setDefaultStatusCode(int)
   */
  public void setStatusCodes(Properties statusCodes) {
    for (String viewName : statusCodes.stringPropertyNames()) {
      Integer statusCode = Integer.valueOf(statusCodes.getProperty(viewName));
      this.statusCodes.put(viewName, statusCode);
    }
  }

  /**
   * An alternative to {@link #setStatusCodes(Properties)} for use with
   * Java-based configuration.
   */
  public void addStatusCode(String viewName, int statusCode) {
    this.statusCodes.put(viewName, statusCode);
  }

  /**
   * Returns the HTTP status codes provided via {@link #setStatusCodes(Properties)}.
   * Keys are view names; values are status codes.
   */
  public Map<String, Integer> getStatusCodesAsMap() {
    return statusCodes;
  }

  /**
   * Set the default HTTP status code that this exception handler will apply
   * if it resolves an error view and if there is no status code mapping defined.
   * <p>Note that this error code will only get applied in case of a top-level request.
   * It will not be set for an include request, since the HTTP status cannot be modified
   * from within an include.
   * <p>If not specified, no status code will be applied, either leaving this to the
   * controller or view, or keeping the servlet engine's default of 200 (OK).
   *
   * @param defaultStatusCode the HTTP status code value, for example 500
   * ({@link infra.http.HttpStatus#INTERNAL_SERVER_ERROR}) or
   * 404 ({@link infra.http.HttpStatus#NOT_FOUND})
   * @see #setStatusCodes(Properties)
   */
  public void setDefaultStatusCode(int defaultStatusCode) {
    this.defaultStatusCode = defaultStatusCode;
  }

  /**
   * Set the name of the model attribute as which the exception should be exposed.
   * Default is "exception".
   * <p>This can be either set to a different attribute name or to {@code null}
   * for not exposing an exception attribute at all.
   *
   * @see #DEFAULT_EXCEPTION_ATTRIBUTE
   */
  public void setExceptionAttribute(@Nullable String exceptionAttribute) {
    this.exceptionAttribute = exceptionAttribute;
  }

  /**
   * Actually resolve the given exception that got thrown during on handler execution,
   * returning a ModelAndView that represents a specific error page if appropriate.
   * <p>May be overridden in subclasses, in order to apply specific exception checks.
   * Note that this template method will be invoked <i>after</i> checking whether this
   * resolved applies ("mappedHandlers" etc), so an implementation may simply proceed
   * with its actual exception handling.
   *
   * @param request current HTTP request
   * @param handler the executed handler, or {@code null} if none chosen at the time
   * of the exception (for example, if multipart resolution failed)
   * @param ex the exception that got thrown during handler execution
   * @return a corresponding {@code ModelAndView} to forward to,
   * or {@code null} for default processing in the resolution chain
   */
  @Nullable
  @Override
  protected ModelAndView handleInternal(RequestContext request, @Nullable Object handler, Throwable ex) {
    // Expose ModelAndView for chosen error view.
    String viewName = determineViewName(ex, request);
    if (viewName != null) {
      // Apply HTTP status code for error views, if specified.
      // Only apply it if we're processing a top-level request.
      Integer statusCode = determineStatusCode(request, viewName);
      if (statusCode != null) {
        applyStatusCodeIfPossible(request, statusCode);
      }
      return getModelAndView(viewName, ex, request);
    }
    else {
      return null; //next
    }
  }

  /**
   * Determine the view name for the given exception, first checking against the
   * {@link #setExcludedExceptions(Class[]) "excludedExecptions"}, then searching the
   * {@link #setExceptionMappings "exceptionMappings"}, and finally using the
   * {@link #setDefaultErrorView "defaultErrorView"} as a fallback.
   *
   * @param ex the exception that got thrown during handler execution
   * @param request current HTTP request (useful for obtaining metadata)
   * @return the resolved view name, or {@code null} if excluded or none found
   */
  @Nullable
  protected String determineViewName(Throwable ex, RequestContext request) {
    String viewName = null;
    if (excludedExceptions != null) {
      for (Class<?> excludedEx : excludedExceptions) {
        if (excludedEx.equals(ex.getClass())) {
          return null;
        }
      }
    }
    // Check for specific exception mappings.
    if (exceptionMappings != null) {
      viewName = findMatchingViewName(exceptionMappings, ex);
    }
    // Return default error view else, if defined.
    if (viewName == null && defaultErrorView != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Resolving to default view '{}'", defaultErrorView);
      }
      viewName = defaultErrorView;
    }
    return viewName;
  }

  /**
   * Find a matching view name in the given exception mappings.
   *
   * @param exceptionMappings mappings between exception class names and error view names
   * @param ex the exception that got thrown during handler execution
   * @return the view name, or {@code null} if none found
   * @see #setExceptionMappings
   */
  @Nullable
  protected String findMatchingViewName(Properties exceptionMappings, Throwable ex) {
    String viewName = null;
    String dominantMapping = null;
    int deepest = Integer.MAX_VALUE;
    for (Enumeration<?> names = exceptionMappings.propertyNames(); names.hasMoreElements(); ) {
      String exceptionMapping = (String) names.nextElement();
      int depth = getDepth(exceptionMapping, ex);
      if (depth >= 0 && (depth < deepest || (depth == deepest && dominantMapping != null
              && exceptionMapping.length() > dominantMapping.length()))) {
        deepest = depth;
        dominantMapping = exceptionMapping;
        viewName = exceptionMappings.getProperty(exceptionMapping);
      }
    }
    if (viewName != null && logger.isDebugEnabled()) {
      logger.debug("Resolving to view '{}' based on mapping [{}]", viewName, dominantMapping);
    }
    return viewName;
  }

  /**
   * Return the depth to the superclass matching.
   * <p>0 means ex matches exactly. Returns -1 if there's no match.
   * Otherwise, returns depth. Lowest depth wins.
   */
  protected int getDepth(String exceptionMapping, Throwable ex) {
    return getDepth(exceptionMapping, ex.getClass(), 0);
  }

  private int getDepth(String exceptionMapping, Class<?> exceptionClass, int depth) {
    if (exceptionClass.getName().contains(exceptionMapping)) {
      // Found it!
      return depth;
    }
    // If we've gone as far as we can go and haven't found it...
    if (exceptionClass == Throwable.class) {
      return -1;
    }
    return getDepth(exceptionMapping, exceptionClass.getSuperclass(), depth + 1);
  }

  /**
   * Determine the HTTP status code to apply for the given error view.
   * <p>The default implementation returns the status code for the given view name (specified through the
   * {@link #setStatusCodes(Properties) statusCodes} property), or falls back to the
   * {@link #setDefaultStatusCode defaultStatusCode} if there is no match.
   * <p>Override this in a custom subclass to customize this behavior.
   *
   * @param request current HTTP request
   * @param viewName the name of the error view
   * @return the HTTP status code to use, or {@code null} for the servlet container's default
   * (200 in case of a standard error view)
   * @see #setDefaultStatusCode
   * @see #applyStatusCodeIfPossible
   */
  @Nullable
  protected Integer determineStatusCode(RequestContext request, String viewName) {
    if (this.statusCodes.containsKey(viewName)) {
      return this.statusCodes.get(viewName);
    }
    return this.defaultStatusCode;
  }

  /**
   * Apply the specified HTTP status code to the given response, if possible (that is,
   * if not executing within an include request).
   *
   * @param request current HTTP request
   * @param statusCode the status code to apply
   * @see #determineStatusCode
   * @see #setDefaultStatusCode
   * @see RequestContext#setStatus
   */
  protected void applyStatusCodeIfPossible(RequestContext request, int statusCode) {
    if (logger.isDebugEnabled()) {
      logger.debug("Applying HTTP status {}", statusCode);
    }
    request.setStatus(statusCode);
  }

  /**
   * Return a ModelAndView for the given request, view name and exception.
   * <p>The default implementation delegates to {@link #getModelAndView(String, Throwable)}.
   *
   * @param viewName the name of the error view
   * @param ex the exception that got thrown during handler execution
   * @param request current HTTP request (useful for obtaining metadata)
   * @return the ModelAndView instance
   */
  protected ModelAndView getModelAndView(String viewName, Throwable ex, RequestContext request) {
    return getModelAndView(viewName, ex);
  }

  /**
   * Return a ModelAndView for the given view name and exception.
   * <p>The default implementation adds the specified exception attribute.
   * Can be overridden in subclasses.
   *
   * @param viewName the name of the error view
   * @param ex the exception that got thrown during handler execution
   * @return the ModelAndView instance
   * @see #setExceptionAttribute
   */
  protected ModelAndView getModelAndView(String viewName, Throwable ex) {
    ModelAndView mv = new ModelAndView(viewName);
    if (exceptionAttribute != null) {
      mv.addObject(exceptionAttribute, ex);
    }
    return mv;
  }

}
