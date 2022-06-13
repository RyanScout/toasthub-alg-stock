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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.model.Symbol;
import org.toasthub.model.TechnicalIndicator;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Repository("AlgorithmCruncherDao")
@Transactional()
public class AlgorithmCruncherDaoImpl implements AlgorithmCruncherDao {

	@Autowired
	protected EntityManager entityManager;

	@Override
	public void getRecentAssetDay(Request request, Response response) {
		String x = (String) request.getParam(GlobalConstant.SYMBOL);

		if (Arrays.asList(Symbol.SYMBOLS).contains(x)) {
			String queryStr = "SELECT * FROM tradeanalyzer_main.ta_asset_day WHERE symbol = \""
					+ x
					+ "\" ORDER BY id DESC LIMIT 0, 1;";

			Query query = entityManager.createNativeQuery(queryStr, AssetDay.class);
			Object result = query.getSingleResult();

			response.addParam(GlobalConstant.ITEM, result);
		} else
			System.out.println("Symbol does not match symbols");
	}

	@Override
	public void getRecentAssetMinute(Request request, Response response) {
		String x = (String) request.getParam(GlobalConstant.SYMBOL);

		if (Arrays.asList(Symbol.SYMBOLS).contains(x)) {

			String queryStr = "SELECT * FROM tradeanalyzer_main.ta_asset_minute WHERE symbol = \""
					+ x
					+ "\" ORDER BY id DESC LIMIT 0, 1;";

			Query query = entityManager.createNativeQuery(queryStr, AssetMinute.class);
			Object result = query.getSingleResult();

			response.addParam(GlobalConstant.ITEM, result);
		} else
			System.out.println("Symbol does not contain symbols");
	}

	@Override
	public void delete(Request request, Response response) throws Exception {
	}

	@Override
	public void save(Request request, Response response) throws Exception {
		entityManager.merge((Object) request.getParam(GlobalConstant.ITEM));
	}

	@Override
	public void saveAll(List<AssetDay> assetDays) {
		for (AssetDay tempAssetDay : assetDays) {
			entityManager.merge(tempAssetDay);
		}
	}

	@Override
	public void items(Request request, Response response) throws Exception {

		String x = "";
		switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
			case "SMA":
				x = "SMA";
				break;
			case "EMA":
				x = "EMA";
				break;
			case "LBB":
				x = "LBB";
				break;
			case "UBB":
				x = "UBB";
				break;
			case "MACD":
				x = "MACD";
				break;
			case "SL":
				x = "SL";
				break;
			case "AssetDay":
				x = "AssetDay";
				break;
			case "AssetMinute":
				x = "AssetMinute";
				break;
			case "TRADE_SIGNAL":
				getTradeSignals(request, response);
				return;
			case "TECHNICAL_INDICATOR":
				getAlgSets(request, response);
				return;
			default:
				System.out.println("Invalid request");
				return;
		}

		String queryStr = "SELECT DISTINCT x FROM " + x + " AS x WHERE x.symbol =:symbol";

