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
package com.alibaba.dubbo.rpc.protocol;

import java.util.Collections;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.ExporterListener;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.InvokerListener;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.listener.ListenerExporterWrapper;
import com.alibaba.dubbo.rpc.listener.ListenerInvokerWrapper;

/**
 * ListenerProtocol
 *
 * 给 Exporter、Invoker 增加 ExporterListener ，监听 Exporter、Invoker 暴露(引用)完成和取消暴露(引用)完成。
 * 
 * @author william.liangf
 */
public class ProtocolListenerWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolListenerWrapper(Protocol protocol){
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) { // 注册中心协议
            return protocol.export(invoker);
        }

        // 暴露服务，创建 Exporter 对象
        Exporter<T> exporter = protocol.export(invoker);

        // 获得 ExporterListener 数组
        // listeners 默认实现为空。可以自行实现 ExporterListener ，并进行配置 @Activate 注解，或者 XML 中 listener 属性
        List<ExporterListener> listeners = Collections.unmodifiableList(ExtensionLoader.getExtensionLoader(ExporterListener.class)
                .getActivateExtension(invoker.getUrl(), Constants.EXPORTER_LISTENER_KEY));

        return new ListenerExporterWrapper<T>(exporter, listeners);
    }

    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) { // 注册中心协议
            return protocol.refer(type, url);
        }

        // 引用服务
        Invoker<T> invoker = protocol.refer(type, url);

        // 获得 InvokerListener 数组
        List<InvokerListener> listeners = Collections.unmodifiableList(ExtensionLoader.getExtensionLoader(InvokerListener.class)
                .getActivateExtension(url, Constants.INVOKER_LISTENER_KEY));

        return new ListenerInvokerWrapper<T>(invoker, listeners);
    }

    public void destroy() {
        protocol.destroy();
    }

}