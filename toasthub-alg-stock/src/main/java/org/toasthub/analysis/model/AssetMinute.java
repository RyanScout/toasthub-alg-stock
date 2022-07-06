package org.toasthub.analysis.model;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ta_asset_minute")
public class AssetMinute extends BaseEntity {

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

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

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

    @JsonIgnore
    @ManyToOne(targetEntity = AssetDay.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_day_id")
    public AssetDay getAssetDay() {
        return assetDay;
    }

    public void setAssetDay(final AssetDay assetDay) {
        this.assetDay = assetDay;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (epochSeconds ^ (epochSeconds >>> 32));
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + (int) (volume ^ (volume >>> 32));
        result = prime * result + ((vwap == null) ? 0 : vwap.hashCode());
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
        final AssetMinute other = (AssetMinute) obj;
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
        if (volume != other.volume)
            return false;
        if (vwap == null) {
            if (other.vwap != null)
                return false;
        } else if (!vwap.equals(other.vwap))
            return false;
        return true;
    }

    public BigDecimal getVwap() {
        return vwap;
    }

    public void setVwap(final BigDecimal vwap) {
        this.vwap = vwap;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(final long volume) {
        this.volume = volume;
    }

    public long getEpochSeconds() {
        return epochSeconds;
    }

    public void setEpochSeconds(final long epochSeconds) {
        this.epochSeconds = epochSeconds;
    }

    public AssetMinute(final String code, final Boolean defaultLang, final String dir) {
        super();
    }
}
