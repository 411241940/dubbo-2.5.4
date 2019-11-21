/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.exchange.support.header;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;

/**
 * ExchangeServerImpl
 * 
 * @author william.liangf
 */
public class HeaderExchangeServer implements ExchangeServer {
    
    protected final Logger        logger = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService scheduled                 = Executors.newScheduledThreadPool(1,
                                                                                                        new NamedThreadFactory(
                                                                                                                               "dubbo-remoting-server-heartbeat",
                                                                                                                               true));

    // 心跳定时器
    private ScheduledFuture<?> heatbeatTimer;

    // 心跳超时，毫秒。缺省0，不会执行心跳。
    private int                            heartbeat;

    private int                            heartbeatTimeout;
    
    private final Server server;

    private volatile boolean closed = false;

    public HeaderExchangeServer(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("server == null");
        }
        this.server = server;
        this.heartbeat = server.getUrl().getParameter(Constants.HEARTBEAT_KEY, 0);
        this.heartbeatTimeout = server.getUrl().getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartbeat * 3);
        if (heartbeatTimeout < heartbeat * 2) {
            throw new IllegalStateException("heartbeatTimeout < heartbeatInterval * 2");
        }
        startHeatbeatTimer();
    }
    
    public Server getServer() {
        return server;
    }

    public boolean isClosed() {
        return server.isClosed();
    }

    private boolean isRunning() {
        Collection<Channel> channels = getChannels();
        for (Channel channel : channels) {
            if (DefaultFuture.hasFuture(channel)) {
                return true;
            }
        }
        return false;
    }

    public void close() {
        doClose();
        server.close();
    }

    public void close(final int timeout) {
        if (timeout > 0) {
            final long max = (long) timeout;
            final long start = System.currentTimeMillis();
            if (getUrl().getParameter(Constants.CHANNEL_SEND_READONLYEVENT_KEY, false)){
                sendChannelReadOnlyEvent();
            }

            // 如果还有进行中的任务并且没有到达等待时间的上限，则继续等待。
            // 这里会有个小问题，因为provider从注册中心撤销服务和上游consumer将其服务从服务列表中删除并不是原子操作，
            // 如果集群规模过大，可能导致上游consumer的服务列表还未更新完成，我们的provider这时发现当前没有进行中的调用就立马关闭服务暴露，导致上游consumer调用该服务失败。
            // 所以，dubbo默认的这种优雅停机方案，需要建立在上游consumer有重试机制的基础之上，但由于consumer增加重试特性会增加故障时的雪崩风险，所以大多数分布式服务不愿意增加服务内部之间的重试机制。
            // 只要用一个参数来设置等待下限，这样整个分布式系统几乎不需要通过重试来保证优雅停机，只需要给与上游consumer少许时间，让他们足够有机会更新完provider的列表就行
            while (HeaderExchangeServer.this.isRunning()
                    && System.currentTimeMillis() - start < max) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        doClose(); // 关闭心跳，停止应答
        server.close(timeout); // 关闭通信通道
    }
    
    private void sendChannelReadOnlyEvent(){
        Request request = new Request();
        request.setEvent(Request.READONLY_EVENT);
        request.setTwoWay(false);
        request.setVersion(Version.getVersion());
        
        Collection<Channel> channels = getChannels();
        for (Channel channel : channels) {
            try {
                if (channel.isConnected())channel.send(request, getUrl().getParameter(Constants.CHANNEL_READONLYEVENT_SENT_KEY, true));
            } catch (RemotingException e) {
                logger.warn("send connot write messge error.", e);
            }
        }
    }
    
    private void doClose() {
        if (closed) {
            return;
        }
        closed = true;
        stopHeartbeatTimer();
        try {
            scheduled.shutdown();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    public Collection<ExchangeChannel> getExchangeChannels() {
        Collection<ExchangeChannel> exchangeChannels  = new ArrayList<ExchangeChannel>();
        Collection<Channel> channels = server.getChannels();
        if (channels != null && channels.size() > 0) {
            for (Channel channel : channels) {
                exchangeChannels.add(HeaderExchangeChannel.getOrAddChannel(channel));
            }
        }
        return exchangeChannels;
    }

    public ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress) {
        Channel channel = server.getChannel(remoteAddress);
        return HeaderExchangeChannel.getOrAddChannel(channel);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection<Channel> getChannels() {
        return (Collection)getExchangeChannels();
    }

    public Channel getChannel(InetSocketAddress remoteAddress) {
        return getExchangeChannel(remoteAddress);
    }

    public boolean isBound() {
        return server.isBound();
    }

    public InetSocketAddress getLocalAddress() {
        return server.getLocalAddress();
    }

    public URL getUrl() {
        return server.getUrl();
    }

    public ChannelHandler getChannelHandler() {
        return server.getChannelHandler();
    }

    public void reset(URL url) {
        server.reset(url);
        try {
            if (url.hasParameter(Constants.HEARTBEAT_KEY)
                    || url.hasParameter(Constants.HEARTBEAT_TIMEOUT_KEY)) {
                int h = url.getParameter(Constants.HEARTBEAT_KEY, heartbeat);
                int t = url.getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, h * 3);
                if (t < h * 2) {
                    throw new IllegalStateException("heartbeatTimeout < heartbeatInterval * 2");
                }
                if (h != heartbeat || t != heartbeatTimeout) {
                    heartbeat = h;
                    heartbeatTimeout = t;
                    startHeatbeatTimer();
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }
    
    @Deprecated
    public void reset(com.alibaba.dubbo.common.Parameters parameters){
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    public void send(Object message) throws RemotingException {
        if (closed) {
            throw new RemotingException(this.getLocalAddress(), null, "Failed to send message " + message + ", cause: The server " + getLocalAddress() + " is closed!");
        }
        server.send(message);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        if (closed) {
            throw new RemotingException(this.getLocalAddress(), null, "Failed to send message " + message + ", cause: The server " + getLocalAddress() + " is closed!");
        }
        server.send(message, sent);
    }

    private void startHeatbeatTimer() {
        stopHeartbeatTimer();
        if (heartbeat > 0) {
            heatbeatTimer = scheduled.scheduleWithFixedDelay(
                    new HeartBeatTask( new HeartBeatTask.ChannelProvider() {
                        public Collection<Channel> getChannels() {
                            return Collections.unmodifiableCollection(
                                    HeaderExchangeServer.this.getChannels() );
                        }
                    }, heartbeat, heartbeatTimeout),
                    heartbeat, heartbeat,TimeUnit.MILLISECONDS);
        }
    }

    private void stopHeartbeatTimer() {
        try {
            ScheduledFuture<?> timer = heatbeatTimer;
            if (timer != null && ! timer.isCancelled()) {
                timer.cancel(true);
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        } finally {
            heatbeatTimer =null;
        }
    }

}