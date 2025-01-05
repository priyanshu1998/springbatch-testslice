package example.billingjob.configuration;

public class BillingJobBeanDirectory {
    public static class FilePreparationStep {
        public static final String STEP = "filePreparationStep";
    }

    public static class GenerateBillingTotalDataStep {
        public static final String READER = "billingDataTableReader";
        public static final String PROCESSOR = "billingDataProcessor";
        public static final String WRITER = "billingDataFileWriter";

        public static final String STEP = "generateBillingTotalDataStep";
    }
}
