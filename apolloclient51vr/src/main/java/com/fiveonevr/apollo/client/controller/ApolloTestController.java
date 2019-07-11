package com.fiveonevr.apollo.client.controller;


import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApolloTestController {
//    @Value("${DEBUG}")
    private String DEBUG;
    @GetMapping("test")
    public Object test() {
//        Map map =  this.toObject(CACHES, Map.class);
        System.out.println(DEBUG);
        return DEBUG;
    }

    private<a> a  toObject(String content, Class<a> clazz) {
        return  JSON.parseObject(content, clazz);
    }
}
