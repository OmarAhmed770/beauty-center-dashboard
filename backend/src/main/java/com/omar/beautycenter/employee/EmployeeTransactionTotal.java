package com.omar.beautycenter.employee;

import java.math.BigDecimal;

public interface EmployeeTransactionTotal {

    EmployeeTransactionType getType();

    BigDecimal getTotal();
}
