package com.esmile.batch.reader;

import lombok.extern.java.Log;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Log
public class SampleReader implements ItemReader<String> {


    private Iterator<String> iterator;

    @BeforeStep
    public void setup() {
        int dataAmount = 9;
        List<String> sampleList = new ArrayList<>();
        for (int i = 1; i < dataAmount+1; i++) {
            sampleList.add("sample-" + i);
        }
        iterator = sampleList.iterator();
        log.info("******************************************************************************");
        log.info("before step sampleList: " + sampleList);
    }

    // Calling it returns one item or null if no more items are left
    @Override
    public String read() {
        if (iterator.hasNext()) {
            final String name = iterator.next();
            log.info("read: "+ name);
            return name;
        }
        return null;
    }

}
