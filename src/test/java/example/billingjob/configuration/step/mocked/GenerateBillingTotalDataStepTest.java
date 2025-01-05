package example.billingjob.configuration.step.mocked;


import example.billingjob.configuration.BatchConfig;
import example.billingjob.configuration.BillingJobBeanDirectory.GenerateBillingTotalDataStep;
import example.billingjob.configuration.step.GenerateBillingTotalDataStepConfiguration;
import example.billingjob.service.PricingService;
import example.blueprint.infrastructure.data.BillingData;
import example.blueprint.infrastructure.data.ReportingData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.test.StepRunner;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBatchTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringJUnitConfig(classes = {GenerateBillingTotalDataStepConfiguration.class,
        BatchConfig.class, PricingService.class})
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
class GenerateBillingTotalDataStepTest {
    private final Step step;
    private final StepRunner stepRunner;
    private final DataSource dataSource;

    @MockitoBean(value= GenerateBillingTotalDataStep.READER)
    private final JdbcCursorItemReader<BillingData> fromBillingDataTable = Mockito.mock();

    @MockitoBean(value= GenerateBillingTotalDataStep.WRITER)
    private final FlatFileItemWriter<ReportingData> toOutputFile = Mockito.mock();

    private void testContextAssertions(ConfigurableApplicationContext context){
        AssertableApplicationContext assertableContext = AssertableApplicationContext.get(() -> context);

        // there should be are two beans: `dataSource` and `batchDataSource`
        assertThat(assertableContext.getBeansOfType(DataSource.class)).hasSize(2)
                .satisfies(map -> assertTrue(Mockito.mockingDetails(map.get("batchDataSource")).isMock()))
                .satisfies(map -> assertFalse(Mockito.mockingDetails(map.get("dataSource")).isMock()));

        assertFalse(Mockito.mockingDetails(dataSource).isMock());
    }



    @Test
    void contextLoads(ConfigurableApplicationContext context) {
        this.testContextAssertions(context);

        assertEquals("generate-billing-total-data", step.getName());

        GenerateBillingTotalDataStepConfiguration bean1 = (GenerateBillingTotalDataStepConfiguration)context.getBean("generateBillingTotalDataStepConfiguration");
        var bean2 = bean1.writer(null);
        assertSame(bean2, toOutputFile);


    }

    @Test
    @lombok.SneakyThrows
    void testExecuteUsingMocks() {
        JdbcCursorItemReader<BillingData> reader = fromBillingDataTable;
        FlatFileItemWriter<ReportingData> writer = toOutputFile;
//
        Mockito.when(reader.read()).thenReturn(
                new BillingData(2023,1,102,"404-555-1002",15.72f,110,827),
                new BillingData(2023,1,101,"404-555-1001",69.87f,289,77),
                null);

        Mockito.doNothing().when(writer).write(Mockito.any());

        JobParameters jobParameters = getJobParameters();
        stepRunner.launchStep(step, jobParameters);

        Mockito.verify(reader, Mockito.times(3)).read();
        Mockito.verify(writer, Mockito.times(1)).write(Mockito.any());
    }

    private JobParameters getJobParameters() {
        return new JobParametersBuilder()
                .addString("output.file", "staging/billing-report-2023-01.csv")
                .addJobParameter("data.year", 2023, Integer.class)
                .addJobParameter("data.month", 1, Integer.class)
                .toJobParameters();
    }
}