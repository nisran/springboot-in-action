package com.esmile.jvm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CPU 演示端点接口测试：start/stop 生命周期 + 低效算法小规模快跑。
 *
 * 测试刻意温和：烧 CPU 只持续毫秒级（start 后立刻 stop），
 * /cpu/slow 用 n=1000 而不是默认 n=100000（后者本机要几十秒，CI 上不可接受）。
 *
 * @Description CpuDemoController 接口测试
 * @Date 11/28/2026
 * @Created by nisran
 */
@AutoConfigureMockMvc
@SpringBootTest
class CpuDemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 点火 → 线程真实存在 → 灭火 → 线程退出：完整生命周期闭环 */
    @Test
    void startThenStop() throws Exception {
        mockMvc.perform(get("/cpu/start"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cpu-burn-demo")));
        Assertions.assertTrue(burnerAlive(), "start 后应存在名为 cpu-burn-demo 的线程");

        mockMvc.perform(get("/cpu/stop"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已灭火")));
        Assertions.assertTrue(awaitBurnerExit(), "stop 后烧 CPU 线程应退出");
    }

    /** 重复点火被拒绝：已经在烧时再 start 不会起第二条线程 */
    @Test
    void doubleStartRejected() throws Exception {
        mockMvc.perform(get("/cpu/start")).andExpect(status().isOk());
        mockMvc.perform(get("/cpu/start"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已在烧了")));
        mockMvc.perform(get("/cpu/stop")).andExpect(status().isOk());
    }

    /** 低效算法用小 n 快跑：验证参数绑定与响应格式，不在 CI 里跑大排序 */
    @Test
    void slowFinishesQuicklyWithSmallN() throws Exception {
        mockMvc.perform(get("/cpu/slow").param("n", "1000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("冒泡排序 1000 个元素")));
    }

    private static boolean burnerAlive() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> "cpu-burn-demo".equals(t.getName()) && t.isAlive());
    }

    /** stop 后线程下一圈检测到开关就退出，最多等 2 秒 */
    private static boolean awaitBurnerExit() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            if (!burnerAlive()) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
