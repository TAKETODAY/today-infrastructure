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

package cn.taketoday.framework.web.error;

import cn.taketoday.beans.factory.annotation.Value;

/**
 * Configuration properties for web error handling.
 *
 * @author Michael Stummvoll
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Scott Frederick
 * @since 4.0
 */
public class ErrorProperties {

  /**
   * Path of the error controller.
   */
  @Value("${error.path:/error}")
  private String path = "/error";

  /**
   * Include the "exception" attribute.
   */
  private boolean includeException;

  /**
   * When to include the "trace" attribute.
   */
  private IncludeAttribute includeStacktrace = IncludeAttribute.NEVER;

  /**
   * When to include "message" attribute.
   */
  private IncludeAttribute includeMessage = IncludeAttribute.NEVER;

  /**
   * When to include "errors" attribute.
   */
  private IncludeAttribute includeBindingErrors = IncludeAttribute.NEVER;

  private final Whitelabel whitelabel = new Whitelabel();

  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public boolean isIncludeException() {
    return this.includeException;
  }

  public void setIncludeException(boolean includeException) {
    this.includeException = includeException;
  }

  public IncludeAttribute getIncludeStacktrace() {
    return this.includeStacktrace;
  }

  public void setIncludeStacktrace(IncludeAttribute includeStacktrace) {
    this.includeStacktrace = includeStacktrace;
  }

  public IncludeAttribute getIncludeMessage() {
    return this.includeMessage;
  }

  public void setIncludeMessage(IncludeAttribute includeMessage) {
    this.includeMessage = includeMessage;
  }

  public IncludeAttribute getIncludeBindingErrors() {
    return this.includeBindingErrors;
  }

  public void setIncludeBindingErrors(IncludeAttribute includeBindingErrors) {
    this.includeBindingErrors = includeBindingErrors;
  }

  public Whitelabel getWhitelabel() {
    return this.whitelabel;
  }

  public static class Whitelabel {

    /**
     * Whether to enable the default error page displayed in browsers in case of a
     * server error.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
      return this.enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

  }

}
