/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.web.handler;

import java.io.Serializable;

import cn.taketoday.context.PathMatcher;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author TODAY <br>
 *         2019-12-05 00:46
 */
@Getter
@AllArgsConstructor
public class ResourceMatchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String requestPath;
    private final String matchedPattern;
    private final PathMatcher pathMatcher;
    private final ResourceRequestHandler handler;

    public final ResourceMapping getMapping() {
        return handler.getMapping();
    }
}
