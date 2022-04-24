package com.esmile.spring.batch.config;

import com.esmile.spring.batch.peocessor.SampleProcessor;
import com.esmile.spring.batch.reader.SampleReader;
import com.esmile.spring.batch.writer.SampleWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileParseException;
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
                .<String, String>chunk(3)
                .reader(new SampleReader())
                .processor(new SampleProcessor())
                .writer(new SampleWriter())
                .faultTolerant()
                //连续尝试两次都失败就执行skip
                .retryLimit(2)
                .retry(IllegalArgumentException.class)
                // skip超过两个，到第三次的时候就会抛异常结束
                .skipLimit(2)
                .skip(IllegalArgumentException.class)
                //默认情况下不管是retry还是skip，抛出的异常都会出现异常，但是如果是writer出现异常（没有成功写进去就可以不用rollback）
                .noRollback(IllegalArgumentException.class)
                //此参数默认是false, 意思如果想要重新跑这个Job不会成功开始
                //之前的另一种做法是添加Job参数来重新使同一个Step跑起来。
                //相比而言，设置这个参数true更为合理
                .allowStartIfComplete(true)
                // .startLimit(1)
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
