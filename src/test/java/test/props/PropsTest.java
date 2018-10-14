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
package test.props;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.ApplicationContext;

import org.junit.Test;

/**
 * @author Today <br>
 * 
 *         2018-10-09 15:06
 */
public class PropsTest {

	@Test
	public void test_Props(){

		ApplicationContext applicationContext = new StandardApplicationContext(false);

		Config_ bean = applicationContext.getBean(Config_.class);

		assert bean.getHost() != null;
		
		System.out.println(bean);

		applicationContext.close();
	}

}
