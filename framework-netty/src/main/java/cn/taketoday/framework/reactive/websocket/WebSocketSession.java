package cn.taketoday.framework.reactive.websocket;

import cn.taketoday.web.session.WebSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;
import java.util.*;

/**
 * @author WangYi
 * @since 2020/8/13
 */
public class WebSocketSession implements WebSession {
    private final Map<String, Object> attrs = new HashMap<>();
    private final Channel channel;
    private final long creationTime;
    private final String id;
    private final ChannelHandlerContext ctx;

    WebSocketSession(ChannelHandlerContext ctx, String id) {
        this.ctx = ctx;
        this.channel = ctx.channel();
        this.id = id;
        this.creationTime = System.currentTimeMillis();
    }

    @Override
    public String[] getNames() {
        return attrs.keySet().toArray(new String[0]);
    }

    @Override
    public Object getAttribute(String name) {
        return attrs.getOrDefault(name, "");
    }

    @Override
    public void removeAttribute(String name) {
        this.attrs.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.attrs.put(name, value);
    }

    @Override
    public void invalidate() {
        this.attrs.clear();
    }

    @Override
    public Serializable getId() {
        return id;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public Set<Serializable> getKeys() {
        return new LinkedHashSet<>(this.attrs.keySet());
    }
}
