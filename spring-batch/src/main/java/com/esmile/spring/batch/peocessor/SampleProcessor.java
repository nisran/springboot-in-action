package com.esmile.spring.batch.peocessor;

import lombok.extern.java.Log;
import org.springframework.batch.item.ItemProcessor;

import java.util.Random;

@Log
public class SampleProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String input) {
        if (new Random().nextInt() % 2 == 0) {
            log.warning("random exception input: " + input);
            throw new IllegalArgumentException();
        }
        log.info("process: " + input);
        return input.toUpperCase();
    }

}
