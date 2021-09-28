/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view.template;

import java.io.IOException;
import java.util.Locale;

import cn.taketoday.core.Constant;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 * 2018-06-26 11:58:24
 */
public abstract class AbstractTemplateRenderer
        extends OrderedSupport implements TemplateRenderer {

  protected Locale locale = Locale.CHINA;
  protected String suffix = Constant.BLANK;
  protected String prefix = DEFAULT_TEMPLATE_PATH;
  protected String encoding = Constant.DEFAULT_ENCODING;

  public AbstractTemplateRenderer() { }

  public AbstractTemplateRenderer(int order) {
    super(order);
  }

  @Override
  public abstract void render(String templateName, RequestContext context)
          throws IOException;

  /**
   * Prepare a template
   *
   * @param name
   *         Template name
   *
   * @return Returns full path of the template.
   */
  protected String prepareTemplate(String name) {
    return new StringBuilder(64)
            .append(prefix)
            .append(StringUtils.formatURL(name))
            .append(suffix)
            .toString();
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public Locale getLocale() {
    return locale;
  }

  public String getEncoding() {
    return encoding;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getSuffix() {
    return suffix;
  }
}
