package org.toasthub.analysis.model;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name="ta_asset_minute")
public class AssetMinute extends BaseEntity{

        private static final long serialVersionUID = 1L;
        private AssetDay assetDay;
        private String symbol;
        private BigDecimal value;
        private long epochSeconds;
        private long volume;
        private BigDecimal vwap;
        private String type;

        public AssetMinute() {
            super();
            this.setIdentifier("AssetMinute");
            this.setType("AssetMinute");
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

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        @JsonIgnore
        @ManyToOne(targetEntity = AssetDay.class , fetch = FetchType.LAZY)
        @JoinColumn(name = "asset_day_id")
        public AssetDay getAssetDay() {
            return assetDay;
        }

        public void setAssetDay(AssetDay assetDay) {
            this.assetDay = assetDay;
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

        public AssetMinute(String code, Boolean defaultLang, String dir){
            super();
        }
}
