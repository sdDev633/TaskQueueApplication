package com.taskqueue.www.model;


import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceData {

    private String customerName;
    private List<Item> items;
    private String invoiceNumber;
    private String invoiceDate;
    private LocalDateTime createdAt;

    // Computed fields
    public Double getSubtotal() {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        return items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQty())
                .sum();
    }

    public Double getTax() {
        // 10% tax
        return getSubtotal() * 0.10;
    }

    public Double getTotal() {
        return getSubtotal() + getTax();
    }

    public String getFormattedInvoiceDate() {
        if (invoiceDate != null) {
            return invoiceDate;
        }
        if (createdAt != null) {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String name;
        private int qty;
        private double price;

        public double getTotal() {
            return this.price * this.qty;
        }
    }
}