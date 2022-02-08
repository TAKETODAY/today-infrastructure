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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.view.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.expression.ExpressionResolverComposite;
import cn.taketoday.expression.FunctionMapper;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.servlet.HttpSessionModelAdapter;
import cn.taketoday.web.servlet.ServletContextModelAdapter;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.ServletRequestModelAdapter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * @author TODAY <br>
 * 2019-11-24 22:28
 */
public class DefaultTemplateRenderer extends AbstractTemplateRenderer {

  private final StandardExpressionContext sharedContext;
  private ExpressionFactory expressionFactory;

  @Nullable
  private ResourceLoader resourceLoader;

  public DefaultTemplateRenderer() {
    this(ExpressionProcessor.getSharedInstance().getManager());
    this.resourceLoader = new DefaultResourceLoader();
  }

  public DefaultTemplateRenderer(ExpressionManager elManager) {
    this.sharedContext = elManager.getContext();
  }

  @Override
  public void render(String templateName, RequestContext context) throws IOException {
    // prepare full template path to load template
    String template = prepareTemplate(templateName);
    // read template as text
    String text = readTemplate(template);
    // render template as text
    String rendered = renderTemplate(text, context);
    // write to client
    write(context, rendered);
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

  /**
   * Use EL or other render the input text.
   *
   * @param text input text string
   * @param context Current {@link RequestContext}
   * @return Rendered text string
   */
  protected String renderTemplate(String text, RequestContext context) {
    ExpressionContext elContext = prepareContext(sharedContext, context);
    if (expressionFactory == null) {
      expressionFactory = ExpressionFactory.getSharedInstance();
    }
    ValueExpression expression =
            expressionFactory.createValueExpression(elContext, text, String.class);
    return expression.getValue(elContext).toString();
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

  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Nullable
  public ResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  public void setExpressionFactory(ExpressionFactory expressionFactory) {
    this.expressionFactory = expressionFactory;
  }

  public ExpressionFactory getExpressionFactory() {
    return expressionFactory;
  }

  protected ExpressionContext prepareContext(ExpressionContext sharedContext, RequestContext context) {
    ExpressionResolver resolver = getResolvers(sharedContext, context);
    return new TemplateViewResolverELContext(sharedContext, resolver);
  }

  static ExpressionResolver getResolvers(ExpressionContext sharedContext, RequestContext context) {
    if (ServletDetector.isPresent()) {
      return ServletDelegate.getResolvers(sharedContext, context);
    }
    return new ExpressionResolverComposite(
            new ModelAttributeResolver(context),
            sharedContext.getResolver()
    );
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

  static class ServletDelegate {

    static ExpressionResolver getResolvers(ExpressionContext sharedContext, RequestContext context) {

      if (context instanceof ServletRequestContext servlet) {
        HttpServletRequest request = servlet.getRequest();
        HttpSession session = request.getSession(false);
        ServletContext servletContext = request.getServletContext();

        ServletRequestModelAdapter servletRequestModelAdapter = new ServletRequestModelAdapter(request);
        ServletContextModelAdapter servletContextModelAdapter = new ServletContextModelAdapter(servletContext);

        if (session != null) {
          HttpSessionModelAdapter httpSessionModelAdapter = new HttpSessionModelAdapter(session);
          return new ExpressionResolverComposite(
                  new ModelAttributeResolver(context),
                  new ModelAttributeResolver(servletRequestModelAdapter), // 1
                  new ModelAttributeResolver(httpSessionModelAdapter), // 2
                  new ModelAttributeResolver(servletContextModelAdapter), // 3
                  sharedContext.getResolver()
          );
        }

        return new ExpressionResolverComposite(
                new ModelAttributeResolver(context),
                new ModelAttributeResolver(servletRequestModelAdapter), // 1
                new ModelAttributeResolver(servletContextModelAdapter), // 2
                sharedContext.getResolver()
        );
      }

      return new ExpressionResolverComposite(
              new ModelAttributeResolver(context),
              sharedContext.getResolver()
      );
    }
  }

}
