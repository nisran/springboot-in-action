package com.esmile.spring.batch.config;

import com.esmile.spring.batch.peocessor.SampleProcessor;
import com.esmile.spring.batch.reader.SampleReader;
import com.esmile.spring.batch.writer.SampleWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Bean
    public Step step() {
        return stepBuilderFactory.get("SampleStep")
                .<String, String>chunk(2)
                .reader(new SampleReader())
                .processor(new SampleProcessor())
                .writer(new SampleWriter())
                .build();
    }

    @Bean
    public Job sampleJob() {
        return jobBuilderFactory.get("sampleJob")
                .incrementer(new RunIdIncrementer())
                .start(step())
                .build();
    }

}
