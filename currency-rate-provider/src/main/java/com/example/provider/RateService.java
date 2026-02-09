package com.example.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class RateService {
    public BigDecimal currentUsdRubRate() {
        double value = 90.0 + ThreadLocalRandom.current().nextDouble(-5.0, 5.0);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
