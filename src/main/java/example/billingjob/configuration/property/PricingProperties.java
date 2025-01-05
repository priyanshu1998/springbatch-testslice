package example.billingjob.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cellular.plan")
@lombok.RequiredArgsConstructor
public class PricingProperties {
    private final float dataRate;
    private final float callRate;
    private final float smsRate;

    public float dataRate(){
        return dataRate;
    }

    public float callRate(){
        return callRate;
    }

    public float smsRate(){
        return smsRate;
    }
}
