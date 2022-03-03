package org.toasthub.analysis.model;

import java.math.BigDecimal;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;


@Entity
@Table(name="sa_stock_day")
public class StockDay extends BaseEntity{

        private static final long serialVersionUID = 1L;
        private String type;
        private String stock;
        private BigDecimal open;
        private BigDecimal close;
        private BigDecimal high;
        private BigDecimal low;
        private long epochSeconds;
        private long volume;
        private BigDecimal vwap;
        private Set<StockMinute> stockMinutes;

        public StockDay() {
            super();
            setType("StockDay");
            this.setIdentifier("StockDay");
        }
    
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @OneToMany(mappedBy = "stockDay", cascade = CascadeType.ALL)
        public Set<StockMinute> getStockMinutes() {
            return stockMinutes;
        }

        public void setStockMinutes(Set<StockMinute> stockMinutes) {
            this.stockMinutes = stockMinutes;
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

        public String getStock() {
            return stock;
        }

        public void setStock(String stock) {
            this.stock = stock;
        }
}
