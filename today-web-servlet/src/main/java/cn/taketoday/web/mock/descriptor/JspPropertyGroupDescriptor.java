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

package cn.taketoday.web.mock.descriptor;

import java.util.Collection;

/**
 * This interface provides access to the <code>&lt;jsp-property-group&gt;</code> related configuration of a web
 * application.
 *
 * <p>
 * The configuration is aggregated from the <code>web.xml</code> and <code>web-fragment.xml</code> descriptor files of
 * the web application.
 *
 * @since Servlet 3.0
 */
public interface JspPropertyGroupDescriptor {

  /**
   * Gets the URL patterns of the JSP property group represented by this <code>JspPropertyGroupDescriptor</code>.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the URL patterns of the JSP property group represented by this
   * <code>JspPropertyGroupDescriptor</code>
   */
  public Collection<String> getUrlPatterns();

  /**
   * Gets the value of the <code>el-ignored</code> configuration, which specifies whether Expression Language (EL)
   * evaluation is enabled for any JSP pages mapped to the JSP property group represented by this
   * <code>JspPropertyGroupDescriptor</code>.
   *
   * @return the value of the <code>el-ignored</code> configuration, or null if unspecified
   */
  public String getElIgnored();

  /**
   * Will the use of an unknown identifier in EL within a JSP page trigger an error for this group?
   *
   * @return {@code true} if an error will be triggered, otherwise {@code false}
   * @since Servlet 6.0
   */
  public String getErrorOnELNotFound();

  /**
   * Gets the value of the <code>page-encoding</code> configuration, which specifies the default page encoding for any JSP
   * pages mapped to the JSP property group represented by this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return the value of the <code>page-encoding</code> configuration, or null if unspecified
   */
  public String getPageEncoding();

  /**
   * Gets the value of the <code>scripting-invalid</code> configuration, which specifies whether scripting is enabled for
   * any JSP pages mapped to the JSP property group represented by this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return the value of the <code>scripting-invalid</code> configuration, or null if unspecified
   */
  public String getScriptingInvalid();

  /**
   * Gets the value of the <code>is-xml</code> configuration, which specifies whether any JSP pages mapped to the JSP
   * property group represented by this <code>JspPropertyGroupDescriptor</code> will be treated as JSP documents (XML
   * syntax).
   *
   * @return the value of the <code>is-xml</code> configuration, or null if unspecified
   */
  public String getIsXml();

  /**
   * Gets the <code>include-prelude</code> configuration of the JSP property group represented by this
   * <code>JspPropertyGroupDescriptor</code>.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the <code>include-prelude</code> configuration of the JSP
   * property group represented by this <code>JspPropertyGroupDescriptor</code>
   */
  public Collection<String> getIncludePreludes();

  /**
   * Gets the <code>include-coda</code> configuration of the JSP property group represented by this
   * <code>JspPropertyGroupDescriptor</code>.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the <code>include-coda</code> configuration of the JSP property
   * group represented by this <code>JspPropertyGroupDescriptor</code>
   */
  public Collection<String> getIncludeCodas();

  /**
   * Gets the value of the <code>deferred-syntax-allowed-as-literal</code> configuration, which specifies whether the
   * character sequence <code>&quot;#{&quot;</code>, which is normally reserved for Expression Language (EL) expressions,
   * will cause a translation error if it appears as a String literal in any JSP pages mapped to the JSP property group
   * represented by this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return the value of the <code>deferred-syntax-allowed-as-literal</code> configuration, or null if unspecified
   */
  public String getDeferredSyntaxAllowedAsLiteral();

  /**
   * Gets the value of the <code>trim-directive-whitespaces</code> configuration, which specifies whether template text
   * containing only whitespaces must be removed from the response output of any JSP pages mapped to the JSP property
   * group represented by this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return the value of the <code>trim-directive-whitespaces</code> configuration, or null if unspecified
   */
  public String getTrimDirectiveWhitespaces();

  /**
   * Gets the value of the <code>default-content-type</code> configuration, which specifies the default response content
   * type for any JSP pages mapped to the JSP property group represented by this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return the value of the <code>default-content-type</code> configuration, or null if unspecified
   */
  public String getDefaultContentType();

  /**
   * Gets the value of the <code>buffer</code> configuration, which specifies the default size of the response buffer for
   * any JSP pages mapped to the JSP property group represented by this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return the value of the <code>buffer</code> configuration, or null if unspecified
   */
  public String getBuffer();

  /**
   * Gets the value of the <code>error-on-undeclared-namespace</code> configuration, which specifies whether an error will
   * be raised at translation time if tag with an undeclared namespace is used in any JSP pages mapped to the JSP property
   * group represented by this <code>JspPropertyGroupDescriptor</code>.
   *
   * @return the value of the <code>error-on-undeclared-namespace</code> configuration, or null if unspecified
   */
  public String getErrorOnUndeclaredNamespace();
}
