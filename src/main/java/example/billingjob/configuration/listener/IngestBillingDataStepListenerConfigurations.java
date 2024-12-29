package example.billingjob.configuration.listener;

import example.blueprint.listener.BillingDataSkipListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestBillingDataStepListenerConfigurations {
    @Bean
    @StepScope
    public BillingDataSkipListener parseFailListener(@Value("#{jobParameters['skip.file']}") String skippedFile) {
        return new BillingDataSkipListener(skippedFile);
    }
}
