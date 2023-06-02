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

package cn.taketoday.web.view;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestToViewNameTranslator;
import cn.taketoday.web.util.WebUtils;

/**
 * {@link RequestToViewNameTranslator} that simply transforms the URI of
 * the incoming request into a view name.
 *
 * <p>Can be explicitly defined as the {@code viewNameTranslator} bean in a
 * {@link cn.taketoday.web.servlet.DispatcherServlet} context.
 * Otherwise, a plain default instance will be used.
 *
 * <p>The default transformation simply strips leading and trailing slashes
 * as well as the file extension of the URI, and returns the result as the
 * view name with the configured {@link #setPrefix prefix} and a
 * {@link #setSuffix suffix} added as appropriate.
 *
 * <p>The stripping of the leading slash and file extension can be disabled
 * using the {@link #setStripLeadingSlash stripLeadingSlash} and
 * {@link #setStripExtension stripExtension} properties, respectively.
 *
 * <p>Find below some examples of request to view name translation.
 * <ul>
 * <li>{@code http://localhost:8080/gamecast/display.html} &raquo; {@code display}</li>
 * <li>{@code http://localhost:8080/gamecast/displayShoppingCart.html} &raquo; {@code displayShoppingCart}</li>
 * <li>{@code http://localhost:8080/gamecast/admin/index.html} &raquo; {@code admin/index}</li>
 * </ul>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see RequestToViewNameTranslator
 * @see ViewResolver
 * @since 4.0
 */
public class DefaultRequestToViewNameTranslator implements RequestToViewNameTranslator {

  private static final String SLASH = "/";

  private String prefix = "";

  private String suffix = "";

  private String separator = SLASH;

  private boolean stripLeadingSlash = true;

  private boolean stripTrailingSlash = true;

  private boolean stripExtension = true;

  private boolean removeSemicolonContent = true;

  /**
   * Set the prefix to prepend to generated view names.
   *
   * @param prefix the prefix to prepend to generated view names
   */
  public void setPrefix(@Nullable String prefix) {
    this.prefix = (prefix != null ? prefix : "");
  }

  /**
   * Set the suffix to append to generated view names.
   *
   * @param suffix the suffix to append to generated view names
   */
  public void setSuffix(@Nullable String suffix) {
    this.suffix = (suffix != null ? suffix : "");
  }

  /**
   * Set the value that will replace '{@code /}' as the separator
   * in the view name. The default behavior simply leaves '{@code /}'
   * as the separator.
   */
  public void setSeparator(String separator) {
    this.separator = separator;
  }

  /**
   * Set whether or not leading slashes should be stripped from the URI when
   * generating the view name. Default is "true".
   */
  public void setStripLeadingSlash(boolean stripLeadingSlash) {
    this.stripLeadingSlash = stripLeadingSlash;
  }

  /**
   * Set whether or not trailing slashes should be stripped from the URI when
   * generating the view name. Default is "true".
   */
  public void setStripTrailingSlash(boolean stripTrailingSlash) {
    this.stripTrailingSlash = stripTrailingSlash;
  }

  /**
   * Set whether or not file extensions should be stripped from the URI when
   * generating the view name. Default is "true".
   */
  public void setStripExtension(boolean stripExtension) {
    this.stripExtension = stripExtension;
  }

  /**
   * Set if ";" (semicolon) content should be stripped from the request URI.
   * <p>Default is "true".
   */
  public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
    this.removeSemicolonContent = removeSemicolonContent;
  }

  /**
   * Translates the request URI of the incoming {@link RequestContext}
   * into the view name based on the configured parameters.
   *
   * @throws IllegalArgumentException if neither a parsed RequestPath, nor a
   * String lookupPath have been resolved and cached as a request attribute.
   * @see #transformPath
   */
  @Override
  public String getViewName(RequestContext request) {
    String lookupPath = removeSemicolonContent
                        ? WebUtils.removeSemicolonContent(request.getLookupPath().value())
                        : request.getLookupPath().value();
    return prefix + transformPath(lookupPath) + suffix;
  }

  /**
   * Transform the request URI (in the context of the webapp) stripping
   * slashes and extensions, and replacing the separator as required.
   *
   * @param lookupPath the lookup path for the current request,
   * as determined by the UrlPathHelper
   * @return the transformed path, with slashes and extensions stripped
   * if desired
   */
  @Nullable
  protected String transformPath(String lookupPath) {
    String path = lookupPath;
    if (this.stripLeadingSlash && path.startsWith(SLASH)) {
      path = path.substring(1);
    }
    if (this.stripTrailingSlash && path.endsWith(SLASH)) {
      path = path.substring(0, path.length() - 1);
    }
    if (this.stripExtension) {
      path = StringUtils.stripFilenameExtension(path);
    }
    if (!SLASH.equals(this.separator)) {
      path = StringUtils.replace(path, SLASH, this.separator);
    }
    return path;
  }

}
