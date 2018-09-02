package com.zrs.spring;

import com.zrs.spring.annotation.*;

import javax.servlet.*;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

/**
 * 前端控制器
 * @author zrs
 */
@WebServlet(loadOnStartup = 1, urlPatterns = "/*",initParams = {@WebInitParam(name = "packagename", value = "com.zrs.demo")})
public class DispatcherServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        try {
            String contextPath = req.getContextPath();
            String servletPath = req.getRequestURI();
            String url = servletPath.replace(contextPath, "");

            Handler handler = urlHanderMap.get(url);
            if (handler == null) {
                resp.getOutputStream().print("404");
                return;
            }
            Method method = handler.getMethod();
            Object control = handler.getControl();
            Map<String, Integer> parmMap = handler.getParmMap();

            Object[] arr = new Object[parmMap.size()];

            //涉及类型转换
            Class<?>[] parameterTypes = method.getParameterTypes();
            Map<String, String[]> parameterMap = req.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                String[] value = entry.getValue();
                String v = Arrays.toString(value).replaceAll("\\]|\\[", "").replaceAll(",\\s", ",");
                if (!parmMap.containsKey(key)) {
                    continue;
                }
                Integer i = parmMap.get(key);
                Object o = castStringValue(v, parameterTypes[i]);
                arr[i] = o;
            }

            try {
                Object invoke = handler.method.invoke(control, arr);

                resp.getOutputStream().print(invoke.toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Object castStringValue(String v, Class<?> clazz) {
        if (clazz == String.class) {
            return v;
        }
        if (clazz == Integer.class) {
            return Integer.valueOf(v);
        }
        if (clazz == int.class) {
            return Integer.valueOf(v);
        }
        return v;
    }


    List<String> classNames = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    Map<String, Handler> urlHanderMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {

        String packagename = config.getInitParameter("packagename");
        System.out.println(packagename);

        //加载
        scanClass(packagename);

        //注册
        instance();

        //注入
        autowired();

        //url映射
        handlerMapping();

        System.out.println("SUCCESS");


    }

    private void handlerMapping() {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object instance = entry.getValue();
            if (!instance.getClass().isAnnotationPresent(MyController.class)) {
                continue;
            }
            MyRequestMapping controllerMapping = instance.getClass().getDeclaredAnnotation(MyRequestMapping.class);
            String baseUrl = controllerMapping.valule();

            Method[] declaredMethods = instance.getClass().getDeclaredMethods();

            for (Method method : declaredMethods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping methodMapping = method.getDeclaredAnnotation(MyRequestMapping.class);
                String url = methodMapping.valule();
                url = baseUrl + url;

                Map<String, Integer> parmMap = new HashMap<>(16);
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                for (int i = 0; i < parameterAnnotations.length; i++) {
                    Annotation[] parameterAnnotation = parameterAnnotations[i];
                    for (Annotation annotation : parameterAnnotation) {
                        if (annotation instanceof MyRequestParameter) {
                            MyRequestParameter r = (MyRequestParameter) annotation;
                            String name = r.value();
                            parmMap.put(name, i);
                        }
                    }
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    if(parameterType == HttpServletRequest.class || parameterType == HttpServletResponse.class){
                        parmMap.put(parameterType.getTypeName(),i);
                    }
                }


//不行
//                Parameter[] parameters = method.getParameters();
//                for (int i = 0; i < parameters.length; i++) {
//                    Parameter parameter = parameters[i];
//                    if(parameter.isAnnotationPresent(MyRequestParameter.class)){
//                        MyRequestParameter annotation = parameter.getAnnotation(MyRequestParameter.class);
//                        if(!"".equals(annotation.value())) {
//                            parmMap.put(annotation.value(), i);
//                            continue;
//                        }
//                    }
//                    parmMap.put(parameter.getName(),i);
//                }


                Handler handler = new Handler();
                handler.setControl(instance);
                handler.setMethod(method);
                handler.setParmMap(parmMap);

                urlHanderMap.put(url, handler);
            }


        }
    }


    private void autowired() {

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : declaredFields) {
                if (!field.isAnnotationPresent(MyAutowried.class)) {
                    continue;
                }

                String beanName = lowcase(field.getType().getSimpleName());
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), map.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }


            }


        }


    }

    private void instance() {

        for (String className : classNames) {
            try {
                Class<?> aClass = Class.forName(className);

                if (aClass.isAnnotationPresent(MyController.class)) {

                    String lowcase = lowcase(aClass.getSimpleName());
                    map.put(lowcase, aClass.newInstance());

                } else if (aClass.isAnnotationPresent(MyService.class)) {

                    Object instance = aClass.newInstance();
                    String lowcase = lowcase(aClass.getSimpleName());
                    map.put(lowcase, instance);

                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        map.put(anInterface.getName(), instance);
                    }

                } else {
                    continue;
                }

            } catch (ClassNotFoundException | IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }

    }

    private void scanClass(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File file = new File(url.getFile());

        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                scanClass(packageName + "/" + f.getName());
            } else {
                classNames.add(packageName + "." + f.getName().replace(".class", ""));
            }
        }

        System.out.println(2);


    }


    public String lowcase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return new String(chars);
    }


    public class Handler {
        private Object control;
        private Method method;
        private Map<String, Integer> parmMap;

        public Object getControl() {
            return control;
        }

        public void setControl(Object control) {
            this.control = control;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Map<String, Integer> getParmMap() {
            return parmMap;
        }

        public void setParmMap(Map<String, Integer> parmMap) {
            this.parmMap = parmMap;
        }
    }


}
