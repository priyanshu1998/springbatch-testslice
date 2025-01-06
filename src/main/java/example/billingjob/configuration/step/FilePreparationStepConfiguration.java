package example.billingjob.configuration.step;

import example.blueprint.tasklet.FilePreparationTasklet;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Bean Name : {@value FilePreparationStep#STEP} <br>
 * Operator  : Stateful Operation <br>
 * Idempotent: YES <br>
 */
@Configuration
@lombok.RequiredArgsConstructor
public class FilePreparationStepConfiguration {

    public static class FilePreparationStep {
        public static final String STEP_NAME = "file-preparation";
        public static final String STEP = "filePreparationStep";
    }


    private final JobRepository repository;
    private final PlatformTransactionManager transactionManager;

    // Step1 ========================================================================================================
    @Bean(FilePreparationStep.STEP)
    public Step create(){
        return new StepBuilder(FilePreparationStep.STEP_NAME, repository)
                .tasklet(new FilePreparationTasklet(), transactionManager)
                .build();
    }
    //================================================================================================================

}
