package com.alibaba.dubbo.check;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.platform.IPlatform;

/**
 * todo
 *
 * @author huanglb
 * @create 2020/3/11
 */
@Activate(group = "entry", order = 2)
public class QuotaChecker implements ICheck {

    public String check(URL url) {
        System.out.println("QuotaCheck");
        return null;
    }

}
