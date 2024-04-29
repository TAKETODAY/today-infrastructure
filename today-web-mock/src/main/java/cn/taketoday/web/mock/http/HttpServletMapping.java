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

package cn.taketoday.web.mock.http;

/**
 * <p>
 * Allows runtime discovery of the manner in which the {@link HttpServlet} for the current {@link HttpServletRequest}
 * was invoked. Invoking any of the methods must not block the caller. The implementation must be thread safe. Instances
 * are immutable and are returned from {@link HttpServletRequest#getHttpServletMapping}.
 * </p>
 *
 * <p>
 * Following are some illustrative examples for various combinations of mappings. Consider the following Servlet
 * declaration:
 * </p>
 *
 * <pre>
 * <code>
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;MyServlet&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;MyServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *     &lt;servlet-name&gt;MyServlet&lt;/servlet-name&gt;
 *     &lt;url-pattern&gt;/MyServlet&lt;/url-pattern&gt;
 *     &lt;url-pattern&gt;""&lt;/url-pattern&gt;
 *     &lt;url-pattern&gt;*.extension&lt;/url-pattern&gt;
 *     &lt;url-pattern&gt;/path/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </code>
 * </pre>
 *
 * <p>
 * The expected values of the properties for various incoming URI path values are as shown in this table. The
 * {@code servletName} column is omitted as its value is always {@code MyServlet}.
 * </p>
 *
 * <table border="1">
 * <caption>Expected values of properties for various URI paths</caption>
 * <tr>
 * <th>URI Path (in quotes)</th>
 * <th>matchValue</th>
 * <th>pattern</th>
 * <th>mappingMatch</th>
 * </tr>
 * <tr>
 * <td>""</td>
 * <td>""</td>
 * <td>""</td>
 * <td>CONTEXT_ROOT</td>
 * </tr>
 * <tr>
 * <td>"/index.html"</td>
 * <td>""</td>
 * <td>/</td>
 * <td>DEFAULT</td>
 * </tr>
 * <tr>
 * <td>"/MyServlet"</td>
 * <td>MyServlet</td>
 * <td>/MyServlet</td>
 * <td>EXACT</td>
 * </tr>
 * <tr>
 * <td>"/foo.extension"</td>
 * <td>foo</td>
 * <td>*.extension</td>
 * <td>EXTENSION</td>
 * </tr>
 * <tr>
 * <td>"/path/foo"</td>
 * <td>foo</td>
 * <td>/path/*</td>
 * <td>PATH</td>
 * </tr>
 *
 * </table>
 *
 * @since Servlet 4.0
 */
public interface HttpServletMapping {

  /**
   * <p>
   * Return the portion of the URI path that caused this request to be matched. If the {@link #getMappingMatch} value is
   * {@code
   * CONTEXT_ROOT} or {@code DEFAULT}, this method must return the empty string. If the {@link #getMappingMatch} value is
   * {@code
   * EXACT}, this method must return the portion of the path that matched the servlet, omitting any leading slash. If the
   * {@link #getMappingMatch} value is {@code EXTENSION} or {@code PATH}, this method must return the value that matched
   * the '*'. See the class javadoc for examples.
   * </p>
   *
   * @return the match.
   */
  public String getMatchValue();

  /**
   * <p>
   * Return the String representation for the {@code url-pattern} for this mapping. If the {@link #getMappingMatch} value
   * is {@code
   * CONTEXT_ROOT}, this method must return the empty string. If the {@link #getMappingMatch} value is {@code
   * EXTENSION}, this method must return the pattern, without any leading slash. Otherwise, this method returns the
   * pattern exactly as specified in the descriptor or Java configuration.
   * </p>
   *
   * @return the String representation for the {@code url-pattern} for this mapping.
   */
  public String getPattern();

  /**
   * <p>
   * Return the String representation for the {@code servlet-name} for this mapping. If the Servlet providing the response
   * is the default servlet, the return from this method is the name of the default servlet, which is container specific.
   * </p>
   *
   * @return the String representation for the {@code servlet-name} for this mapping.
   */
  public String getServletName();

  /**
   * <p>
   * Return the {@link MappingMatch} for this instance
   * </p>
   *
   * @return the {@code MappingMatch} for this instance.
   */
  public MappingMatch getMappingMatch();

}
