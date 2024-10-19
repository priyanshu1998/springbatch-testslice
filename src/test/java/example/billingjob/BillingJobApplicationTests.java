package example.billingjob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@lombok.RequiredArgsConstructor
@Sql(statements = BillingJobApplicationTests.CREATE_BILLING_TABLE)
class BillingJobApplicationTests {

	private final JobRepositoryTestUtils jobRepositoryTestUtils;
	private final JobLauncherTestUtils jobLauncherTestUtils;
	private final JdbcTemplate jdbcTemplate;

	public static final String CREATE_BILLING_TABLE = """
		CREATE TABLE IF NOT EXISTS billing_data (
			data_year INTEGER,
			data_month INTEGER,
			account_id INTEGER,
			phone_number VARCHAR(12),
			data_usage DOUBLE PRECISION,
			call_duration INTEGER,
			sms_count INTEGER
		);
	""";


	@BeforeEach
	void tearDown() {
		this.jobRepositoryTestUtils.removeJobExecutions();
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "billing_data");
	}



	@Test
	void contextLoads() {
		assertThat(jobLauncherTestUtils.getJob().getName())
				.isEqualTo("BillingJob");
	}

	@Test
	void testJobExecution() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder()
				.addString("input.file", "input/billing-2023-01.csv")
				.addString("output.file", "staging/billing-report-2023-01.csv")
				.addJobParameter("data.year", 2023, Integer.class)
				.addJobParameter("data.month", 1, Integer.class)
				.toJobParameters();

		// when
		JobExecution execution = jobLauncherTestUtils.launchJob(jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());
		Assertions.assertTrue(Files.exists(Paths.get("staging", "billing-2023-01.csv")));

		Assertions.assertEquals(1000, JdbcTestUtils.countRowsInTable(jdbcTemplate, "billing_data"));

		Path billingReport = Paths.get("staging", "billing-report-2023-01.csv");
		Assertions.assertTrue(Files.exists(billingReport));

		try(var lines = Files.lines(billingReport)) {
			Assertions.assertEquals(781,lines.count());
		}
	}

}
