/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.ui.Model;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;

/**
 * A specialization of the {@link Model} interface that controllers can use to
 * select attributes for a redirect scenario. Since the intent of adding
 * redirect attributes is very explicit --  i.e. to be used for a redirect URL,
 * attribute values may be formatted as Strings and stored that way to make
 * them eligible to be appended to the query string or expanded as URI
 * variables in {@code cn.taketoday.web.view.RedirectView}.
 *
 * <p>Example usage in an {@code @Controller}:
 * <pre class="code">
 * &#064;RequestMapping(value = "/accounts", method = HttpMethod.POST)
 * public String handle(Account account, BindingResult result, RedirectModel redirectAttrs) {
 *   if (result.hasErrors()) {
 *     return "accounts/new";
 *   }
 *   // Save account ...
 *   redirectAttrs.addAttribute("id", account.getId());
 *   redirectAttrs.addAttribute("message", "Account created!");
 *   return "redirect:/accounts/{id}";
 * }
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestContextUtils#getOutputRedirectModel
 * @see RequestContext#getInputRedirectModel
 * @since 2.3.3 2018-11-18 16:39
 */
public class RedirectModel extends ModelMap implements Serializable, Comparable<RedirectModel> {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Name of request attribute that holds a RedirectModel with "input"
   * redirect attributes saved by a previous request, if any.
   *
   * @see RequestContext#getInputRedirectModel()
   */
  public static final String INPUT_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          RedirectModel.class, "INPUT");

  /**
   * Name of request attribute that holds the "output" {@link RedirectModel} with
   * attributes to save for a subsequent request.
   *
   * @see RequestContextUtils#getOutputRedirectModel(RequestContext)
   */
  public static final String OUTPUT_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          RedirectModel.class, "OUTPUT");

  // @since 4.0
  @Nullable
  private String targetRequestPath;

  // @since 4.0
  private final MultiValueMap<String, String> targetRequestParams = MultiValueMap.forLinkedHashMap(3);

  // @since 4.0
  private long expirationTime = -1;

  /**
   * Construct a new, empty RedirectModel.
   */
  public RedirectModel() { }

  /**
   * Construct a new {@code ModelMap} containing the supplied attribute
   * under the supplied name.
   *
   * @see #setAttribute(String, Object)
   */
  public RedirectModel(String attributeName, @Nullable Object attributeValue) {
    setAttribute(attributeName, attributeValue);
  }

  /**
   * Provide a URL path to help identify the target request for this RedirectModel.
   * <p>The path may be absolute (e.g. "/application/resource") or relative to the
   * current request (e.g. "../resource").
   *
   * @since 4.0
   */
  public void setTargetRequestPath(@Nullable String path) {
    this.targetRequestPath = path;
  }

  /**
   * Return the target URL path (or {@code null} if none specified).
   *
   * @since 4.0
   */
  @Nullable
  public String getTargetRequestPath() {
    return this.targetRequestPath;
  }

  /**
   * Provide request parameters identifying the request for this RedirectModel.
   *
   * @param params a Map with the names and values of expected parameters
   * @since 4.0
   */
  public void addTargetRequestParams(@Nullable MultiValueMap<String, String> params) {
    if (params != null) {
      for (Map.Entry<String, List<String>> entry : params.entrySet()) {
        String key = entry.getKey();
        for (String value : entry.getValue()) {
          addTargetRequestParam(key, value);
        }
      }
    }
  }

  /**
   * Provide a request parameter identifying the request for this RedirectModel.
   *
   * @param name the expected parameter name (skipped if empty)
   * @param value the expected value (skipped if empty)
   * @since 4.0
   */
  public void addTargetRequestParam(String name, String value) {
    if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
      this.targetRequestParams.add(name, value);
    }
  }

  /**
   * Return the parameters identifying the target request, or an empty map.
   *
   * @since 4.0
   */
  public MultiValueMap<String, String> getTargetRequestParams() {
    return this.targetRequestParams;
  }

  /**
   * Start the expiration period for this instance.
   *
   * @param timeToLive the number of seconds before expiration
   * @since 4.0
   */
  public void startExpirationPeriod(int timeToLive) {
    this.expirationTime = System.currentTimeMillis() + timeToLive * 1000L;
  }

  /**
   * Set the expiration time for the RedirectModel. This is provided for serialization
   * purposes but can also be used instead {@link #startExpirationPeriod(int)}.
   *
   * @since 4.0
   */
  public void setExpirationTime(long expirationTime) {
    this.expirationTime = expirationTime;
  }

  /**
   * Return the expiration time for the RedirectModel or -1 if the expiration
   * period has not started.
   *
   * @since 4.0
   */
  public long getExpirationTime() {
    return this.expirationTime;
  }

  /**
   * Return whether this instance has expired depending on the amount of
   * elapsed time since the call to {@link #startExpirationPeriod}.
   *
   * @since 4.0
   */
  public boolean isExpired() {
    return this.expirationTime != -1 && System.currentTimeMillis() > this.expirationTime;
  }

  /**
   * Compare two RedirectModels and prefer the one that specifies a target URL
   * path or has more target URL parameters. Before comparing RedirectModel
   * instances ensure that they match a given request.
   */
  @Override
  public int compareTo(RedirectModel other) {
    int thisUrlPath = this.targetRequestPath != null ? 1 : 0;
    int otherUrlPath = other.targetRequestPath != null ? 1 : 0;
    if (thisUrlPath != otherUrlPath) {
      return otherUrlPath - thisUrlPath;
    }
    else {
      return other.targetRequestParams.size() - this.targetRequestParams.size();
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RedirectModel model)) {
      return false;
    }
    return super.equals(model)
            && Objects.equals(this.targetRequestPath, model.targetRequestPath)
            && this.targetRequestParams.equals(model.targetRequestParams);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.targetRequestPath);
    result = 31 * result + this.targetRequestParams.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "RedirectModel [attributes=" + super.toString() + ", targetRequestPath=" +
            targetRequestPath + ", targetRequestParams=" + targetRequestParams + "]";
  }

  /**
   * Return "output" RedirectModel to save attributes for request after redirect.
   *
   * @param attributeAccessor attributeAccessor to use for saving attributes
   * @return a {@link RedirectModel} instance
   */
  @Nullable
  @Experimental
  public static RedirectModel findOutputModel(AttributeAccessor attributeAccessor) {
    Object attribute = attributeAccessor.getAttribute(RedirectModel.OUTPUT_ATTRIBUTE);
    if (attribute instanceof RedirectModel) {
      return (RedirectModel) attribute;
    }
    return null;
  }

}
