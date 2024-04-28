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

package cn.taketoday.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aot.hint.annotation.Reflective;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.ui.Model;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RequestToViewNameTranslator;
import cn.taketoday.web.handler.method.RequestMappingHandlerAdapter;
import cn.taketoday.web.view.View;

/**
 * Annotation for handling exceptions in specific handler classes and/or
 * handler methods.
 *
 * <p>Handler methods which are annotated with this annotation are allowed to
 * have very flexible signatures. They may have parameters of the following
 * types, in arbitrary order:
 * <ul>
 * <li>An exception argument: declared as a general Exception or as a more
 * specific exception. This also serves as a mapping hint if the annotation
 * itself does not narrow the exception types through its {@link #value()}.
 * You may refer to a top-level exception being propagated or to a nested
 * cause within a wrapper exception. As of 4.0, any cause level is being
 * exposed, whereas previously only an immediate cause was considered.
 * <li>Request and/or response objects You may choose any specific request
 * type, e.g. {@link cn.taketoday.web.RequestContext}.
 * <li>Session object: typically {@link cn.taketoday.session.WebSession}.
 * An argument of this type will enforce the presence of a corresponding session.
 * As a consequence, such an argument will never be {@code null}.
 * <i>Note that session access may not be thread-safe, in particular in a
 * Web environment: Consider switching the
 * {@link RequestMappingHandlerAdapter#setSynchronizeOnSession
 * "synchronizeOnSession"} flag to "true" if multiple requests are allowed to
 * access a session concurrently.</i>
 *
 * <li>{@link cn.taketoday.web.RequestContext}.
 * Allows for generic request parameter access as well as request/session
 * attribute access, without ties to the native Web API.
 *
 * <li>{@link java.util.Locale} for the current request locale
 * (determined by the most specific locale resolver available,
 * i.e. the configured {@link LocaleResolver} in a Web environment).
 * <li>{@link java.io.InputStream} / {@link java.io.Reader} for access
 * to the request's content. This will be the raw InputStream/Reader as
 * exposed by the Web API.
 * <li>{@link java.io.OutputStream} / {@link java.io.Writer} for generating
 * the response's content. This will be the raw OutputStream/Writer as
 * exposed by the Web API.
 * <li>{@link Model} as an alternative to returning
 * a model map from the handler method. Note that the provided model is not
 * pre-populated with regular model attributes and therefore always empty,
 * as a convenience for preparing the model for an exception-specific view.
 * </ul>
 *
 * <p>The following return types are supported for handler methods:
 * <ul>
 * <li>A {@code ModelAndView} object (from Web MVC).
 * <li>A {@link Model} object, with the view name implicitly
 * determined through a {@link RequestToViewNameTranslator}.
 * <li>A {@link java.util.Map} object for exposing a model,
 * with the view name implicitly determined through a
 * {@link RequestToViewNameTranslator}.
 * <li>A {@link View} object.
 * <li>A {@link String} value which is interpreted as view name.
 * <li>{@link ResponseBody @ResponseBody} annotated methods
 * to set the response content. The return value will be converted to the
 * response stream using {@linkplain HttpMessageConverter message converters}.
 * <li>An {@link HttpEntity HttpEntity&lt;?&gt;} or
 * {@link ResponseEntity ResponseEntity&lt;?&gt;} object
 * to set response headers and content. The ResponseEntity body
 * will be converted and written to the response stream using
 * {@linkplain HttpMessageConverter message converters}.
 * <li>{@code void} if the method handles the response itself (by
 * writing the response content directly, declaring an argument of type
 * {@link cn.taketoday.web.RequestContext} for that purpose) or if the
 * view name is supposed to be implicitly determined through a {@link RequestToViewNameTranslator}
 * (not declaring a response argument in the handler method signature).
 * </ul>
 *
 * <p>You may combine the {@code ExceptionHandler} annotation with
 * {@link ResponseStatus @ResponseStatus} for a specific HTTP error status.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ControllerAdvice
 * @since 2.3.7 2019-06-18 14:30
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Reflective(ExceptionHandlerReflectiveProcessor.class)
public @interface ExceptionHandler {

  /**
   * Exceptions handled by the annotated method. If empty, will default to any
   * exceptions listed in the method argument list.
   */
  Class<? extends Throwable>[] value() default {};

}
