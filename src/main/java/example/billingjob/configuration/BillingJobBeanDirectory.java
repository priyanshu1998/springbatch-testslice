package example.billingjob.configuration;

public class BillingJobBeanDirectory {
    public static class FilePreparationStep {
        public static final String STEP = "filePreparationStep";
    }

    public static class IngestBillingDataStep {
        public static final String READER = "billingDataFileReader";
        public static final String WRITER = "billingDataTableWriter";
        public static final String SKIP_LISTENER = "parseFailListener";
    }

    public static class GenerateBillingTotalDataStep {
        public static final String READER = "billingDataTableReader";
        public static final String PROCESSOR = "billingDataProcessor";
        public static final String WRITER = "billingDataFileWriter";

        public static final String STEP = "generateBillingTotalDataStep";
    }
}
