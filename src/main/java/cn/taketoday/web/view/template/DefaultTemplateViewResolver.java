/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import java.beans.FeatureDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Objects;

import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELManager;
import javax.el.ELProcessor;
import javax.el.ELResolver;
import javax.el.EvaluationListener;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.StandardELContext;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ui.Model;

/**
 * @author TODAY <br>
 *         2019-11-24 22:28
 */
@Props(prefix = "web.mvc.view.")
public class DefaultTemplateViewResolver extends AbstractTemplateViewResolver {

    private final StandardELContext sharedContext;
    private final ExpressionFactory expressionFactory = ELManager.getExpressionFactory();

    public DefaultTemplateViewResolver() {
        final ELProcessor elProcessor = ContextUtils.getELProcessor();
        final ELManager elManager = elProcessor.getELManager();
        sharedContext = elManager.getELContext();
    }

    @Override
    public void resolveView(final String templateName, final RequestContext context) throws Throwable {

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
     *            Current {@link RequestContext}
     * @param rendered
     *            Rendered text
     * @throws IOException
     *             If an input or output exception occurred
     * @throws IllegalStateException
     *             For Servlet Environment if the <code>getOutputStream</code>
     *             method has already been called for this response object
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
     *            input text string
     * @param context
     *            Current {@link RequestContext}
     * @return Rendered text string
     */
    protected String renderTemplate(final String text, RequestContext context) {

        final ELContext elContext = prepareContext(context);

        final ValueExpression expression = //
                expressionFactory.createValueExpression(elContext, text, String.class);

        return expression.getValue(elContext).toString();
    }

    /**
     * Read template source as text string
     * 
     * @param template
     *            Template location
     * @return template source text string
     * @throws IOException
     *             If an input or output exception occurred
     */
    protected String readTemplate(final String template) throws IOException {
        final Resource resource = ResourceUtils.getResource(template);
        return StringUtils.readAsText(resource.getInputStream());
    }

    protected ELContext prepareContext(RequestContext context) {
        return new TemplateViewResolverELContext(this.sharedContext, context);
    }

    public static class TemplateViewResolverELContext extends ELContext {

        private final ELResolver elResolver;
        private final StandardELContext delegate;

        public TemplateViewResolverELContext(StandardELContext delegate, RequestContext context) {
            this.delegate = delegate;
            this.elResolver = new CompositeELResolver(new ModelAttributeELResolver(context),
                                                      delegate.getELResolver());
        }

        @Override
        public ELResolver getELResolver() {
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
        public void addEvaluationListener(EvaluationListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void notifyAfterEvaluation(String expr) {}

        @Override
        public void notifyBeforeEvaluation(String expr) {}

        @Override
        public void notifyPropertyResolved(Object base, Object property) {}

    }

    /**
     * For the {@link Model} attribute
     * 
     * @author TODAY <br>
     *         2019-11-25 19:48
     */
    public static class ModelAttributeELResolver extends ELResolver {

        private final RequestContext context;

        public ModelAttributeELResolver(RequestContext context) {
            this.context = context;
        }

        @Override
        public Object getValue(ELContext context, Object base, Object property) {

            if (base == null && property instanceof String) {
                final RequestContext requestContext = this.context;
                if (requestContext.containsAttribute((String) property)) {
                    Objects.requireNonNull(context).setPropertyResolved(base, property);
                    return requestContext.attribute((String) property);
                }
            }
            return null;
        }

        @Override
        public void setValue(ELContext elContext, Object base, Object property, Object value) {

            if (base == null && property instanceof String) {
                final String beanName = (String) property;
                final RequestContext context = this.context;
                if (context.containsAttribute(beanName)) {
                    context.attribute(beanName, value);
                    Objects.requireNonNull(elContext).setPropertyResolved(base, property);
                }
            }
        }

        @Override
        public Class<?> getType(ELContext elContext, Object base, Object property) {

            if (base == null && property instanceof String) {
                final RequestContext context = this.context;
                if (context.containsAttribute((String) property)) {
                    Objects.requireNonNull(elContext).setPropertyResolved(true);
                    return context.attribute((String) property).getClass();
                }
            }
            return null;
        }

        @Override
        public boolean isReadOnly(ELContext elContext, Object base, Object property) {

            if (base == null && property instanceof String) {
                if (context.containsAttribute((String) property)) {
                    Objects.requireNonNull(elContext).setPropertyResolved(true);
                    return false;
                }
            }
            return false;
        }

        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
            return null;
        }

        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            return String.class;
        }
    }

}
