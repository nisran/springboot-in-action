package com.esmile;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.quartz.QuartzJobBean;

// @EnableScheduling
@SpringBootApplication
public class ScheduledAPP {

    public static void main(String[] args) {
        SpringApplication.run(ScheduledAPP.class, args);
        ScheduledTaskRegistrar registrar;

        // Quartz核心概念：
        Job job;
        QuartzJobBean quartzJobBean; //一般继承他，编写实际的任务

        JobDetail jobDetail;  //Job详情，
        Trigger trigger;
        SimpleTrigger simpleTrigger;


    }

}
