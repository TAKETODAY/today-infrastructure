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

package cn.taketoday.web.servlet.view;

import cn.taketoday.context.MessageSource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Specialization of {@link InternalResourceView} for JSTL pages,
 * i.e. JSP pages that use the JSP Standard Tag Library.
 *
 * <p>Exposes JSTL-specific request attributes specifying locale
 * and resource bundle for JSTL's formatting and message tags,
 * using Framework's locale and {@link cn.taketoday.context.MessageSource}.
 *
 * <p>Typical usage with {@link InternalResourceViewResolver} would look as follows,
 * from the perspective of the DispatcherServlet context definition:
 *
 * <pre class="code">
 * &lt;bean id="viewResolver" class="cn.taketoday.web.servlet.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="viewClass" value="cn.taketoday.web.servlet.view.JstlView"/&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="messageSource" class="cn.taketoday.context.support.ResourceBundleMessageSource"&gt;
 *   &lt;property name="basename" value="messages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Every view name returned from a handler will be translated to a JSP
 * resource (for example: "myView" &rarr; "/WEB-INF/jsp/myView.jsp"), using
 * this view class to enable explicit JSTL support.
 *
 * <p>The specified MessageSource loads messages from "messages.properties" etc
 * files in the class path. This will automatically be exposed to views as
 * JSTL localization context, which the JSTL fmt tags (message etc) will use.
 * Consider using Framework's ReloadableResourceBundleMessageSource instead of
 * the standard ResourceBundleMessageSource for more sophistication.
 * Of course, any other Framework components can share the same MessageSource.
 *
 * <p>This is a separate class mainly to avoid JSTL dependencies in
 * {@link InternalResourceView} itself. JSTL has not been part of standard
 * J2EE up until J2EE 1.4, so we can't assume the JSTL API jar to be
 * available on the class path.
 *
 * <p>Hint: Set the {@link #setExposeContextBeansAsAttributes} flag to "true"
 * in order to make all Framework beans in the application context accessible
 * within JSTL expressions (e.g. in a {@code c:out} value expression).
 * This will also make all such beans accessible in plain {@code ${...}}
 * expressions in a JSP 2.0 page.
 *
 * @author Juergen Hoeller
 * @see JstlUtils#exposeLocalizationContext
 * @see InternalResourceViewResolver
 * @see cn.taketoday.context.support.ResourceBundleMessageSource
 * @see cn.taketoday.context.support.ReloadableResourceBundleMessageSource
 * @since 4.0
 */
@Deprecated
public class JstlView extends InternalResourceView {

  @Nullable
  private MessageSource messageSource;

  /**
   * Constructor for use as a bean.
   *
   * @see #setUrl
   */
  public JstlView() { }

  /**
   * Create a new JstlView with the given URL.
   *
   * @param url the URL to forward to
   */
  public JstlView(String url) {
    super(url);
  }

  /**
   * Create a new JstlView with the given URL.
   *
   * @param url the URL to forward to
   * @param messageSource the MessageSource to expose to JSTL tags
   * (will be wrapped with a JSTL-aware MessageSource that is aware of JSTL's
   * {@code jakarta.servlet.jsp.jstl.fmt.localizationContext} context-param)
   * @see JstlUtils#getJstlAwareMessageSource
   */
  public JstlView(String url, @Nullable MessageSource messageSource) {
    this(url);
    this.messageSource = messageSource;
  }

  /**
   * Wraps the MessageSource with a JSTL-aware MessageSource that is aware
   * of JSTL's {@code jakarta.servlet.jsp.jstl.fmt.localizationContext}
   * context-param.
   *
   * @see JstlUtils#getJstlAwareMessageSource
   */
  @Override
  protected void initServletContext(ServletContext servletContext) {
    if (this.messageSource != null) {
      this.messageSource = JstlUtils.getJstlAwareMessageSource(servletContext, this.messageSource);
    }
    super.initServletContext(servletContext);
  }

  /**
   * Exposes a JSTL LocalizationContext for Framework's locale and MessageSource.
   *
   * @see JstlUtils#exposeLocalizationContext
   */
  @Override
  protected void exposeHelpers(HttpServletRequest servletRequest, RequestContext request) throws Exception {
    if (this.messageSource != null) {
      JstlUtils.exposeLocalizationContext(request, servletRequest, this.messageSource);
    }
    else {
      JstlUtils.exposeLocalizationContext(request, servletRequest);
    }
  }

}
