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
package cn.taketoday.test.domain;

import java.util.Properties;

import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.InitializingBean;

/**
 * @author Today <br>
 * 	
 *		2018-08-08 15:06
 */
@Singleton
public class ConfigFactoryBean implements FactoryBean<Config>, InitializingBean, DisposableBean {

	@Props(value = "info", prefix = "site.")
	private Properties pro;
	
	private Config bean;
	
	@Override
	public Config getBean() {
		return bean;
	}
	
	@Override
	public String getBeanName() {
		return "FactoryBean-Config";
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() throws Exception {
		pro.clear();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		bean = new Config();
		
		bean.setCdn(pro.getProperty("site.cdn"));
		bean.setHost(pro.getProperty("site.host"));
		bean.setBaiduCode(pro.getProperty("site.baiduCode"));
		bean.setCopyright(pro.getProperty("site.copyright"));
	}

}
