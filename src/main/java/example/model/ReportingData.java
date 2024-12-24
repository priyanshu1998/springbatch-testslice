package example.model;

@lombok.Data
@lombok.AllArgsConstructor
public class ReportingData {
    BillingData billingData;
    double billingTotal;
}