		Query query = entityManager.createQuery(queryStr);
		query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
		List<?> items = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, items);
	}

	@Override
	public void itemCount(Request request, Response response) throws Exception {

		String x = "";
		switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
			case "SMA":
				x = "SMA";
				break;
			case "EMA":
				x = "EMA";
				break;
			case "LBB":
				x = "LBB";
				break;
			case "UBB":
				x = "UBB";
				break;
			case "MACD":
				x = "MACD";
				break;
			case "SL":
				x = "SL";
				break;
			case "AssetDay":
				x = "AssetDay";
				break;
			case "AssetMinute":
				x = "AssetMinute";
				break;
			default:
				break;
		}

		String queryStr = "SELECT COUNT(DISTINCT x) FROM " + x + " AS x ";

		boolean and = false;
		if (request.containsParam(GlobalConstant.EPOCHSECONDS)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.epochSeconds =:epochSeconds ";
			and = true;
		}
		if (request.containsParam(GlobalConstant.SYMBOL)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.symbol =:symbol ";
			and = true;
		}
		if (request.containsParam(GlobalConstant.TYPE)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.type =:type ";
			and = true;
		}
		if (x.equals("LBB") || x.equals("UBB")) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.standardDeviations =:standardDeviations ";
			and = true;
		}

		Query query = entityManager.createQuery(queryStr);

		if (request.containsParam(GlobalConstant.EPOCHSECONDS)) {
			query.setParameter("epochSeconds", (long) request.getParam(GlobalConstant.EPOCHSECONDS));
		}
		if (request.containsParam(GlobalConstant.TYPE)) {
			query.setParameter("type", (String) request.getParam(GlobalConstant.TYPE));
		}
		if (request.containsParam(GlobalConstant.SYMBOL)) {
			query.setParameter("symbol", (String) request.getParam(GlobalConstant.SYMBOL));
		}
		if (x.equals("LBB") || x.equals("UBB")) {
			query.setParameter("standardDeviations", (BigDecimal) request.getParam("STANDARD_DEVIATIONS"));
		}

		Long count = (Long) query.getSingleResult();
		if (count == null) {
			count = 0l;
		}
		response.addParam(GlobalConstant.ITEMCOUNT, count);
	}

	@Override
	public void item(Request request, Response response) throws NoResultException{

		String x = "";
		switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
			case "SMA":
				x = "SMA";
				break;
			case "EMA":
				x = "EMA";
				break;
			case "LBB":
				x = "LBB";
				break;
			case "UBB":
				x = "UBB";
				break;
			case "MACD":
				x = "MACD";
				break;
			case "SL":
				x = "SL";
				break;
			case "AssetDay":
				x = "AssetDay";
				break;
			case "AssetMinute":
				x = "AssetMinute";
				break;
			default:
				break;
		}
		String queryStr = "SELECT DISTINCT x FROM " + x + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.symbol =:symbol";
		Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
		query.setParameter("type", request.getParam(GlobalConstant.TYPE));
		query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
		Object result = query.getSingleResult();

		response.addParam(GlobalConstant.ITEM, result);
	}

	@Override
	public void initializedAssetDay(Request request, Response response) throws NoResultException {
		String queryStr = "SELECT DISTINCT x FROM AssetDay" + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.symbol =:symbol";
		Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
		query.setParameter("type", request.getParam(GlobalConstant.TYPE));
		query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
		AssetDay result = (AssetDay) query.getSingleResult();
		Hibernate.initialize(result.getAssetMinutes());

		response.addParam(GlobalConstant.ITEM, result);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void getRecentAssetMinutes(Request request, Response response) {
		String x = (String) request.getParam(GlobalConstant.SYMBOL);

		if (Arrays.asList(Symbol.SYMBOLS).contains(x)) {

			String queryStr = "SELECT * FROM tradeanalyzer_main.ta_asset_minute WHERE symbol = \""
					+ x
					+ "\" ORDER BY id DESC LIMIT 200;";

			Query query = entityManager.createNativeQuery(queryStr, AssetMinute.class);
			List<AssetMinute> assetMinutes = query.getResultList();
			Collections.reverse(assetMinutes);

			response.addParam(GlobalConstant.ITEMS, assetMinutes);
		} else
			System.out.println("Symbol does not match symbols");
	}

	public void getTradeSignals(Request request, Response response) {

		Query query = null;
		String evalPeriod = null;
		List<Properties> properties = new ArrayList<Properties>();

		switch ((String) request.getParam("EVAL_PERIOD")) {
			case "DAY":
				evalPeriod = "DAY";
				break;
			case "MINUTE":
				evalPeriod = "MINUTE";
				break;
			default:
				System.out.println("Invalid request");
				return;
		}

		switch ((String) request.getParam("TRADE_SIGNAL")) {
			case "GoldenCross":
				query = entityManager.createQuery(
						"Select x.shortSMAType , x.longSMAType , x.symbol FROM GoldenCross x WHERE x.evalPeriod =:evalPeriod");
				query.setParameter("evalPeriod", evalPeriod);

				for (Object obj : query.getResultList()) {
					Object[] arr = (Object[]) obj;
					Properties p = new Properties();
					p.put("SHORT_SMA_TYPE", arr[0]);
					p.put("LONG_SMA_TYPE", arr[1]);
					p.put("SYMBOL", arr[2]);
					properties.add(p);
				}

				break;

			case "LowerBollingerBand":
				query = entityManager.createQuery(
						"Select x.LBBType , x.standardDeviations , x.symbol FROM LowerBollingerBand x WHERE x.evalPeriod =:evalPeriod");
				query.setParameter("evalPeriod", evalPeriod);

				for (Object obj : query.getResultList()) {
					Object[] arr = (Object[]) obj;
					Properties p = new Properties();
					p.put("LBB_TYPE", arr[0]);
					p.put("STANDARD_DEVIATIONS", arr[1]);
					p.put("SYMBOL", arr[2]);
					properties.add(p);
				}

				break;

			case "UpperBollingerBand":
				query = entityManager.createQuery(
						"Select x.UBBType , x.standardDeviations , x.symbol FROM UpperBollingerBand x WHERE x.evalPeriod =:evalPeriod");
				query.setParameter("evalPeriod", evalPeriod);

				for (Object obj : query.getResultList()) {
					Object[] arr = (Object[]) obj;
					Properties p = new Properties();
					p.put("UBB_TYPE", arr[0]);
					p.put("STANDARD_DEVIATIONS", arr[1]);
					p.put("SYMBOL", arr[2]);
					properties.add(p);
				}

				break;
			default:
				System.out.println("Invalid request");
				return;
		}

		response.addParam(GlobalConstant.ITEMS, properties);
	}

	public void getAlgSets(Request request, Response response) {
		String queryStr = "Select DISTINCT x FROM TechnicalIndicator x WHERE x.evaluationPeriod =:evaluationPeriod";
		Query query = entityManager.createQuery(queryStr);
		query.setParameter("evaluationPeriod", (String) request.getParam("EVALUATION_PERIOD"));

		Set<SMA> smaSet = new HashSet<SMA>();
		Set<LBB> lbbSet = new HashSet<LBB>();
		Set<UBB> ubbSet = new HashSet<UBB>();

		for (Object o : ArrayList.class.cast(query.getResultList())) {

			TechnicalIndicator x = (TechnicalIndicator) o;

			switch (x.getTechnicalIndicatorType()) {

				case TechnicalIndicator.GOLDENCROSS:
					SMA shortSMA = new SMA();
					shortSMA.setSymbol(x.getSymbol());
					shortSMA.setType(x.getShortSMAType());
					SMA longSMA = new SMA();
					longSMA.setSymbol(x.getSymbol());
					longSMA.setType(x.getLongSMAType());
					smaSet.add(shortSMA);
					smaSet.add(longSMA);
					break;

				case TechnicalIndicator.LOWERBOLLINGERBAND:
					LBB lbb = new LBB();
					lbb.setSymbol(x.getSymbol());
					lbb.setType(x.getLBBType());
					lbb.setStandardDeviations(x.getStandardDeviations());
					lbbSet.add(lbb);
					break;

				case TechnicalIndicator.UPPERBOLLINGERBAND:
					UBB ubb = new UBB();
					ubb.setSymbol(x.getSymbol());
					ubb.setType(x.getUBBType());
					ubb.setStandardDeviations(x.getStandardDeviations());
					ubbSet.add(ubb);
					break;
			}
		}

		response.addParam("SMA_SET", smaSet);
		response.addParam("LBB_SET", lbbSet);
		response.addParam("UBB_SET", ubbSet);
	}
}
