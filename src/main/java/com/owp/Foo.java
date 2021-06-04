package com.owp;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.util.Random;

public class Foo {
    private static final StatsDClient statsd = new NonBlockingStatsDClient("x9e", "47.106.135.182", 8125);

    public static final void main(String[] args) throws InterruptedException {
        for (int j = 0; j < 1000; j++) {
            for (int i = 0; i < 286; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 412; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 189; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 111; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 875; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 2000; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 55; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 127; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 555; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 555; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 555; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 555; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 999; i++) {
                statsd.increment("vinLogin");
            }
            Thread.sleep(1000);
            for (int i = 0; i < 1329; i++) {
                statsd.increment("vinLogin");
            }
        }
    }
}
