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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.expression.CompositeExpressionResolver;
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
import cn.taketoday.web.servlet.HttpSessionModelAdapter;
import cn.taketoday.web.servlet.ServletContextModelAdapter;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.ServletRequestModelAdapter;

/**
 * @author TODAY <br>
 * 2019-11-24 22:28
 */
@Props(prefix = "web.mvc.view.")
public class DefaultTemplateViewResolver extends AbstractTemplateViewResolver {

  private final StandardExpressionContext sharedContext;
  private final ExpressionFactory expressionFactory = ExpressionFactory.getSharedInstance();
  private boolean runInServlet;

  public DefaultTemplateViewResolver() {
    this(ContextUtils.getExpressionProcessor().getManager());
  }

  public DefaultTemplateViewResolver(ExpressionManager elManager) {
    sharedContext = elManager.getContext();
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
      writer.flush();
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
    final ExpressionContext elContext = prepareContext(context);
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

  protected ExpressionContext prepareContext(RequestContext context) {
    final ExpressionResolver[] resolvers;
    if (runInServlet) {
      resolvers = ServletResolvers.getResolvers(sharedContext, context);
    }
    else {
      resolvers = new ExpressionResolver[] {
              new ModelAttributeResolver(context),
              sharedContext.getResolver()
      };
    }

    final CompositeExpressionResolver resolver = new CompositeExpressionResolver(resolvers);
    return new TemplateViewResolverELContext(this.sharedContext, resolver);
  }

  public void setRunInServlet(boolean runInServlet) {
    this.runInServlet = runInServlet;
  }

  /**
   * is Run In Servlet
   */
  public boolean isRunInServlet() {
    return runInServlet;
  }

  static class ServletResolvers {

    static ExpressionResolver[] getResolvers(StandardExpressionContext sharedContext, RequestContext context) {
      if (context instanceof ServletRequestContext) {
        final HttpServletRequest request = ((ServletRequestContext) context).getRequest();
        final HttpSession session = request.getSession(false);
        final ServletContext servletContext = request.getServletContext();

        final ServletContextModelAdapter servletContextModelAdapter = new ServletContextModelAdapter(servletContext);
        final ServletRequestModelAdapter servletRequestModelAdapter = new ServletRequestModelAdapter(request);

        if (session != null) {
          final HttpSessionModelAdapter httpSessionModelAdapter = new HttpSessionModelAdapter(session);
          return new ExpressionResolver[] {
                  new ModelAttributeResolver(context),
                  new ModelAttributeResolver(servletRequestModelAdapter), // 1
                  new ModelAttributeResolver(httpSessionModelAdapter), // 2
                  new ModelAttributeResolver(servletContextModelAdapter), // 3
                  sharedContext.getResolver()
          };
        }

        return new ExpressionResolver[] {
                new ModelAttributeResolver(context),
                new ModelAttributeResolver(servletRequestModelAdapter), // 1
                new ModelAttributeResolver(servletContextModelAdapter), // 2
                sharedContext.getResolver()
        };
      }
      throw new IllegalStateException("Not run in servlet");
    }
  }

  private static final class TemplateViewResolverELContext extends ExpressionContext {

    private final ExpressionResolver elResolver;
    private final StandardExpressionContext delegate;

    public TemplateViewResolverELContext(StandardExpressionContext delegate, ExpressionResolver elResolver) {
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
