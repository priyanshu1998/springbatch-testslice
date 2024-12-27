package example.model;


public record ReportingData (
    BillingData billingData,
    double billingTotal) {

    public static class Fields {
        public static final String BILLING_DATA = "billingData";
        public static final String BILLING_TOTAL = "billingTotal";
    }
}
