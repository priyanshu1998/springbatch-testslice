package example.billingjob;

import org.springframework.batch.core.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.lang.NonNull;


public class BillingJob implements Job {
    @Override
    @NonNull
    public String getName() {
        return "BillingJob";
    }

    private final JobRepository jobRepository;

    public BillingJob(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public boolean isRestartable() {
        return Job.super.isRestartable();
    }


    @Override
    public void execute(JobExecution execution) {
        try {
            System.out.println("processing billing information");
            execution.setStatus(BatchStatus.COMPLETED);
            execution.setExitStatus(ExitStatus.COMPLETED);
//            throw new Exception("Unable to process billing information");
        } catch (Exception exception) {
            execution.addFailureException(exception);
            execution.setStatus(BatchStatus.COMPLETED);
            execution.setExitStatus(ExitStatus.FAILED.addExitDescription(exception.getMessage()));
        } finally {
            this.jobRepository.update(execution);
        }
    }

    @Override
    public JobParametersIncrementer getJobParametersIncrementer() {
        return Job.super.getJobParametersIncrementer();
    }

    @Override
    public JobParametersValidator getJobParametersValidator() {
        return Job.super.getJobParametersValidator();
    }
}
