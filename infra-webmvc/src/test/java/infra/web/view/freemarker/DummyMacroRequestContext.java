/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.view.freemarker;

import java.util.List;
import java.util.Map;

import infra.web.BindStatus;
import infra.web.RequestContext;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;

/**
 * Dummy request context used for FreeMarker macro tests.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 25.01.2005
 */
public class DummyMacroRequestContext {

  private final RequestContext request;

  private Map<String, String> messageMap;

  private String contextPath;

  public DummyMacroRequestContext(RequestContext request) {
    this.request = request;
  }

  public void setMessageMap(Map<String, String> messageMap) {
    this.messageMap = messageMap;
  }

  /**
   * @see infra.web.RequestContext#getMessage(String)
   */
  public String getMessage(String code) {
    return this.messageMap.get(code);
  }

  /**
   * @see infra.web.RequestContext#getMessage(String, String)
   */
  public String getMessage(String code, String defaultMsg) {
    String msg = this.messageMap.get(code);
    return (msg != null ? msg : defaultMsg);
  }

  /**
   * @see infra.web.RequestContext#getMessage(String, List)
   */
  public String getMessage(String code, List<?> args) {
    return this.messageMap.get(code) + args;
  }

  /**
   * @see infra.web.RequestContext#getMessage(String, List, String)
   */
  public String getMessage(String code, List<?> args, String defaultMsg) {
    String msg = this.messageMap.get(code);
    return (msg != null ? msg + args : defaultMsg);
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public String getContextPath() {
    return this.contextPath;
  }

  public String getContextUrl(String relativeUrl) {
    return getContextPath() + relativeUrl;
  }

  public String getContextUrl(String relativeUrl, Map<String, String> params) {
    UriComponents uric = UriComponentsBuilder.forURIString(relativeUrl).buildAndExpand(params);
    return getContextPath() + uric.toURI().toASCIIString();
  }

  /**
   * @see infra.web.RequestContext#getBindStatus(String)
   */
  public BindStatus getBindStatus(String path) throws IllegalStateException {
    return getBindStatus(path, false);
  }

  /**
   * @see infra.web.RequestContext#getBindStatus(String, boolean)
   */
  public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
    return new BindStatus(request, path, htmlEscape);
  }

}
