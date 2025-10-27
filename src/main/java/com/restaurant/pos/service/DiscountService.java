package com.restaurant.pos.service;

public class DiscountService {
    public enum DiscountType {
        REGULAR(0.0),
        SENIOR(0.20),
        PWD(0.20);

        private final double rate;

        DiscountType(double rate) {
            this.rate = rate;
        }

        public double getRate() {
            return rate;
        }
    }

    public static record DiscountResult(int discountCents, String description) {}

    public DiscountResult calculateDiscount(int subtotalCents, DiscountType type) {
        if (type == DiscountType.REGULAR) {
            return new DiscountResult(0, "No discount");
        }
        
        int discountAmount = (int) Math.round(subtotalCents * type.getRate());
        String description = String.format("%s Discount (%.0f%%)", 
            type == DiscountType.SENIOR ? "Senior Citizen" : "PWD", 
            type.getRate() * 100);
        
        return new DiscountResult(discountAmount, description);
    }

    public int applyDiscount(int subtotalCents, DiscountType type) {
        return subtotalCents - calculateDiscount(subtotalCents, type).discountCents();
    }
}
