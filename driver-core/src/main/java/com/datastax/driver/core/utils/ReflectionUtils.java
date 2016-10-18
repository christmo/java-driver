/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.core.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

/**
 * Handling the reflection within core driver, it is recycled from mapping driver jar
 *
 * Created by @christmo(christmo99@gmail.com) on 14/10/16.
 */
public class ReflectionUtils {

    @SuppressWarnings("all")
    public static <T> Map<String, PropertyDescriptor> scanProperties(Class<T> baseClass) {
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(baseClass);
        } catch (IntrospectionException e) {
        }
        Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor>();
        if (beanInfo != null) {
            for (PropertyDescriptor property : beanInfo.getPropertyDescriptors()) {
                if (property.getName().equals("class"))
                    continue;
                properties.put(property.getName(), property);
            }
        }
        return properties;
    }
}
