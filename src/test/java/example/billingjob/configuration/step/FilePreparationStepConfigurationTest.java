package example.billingjob.configuration.step;

import example.billingjob.configuration.BatchConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.test.StepRunner;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringJUnitConfig(classes = {BatchConfig.class, FilePreparationStepConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@lombok.RequiredArgsConstructor
class FilePreparationStepConfigurationTest {
    private final Step step;
    private final StepRunner stepRunner;

    @Test
    void contextLoads(ConfigurableApplicationContext context) {
        AssertableApplicationContext assertableContext = AssertableApplicationContext.get(() -> context);

        assertThat(assertableContext).hasSingleBean(Step.class);
        assertEquals(context.getBean(Step.class).getName(), "file-preparation");
    }

    @Test
    void testExecute(){
        JobParameters jobParameters = getJobParameters();

        JobExecution jobExecution = stepRunner.launchStep(step, jobParameters);
        assertEquals(jobExecution.getExitStatus(), ExitStatus.COMPLETED);

        Path billingReport = Paths.get("staging", "billing-report-2023-01.csv");
        Assertions.assertTrue(Files.exists(billingReport));
    }

    private static JobParameters getJobParameters() {
        return new JobParametersBuilder()
                .addString("input.file", "input/billing-2023-01.csv")
                .toJobParameters();
    }

}