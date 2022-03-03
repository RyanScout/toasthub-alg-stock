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
@Table(name="sa_stock_minute")
public class StockMinute extends BaseEntity{

        private static final long serialVersionUID = 1L;
        private StockDay stockDay;
        private String stock;
        private BigDecimal value;
        private long epochSeconds;
        private long volume;
        private BigDecimal vwap;
        private String type;

        public StockMinute() {
            super();
            this.setIdentifier("StockMinute");
            this.setType("StockMinute");
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
        @ManyToOne(targetEntity = StockDay.class , fetch = FetchType.LAZY)
        @JoinColumn(name = "stock_day_id")
        public StockDay getStockDay() {
            return stockDay;
        }

        public void setStockDay(StockDay stockDay) {
            this.stockDay = stockDay;
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

        public String getStock() {
            return stock;
        }

        public void setStock(String stock) {
            this.stock = stock;
        }

        public StockMinute(String code, Boolean defaultLang, String dir){
            super();
        }
}
