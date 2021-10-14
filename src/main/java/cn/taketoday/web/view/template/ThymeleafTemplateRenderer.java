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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view.template;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.beans.InitializingBean;
import cn.taketoday.lang.Value;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletContextAware;
import cn.taketoday.web.servlet.ServletUtils;

/**
 * @author TODAY 2018-06-26 11:26:01
 */
public class ThymeleafTemplateRenderer
        extends AbstractTemplateRenderer implements InitializingBean, ServletContextAware {

  /**
   * @see ServletContextTemplateResolver#setCacheable(boolean)
   */
  @Value(value = "${thymeleaf.cacheable}", required = false)
  private boolean cacheable = true;
  private ServletContext servletContext;
  private final TemplateEngine templateEngine;

  public ThymeleafTemplateRenderer() {
    this(new TemplateEngine());
  }

  public ThymeleafTemplateRenderer(TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  /**
   * Init Thymeleaf View Resolver.
   */
  @Override
  public void afterPropertiesSet() {
    ServletContextTemplateResolver templateResolver
            = new ServletContextTemplateResolver(servletContext);

    templateResolver.setPrefix(prefix);
    templateResolver.setSuffix(suffix);
    templateResolver.setCacheable(cacheable);
    templateResolver.setCharacterEncoding(encoding);
    templateResolver.setTemplateMode(TemplateMode.HTML);

    templateEngine.setTemplateResolver(templateResolver);

    LoggerFactory.getLogger(getClass()).info("Configuration Thymeleaf View Resolver Success.");
  }

  /**
   * Resolve Thymeleaf View.
   */
  @Override
  public void render(String template, RequestContext context) throws IOException {
    HttpServletRequest servletRequest = ServletUtils.getServletRequest(context);
    HttpServletResponse servletResponse = ServletUtils.getServletResponse(context);
    WebContext webContext = new WebContext(servletRequest, servletResponse, servletContext, locale);
    templateEngine.process(template, webContext, context.getWriter());
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  public ServletContext getServletContext() {
    return servletContext;
  }

  public void setCacheable(boolean cacheable) {
    this.cacheable = cacheable;
  }

  public boolean isCacheable() {
    return cacheable;
  }

  public TemplateEngine getTemplateEngine() {
    return templateEngine;
  }
}
