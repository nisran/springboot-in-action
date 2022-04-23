package com.esmile.spring.batch.reader;

import lombok.extern.java.Log;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Log
public class SampleReader implements ItemReader<String> {


    private Iterator<String> iterator;

    @BeforeStep
    public void setup() {
        List<String> sampleList = Arrays.asList("sample1", "sample2", "sample3", "sample4");
        iterator = sampleList.iterator();
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
