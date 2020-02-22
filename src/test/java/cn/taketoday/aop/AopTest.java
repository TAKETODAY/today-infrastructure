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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import cn.taketoday.aop.listener.AspectsDestroyListener;
import cn.taketoday.aop.proxy.AutoProxyCreator;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.factory.StandardBeanFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2018-08-10 21:29
 */
@Slf4j
public class AopTest {

    static {
//        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:/debug");
    }

    @Import({ //
        AspectsDestroyListener.class, //
        DefaultUserService.class, //
        LogAspect.class, //
        MemUserDao.class, //
        TestInterceptor.class, //
        TimeAspect.class//
    })
    static class AopConfig {

    }

    @Test
    public void testAop() throws Throwable {

        try (StandardApplicationContext context = new StandardApplicationContext()) {

            final StandardBeanFactory beanFactory = context.getBeanFactory();

            beanFactory.loadImportBeans(AopConfig.class);
            context.addBeanPostProcessor(new AutoProxyCreator(context));
            
            UserService userService = context.getBean(UserService.class);
            
            final Class<? extends UserService> userServiceClass = userService.getClass();
            assertNotEquals(DefaultUserService.class, userServiceClass);
            assertEquals(DefaultUserService.class, userServiceClass.getSuperclass());
            
            
            User user = new User();
            user.setPassword("666");
            user.setEmail("666");
            long start = System.currentTimeMillis();
            User login = userService.login(user);

//            for (int i = 0; i < 1000; i++) {
//                login = bean.login(user);
//            }

            log.debug("{}ms", System.currentTimeMillis() - start);
            log.debug("Result:[{}]", login);
            log.debug("{}ms", System.currentTimeMillis() - start);

            TestInterceptor bean2 = context.getBean(TestInterceptor.class);

            System.err.println(bean2);
        }
    }

}
