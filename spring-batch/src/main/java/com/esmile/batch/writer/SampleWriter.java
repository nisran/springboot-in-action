package com.esmile.batch.writer;

import lombok.extern.java.Log;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

@Log
public class SampleWriter implements ItemWriter<String> {

    private final List<String> resultList = new ArrayList<>();

    @Override
    public void write(List<? extends String> list) {
        resultList.addAll(list);
        log.info("writer:" + list);
    }

    @AfterStep
    public void getResultList() {
        log.info("after step resultList: " + resultList);
        log.info("******************************************************************************");

    }

}
