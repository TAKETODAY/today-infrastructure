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
package cn.taketoday.framework.config;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.utils.DataSize;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author TODAY <br>
 *         2019-02-07 16:31
 */
@Setter
@Getter
@MissingBean
public class CompressionConfiguration {

    private String level = "on";
    private boolean enable = false;

    private String[] excludePaths;
    private String[] includedPaths;
    private String[] excludeMethods;
    private String[] includeMethods;

    private String[] excludeUserAgents;
    private String[] excludeAgentPatterns;

    private String[] includeAgentPatterns;

    private DataSize minResponseSize = DataSize.ofKilobytes(2);

    private String[] mimeTypes = new String[] { //
            "text/html", "text/xml", "text/plain", //
            "text/javascript", "application/javascript", //
            "text/css", "text/javascript", "application/xml"//
    };
}
