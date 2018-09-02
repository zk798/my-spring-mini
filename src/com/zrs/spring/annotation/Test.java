package com.zrs.spring.annotation;

import java.lang.reflect.Field;

public class Test {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Integer a = 1;
        Integer b = 2;
Integer.valueOf(1);
        swap(a,b);
        System.out.println("a=" + a + ",b=" + b);

        Integer c = new Integer(2);
        System.out.println(c.intValue());
    }

    private static void swap(Integer a1, Integer b1) throws NoSuchFieldException, IllegalAccessException {
        //反射
        Field value = Integer.class.getDeclaredField("value");
        value.setAccessible(true);
        value.set(a1,b1);
        value.set(b1,new Integer(1));

    }
}
