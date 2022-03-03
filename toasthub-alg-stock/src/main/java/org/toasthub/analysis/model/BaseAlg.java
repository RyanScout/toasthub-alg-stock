package org.toasthub.analysis.model;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.toasthub.common.BaseEntity;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;


@MappedSuperclass()
public abstract class BaseAlg extends BaseEntity{
        
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
        private String stock;
        private BigDecimal value;
        private String type;
        private long epochSeconds;
        private List<StockBar> stockBars;
    
        public BaseAlg() {
            super();
        }

        public BaseAlg(String stock) {
            super();
        }
    
        public BaseAlg(String code, Boolean defaultLang, String dir){
            super();
        }
    
        //getters and setters
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
        public String getStock() {
            return stock;
        }
        public void setStock(String stock) {
            this.stock = stock;
        }
        public long getEpochSeconds() {
            return epochSeconds;
        }
        public void setEpochSeconds(long epochSeconds) {

            this.epochSeconds = epochSeconds;
        }
        @Transient
        public List<StockBar> getStockBars() {
            return stockBars;
        }
        public void setStockBars(List<StockBar> stockBars) {
            this.stockBars = stockBars;
        }
}
