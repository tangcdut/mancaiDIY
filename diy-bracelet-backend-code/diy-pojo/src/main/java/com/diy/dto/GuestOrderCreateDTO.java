package com.diy.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Guest order creation DTO (no login required)
 */
@Data
public class GuestOrderCreateDTO {

    private List<GuestOrderItemDTO> items;

    private GuestAddressDTO address;

    private BigDecimal amount;

    private String currency;

    private String remark;

    private String email;

    @Data
    public static class GuestOrderItemDTO {
        private Long productId;
        private String title;
        private BigDecimal price;
        private Integer quantity;
        private String image;
    }

    @Data
    public static class GuestAddressDTO {
        private String name;
        private String phone;
        private String address;
        private String city;
        private String state;
        private String zip;
        private String country;
    }
}
