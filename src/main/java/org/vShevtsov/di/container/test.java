package org.vShevtsov.di.container;

import org.vShevtsov.di.container.annotations.Component;
import org.vShevtsov.di.container.annotations.PostConstruct;

@Component
public class test {
    @PostConstruct
    public void test(){
        System.out.println("testik works");
    }
}
