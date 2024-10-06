package example.billingjob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@lombok.RequiredArgsConstructor
class BillingJobApplicationTests {

	private final ConfigurableApplicationContext context;

	@Test
	void contextLoads() {
		assertThat(AssertableApplicationContext.get(() -> context)) //
				.hasSingleBean(JobRepository.class)
				.hasSingleBean(TransactionManager.class)
				.satisfies(ctx -> {
					// assert that correct job bean is present
					assertThat(ctx.getBean(Job.class).getName())
							.isEqualTo("BillingJob");
				});
	}

	@Test
	void testJobExecution(CapturedOutput output) throws Exception {
		// given
		JobParameters jobParameters = new JobParameters();
		var jobLauncher = context.getBean(JobLauncher.class);
		var job = context.getBean(BillingJob.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);

		// then
		Assertions.assertTrue(output.getOut().contains("processing billing information"));
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

}
