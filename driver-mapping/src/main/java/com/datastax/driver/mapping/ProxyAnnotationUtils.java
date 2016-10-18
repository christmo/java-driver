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
package com.datastax.driver.mapping;

import com.datastax.driver.mapping.annotations.UDT;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle Proxy annotations within an OSGI environment
 *
 * Created by @christmo (christmo99@gmail.com) on 10/10/16.
 */
public class ProxyAnnotationUtils {

    /**
     * Package where a Proxy come from
     */
    private static final String PACKAGE_PROXY = "com.sun.proxy";

    /**
     * In an OSGI environment, we need to get the correct class from the actual classloader
     *
     * @param klass class to get from the actual classloader
     * @return New class within actual classloader
     */
    public static Class<?> getClassFromOSGIClassLoader(Class<?> klass) {
        Class newClass = klass;
        if (klass != null) {
            try {
                newClass = ProxyAnnotationUtils.class.getClassLoader().loadClass(klass.getName());
            } catch (ClassNotFoundException e) {
                return klass;
            }
        }
        return newClass;
    }

    /**
     * Check if there is an UDT annotation in the class, when klass is a Proxy from an OSGI environment
     *
     * @param klass Class to check
     * @param udtClass UDT Annotation
     * @return true if klass is annotated like UDT
     */
    public static boolean checkUDTAnnotationProxy(Class<?> klass, Class<UDT> udtClass) {
        Annotation[] annotations = klass.getAnnotations();
        for (Annotation annotation : annotations) {
            annotation = ProxyAnnotationUtils.convertProxyAnnotation(annotation);
            return annotation.annotationType().equals(udtClass);
        }
        return false;
    }

    /**
     * Within an OSGI environment, we cannot get annotations directly, it comes like a Proxy, we need to handle it
     * with an extra class Handler, this method return a new Annotation with a Handler to interact with the annotation.
     * @param annotatedClass Class which have some annotation.
     * @param annotation Class annotation to handle from the clas annotated
     * @return T new Annotation handled by a class
     */
    public static <T extends Annotation> T rebuildAnnotationFromProxy(Class<?> annotatedClass, Class<T> annotation) {
        boolean hasAnnotation = false;
        Map<String, Object> properties = new HashMap<String, Object>();

        for (Annotation classAnnotation : annotatedClass.getAnnotations()) {
            if (isProxy(classAnnotation)) {
                if (classAnnotation.annotationType().getCanonicalName().equals(annotation.getCanonicalName())) {
                    for (Method method : classAnnotation.annotationType().getDeclaredMethods()) {
                        try {
                            Object value = method.invoke(classAnnotation);
                            properties.put(method.getName(), value);
                        } catch (Exception e) {
                            //Do not do anything
                        }
                    }
                    hasAnnotation = true;
                    break;
                }
            }
        }

        if (hasAnnotation) {
            T newAnnotation = (T) Proxy.newProxyInstance(annotation.getClassLoader(), new Class[]{annotation},
                    new Handler(annotation, properties));
            return newAnnotation;
        }

        return null;
    }

    /**
     * Convert a Proxy Annotation to a Handled Annotation
     *
     * @param annotation Proxy annotation
     * @return T new annotation Handled by a class
     */
    public static <T extends Annotation> T convertProxyAnnotation(Annotation annotation) {
        Map<String, Object> properties = new HashMap<String, Object>();
        T newAnnotation = null;
        Class classAnnotation = null;

        if (isProxy(annotation)) {
            classAnnotation = annotation.annotationType();
            for (Method method : classAnnotation.getDeclaredMethods()) {
                try {
                    Object value = method.invoke(annotation);
                    properties.put(method.getName(), value);
                } catch (Exception e) {
                    //Do not do anything
                }
            }
            for (Method method : annotation.getClass().getMethods()) {
                try {
                    Object value = method.invoke(annotation);
                    properties.put(method.getName(), value);
                } catch (Exception e) {
                    //Do not do anything
                }
            }

            newAnnotation = (T) Proxy.newProxyInstance(classAnnotation.getClassLoader(),
                    new Class[]{classAnnotation}, new Handler(classAnnotation, properties));
        }
        return newAnnotation;
    }

    /**
     * Check if the annotation come from a Proxy, it could proceed from an OSGI environment
     *
     * @param annotation Annotation to check if it is a Proxy
     * @return true if the annotation is a Proxy
     */
    private static boolean isProxy(Annotation annotation) {
        if (annotation != null) {
            return annotation.getClass().getName().contains(PACKAGE_PROXY);
        }
        return false;
    }
/*
    private static boolean isProxy(Class annotation) {
        if (annotation != null) {
            return annotation.getName().contains(PACKAGE_PROXY);
        }
        return false;
    }
*/
}

/**
 * Class to handle annotation properties come from a Proxy within an OSGI environment
 */
class Handler implements InvocationHandler {

    private final String TO_STRING_METHOD = "toString";
    private final String CODEC_METHOD = "codec";
    private final String HASHCODE_METHOD = "hashcode";
    private final String CLASS_METHOD = "getClass";
    private final String ANNOTATION_TYPE_METHOD = "annotationType";

    private Map<String, Object> properties;
    private Class annotation;

    public Handler(Class<? extends Annotation> annotation, Map<String, Object> props) {
        this.annotation = annotation;
        this.properties = props;
    }

    /**
     * This method is called when an annotation property is invoked.
     *
     * @param proxy Proxy Object which have data
     * @param method method executed
     * @param args optional args
     * @return Value from the proxy annotation
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        if (TO_STRING_METHOD.equals(method.getName()) && annotation != null) {
            return annotation.toString();
        }
        if (properties != null && method != null) {
            result = properties.get(method.getName());
            if (result != null && ANNOTATION_TYPE_METHOD.equals(method.getName()) || CODEC_METHOD.equals(method.getName())) {
                return this.getClass().getClassLoader().loadClass(((Class) result).getName());
            }
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
