package example.model;

@lombok.Data
@lombok.AllArgsConstructor
@lombok.experimental.FieldNameConstants
public class ReportingData {
    private BillingData billingData;
    private double billingTotal;
}
