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
package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.check.ICheck;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.platform.IPlatform;
import com.google.common.base.Strings;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class DemoProvider {

    public static void main(String[] args) {

//        // java SPI
//        ServiceLoader<IPlatform> iPlatforms = ServiceLoader.load(IPlatform.class);
//        iPlatforms.forEach(iPlatform -> System.out.println(iPlatform.platformType()));

//        // dubbo SPI
//        System.out.println(ExtensionLoader.getExtensionLoader(IPlatform.class).getExtension("EBAY").platformType());

//        // IOC、AOP
//        URL url = new URL(null, null, 0);
//        url = url.addParameter("platformType", "EBAY");
//        ExtensionLoader.getExtensionLoader(ICheck.class).getExtension("entry").check(url);

//        // Activate
//        URL url = new URL(null, null, 0);
//        List<ICheck> list = ExtensionLoader.getExtensionLoader(ICheck.class)
//                .getActivateExtension(url, new String[]{"-token"}, "entry");
//        list.forEach(o -> o.check(url));

//        URL url = new URL(null, null, 0);
//        url = url.addParameter("platformType", "EBAY");
//        ExtensionLoader.getExtensionLoader(ICheck.class).getExtension("entry").check(url);


        com.alibaba.dubbo.container.Main.main(args);
    }

}