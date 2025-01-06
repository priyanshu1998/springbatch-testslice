package example.billingjob.configuration;

import example.billingjob.configuration.step.FilePreparationStepConfiguration.FilePreparationStep;
import example.billingjob.configuration.step.GenerateBillingTotalDataStepConfiguration.GenerateBillingTotalDataStep;
import example.billingjob.configuration.step.IngestBillingDataStepConfiguration.IngestBillingDataStep;
import example.blueprint.infrastructure.validator.BillingJobParametersValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.Step;

@Configuration
@lombok.RequiredArgsConstructor
public class BillingJobConfiguration {
  private final JobRepository jobRepository;
  public static final String BILLING_DATA_TABLE = "billing_data";

  // Job ===================================================================================================
  @Bean
  public Job job(@Qualifier(FilePreparationStep.STEP) Step prepareFlatFile,
                 @Qualifier(IngestBillingDataStep.STEP) Step ingestBillingDataToRdbms,
                 @Qualifier(GenerateBillingTotalDataStep.STEP) Step readFromRdbmsGenerateBillingTotalDataAsCsvFile) {
    JobParametersValidator validateInputFileParam = new BillingJobParametersValidator();

    return new JobBuilder("billing-job", jobRepository)
            .validator(validateInputFileParam)
            .start(prepareFlatFile)
            .next(ingestBillingDataToRdbms)
            .next(readFromRdbmsGenerateBillingTotalDataAsCsvFile)
            .build();
  }
}



