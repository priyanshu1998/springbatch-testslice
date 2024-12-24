package example.model;

@lombok.Data
@lombok.RequiredArgsConstructor
public class BillingData {
    int dataYear;
    int dataMonth;
    int accountId;
    String phoneNumber;
    float dataUsage;
    int callDuration;
    int smsCount;
}
