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
package cn.taketoday.web.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * views request mapping
 * 
 * @author Today <br>
 * 
 *         2018-06-25 19:58:07
 * @version 2.0.0
 */
@SuppressWarnings("serial")
public class ViewMapping extends HandlerMapping implements WebMapping {

    /** 资源路径 */
    private String assetsPath = "";
    /** The resource's content type @since 2.3.3 */
    private String contentType = null;

    /** The request status @since 2.3.7 */
    private int status;

    /** view 视图映射池 */
    private static final Map<String, ViewMapping> VIEW_REQUEST_MAPPING = new HashMap<>(16, 1f);

    public ViewMapping(Object bean, HandlerMethod handlerMethod) {
        super(bean, handlerMethod, Collections.emptyList());
    }

    public final boolean hasAction() {
        return getHandlerMethod() != null;
    }

    public int getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAssetsPath() {
        return assetsPath;
    }

    public void setAssetsPath(String assetsPath) {
        this.assetsPath = assetsPath;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[assetsPath=");
        builder.append(assetsPath);
        builder.append(", contentType=");
        builder.append(contentType);
        builder.append(", status=");
        builder.append(status);
        builder.append("]");
        return builder.toString();
    }

    // ---------------static

    public static ViewMapping get(String key) {
        return VIEW_REQUEST_MAPPING.get(key);
    }

    public static Map<String, ViewMapping> getMappings() {
        return VIEW_REQUEST_MAPPING;
    }

    public static void register(String name, ViewMapping viewMapping) {
        VIEW_REQUEST_MAPPING.put(name, viewMapping);
    }
}
