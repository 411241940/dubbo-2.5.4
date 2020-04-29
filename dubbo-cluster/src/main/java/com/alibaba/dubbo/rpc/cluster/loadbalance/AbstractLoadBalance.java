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
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

/**
 * AbstractLoadBalance
 * 
 * @author william.liangf
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (invokers == null || invokers.size() == 0)
            return null;
        if (invokers.size() == 1)
            return invokers.get(0);
        return doSelect(invokers, url, invocation);
    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
        // 获取 Invoker 配置的权重
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT);

        if (weight > 0) {
            // 获取 Invoker 的启动时间戳
	        long timestamp = invoker.getUrl().getParameter(Constants.TIMESTAMP_KEY, 0L);
	    	if (timestamp > 0L) {

                // 获取 Invoker 的启动时间
	    		int uptime = (int) (System.currentTimeMillis() - timestamp);

                // 获取 Invoker 的预热时间，默认为10分钟 Constants.DEFAULT_WARMUP
                int warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP);

                // 如果服务启动时间小于预热时间，重新计算权重
	    		if (uptime > 0 && uptime < warmup) {
	    			weight = calculateWarmupWeight(uptime, warmup, weight);
	    		}
	    	}
        }
    	return weight;
    }


    // 权重预热，随着服务启动时间的增加，权重会慢慢接近配置权重，直到达到预热时间等于配置权重。预热时间之前，控制流量线性上升，避免服务在启动之初就处于高负载状态
    // 权重=uptime/(warmup/weight)
    // 上述公式可以转为(uptime/warmup)*weight，可以看出：随着服务启动时间的增加(uptime)，计算后的权重会越来越接近weight
    // 计算后的权重小于1则返回1，否则返回min(计算权重,配置权重)
    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
    	int ww = (int) ( (float) uptime / ( (float) warmup / (float) weight ) );
    	return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }

}