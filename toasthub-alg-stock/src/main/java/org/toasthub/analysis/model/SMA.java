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

@Entity
@Table(name = "ta_SMA")
//Simple Moving Average
public class SMA extends BaseAlg{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SMA() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("SMA");
	}

	public SMA(final String symbol){
		super();
		setSymbol(symbol);
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("SMA");
	}

	public SMA(final String code, final Boolean defaultLang, final String dir){
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("SMA");
	}

	public static BigDecimal calculateSMA(final List<BigDecimal> list) {
        BigDecimal sma = BigDecimal.ZERO;
        for (int i = 0; i < list.size(); i++)
        sma = sma.add(list.get(i));
        return sma.divide( new BigDecimal(list.size()) , MathContext.DECIMAL32);
    }

	public static BigDecimal calculateSD(final List<BigDecimal> list) {
        double sum = 0.0, standardDeviation = 0.0;
        final int length = list.size();

        for (int i = 0; i < length; i++) {
            sum += list.get(i).doubleValue();
        }
        final double mean = sum / length;

        for (int i = 0; i < length; i++) {
            standardDeviation += Math.pow(list.get(i).doubleValue() - mean, 2);
        }
        return BigDecimal.valueOf(Math.sqrt(standardDeviation / length));
    }
}
