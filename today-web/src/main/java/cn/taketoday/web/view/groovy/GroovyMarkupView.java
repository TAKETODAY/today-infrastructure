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

package cn.taketoday.web.view.groovy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.AbstractTemplateView;
import cn.taketoday.web.view.ViewRenderingException;
import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;

/**
 * An {@link AbstractTemplateView} subclass based on Groovy XML/XHTML markup templates.
 *
 * <p>Framework's Groovy Markup Template support requires Groovy 2.3.1 and higher.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see GroovyMarkupViewResolver
 * @see GroovyMarkupConfigurer
 * @see <a href="http://groovy-lang.org/templating.html#_the_markuptemplateengine">
 * Groovy Markup Template engine documentation</a>
 */
public class GroovyMarkupView extends AbstractTemplateView {

  @Nullable
  private MarkupTemplateEngine engine;

  /**
   * Set the MarkupTemplateEngine to use in this view.
   * <p>If not set, the engine is auto-detected by looking up a single
   * {@link GroovyMarkupConfig} bean in the web application context and using
   * it to obtain the configured {@code MarkupTemplateEngine} instance.
   *
   * @see GroovyMarkupConfig
   */
  public void setTemplateEngine(MarkupTemplateEngine engine) {
    this.engine = engine;
  }

  /**
   * Invoked at startup.
   * If no {@link #setTemplateEngine(MarkupTemplateEngine) templateEngine} has
   * been manually set, this method looks up a {@link GroovyMarkupConfig} bean
   * by type and uses it to obtain the Groovy Markup template engine.
   *
   * @see GroovyMarkupConfig
   * @see #setTemplateEngine(groovy.text.markup.MarkupTemplateEngine)
   */
  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext();
    if (this.engine == null) {
      setTemplateEngine(autodetectMarkupTemplateEngine());
    }
  }

  /**
   * Autodetect a MarkupTemplateEngine via the ApplicationContext.
   * Called if a MarkupTemplateEngine has not been manually configured.
   */
  protected MarkupTemplateEngine autodetectMarkupTemplateEngine() throws BeansException {
    try {
      return BeanFactoryUtils.beanOfTypeIncludingAncestors(obtainApplicationContext(),
              GroovyMarkupConfig.class, true, false).getTemplateEngine();
    }
    catch (NoSuchBeanDefinitionException ex) {
      throw new ApplicationContextException("Expected a single GroovyMarkupConfig bean in the current " +
              "Web application context or the parent root context: GroovyMarkupConfigurer is " +
              "the usual implementation. This bean may have any name.", ex);
    }
  }

  @Override
  public boolean checkResource(Locale locale) throws Exception {
    Assert.state(this.engine != null, "No MarkupTemplateEngine set");
    try {
      this.engine.resolveTemplate(getUrl());
    }
    catch (IOException ex) {
      return false;
    }
    return true;
  }

  @Override
  protected void renderMergedTemplateModel(Map<String, Object> model, RequestContext context) throws Exception {
    String url = getUrl();
    Assert.state(url != null, "'url' not set");

    Template template = getTemplate(url);
    template.make(model)
            .writeTo(new BufferedWriter(context.getWriter()));
  }

  /**
   * Return a template compiled by the configured Groovy Markup template engine
   * for the given view URL.
   */
  protected Template getTemplate(String viewUrl) throws Exception {
    Assert.state(this.engine != null, "No MarkupTemplateEngine set");
    try {
      return this.engine.createTemplateByPath(viewUrl);
    }
    catch (ClassNotFoundException ex) {
      Throwable cause = (ex.getCause() != null ? ex.getCause() : ex);
      throw new ViewRenderingException("Could not find class while rendering Groovy Markup view with name '%s': %s'"
              .formatted(getUrl(), ex.getMessage()), cause);
    }
  }

}
