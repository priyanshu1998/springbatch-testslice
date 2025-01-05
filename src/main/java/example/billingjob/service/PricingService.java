package example.billingjob.service;
import java.util.Random;

import example.billingjob.configuration.property.PricingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

/**
 * {@link PricingProperties }
 */
@Service
@lombok.RequiredArgsConstructor
public class PricingService {
    private final PricingProperties props;
    private final Random random = new Random();

    public float getDataPricing() {
        int randVal = this.random.nextInt(1000);
//        if (randVal %  2 == 0) {
//            throw new PricingException("Error while retrieving data pricing");
//        }
        return props.dataRate();
    }

    public float getCallPricing() {
        return props.callRate();
    }

    public float getSmsPricing() {
        return props.smsRate();
    }
}
