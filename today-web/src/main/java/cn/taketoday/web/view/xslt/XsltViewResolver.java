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

package cn.taketoday.web.view.xslt;

import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.view.AbstractUrlBasedView;
import cn.taketoday.web.view.UrlBasedViewResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * {@link ViewResolver} implementation that
 * resolves instances of {@link XsltView} by translating the supplied view name
 * into the URL of the XSLT stylesheet.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class XsltViewResolver extends UrlBasedViewResolver {

  @Nullable
  private String sourceKey;

  @Nullable
  private URIResolver uriResolver;

  @Nullable
  private ErrorListener errorListener;

  private boolean indent = true;

  @Nullable
  private Properties outputProperties;

  private boolean cacheTemplates = true;

  /**
   * This resolver requires {@link XsltView}.
   */
  public XsltViewResolver() {
    setViewClass(requiredViewClass());
  }

  /**
   * Set the name of the model attribute that represents the XSLT Source.
   * If not specified, the model map will be searched for a matching value type.
   * <p>The following source types are supported out of the box:
   * {@link javax.xml.transform.Source}, {@link org.w3c.dom.Document},
   * {@link org.w3c.dom.Node}, {@link java.io.Reader}, {@link java.io.InputStream}
   * and {@link cn.taketoday.core.io.Resource}.
   */
  public void setSourceKey(String sourceKey) {
    this.sourceKey = sourceKey;
  }

  /**
   * Set the URIResolver used in the transform.
   * <p>The URIResolver handles calls to the XSLT {@code document()} function.
   */
  public void setUriResolver(URIResolver uriResolver) {
    this.uriResolver = uriResolver;
  }

  /**
   * Set an implementation of the {@link ErrorListener}
   * interface for custom handling of transformation errors and warnings.
   * <p>If not set, a default {@link SimpleTransformErrorListener} is
   * used that simply logs warnings using the logger instance of the view class,
   * and rethrows errors to discontinue the XML transformation.
   *
   * @see SimpleTransformErrorListener
   */
  public void setErrorListener(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  /**
   * Set whether the XSLT transformer may add additional whitespace when
   * outputting the result tree.
   * <p>Default is {@code true} (on); set this to {@code false} (off)
   * to not specify an "indent" key, leaving the choice up to the stylesheet.
   *
   * @see javax.xml.transform.OutputKeys#INDENT
   */
  public void setIndent(boolean indent) {
    this.indent = indent;
  }

  /**
   * Set arbitrary transformer output properties to be applied to the stylesheet.
   * <p>Any values specified here will override defaults that this view sets
   * programmatically.
   *
   * @see javax.xml.transform.Transformer#setOutputProperty
   */
  public void setOutputProperties(Properties outputProperties) {
    this.outputProperties = outputProperties;
  }

  /**
   * Turn on/off the caching of the XSLT templates.
   * <p>The default value is "true". Only set this to "false" in development,
   * where caching does not seriously impact performance.
   */
  public void setCacheTemplates(boolean cacheTemplates) {
    this.cacheTemplates = cacheTemplates;
  }

  @Override
  protected Class<?> requiredViewClass() {
    return XsltView.class;
  }

  @Override
  protected AbstractUrlBasedView instantiateView() {
    return (getViewClass() == XsltView.class ? new XsltView() : super.instantiateView());
  }

  @Override
  protected AbstractUrlBasedView buildView(String viewName) throws Exception {
    XsltView view = (XsltView) super.buildView(viewName);
    if (this.sourceKey != null) {
      view.setSourceKey(this.sourceKey);
    }
    if (this.uriResolver != null) {
      view.setUriResolver(this.uriResolver);
    }
    if (this.errorListener != null) {
      view.setErrorListener(this.errorListener);
    }
    view.setIndent(this.indent);
    if (this.outputProperties != null) {
      view.setOutputProperties(this.outputProperties);
    }
    view.setCacheTemplates(this.cacheTemplates);
    return view;
  }

}
