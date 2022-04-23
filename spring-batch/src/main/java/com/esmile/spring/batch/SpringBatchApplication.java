package com.esmile.spring.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Date;

@SpringBootApplication
public class SpringBatchApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(SpringBatchApplication.class, args);

        Job sampleJob = (Job) applicationContext.getBean("sampleJob");
        JobLauncher jobLauncher = (JobLauncher) applicationContext.getBean("jobLauncher");

        //避免出现 <spring batch restart Step already complete or not restartable>, 无法重复测试batchJob
        final JobParameters jobParameters = new JobParametersBuilder().addDate("date", new Date()).toJobParameters();

        try {
            jobLauncher.run(sampleJob, jobParameters);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            e.printStackTrace();
        }

    }

}