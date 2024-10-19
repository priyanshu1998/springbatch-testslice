package example.billingjob.entity;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class ReportingData {
    BillingData billingData;
    double billingTotal;
}
