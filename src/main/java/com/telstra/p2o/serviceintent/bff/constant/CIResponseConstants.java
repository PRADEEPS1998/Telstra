package com.telstra.p2o.serviceintent.bff.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public class CIResponseConstants {

    public enum SOURCE_SYSTEMS {
        B2CFORCE("B2CFORCE"),
        MICA("MICA");
        private final String value;

        SOURCE_SYSTEMS(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    public enum PRODUCT_RELATIONSHIP_TYPE {
        CONTRACT("CONTRACT");

        private final String value;

        PRODUCT_RELATIONSHIP_TYPE(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    public enum PRODUCT_OFFERING_TYPE {
        PLAN("PLAN"),
        HARDWARE("HARDWARE"),
        CONTRACT("CONTRACT"),
        SERVICE("SERVICE"),
        DISCOUNT("DISCOUNT");
        private final String value;

        PRODUCT_OFFERING_TYPE(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    public enum PRODUCT_OFFERING_SUBTYPE {
        HANDSET("HANDSET"),
        MODEM("MODEM"),
        NPFT("NPFT"),
        TABLET("TABLET"),
        DRT_HANDSET_PRODUCT_CODE("MHDWHST-XFER"),
        DRT_TABLET_PRODUCT_CODE("MHDWTAB-XFER"),
        DRT_MODEM_PRODUCT_CODE("MHDWMDM-XFER"),
        DPC("DPC"),
        DEVICE_REPAYMENT("MDP_000001");

        private final String value;

        PRODUCT_OFFERING_SUBTYPE(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    public enum PRODUCT_STATUS {
        ACTIVE("ACTIVE");
        private final String value;

        PRODUCT_STATUS(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }
}
