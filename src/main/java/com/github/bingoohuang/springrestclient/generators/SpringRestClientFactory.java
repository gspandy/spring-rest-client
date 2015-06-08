package com.github.bingoohuang.springrestclient.generators;

import com.github.bingoohuang.springrestclient.annotations.*;
import com.github.bingoohuang.springrestclient.provider.BaseUrlProvider;
import com.github.bingoohuang.springrestclient.provider.FixedBaseUrlProvider;
import com.github.bingoohuang.springrestclient.provider.NoneBaseUrlProvider;
import com.github.bingoohuang.springrestclient.utils.Obj;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SpringRestClientFactory {
    private static LoadingCache<Class, Object> restClientCache =
            CacheBuilder.newBuilder().build(new CacheLoader<Class, Object>() {
                @Override
                public Object load(Class restClientClass) throws Exception {
                    ClassGenerator generator = new ClassGenerator(restClientClass);
                    Class<?> restClientImplClass = generator.generate();
                    Object object = Obj.createObject(restClientImplClass);

                    setBaseUrlProvider(restClientImplClass, object, restClientClass);
                    setStatusMappings(restClientImplClass, object, restClientClass);
                    setFixedRequestParams(restClientImplClass, object, restClientClass);
                    setSuccInResponseJSONProperty(restClientImplClass, object, restClientClass);

                    return object;
                }
            });

    public static <T> T getRestClient(final Class<T> restClientClass) {
        Obj.ensureInterface(restClientClass);
        try {
            return (T) restClientCache.get(restClientClass);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw Throwables.propagate(cause);
        } catch (UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            throw Throwables.propagate(cause);
        }
    }

    private static void setSuccInResponseJSONProperty(Class<?> restClientImplClass, Object object, Class restClientClass) {
        for (Method method : restClientClass.getDeclaredMethods()) {
            SuccInResponseJSONProperty property = method.getAnnotation(SuccInResponseJSONProperty.class);
            String fieldName = method.getName() + MethodGenerator.SuccInResponseJSONProperty;
            Obj.setField(restClientImplClass, object, fieldName, property);
        }
    }

    private static void setFixedRequestParams(Class<?> restClientImplClass, Object object, Class restClientClass) {
        for (Method method : restClientClass.getDeclaredMethods()) {
            Map<String, Object> mappings = createFixedRequestParams(method);
            String fieldName = method.getName() + MethodGenerator.FixedRequestParams;
            Obj.setField(restClientImplClass, object, fieldName, mappings);
        }
    }

    private static Map<String, Object> createFixedRequestParams(Method method) {
        HashMap<String, Object> map = Maps.newHashMap();

        FixedRequestParam fixedRequestParam = method.getAnnotation(FixedRequestParam.class);
        if (fixedRequestParam != null) {
            map.put(fixedRequestParam.name(), fixedRequestParam.value());
        }

        FixedRequestParams fixedRequestParams = method.getAnnotation(FixedRequestParams.class);
        if (fixedRequestParams != null) {
            for (FixedRequestParam paramValue : fixedRequestParams.value()) {
                map.put(paramValue.name(), paramValue.value());
            }
        }

        return Collections.unmodifiableMap(map);
    }


    private static void setStatusMappings(Class<?> restClientImplClass, Object object, Class restClientClass) {
        for (Method method : restClientClass.getDeclaredMethods()) {
            Map<Integer, Class<? extends Throwable>> mappings = createStatusExceptionMappings(method);
            String fieldName = method.getName() + MethodGenerator.StatusExceptionMappings;
            Obj.setField(restClientImplClass, object, fieldName, mappings);
        }
    }

    private static Map<Integer, Class<? extends Throwable>> createStatusExceptionMappings(Method method) {
        Map<Integer, Class<? extends Throwable>> statusExceptionMappings = Maps.newHashMap();

        RespStatusMappings respStatusMappings = method.getAnnotation(RespStatusMappings.class);
        if (respStatusMappings != null) {
            for (RespStatusMapping respStatusMapping : respStatusMappings.value()) {
                Class<? extends Throwable> exceptionClass = respStatusMapping.exception();
                checkMethodException(method, exceptionClass);
                statusExceptionMappings.put(respStatusMapping.status(), exceptionClass);
            }
        }

        return Collections.unmodifiableMap(statusExceptionMappings);
    }

    private static void checkMethodException(Method method,
                                             Class<? extends Throwable> exceptionClass) {
        if (RuntimeException.class.isAssignableFrom(exceptionClass)) return;

        // checked exception should be declared
        for (Class<?> declaredExceptionType : method.getExceptionTypes()) {
            if (declaredExceptionType == exceptionClass) return;
        }

        throw new RuntimeException(exceptionClass
                + " is checked exception and should be declared on the method " + method);
    }

    private static void setBaseUrlProvider(Class<?> restClientImplClass,
                                           Object object, Class restClientClass) {
        BaseUrlProvider provider = createBaseUrlProvider(restClientClass);
        String fieldName = "baseUrlProvider";

        Obj.setField(restClientImplClass, object, fieldName, provider);
    }

    private static BaseUrlProvider createBaseUrlProvider(Class<?> restClientClass) {
        SpringRestClientEnabled restClientEnabled =
                restClientClass.getAnnotation(SpringRestClientEnabled.class);
        String baseUrl = restClientEnabled.baseUrl();
        if (!Strings.isNullOrEmpty(baseUrl)) return new FixedBaseUrlProvider(baseUrl);

        Class<? extends BaseUrlProvider> providerClass = restClientEnabled.baseUrlProvider();
        if (providerClass == NoneBaseUrlProvider.class) {
            throw new RuntimeException("base url should be configured for api " + restClientClass);
        }

        try {
            return providerClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("base url configuration error for api " + restClientClass, e);
        }
    }


}
