/*
 * Copyright (C) 2020 The ToastHub Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Edward H. Seufert
 */

package org.toasthub.analysis.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;

@Entity
@Table(name = "tb_EMA")
//Exponential Moving Average
public class EMA extends BaseAlg{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EMA() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
	}

	public EMA(String stock) {
		super();
        this.setStock(stock);
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
	}

	public EMA(String code, Boolean defaultLang, String dir){
		this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
	}

	public void initializer(List<StockBar> stockBars, int period){
		setType(period+"-period");
		setStockBars(stockBars.subList(stockBars.size()-period, stockBars.size()));
		setMinute(stockBars.get(stockBars.size()-1).getTimestamp().getMinute());
		setHour(stockBars.get(stockBars.size()-1).getTimestamp().getHour());
		setDay(stockBars.get(stockBars.size()-1).getTimestamp().getDayOfMonth());
		setMonth(stockBars.get(stockBars.size()-1).getTimestamp().getMonthValue());
		setYear(stockBars.get(stockBars.size()-1).getTimestamp().getYear());
		setEpochSeconds((long)stockBars.get(stockBars.size()-1).getTimestamp().toEpochSecond());
	}


	public static BigDecimal calculateEMA(List<StockBar> stockBars){
        BigDecimal initEma = SMA.calculateSMA(stockBars);
        BigDecimal multiplier = BigDecimal.valueOf( 2.0/(stockBars.size()+1) );
        return (BigDecimal.valueOf
        (stockBars.get(stockBars.size()-1).getClose()).multiply(multiplier) )
        .add(initEma.multiply((BigDecimal.ONE.subtract(multiplier))));
    }

	public static BigDecimal calculateEMA(List<StockBar> stockBars, BigDecimal EMA){
        BigDecimal multiplier = BigDecimal.valueOf( 2.0/(stockBars.size()+1) );
        return (BigDecimal.valueOf
        (stockBars.get(stockBars.size()-1).getClose()).multiply(multiplier) )
        .add(EMA.multiply((BigDecimal.ONE.subtract(multiplier))));
    }
}
