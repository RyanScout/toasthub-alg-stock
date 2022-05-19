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
@Table(name = "ta_UBB")
// Lower Bollinger Band
public class UBB extends BaseAlg {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BigDecimal standardDeviations;

	public UBB() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("UBB");
	}

	public BigDecimal getStandardDeviations() {
		return standardDeviations;
	}

	public void setStandardDeviations(BigDecimal standardDeviations) {
		this.standardDeviations = standardDeviations;
	}

	public UBB(String symbol) {
		super();
		this.setSymbol(symbol);
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("UBB");
	}

	public UBB(String code, Boolean defaultLang, String dir) {
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("UBB");
	}

	public static BigDecimal calculateUBB(List<BigDecimal> list, BigDecimal standardDeviations) {
		BigDecimal sma = SMA.calculateSMA(list);

		return sma.add(
				SMA.calculateSD(list).multiply(standardDeviations));
	}

	public static BigDecimal calculateUBB(List<BigDecimal> list, BigDecimal sma, BigDecimal standardDeviations) {
		return sma.add(
				SMA.calculateSD(list).multiply(standardDeviations));
	}
}
