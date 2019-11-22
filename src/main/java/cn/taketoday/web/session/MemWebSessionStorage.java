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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.session;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TODAY <br>
 *         2019-09-28 10:31
 */
public class MemWebSessionStorage implements WebSessionStorage {

    private final Map<String, WebSession> sessions;

    private final long expire;

    public MemWebSessionStorage() {
        this(3600_000);
    }

    public MemWebSessionStorage(long expire) {
        this(expire, new HashMap<>(128));
    }

    public MemWebSessionStorage(long expire, Map<String, WebSession> sessions) {
        this.expire = expire;
        this.sessions = sessions;
    }

    @Override
    public WebSession get(String id) {

        final WebSession ret = sessions.get(id);

        if (ret == null || System.currentTimeMillis() - ret.getCreationTime() > expire) {
            return null;
        }
        return ret;
    }

    @Override
    public WebSession remove(String id) {
        return sessions.remove(id);
    }

    @Override
    public boolean contains(String id) {
        return sessions.containsKey(id);
    }

    @Override
    public void store(String id, WebSession session) {
        sessions.put(id, session);
    }

}
