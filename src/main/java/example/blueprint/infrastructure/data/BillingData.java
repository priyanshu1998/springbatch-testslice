package example.blueprint.infrastructure.data;

public record BillingData (
    int dataYear,
    int dataMonth,
    int accountId,
    String phoneNumber,
    float dataUsage,
    int callDuration,
    int smsCount) {

    public static class Fields {
        public static final String DATA_YEAR = "dataYear";
        public static final String DATA_MONTH = "dataMonth";
        public static final String ACCOUNT_ID = "accountId";
        public static final String PHONE_NUMBER = "phoneNumber";
        public static final String DATA_USAGE = "dataUsage";
        public static final String CALL_DURATION = "callDuration";
        public static final String SMS_COUNT = "smsCount";
    }
}