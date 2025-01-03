package example.billingjob.configuration.step;

import example.billingjob.configuration.BatchConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.test.StepRunner;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBatchTest
@SpringJUnitConfig(classes = {BatchConfig.class, FilePreparationStepConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@lombok.RequiredArgsConstructor
class FilePreparationStepTest {
    private final Step step;
    private final StepRunner stepRunner;

    @Test
    void contextLoads() {
        assertEquals("file-preparation", step.getName());
    }

    @Test
    void testExecute() {
        JobParameters jobParameters = getJobParameters();

        JobExecution jobExecution = stepRunner.launchStep(step, jobParameters);

        // verify the step run
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        Path billingReport = Paths.get("staging", "billing-2023-01.csv");

        // verify that file is created
        Assertions.assertTrue(Files.exists(billingReport));
    }

    private JobParameters getJobParameters() {
        return new JobParametersBuilder()
                .addString("input.file", "input/billing-2023-01.csv")
                .toJobParameters();
    }

}