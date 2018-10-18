/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.config;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.mapping.ViewMapping;
import cn.taketoday.web.servlet.ViewDispatcher;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-06-23 16:19:53
 */
@Slf4j
public final class ViewConfig {

	public void init(Element element, String contextPath) throws Exception {
		// <common/> element
		String prefix = element.getAttribute(Constant.ATTR_PREFIX); // prefix
		String suffix = element.getAttribute(Constant.ATTR_SUFFIX); // suffix

		NodeList nl = element.getChildNodes(); // <view/>
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				process(element, prefix, suffix, node, contextPath);
			}
		}
	}

	/**
	 * process
	 * 
	 * @param element
	 * @param prefix
	 * @param suffix
	 * @param node
	 * @param contextPath
	 * @throws ConfigurationException
	 */
	private void process(Element element, String prefix, String suffix, Node node, String contextPath)
			throws ConfigurationException {

		ViewMapping mapping = new ViewMapping();
		Element view = (Element) node;
		String name = view.getAttribute(Constant.ATTR_NAME); // request uri
		String res = view.getAttribute(Constant.ATTR_ASSET); // res
		String returnType = view.getAttribute(Constant.ATTR_TYPE); // return type

		if (StringUtils.isEmpty(res)) {
			throw new ConfigurationException(
					"You must specify a 'res' attribute like this: [<view res=\"https://taketoday.cn\" name=\"TODAY-BLOG\" type=\"redirect\"/>]");
		}

		res = prefix + res + suffix;
		if (Constant.VALUE_REDIRECT.equals(returnType)) {// redirect
			mapping.setReturnType(Constant.TYPE_REDIRECT);
			if (!res.startsWith(Constant.HTTP)) {
				res = contextPath + res;
			}
		}

		mapping.setAssetsPath(res);

		name = contextPath + (name.startsWith("/") ? name : "/" + name);
		ViewDispatcher.VIEW_REQUEST_MAPPING.put(name, mapping);
		log.info("View Mapped [{} -> {}]", name, mapping);
	}

}
