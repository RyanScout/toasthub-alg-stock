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
@Table(name = "sa_MACD")
//Moving Average Convergence/Divergence Indicator
public class MACD extends BaseAlg{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MACD() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("MACD");
	}

	public MACD(String stock) {
		super();
		this.setStock(stock);
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("MACD");
	}

	public MACD(String code, Boolean defaultLang, String dir){
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("MACD");
	}

	public void initializer(List<StockBar> stockBars){
		setType("MACD");
		setStockBars(stockBars);
		setEpochSeconds((long)stockBars.get(stockBars.size()-1).getTimestamp().toEpochSecond());
	}

	public static BigDecimal calculateMACD(List<BigDecimal> list){
        List<BigDecimal> trimmedList = list.subList(list.size()-25, list.size());
        BigDecimal longEMA = EMA.calculateEMA(trimmedList);
        trimmedList = list.subList(list.size()-11, list.size());
        BigDecimal shortEMA = EMA.calculateEMA(trimmedList);
        return shortEMA.subtract(longEMA);
    }
}
