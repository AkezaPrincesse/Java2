package com.exam.utility.service;

import com.exam.utility.dto.request.billing.ApproveBillRequest;
import com.exam.utility.dto.request.billing.GenerateBillsRequest;
import com.exam.utility.dto.request.billing.RejectBillRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.billing.BillResponse;
import com.exam.utility.enums.BillStatus;
import com.exam.utility.enums.MeterType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BillingService {
    List<BillResponse> generateBills(GenerateBillsRequest request);
    BillResponse approveBill(ApproveBillRequest request);
    BillResponse rejectBill(RejectBillRequest request);
    BillResponse getBillByNumber(String billNumber);
    BillResponse getBillById(Long id);
    PagedResponse<BillResponse> getAllBills(Pageable pageable);
    PagedResponse<BillResponse> getBillsByCustomer(Long customerId, Pageable pageable);
    PagedResponse<BillResponse> getBillsByStatus(BillStatus status, Pageable pageable);
    PagedResponse<BillResponse> getMyBills(Pageable pageable);
    PagedResponse<BillResponse> searchBills(String keyword, Pageable pageable);
    void cancelBill(Long id);
    byte[] generateBillPdf(String billNumber);
}
