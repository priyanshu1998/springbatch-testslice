package example.billingjob;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BillingJobConfiguration {


  public static final String BILLING_JOB = "billingJob";

  private String decorateStepName(String stepName) {
    return String.format("[%s]%s", BILLING_JOB, stepName);
  }

  @Bean
  public JobParametersValidator inputFileParamValidator() {
    return new BillingJobParametersValidator();
  }

  @Bean
  public Step filePreparationStep(JobRepository repository, PlatformTransactionManager transactionManager){
    String stepName = decorateStepName("filePreparationStep");
    return new StepBuilder(stepName, repository)
      .tasklet(new FilePreparationTasklet(), transactionManager)
      .build();
  }

  @Bean
  public Job job(JobRepository jobRepository,
                 @Qualifier("filePreparationStep") Step prepareFile,
                 @Qualifier("inputFileParamValidator") JobParametersValidator validInputFileParam) {
    return new JobBuilder("BillingJob", jobRepository)
            .validator(validInputFileParam)
            .start(prepareFile).build();
  }

}
