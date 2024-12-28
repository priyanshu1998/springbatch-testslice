package example.billingjob.configuration.step;

import example.billingjob.configuration.BatchConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.test.StepRunner;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringJUnitConfig(classes = {BatchConfig.class, FilePreparationStepConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@lombok.RequiredArgsConstructor
class FilePreparationStepConfigurationTest {
    private final StepRunner stepRunner;

    @Test
    void contextLoads(ConfigurableApplicationContext context) {
        // verify no profile is loaded
        assertThat(context.getBean(Environment.class).getActiveProfiles()).isEmpty();

        // verify correct step is loaded
        AssertableApplicationContext assertableContext = AssertableApplicationContext.get(() -> context);
        assertThat(assertableContext).hasSingleBean(Step.class);
        assertEquals("file-preparation", context.getBean(Step.class).getName());
    }

    @Test
    void testExecute(@Qualifier("filePreparationStep") Step step){
        JobParameters jobParameters = getJobParameters();

        JobExecution jobExecution = stepRunner.launchStep(step, jobParameters);

        // verify the step run
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        Path billingReport = Paths.get("staging", "billing-2023-01.csv");

        // verify that file is created
        Assertions.assertTrue(Files.exists(billingReport));
    }

    private static JobParameters getJobParameters() {
        return new JobParametersBuilder()
                .addString("input.file", "input/billing-2023-01.csv")
                .toJobParameters();
    }

}