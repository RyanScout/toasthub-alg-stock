package org.toasthub.analysis.model;

import java.math.BigDecimal;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;


@Entity
@Table(name="ta_asset_day")
public class AssetDay extends BaseEntity{

        private static final long serialVersionUID = 1L;
        private String type;
        private String symbol;
        private BigDecimal open;
        private BigDecimal close;
        private BigDecimal high;
        private BigDecimal low;
        private long epochSeconds;
        private long volume;
        private BigDecimal vwap;
        private Set<AssetMinute> assetMinutes;

        public AssetDay() {
            super();
            setType("AssetDay");
            this.setIdentifier("AssetDay");
        }
    
        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @OneToMany(mappedBy = "assetDay", cascade = CascadeType.ALL)
        public Set<AssetMinute> getAssetMinutes() {
            return assetMinutes;
        }

        public void setAssetMinutes(Set<AssetMinute> assetMinutes) {
            this.assetMinutes = assetMinutes;
        }

        public BigDecimal getLow() {
            return low;
        }

        public void setLow(BigDecimal low) {
            this.low = low;
        }

        public BigDecimal getHigh() {
            return high;
        }

        public void setHigh(BigDecimal high) {
            this.high = high;
        }

        public BigDecimal getClose() {
            return close;
        }

        public void setClose(BigDecimal close) {
            this.close = close;
        }

        public BigDecimal getOpen() {
            return open;
        }

        public void setOpen(BigDecimal open) {
            this.open = open;
        }

        public BigDecimal getVwap() {
            return vwap;
        }

        public void setVwap(BigDecimal vwap) {
            this.vwap = vwap;
        }

        public long getVolume() {
            return volume;
        }

        public void setVolume(long volume) {
            this.volume = volume;
        }

        public long getEpochSeconds() {
            return epochSeconds;
        }

        public void setEpochSeconds(long epochSeconds) {
            this.epochSeconds = epochSeconds;
        }
}
