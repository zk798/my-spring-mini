package com.zrs.demo;

import com.zrs.spring.annotation.MyService;

/**
 * Service-test
 * @author zrs
 */
@MyService
public class TestService {

    public String doSomething(){
        return "do-something";
    }
}
