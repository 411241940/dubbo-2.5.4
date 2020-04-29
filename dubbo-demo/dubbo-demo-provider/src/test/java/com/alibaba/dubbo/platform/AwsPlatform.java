package com.alibaba.dubbo.platform;

import com.alibaba.dubbo.common.URL;

/**
 * todo
 *
 * @author huanglb
 * @create 2020/3/11
 */
public class AwsPlatform implements IPlatform {
    @Override
    public String platformType() {
        return "AWS";
    }

    public String entryCheck(URL url) {
        System.out.println("AWS entryCheck");
        return null;
    }

    public String tokenCheck(URL url) {
        System.out.println("AWS tokenCheck");
        return null;
    }
}
