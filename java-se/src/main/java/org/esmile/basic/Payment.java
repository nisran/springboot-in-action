package org.esmile.basic;

import java.math.BigDecimal;


public sealed interface Payment permits AliPay, WeChatPay, BankCard {
}

record AliPay(String tradeNo, BigDecimal feeRate) implements Payment {
}

record WeChatPay(String openId, BigDecimal feeRate) implements Payment {
}

record BankCard(String cardNo, boolean domestic) implements Payment {
}

class Test {
    BigDecimal fee(Payment p) {
        return switch (p) {                                   // 对 sealed 类型 switch：编译器查穷尽性
            case AliPay a -> a.feeRate();
            case WeChatPay w -> w.feeRate().multiply(new BigDecimal("1.1"));
            case BankCard b when !b.domestic() -> new BigDecimal("3.00");          // when 守卫细分
            case BankCard b -> new BigDecimal("1.50");
        };
    }
}
