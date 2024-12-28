package example.blueprint.infrastructure.validator;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@lombok.extern.slf4j.Slf4j
public class BillingJobParametersValidator implements JobParametersValidator {

    private void checkIfFileExists(String inputFile) throws FileNotFoundException {
        if (!Files.exists(Paths.get(Objects.requireNonNull(inputFile)))){
            throw new FileNotFoundException("File not found");
        }
    }

    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {

        log.debug("Validating job parameters");
        try {
            String inputFile = parameters.getString("input.file");
            checkIfFileExists(inputFile);
        } catch (NullPointerException e) {
            throw new JobParametersInvalidException("input.file parameter is required");
        } catch (FileNotFoundException e) {
            throw new JobParametersInvalidException("File not found");
        }
    }
}
