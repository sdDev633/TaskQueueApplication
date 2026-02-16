package com.taskqueue.www.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceData {
    private String invoiceNumber;
    private String date;
    private String customerName;
    private List<InvoiceItem> items;
    private double grandTotal;
}
