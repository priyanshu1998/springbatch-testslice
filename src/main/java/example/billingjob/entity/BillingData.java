package example.billingjob.entity;

@lombok.Getter()
@lombok.Setter
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
