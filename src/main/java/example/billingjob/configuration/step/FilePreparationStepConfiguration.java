package example.billingjob.configuration.step;

import example.billingjob.configuration.BillingJobBeanDirectory.FilePreparationStep ;
import example.blueprint.tasklet.FilePreparationTasklet;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@lombok.RequiredArgsConstructor
public class FilePreparationStepConfiguration {
    public static final String STEP_NAME = "file-preparation";

    private final JobRepository repository;
    private final PlatformTransactionManager transactionManager;

    // Step1 ========================================================================================================
    @Bean(FilePreparationStep.STEP)
    public Step create(){
        return new StepBuilder(STEP_NAME, repository)
                .tasklet(new FilePreparationTasklet(), transactionManager)
                .build();
    }
    //================================================================================================================

}
