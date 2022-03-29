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

@Entity
@Table(name = "ta_EMA")
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
		this.setIdentifier("EMA");
	}

	public EMA(String symbol) {
		super();
        this.setSymbol(symbol);
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
		this.setIdentifier("EMA");
	}

	public EMA(String code, Boolean defaultLang, String dir){
		this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
		this.setIdentifier("EMA");
	}

	public static BigDecimal calculateEMA(List<BigDecimal> list){
        BigDecimal initEma = SMA.calculateSMA(list);
        BigDecimal multiplier = BigDecimal.valueOf( 2.0/(list.size()+1) );
        return
        (list.get(list.size()-1)
		.multiply(multiplier))
        .add
        (initEma.multiply((BigDecimal.ONE.subtract(multiplier))));
    }

	public static BigDecimal calculateEMA(List<BigDecimal> list, BigDecimal EmaValue){
        BigDecimal multiplier = BigDecimal.valueOf( 2.0/(list.size()+1) );
        return 
        (list.get(list.size()-1)).multiply(multiplier)
        .add(EmaValue.multiply((BigDecimal.ONE.subtract(multiplier))));
    }
}
