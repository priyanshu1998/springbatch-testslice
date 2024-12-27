package example.billingjob.configuration.property;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@lombok.Data
@PropertySource("classpath:cellular-plan.properties")
@ConfigurationProperties(prefix = "cellular.plan")
@Slf4j
public class PricingProperties {
    private float dataRate = 0.01f;
    private float callRate = 0.5f;
    private float smsRate = 0.1f;

    @lombok.Generated
    public float dataRate(){
        return dataRate;
    }

    @lombok.Generated
    public float callRate(){
        return callRate;
    }

    @lombok.Generated
    public float smsRate(){
        return smsRate;
    }




    @PostConstruct
    void init(){
        log.debug(this.toString());
    }
}
