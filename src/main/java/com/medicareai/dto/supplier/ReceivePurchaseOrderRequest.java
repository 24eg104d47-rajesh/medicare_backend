package com.medicareai.dto.supplier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivePurchaseOrderRequest {
    @NotEmpty(message = "Batch details are required for receiving purchase order")
    @Valid
    private List<ReceiveItemBatchRequest> batchDetails;
}
