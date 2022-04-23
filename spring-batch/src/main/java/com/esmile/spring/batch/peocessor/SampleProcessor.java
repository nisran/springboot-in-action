package com.esmile.spring.batch.peocessor;

import lombok.extern.java.Log;
import org.springframework.batch.item.ItemProcessor;

@Log
public class SampleProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String input) {
        log.info("process: " + input);
        return input.toUpperCase();
    }

}
