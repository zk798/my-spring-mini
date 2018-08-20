package com.zrs.demo;


import com.zrs.spring.annotation.*;

/**
 * Controller-test
 * @author zrs
 */
@MyController
@MyRequestMapping(valule = "/test")
public class TestController {

    @MyAutowried
    private TestService testService;

    @MyRequestMapping(valule = "/id")
    public String get(@MyRequestParameter(value="id") String id){
        String s = testService.doSomething();
        System.out.println("s=" + s + "，d=" + id);
        return "spring-mvc:"+s;
    }

}
