package example.billingjob.configprops;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;


@ConfigurationProperties(prefix = "cellular.plan")
@lombok.Getter
@lombok.Setter
public class BillingDataProcessorProperties {
    private float dataRate = 0.01f;
    private float callRate = 0.5f;
    private float smsRate = 0.1f;
    private float spendingThreshold = 150.f;
}
