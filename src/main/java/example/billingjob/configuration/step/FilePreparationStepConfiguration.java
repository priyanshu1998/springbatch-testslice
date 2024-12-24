package example.billingjob.configuration.step;

import example.tasklet.FilePreparationTasklet;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@lombok.RequiredArgsConstructor
public class FilePreparationStepConfiguration {
    private final JobRepository repository;
    private final PlatformTransactionManager transactionManager;

    // Step1 ========================================================================================================
    @Bean
    public Step filePreparationStep(){
        return new StepBuilder("file-preparation", repository)
                .tasklet(new FilePreparationTasklet(), transactionManager)
                .build();
    }
    //================================================================================================================

}
