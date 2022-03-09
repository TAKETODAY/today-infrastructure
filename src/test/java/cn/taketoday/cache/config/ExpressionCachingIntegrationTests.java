/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.cache.config;

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCacheManager;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

/**
 * @author Stephane Nicoll
 */
public class ExpressionCachingIntegrationTests {

	@Test  // SPR-11692
	@SuppressWarnings("unchecked")
	public void expressionIsCacheBasedOnActualMethod() {
		ConfigurableApplicationContext context =
				new AnnotationConfigApplicationContext(SharedConfig.class, Spr11692Config.class);

		BaseDao<User> userDao = (BaseDao<User>) context.getBean("userDao");
		BaseDao<Order> orderDao = (BaseDao<Order>) context.getBean("orderDao");

		userDao.persist(new User("1"));
		orderDao.persist(new Order("2"));

		context.close();
	}


	@Configuration
	static class Spr11692Config {

		@Bean
		public BaseDao<User> userDao() {
			return new UserDaoImpl();
		}

		@Bean
		public BaseDao<Order> orderDao() {
			return new OrderDaoImpl();
		}
	}


	private interface BaseDao<T> {

		T persist(T t);
	}


	private static class UserDaoImpl implements BaseDao<User> {

		@Override
		@CachePut(value = "users", key = "#user.id")
		public User persist(User user) {
			return user;
		}
	}


	private static class OrderDaoImpl implements BaseDao<Order> {

		@Override
		@CachePut(value = "orders", key = "#order.id")
		public Order persist(Order order) {
			return order;
		}
	}


	private static class User {

		private final String id;

		public User(String id) {
			this.id = id;
		}

		@SuppressWarnings("unused")
		public String getId() {
			return this.id;
		}
	}


	private static class Order {

		private final String id;

		public Order(String id) {
			this.id = id;
		}

		@SuppressWarnings("unused")
		public String getId() {
			return this.id;
		}
	}


	@Configuration
	@EnableCaching
	static class SharedConfig implements CachingConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}
	}

}
