package example.billingjob.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

@ConfigurationProperties(prefix = "cellular.plan")
@lombok.RequiredArgsConstructor
public class PricingProperties {

    @NonNull private final Float dataRate;
    @NonNull private final Float callRate;
    @NonNull private final Float smsRate;

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
