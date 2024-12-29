package example.billingjob.configuration.listener;

import example.billingjob.configuration.BatchConfig;
import example.blueprint.listener.BillingDataSkipListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@SpringBatchTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringJUnitConfig(classes = {BatchConfig.class, IngestBillingDataStepListenerConfigurations.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
class IngestBillingDataStepListenerTests {
    private final BillingDataSkipListener billingDataSkipListener;

    @Test
    void contextTest(){
        Assertions.assertInstanceOf(SkipListener.class, billingDataSkipListener);
    }

    @BeforeAll
    @lombok.SneakyThrows
    void init() {
        var params = getJobParameters();
        FileSystemUtils.deleteRecursively(Paths.get(Objects.requireNonNull(params.getString("skip.file"))));
    }

    @Test
    @lombok.SneakyThrows
    void testOnSkipRead(){
        // given
        StepExecution stepExecution = MetaDataInstanceFactory
                .createStepExecution(getJobParameters());

        // when
        StepScopeTestUtils.doInStepScope(stepExecution, () -> {
            billingDataSkipListener.onSkipInRead(
                    new FlatFileParseException("<error msg>", "<input line containing error>", 7));

            Path skipFile = Paths.get("staging", "billing-skipped-2023-03.csv");

            // verify that file is created
            Assertions.assertTrue(Files.exists(skipFile));
            Assertions.assertEquals(1L, countLines(skipFile));

            return null;
        });
    }

    private JobParameters getJobParameters() {
        return new JobParametersBuilder()
                .addString("skip.file", "staging/billing-skipped-2023-03.csv")
                .toJobParameters();
    }

    @lombok.SneakyThrows
    private long countLines(Path filePath) {
        try (var lines = Files.lines(filePath)) {
            return lines.count();
        }
    }
}