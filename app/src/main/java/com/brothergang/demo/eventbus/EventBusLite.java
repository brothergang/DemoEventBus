package com.brothergang.demo.eventbus;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by brothergang on 2017/7/5.
 */

public class EventBusLite {

    public static String TAG = "EventBusLite";

    private static EventBusLite INSTANCE;

    //利用反射获取的方法
    private static final Map<String, List<Method>> methodCache = new HashMap<String, List<Method>>();
    private static final Map<Class<?>, List<Class<?>>> eventClassesCache = new HashMap<>();

    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventClass;

    public static EventBusLite getInstance() {
        if(null == INSTANCE){
            INSTANCE = new EventBusLite();
        }
        return INSTANCE;
    }

    public EventBusLite() {
        subscriptionsByEventClass = new HashMap<>();
    }

    /**
     * 注册订阅者
     * @param subscriber 订阅者
     * @param callbackMethodName 回调的方法
     */
    public void register(Object subscriber, String callbackMethodName) {
        List<Method> subscriberMethods = findSubscriberMethods(subscriber.getClass(), callbackMethodName);
        for (Method method : subscriberMethods) {
            //找到回调方法中的第一个参数类
            Class<?> eventClass = method.getParameterTypes()[0];
            subscribe(subscriber, method, eventClass);
        }
    }

    private List<Method> findSubscriberMethods(Class<?> subscriberClass, String callbackMethodName) {
        String key = subscriberClass.getName() + '.' + callbackMethodName;
        List<Method> subscriberMethods;
        synchronized (methodCache) {
            subscriberMethods = methodCache.get(key);
        }
        if (subscriberMethods != null) {
            return subscriberMethods;
        }
        subscriberMethods = new ArrayList<Method>();
        Class<?> clazz = subscriberClass;
        HashSet<Class<?>> eventClass = new HashSet<Class<?>>();
        while (clazz != null) {
            String name = clazz.getName();
            if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")) {
                // Skip system classes, this just degrades performance
                break;
            }

            //获取订阅类中的所有方法
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                //如果和指定的回调方法名一致，加入
                if (method.getName().equals(callbackMethodName)) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    //这里写死回调里有且只有一个参数
                    if (parameterTypes.length == 1) {
                        if (eventClass.add(parameterTypes[0])) {
                            // Only add if not already found in a sub class
                            subscriberMethods.add(method);
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        if (subscriberMethods.isEmpty()) {
            throw new RuntimeException("Subscriber " + subscriberClass + " has no methods called " + callbackMethodName);
        } else {
            synchronized (methodCache) {
                methodCache.put(key, subscriberMethods);
            }
            return subscriberMethods;
        }
    }

    /**
     * 开始订阅
     * @param subscriber 订阅者，初始化的类
     * @param subscriberMethod 订阅（回调）的方法
     * @param eventClass 回调方法中的参数类
     */
    private void subscribe(Object subscriber, Method subscriberMethod, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventClass.get(eventClass);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventClass.put(eventClass, subscriptions);
        } else {
            for (Subscription subscription : subscriptions) {
                if (subscription.subscriber == subscriber) {
                    throw new RuntimeException("Subscriber " + subscriber.getClass() + " already registered to event "
                            + eventClass);
                }
            }
        }

        subscriberMethod.setAccessible(true);
        Subscription subscription = new Subscription(subscriber, subscriberMethod);
        subscriptions.add(subscription);
    }

    final static class Subscription {
        final Object subscriber;
        final Method method;

        Subscription(Object subscriber, Method method) {
            this.subscriber = subscriber;
            this.method = method;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Subscription) {
                Subscription otherSubscription = (Subscription) other;
                // Super slow (improve once used): http://code.google.com/p/android/issues/detail?id=7811
                return subscriber == otherSubscription.subscriber && method.equals(otherSubscription.method);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            // Check performance once used
            return subscriber.hashCode() + method.hashCode();
        }
    }


    /** Posts the given event to the event bus. */
    public void post(Object event) {
        List<Class<?>> eventClasses = findEventClasses(event.getClass());
        boolean subscriptionFound = false;
        int eventCount = eventClasses.size();
        for (int  i = 0; i < eventCount; i++) {
            Class<?> clazz = eventClasses.get(i);
            CopyOnWriteArrayList<Subscription> subscriptions;
            synchronized (this) {
                subscriptions = subscriptionsByEventClass.get(clazz);
            }
            if (subscriptions != null) {
                for (Subscription subscription : subscriptions) {
                    postToSubscribtion(subscription, event);
                }
                subscriptionFound = true;
            }
        }
        if (!subscriptionFound) {
            Log.d(TAG, "No subscripers registered for event " + event.getClass());
        }
    }

    /**
     * 搜索定义的事件类
     * @param eventClass
     * @return
     */
    private List<Class<?>> findEventClasses(Class<?> eventClass) {
        synchronized (eventClassesCache) {
            List<Class<?>> eventClasses = eventClassesCache.get(eventClass);
            if (eventClasses == null) {
                eventClasses = new ArrayList<>();
                Class<?> clazz = eventClass;
                while (clazz != null) {
                    eventClasses.add(clazz);
                    clazz = clazz.getSuperclass();
                }
                eventClassesCache.put(eventClass, eventClasses);
            }
            return eventClasses;
        }
    }

    static void postToSubscribtion(Subscription subscription, Object event) throws Error {
        try {
            subscription.method.invoke(subscription.subscriber, event);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            Log.e(TAG, "Could not dispatch event: " + event.getClass() + " to subscribing class "
                    + subscription.subscriber.getClass(), cause);
            if (cause instanceof Error) {
                throw (Error) cause;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }
}
