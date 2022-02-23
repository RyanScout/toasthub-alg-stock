package org.toasthub.analysis.model;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;


@MappedSuperclass()
public abstract class BaseAlg extends BaseEntity{
        
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		private int minute;
        private int hour;
        private int day;
        private int month;
        private int year;
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
        public int getYear() {
            return year;
        }
        public void setYear(int year) {
            this.year = year;
        }
        public int getMonth() {
            return month;
        }
        public void setMonth(int month) {
            this.month = month;
        }
        public int getDay() {
            return day;
        }
        public void setDay(int day) {
            this.day = day;
        }
        public int getHour() {
            return hour;
        }
        public void setHour(int hour) {
            this.hour = hour;
        }
        public int getMinute() {
            return minute;
        }
        public void setMinute(int minute) {
            this.minute = minute;
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
