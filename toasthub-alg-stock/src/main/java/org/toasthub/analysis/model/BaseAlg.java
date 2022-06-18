package org.toasthub.analysis.model;

import java.math.BigDecimal;

import javax.persistence.MappedSuperclass;

import org.toasthub.common.BaseEntity;


@MappedSuperclass()
public abstract class BaseAlg extends BaseEntity{
        
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
        private String symbol;
        private BigDecimal value;
        private String type;
        private long epochSeconds;
        private long correspondingDay;
    
        public BaseAlg() {
            super();
        }

        public long getCorrespondingDay() {
            return correspondingDay;
        }

        public void setCorrespondingDay(long correspondingDay) {
            this.correspondingDay = correspondingDay;
        }

        public BaseAlg(String symbol) {
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
        public String getSymbol() {
            return symbol;
        }
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        public long getEpochSeconds() {
            return epochSeconds;
        }
        public void setEpochSeconds(long epochSeconds) {

            this.epochSeconds = epochSeconds;
        }
}
