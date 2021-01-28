/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletContextAware;

/**
 * @author TODAY <br>
 * 2018-06-26 11:26:01
 */
public class ThymeleafTemplateViewResolver
        extends AbstractTemplateViewResolver implements InitializingBean, ServletContextAware {

  @Value(value = "#{thymeleaf.cacheable}", required = false)
  private boolean cacheable = true;
  private ServletContext servletContext;
  private final TemplateEngine templateEngine;

  public ThymeleafTemplateViewResolver() {
    this(new TemplateEngine());
  }

  public ThymeleafTemplateViewResolver(TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  /**
   * Init Thymeleaf View Resolver.
   */
  @Override
  public void afterPropertiesSet() throws Exception {

    final ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);

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
  public void resolveView(final String template, final RequestContext context) throws IOException {

    templateEngine.process(template,
                           new WebContext(context.nativeRequest(),
                                          context.nativeResponse(),
                                          servletContext,
                                          locale),
                           context.getWriter());
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

}
