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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.view.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.expression.FunctionMapper;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 * 2019-11-24 22:28
 */
@Props(prefix = "web.mvc.view.")
public class DefaultTemplateViewResolver extends AbstractTemplateViewResolver {

  private final StandardExpressionContext sharedContext;
  private final ExpressionFactory expressionFactory = ExpressionFactory.getSharedInstance();
  /** @since 3.0 */
  private ResolversSupplier resolversSupplier;

  public DefaultTemplateViewResolver() {
    this(ContextUtils.getExpressionProcessor().getManager(), new ResolversSupplier());
  }

  public DefaultTemplateViewResolver(ExpressionManager elManager) {
    this(elManager, new ResolversSupplier());
  }

  public DefaultTemplateViewResolver(ExpressionManager elManager, ResolversSupplier resolversSupplier) {
    this.sharedContext = elManager.getContext();
    this.resolversSupplier = resolversSupplier;
  }

  @Override
  public void resolveView(final String templateName, final RequestContext context) throws IOException {

    // prepare full template path to load template
    final String template = prepareTemplate(templateName);
    // read template as text
    final String text = readTemplate(template);
    // render template as text
    final String rendered = renderTemplate(text, context);
    // write to client
    write(context, rendered);
  }

  /**
   * Write to client use request context's {@link Writer}
   *
   * @param context
   *         Current {@link RequestContext}
   * @param rendered
   *         Rendered text
   *
   * @throws IOException
   *         If an input or output exception occurred
   * @throws IllegalStateException
   *         For Servlet Environment if the <code>getOutputStream</code>
   *         method has already been called for this response object
   */
  protected void write(final RequestContext context, final String rendered) throws IOException {
    if (rendered != null) {
      final PrintWriter writer = context.getWriter();
      writer.write(rendered);
    }
  }

  /**
   * Use EL or other render the input text.
   *
   * @param text
   *         input text string
   * @param context
   *         Current {@link RequestContext}
   *
   * @return Rendered text string
   */
  protected String renderTemplate(final String text, RequestContext context) {
    final ExpressionContext elContext = prepareContext(sharedContext, context);
    final ValueExpression expression =
            expressionFactory.createValueExpression(elContext, text, String.class);
    return expression.getValue(elContext).toString();
  }

  /**
   * Read template source as text string
   *
   * @param template
   *         Template location
   *
   * @return template source text string
   *
   * @throws IOException
   *         If an input or output exception occurred
   */
  protected String readTemplate(final String template) throws IOException {
    final Resource resource = ResourceUtils.getResource(template);
    return StringUtils.readAsText(resource.getInputStream());
  }

  public void setResolversSupplier(ResolversSupplier resolversSupplier) {
    Assert.notNull(resolversSupplier, "resolversSupplier must not be null");
    this.resolversSupplier = resolversSupplier;
  }

  protected ExpressionContext prepareContext(ExpressionContext sharedContext, RequestContext context) {
    final ExpressionResolver resolver = resolversSupplier.getResolvers(sharedContext, context);
    return new TemplateViewResolverELContext(sharedContext, resolver);
  }

  private static final class TemplateViewResolverELContext extends ExpressionContext {

    private final ExpressionContext delegate;
    private final ExpressionResolver elResolver;

    public TemplateViewResolverELContext(ExpressionContext delegate, ExpressionResolver elResolver) {
      this.delegate = delegate;
      this.elResolver = elResolver;
    }

    @Override
    public ExpressionResolver getResolver() {
      return elResolver;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
      return this.delegate.getFunctionMapper();
    }

    @Override
    public VariableMapper getVariableMapper() {
      return this.delegate.getVariableMapper();
    }

    @Override
    public void setPropertyResolved(Object base, Object property) {
      setPropertyResolved(true);
    }

    @Override
    public Object handlePropertyNotResolved(Object base, Object property, EvaluationContext ctx) {
      return Constant.BLANK;
    }

  }

}
