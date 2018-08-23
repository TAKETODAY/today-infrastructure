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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cn.taketoday.web.Constant;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.mapping.ViewMapping;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月23日 下午4:19:53
 */
@Slf4j
public final class ViewConfig {

	public void init(Element element) throws Exception {

		// <common/> element
		String prefix = element.getAttribute(Constant.ATTR_PREFIX); // prefix
		String suffix = element.getAttribute(Constant.ATTR_SUFFIX); // suffix

		NodeList nl = element.getChildNodes(); // <view/>
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				process(element, prefix, suffix, node);
			}
		}
	}

	/**
	 * put mapping
	 * 
	 * @param element
	 * @param baseDir
	 * @param suffix
	 * @param node
	 */
	private void process(Element element, String baseDir, String suffix, Node node) {
		ViewMapping mapping = new ViewMapping();
		Element view = (Element) node;
		String name = view.getAttribute(Constant.ATTR_NAME); // request uri
		String res = view.getAttribute(Constant.ATTR_ASSET); // res
		String returnType = element.getAttribute(Constant.ATTR_TYPE); // return type

		if (Constant.REDIRECT_URL_PREFIX.equals(returnType)) {
			mapping.setReturnType(Constant.TYPE_REDIRECT);
		}
		mapping.setAssetsPath(baseDir + res + suffix);

		name = (name.startsWith("/") ? name : "/" + name);

		DispatchHandler.VIEW_REQUEST_MAPPING.put(name, mapping);
		log.info("View Mapped [{} -> {}]", name, mapping);
	}

}
