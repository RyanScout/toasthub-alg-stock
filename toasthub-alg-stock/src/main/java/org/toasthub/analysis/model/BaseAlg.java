package org.toasthub.analysis.model;

import java.math.BigDecimal;

import javax.persistence.MappedSuperclass;

import org.toasthub.common.BaseEntity;

@MappedSuperclass()
public abstract class BaseAlg extends BaseEntity {

    /**
    * 
    */
    private static final long serialVersionUID = 1L;
    private String symbol = "";

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // TODO Auto-generated method stub
        return super.clone();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (correspondingDay ^ (correspondingDay >>> 32));
        result = prime * result + (int) (epochSeconds ^ (epochSeconds >>> 32));
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final BaseAlg other = (BaseAlg) obj;
        if (correspondingDay != other.correspondingDay)
            return false;
        if (epochSeconds != other.epochSeconds)
            return false;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    private BigDecimal value = BigDecimal.ZERO;
    private String type = "";
    private long epochSeconds = 0;
    private long correspondingDay = 0;

    public BaseAlg() {
        super();
    }

    public long getCorrespondingDay() {
        return correspondingDay;
    }

    public void setCorrespondingDay(final long correspondingDay) {
        this.correspondingDay = correspondingDay;
    }

    public BaseAlg(final String symbol) {
        super();
    }

    public BaseAlg(final String code, final Boolean defaultLang, final String dir) {
        super();
    }

    // getters and setters
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public long getEpochSeconds() {
        return epochSeconds;
    }

    public void setEpochSeconds(final long epochSeconds) {

        this.epochSeconds = epochSeconds;
    }
}
