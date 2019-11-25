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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view.template;

import java.util.Locale;

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2018-06-26 11:58:24
 */
@Setter
@Getter
public abstract class AbstractTemplateViewResolver implements TemplateViewResolver {

    protected String suffix = Constant.BLANK;
    protected Locale locale = Locale.CHINA;
    protected String prefix = Constant.DEFAULT_TEMPLATE_PATH;
    protected String encoding = Constant.DEFAULT_ENCODING;

    /**
     * Prepare a template
     * 
     * @param name
     *            Template name
     * @return Returns full path of the template.
     */
    protected String prepareTemplate(String name) {
        return new StringBuilder(64)
                .append(prefix)
                .append(StringUtils.checkUrl(name))
                .append(suffix)
                .toString();
    }

}
