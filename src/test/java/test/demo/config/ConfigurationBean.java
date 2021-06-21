/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package test.demo.config;

import javax.annotation.PostConstruct;

import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.annotation.condition.WindowsCondition;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;

/**
 * @author Today <br>
 * 
 *         2018-09-06 15:30
 */
@Configuration
public class ConfigurationBean {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationBean.class);

    @PostConstruct
    public void init() {
        log.info("ConfigurationBean.init()");
    }

    @Prototype("prototype_user")
    public User user() {
        return new User().setId(12);
    }

    @Singleton("user__")
    public User user__() {
        return new User().setId(12);
    }

    @Profile("test")
    @Prototype("user")
    public User testUser() {
        return new User().setUserName("TEST");
    }

    @Profile("prod")
    @Singleton("user")
    public User prodUser() {
        return new User().setUserName("PROD");
    }

    @Singleton("yhj")
    @Profile("!test")
    public User yhj() {
        return new User().setUserName("yhj");
    }

    @Singleton("user_windows")
    @Conditional(WindowsCondition.class)
    public User windowsUser() {
        return new User().setUserName("Windows");
    }

}
