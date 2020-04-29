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
@Activate(group = "entry", order = -1)
public class TokenChecker implements ICheck {

    private IPlatform iPlatform;

    public void setiPlatform(IPlatform iPlatform) {
        this.iPlatform = iPlatform;
    }

    public String check(URL url) {
        System.out.println("TokenCheck");
        iPlatform.tokenCheck(url);
        return null;
    }
}
