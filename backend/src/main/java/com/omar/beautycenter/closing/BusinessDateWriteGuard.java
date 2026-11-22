package com.omar.beautycenter.closing;

import java.time.LocalDate;

/** Integration boundary used by every module that writes business-dated financial records. */
public interface BusinessDateWriteGuard {

    void assertWritable(LocalDate businessDate);
}
