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

package cn.taketoday.web.view.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.expression0.ExpressionContext;
import cn.taketoday.expression0.ExpressionFactory;
import cn.taketoday.expression0.StandardExpressionContext;
import cn.taketoday.expression0.ValueExpression;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.AbstractTemplateView;

/**
 * DefaultTemplateView use cn.taketoday.expression tech
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/9 21:09
 */
public class DefaultTemplateView extends AbstractTemplateView {

  @Nullable
  private ResourceLoader resourceLoader;

  @Nullable
  private ExpressionFactory expressionFactory;

  @Nullable
  private ExpressionContext sharedContext;

  @Override
  protected void renderMergedTemplateModel(
          Map<String, Object> model, RequestContext context) throws Exception {
    // prepare full template path to load template
    String template = getUrl();
    // read template as text
    String text = readTemplate(template);
    // render template as text
    String rendered = renderTemplate(text, model);
    // write to client
    write(context, rendered);
  }

  /**
   * Read template source as text string
   *
   * @param template Template location
   * @return template source text string
   * @throws IOException If an input or output exception occurred
   */
  protected String readTemplate(String template) throws IOException {
    if (resourceLoader == null) {
      resourceLoader = new DefaultResourceLoader();
    }
    Resource resource = resourceLoader.getResource(template);
    return StreamUtils.copyToString(resource.getInputStream());
  }

  /**
   * Use EL or other render the input text.
   *
   * @param text input text string
   * @param model model
   * @return Rendered text string
   */
  protected String renderTemplate(String text, Map<String, Object> model) {
    StandardExpressionContext context = new StandardExpressionContext(sharedContext);
    context.addVariables(model);

    ValueExpression expression =
            expressionFactory().createValueExpression(context, text, String.class);
    return expression.getValue(context).toString();
  }

  private ExpressionFactory expressionFactory() {
    ExpressionFactory expressionFactory = this.expressionFactory;
    if (expressionFactory == null) {
      expressionFactory = ExpressionFactory.getSharedInstance();
      this.expressionFactory = expressionFactory;
    }
    return expressionFactory;
  }

  /**
   * Write to client use request context's {@link Writer}
   *
   * @param context Current {@link RequestContext}
   * @param rendered Rendered text
   * @throws IOException If an input or output exception occurred
   * @throws IllegalStateException For Servlet Environment if the <code>getOutputStream</code>
   * method has already been called for this response object
   */
  protected void write(RequestContext context, String rendered) throws IOException {
    if (rendered != null) {
      PrintWriter writer = context.getWriter();
      writer.write(rendered);
    }
  }

  public void setSharedContext(@Nullable ExpressionContext sharedContext) {
    this.sharedContext = sharedContext;
  }

  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public void setExpressionFactory(@Nullable ExpressionFactory expressionFactory) {
    this.expressionFactory = expressionFactory;
  }

}
