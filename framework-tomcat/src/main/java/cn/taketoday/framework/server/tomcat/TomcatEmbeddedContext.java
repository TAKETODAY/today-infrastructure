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
package cn.taketoday.framework.server.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Manager;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.ManagerBase;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author TODAY <br>
 *         2019-02-05 13:30
 */
@Slf4j
public class TomcatEmbeddedContext extends StandardContext {

    private final SessionIdGenerator sessionIdGenerator;

    public TomcatEmbeddedContext(SessionIdGenerator generator) {
        this.sessionIdGenerator = generator;
    }

    @Override
    public boolean loadOnStartup(Container[] children) {
        return true;
    }

    @Override
    public void setManager(Manager manager) {

        log.info("Setting SessionManager: [{}]", manager);// SessionIdGenerator

        if (manager instanceof ManagerBase) {

            ((ManagerBase) manager).setSessionIdGenerator(getSessionIdGenerator());
        }
        super.setManager(manager);
    }

    public SessionIdGenerator getSessionIdGenerator() {
        return sessionIdGenerator;
    }

}
