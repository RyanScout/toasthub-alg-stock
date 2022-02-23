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
import java.math.MathContext;
import java.time.Instant;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;

@Entity
@Table(name = "tb_SL")
//Signal Line
public class SL extends BaseAlg{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public SL() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
	}

	public SL(String stock) {
		super();
		this.setStock(stock);
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
	}

	public SL(String code, Boolean defaultLang, String dir){
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		
	}

	public void initializer(List<StockBar> stockBars){
		setType("SL");
		setStockBars(stockBars.subList(stockBars.size()-40, stockBars.size()));
		setMinute(stockBars.get(stockBars.size()-1).getTimestamp().getMinute());
		setHour(stockBars.get(stockBars.size()-1).getTimestamp().getHour());
		setDay(stockBars.get(stockBars.size()-1).getTimestamp().getDayOfMonth());
		setMonth(stockBars.get(stockBars.size()-1).getTimestamp().getMonthValue());
		setYear(stockBars.get(stockBars.size()-1).getTimestamp().getYear());
		setEpochSeconds((long)stockBars.get(stockBars.size()-1).getTimestamp().toEpochSecond());
	}

	public static BigDecimal calculateSL(List<StockBar> stockBars){
        BigDecimal multiplier = BigDecimal.valueOf( 2.0/(9+1) );
        BigDecimal macdAverage = BigDecimal.ZERO;
        List<StockBar> stockBar;
        for(int i = 8 ; i > 0 ; i--){
            stockBar = stockBars.subList(0, stockBars.size()-i);
            macdAverage = macdAverage.add(MACD.calculateMACD(stockBar));
        }
        macdAverage = macdAverage.divide(BigDecimal.valueOf(8), MathContext.DECIMAL32);
        macdAverage = macdAverage.multiply((BigDecimal.ONE.subtract(multiplier)));
        return MACD.calculateMACD(stockBars).multiply(multiplier).add(macdAverage);
    }
    public static BigDecimal calculateSL(MACD[] macdArr){
        BigDecimal multiplier = BigDecimal.valueOf( 2.0/(10) );
        BigDecimal macdAverage = BigDecimal.ZERO;
        for(int i =8 ; i > 1 ; i--){
            macdAverage = macdAverage.add(macdArr[i].getValue());
        }
        macdAverage = macdAverage.divide(BigDecimal.valueOf(8), MathContext.DECIMAL32);
        macdAverage = macdAverage.multiply((BigDecimal.ONE.subtract(multiplier)));
        return macdArr[0].getValue().multiply(multiplier).add(macdAverage);
    }
}
