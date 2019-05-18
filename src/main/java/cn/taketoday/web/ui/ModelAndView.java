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
package cn.taketoday.web.ui;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Today <br>
 *
 *         2018-12-02 19:54
 */
@Getter
@Setter
public class ModelAndView {

    private Object view;
    private String contentType = null;
    private final Map<String, Object> dataModel;

    public ModelAndView() {
        dataModel = new HashMap<>(16, 1.0f);
    }

    public ModelAndView(Object view) {
        this();
        this.view = view;
    }

    public ModelAndView(Object view, Map<String, Object> dataModel) {
        this.view = view;
        this.dataModel = dataModel;
    }

    public ModelAndView(Object view, String modelName, Object modelObject) {
        this();
        this.view = view;
        addAttribute(modelName, modelObject);
    }

    public ModelAndView setView(Object view) {
        this.view = view;
        return this;
    }

    /**
     * 
     * @return
     */
    public final boolean noView() {
        return this.view == null;
    }

    public ModelAndView addAttribute(String attributeName, Object attributeValue) {
        dataModel.put(attributeName, attributeValue);
        return this;
    }

    public ModelAndView addAllAttributes(Map<String, Object> attributes) {
        dataModel.putAll(attributes);
        return this;
    }

}
