package com.github.bingoohuang.springrestclient.tests.api;

import com.github.bingoohuang.springrestclient.boot.domain.Account;
import com.github.bingoohuang.springrestclient.boot.domain.Car;
import com.github.bingoohuang.springrestclient.spring.SpringRestClientConfig;
import com.github.bingoohuang.springrestclient.spring.api.RequestBodyApi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringRestClientConfig.class)
public class RequestBodyApiTest {
    @Autowired RequestBodyApi api;

    @Test public void transfer() {
        String resp = api.transfer(new Account(123, "bjh"), new Car("BMW", 456));

        assertThat(resp, is(equalTo("account:Account(money=123, name=bjh), car:Car(brand=BMW, age=456)")));
    }

    @Test public void transferXml() {
        String resp = api.transferXml(new Account(123, "bjh"), new Car("BMW", 456));

        assertThat(resp, is(equalTo("account:Account(money=123, name=bjh), car:Car(brand=BMW, age=456)")));
    }
}
