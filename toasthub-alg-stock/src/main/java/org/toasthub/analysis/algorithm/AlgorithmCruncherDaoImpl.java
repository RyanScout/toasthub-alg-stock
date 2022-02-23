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

package org.toasthub.analysis.algorithm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.analysis.model.EMA;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.MACD;
import org.toasthub.analysis.model.SL;
import org.toasthub.analysis.model.SMA;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Repository("AlgorithmCruncherDao")
@Transactional()
public class AlgorithmCruncherDaoImpl implements AlgorithmCruncherDao {

	@Autowired
	protected EntityManager entityManager;

	@Override
	public void delete(Request request, Response response) throws Exception {
	}
	@Override
	public void save(Request request, Response response) throws Exception {
	}

	@Override
	@SuppressWarnings("unchecked")
	public void saveAll(Map<String, List<?>> map) {
		for (String key : map.keySet()) {
			List<Object> list = (List<Object>) map.get(key);
			for (Object obj : list) {
				entityManager.merge(obj);
			}
		}
	}
	@Override
	public Boolean queryChecker(String alg, long epochSeconds, String type, String stock){
		Query query = queryBuilder(alg, epochSeconds, type, stock);
		try {
			query.getSingleResult();
			return true;
		} catch (Exception e) {
			if (e.getMessage().equals("No entity found for query"))
				return false;
			else
			System.out.println(e.getMessage());
		}
		return true;
	}

	@Override
	public void items(Request request, Response response) throws Exception {
	}

	@Override
	public void itemCount(Request request, Response response) throws Exception {
	}

	@Override
	public void item(Request request, Response response) throws Exception {
	}


	@Override
	public BigDecimal querySLValue(SL sl) {
		Query query = queryBuilder("MACD" , sl.getEpochSeconds() , sl.getType() ,sl.getStock());
		try {
			MACD[] macdArr = new MACD[9];
			MACD macd;
			for (int i = 0; i < 9; i++) {
				query.setParameter("epochSeconds", sl.getEpochSeconds() - (60 * i));
				macd = (MACD) query.getSingleResult();
				macdArr[i] = macd;
			}
			return SL.calculateSL(macdArr);
		} catch (Exception e) {
			if (e.getMessage().equals("No entity found for query"))
				return SL.calculateSL(sl.getStockBars());
			else
				System.out.println(e.getMessage());
			return null;
		}
	}

	@Override
	public BigDecimal queryMACDValue(MACD macd) {
		Query query = queryBuilder("EMA" , macd.getEpochSeconds() , macd.getType() , macd.getStock());
		try {
			query.setParameter("type", "26-period");
			EMA longEMA = (EMA) query.getSingleResult();
			query.setParameter("type", "13-period");
			EMA shortEMA = (EMA) query.getSingleResult();
			return shortEMA.getValue().subtract(longEMA.getValue());
		} catch (Exception e) {
			if (e.getMessage().equals("No entity found for query"))
				return MACD.calculateMACD(macd.getStockBars());
			else
				System.out.println(e.getMessage());
			return null;
		}
	}

	@Override
	public BigDecimal queryLBBValue(LBB lbb) {
		Query query = queryBuilder("SMA" , lbb.getEpochSeconds() , lbb.getType() , lbb.getStock());
		try {
			SMA sma = (SMA) query.getSingleResult();
			return LBB.calculateLBB(lbb.getStockBars(), sma.getValue());
		} catch (Exception e) {
			if (e.getMessage().equals("No entity found for query"))
				return LBB.calculateLBB(lbb.getStockBars());
			else
				System.out.println(e.getMessage());
			return null;
		}
	}

	@Override
	public BigDecimal queryEMAValue(EMA ema){
		Query query = queryBuilder("EMA" , ema.getEpochSeconds() , ema.getType() , ema.getStock());
		try {
			EMA prevEMA = (EMA) query.getSingleResult();
			return EMA.calculateEMA(ema.getStockBars(), prevEMA.getValue());
		} catch (Exception e) {
			if (e.getMessage().equals("No entity found for query"))
				return EMA.calculateEMA(ema.getStockBars());
			else
				System.out.println(e.getMessage());
			return null;
		}
	}

	@Override
	public Query queryBuilder(String alg, long epochSeconds, String type, String stock){
		String queryStr = "SELECT DISTINCT x FROM " + alg + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.stock =:stock";
		Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", epochSeconds);
		query.setParameter("type", type);
		query.setParameter("stock", stock);
		return query;
	}

	@Override
	public BigDecimal queryAlgValue(String alg, String stock, String type, long epochSeconds) {
		Query query = queryBuilder(alg, epochSeconds , type , stock);
		try {
			switch (alg) {
				case "SMA":
					SMA sma = (SMA) query.getSingleResult();
					return sma.getValue();

				case "MACD":
					MACD macd = (MACD) query.getSingleResult();
					return macd.getValue();

				case "SL":
					SL sl = (SL) query.getSingleResult();
					return sl.getValue();

				case "EMA":
					EMA ema = (EMA) query.getSingleResult();
					return ema.getValue();

				case "LBB":
					LBB lbb = (LBB) query.getSingleResult();
					return lbb.getValue();

				default:
					return null;
			}
		} catch (Exception e) {
			if (e.getMessage().equals("No entity found for query"))
				return null;
			else {
				e.printStackTrace();
				return null;
			}
		}
	}
}
