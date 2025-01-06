package example.billingjob.configuration.listener;

import example.billingjob.configuration.step.IngestBillingDataStepConfiguration.IngestBillingDataStep;
import example.blueprint.listener.BillingDataSkipListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestBillingDataStepListenerConfigurations {
    @Bean(IngestBillingDataStep.SKIP_LISTENER)
    @StepScope
    public BillingDataSkipListener skipListener(@Value("#{jobParameters['skip.file']}") String skippedFile) {
        return new BillingDataSkipListener(skippedFile);
    }
}
