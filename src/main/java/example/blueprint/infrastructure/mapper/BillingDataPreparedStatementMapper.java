package example.blueprint.infrastructure.mapper;

import example.blueprint.infrastructure.data.BillingData;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BillingDataPreparedStatementMapper implements ItemPreparedStatementSetter<BillingData> {
    @Override
    public void setValues(BillingData item, PreparedStatement ps) throws SQLException {
        ps.setInt(1, item.dataYear());
        ps.setInt(2, item.dataMonth());
        ps.setInt(3, item.accountId());
        ps.setString(4, item.phoneNumber());
        ps.setFloat(5, item.dataUsage());
        ps.setInt(6, item.callDuration());
        ps.setInt(7, item.smsCount());
    }
}
