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

package cn.taketoday.web.handler.mvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.util.WebUtils;

/**
 * Simple {@code Controller} implementation that transforms the virtual
 * path of a URL into a view name and returns that view.
 *
 * <p>Can optionally prepend a {@link #setPrefix prefix} and/or append a
 * {@link #setSuffix suffix} to build the viewname from the URL filename.
 *
 * <p>Find some examples below:
 * <ol>
 * <li>{@code "/index" -> "index"}</li>
 * <li>{@code "/index.html" -> "index"}</li>
 * <li>{@code "/index.html"} + prefix {@code "pre_"} and suffix {@code "_suf" -> "pre_index_suf"}</li>
 * <li>{@code "/products/view.html" -> "products/view"}</li>
 * </ol>
 *
 * <p>Thanks to David Barri for suggesting prefix/suffix support!
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setPrefix
 * @see #setSuffix
 * @since 4.0 2022/2/8 17:06
 */
public class UrlFilenameViewController extends AbstractUrlViewController {

  private String prefix = "";

  private String suffix = "";

  private boolean removeSemicolonContent = true;

  /** Request URL path String to view name String. */
  private final Map<String, String> viewNameCache = new ConcurrentHashMap<>(256);

  /**
   * Set the prefix to prepend to the request URL filename
   * to build a view name.
   */
  public void setPrefix(@Nullable String prefix) {
    this.prefix = (prefix != null ? prefix : "");
  }

  /**
   * Return the prefix to prepend to the request URL filename.
   */
  protected String getPrefix() {
    return this.prefix;
  }

  /**
   * Set the suffix to append to the request URL filename
   * to build a view name.
   */
  public void setSuffix(@Nullable String suffix) {
    this.suffix = (suffix != null ? suffix : "");
  }

  /**
   * Return the suffix to append to the request URL filename.
   */
  protected String getSuffix() {
    return this.suffix;
  }

  /**
   * Set if ";" (semicolon) content should be stripped from the request URI.
   * <p>Default is "true".
   */
  public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
    this.removeSemicolonContent = removeSemicolonContent;
  }

  /**
   * Returns view name based on the URL filename,
   * with prefix/suffix applied when appropriate.
   *
   * @see #extractViewNameFromUrlPath
   * @see #setPrefix
   * @see #setSuffix
   */
  @Override
  protected String getViewNameForRequest(RequestContext request) {
    String uri = extractOperableUrl(request);
    return getViewNameForUrlPath(uri);
  }

  /**
   * Extract a URL path from the given request,
   * suitable for view name extraction.
   *
   * @param request current HTTP request
   * @return the URL to use for view name extraction
   */
  protected String extractOperableUrl(RequestContext request) {
    String path = request.getRequestPath().value();
    path = removeSemicolonContent
           ? WebUtils.removeSemicolonContent(path)
           : path;
    return path;
  }

  /**
   * Returns view name based on the URL filename,
   * with prefix/suffix applied when appropriate.
   *
   * @param uri the request URI; for example {@code "/index.html"}
   * @return the extracted URI filename; for example {@code "index"}
   * @see #extractViewNameFromUrlPath
   * @see #postProcessViewName
   */
  protected String getViewNameForUrlPath(String uri) {
    return this.viewNameCache.computeIfAbsent(uri, u -> postProcessViewName(extractViewNameFromUrlPath(u)));
  }

  /**
   * Extract the URL filename from the given request URI.
   *
   * @param uri the request URI; for example {@code "/index.html"}
   * @return the extracted URI filename; for example {@code "index"}
   */
  protected String extractViewNameFromUrlPath(String uri) {
    int start = uri.charAt(0) == '/' ? 1 : 0;
    int lastIndex = uri.lastIndexOf('.');
    int end = lastIndex < 0 ? uri.length() : lastIndex;
    return uri.substring(start, end);
  }

  /**
   * Build the full view name based on the given view name
   * as indicated by the URL path.
   * <p>The default implementation simply applies prefix and suffix.
   * This can be overridden, for example, to manipulate upper case
   * / lower case, etc.
   *
   * @param viewName the original view name, as indicated by the URL path
   * @return the full view name to use
   * @see #getPrefix()
   * @see #getSuffix()
   */
  protected String postProcessViewName(String viewName) {
    return getPrefix() + viewName + getSuffix();
  }

}
