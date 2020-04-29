package com.alibaba.dubbo.platform;

import com.alibaba.dubbo.common.URL;

/**
 * todo
 *
 * @author huanglb
 * @create 2020/3/11
 */
public class EbayPlatform implements IPlatform {
    @Override
    public String platformType() {
        return "EBAY";
    }

    public String entryCheck(URL url) {
        System.out.println("eBay entryCheck");
        return null;
    }

    public String tokenCheck(URL url) {
        System.out.println("eBay tokenCheck");
        return null;
    }
}
