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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

import java.time.Instant;

@Repository("AlgorithmCruncherDao")
@Transactional()
public class AlgorithmCruncherDaoImpl implements AlgorithmCruncherDao {

	@Autowired
	protected EntityManager entityManager;

	@Override
	public void getRecentAssetDay(final Request request, final Response response) {
		final String x = (String) request.getParam(GlobalConstant.SYMBOL);

		if (Arrays.asList(Symbol.SYMBOLS).contains(x)) {
			final String queryStr = "SELECT * FROM tradeanalyzer_main.ta_asset_day WHERE symbol = \""
					+ x
					+ "\" ORDER BY id DESC LIMIT 0, 1;";

			final Query query = entityManager.createNativeQuery(queryStr, AssetDay.class);
			final Object result = query.getSingleResult();

			response.addParam(GlobalConstant.ITEM, result);
		} else
			System.out.println("Symbol does not match symbols");
	}

	@Override
	public void getRecentAssetMinute(final Request request, final Response response) {
		final String x = (String) request.getParam(GlobalConstant.SYMBOL);

		if (Arrays.asList(Symbol.SYMBOLS).contains(x)) {

			final String queryStr = "SELECT * FROM tradeanalyzer_main.ta_asset_minute WHERE symbol = \""
					+ x
					+ "\" ORDER BY id DESC LIMIT 0, 1;";

			final Query query = entityManager.createNativeQuery(queryStr, AssetMinute.class);
			final Object result = query.getSingleResult();

			response.addParam(GlobalConstant.ITEM, result);
		} else
			System.out.println("Symbol does not contain symbols");
	}

	@Override
	public void delete(final Request request, final Response response) throws Exception {
	}

	@Override
	public void save(final Request request, final Response response) throws Exception {
		entityManager.merge((Object) request.getParam(GlobalConstant.ITEM));
	}

	@Override
	public void saveAll(final Request request, final Response response) {
		for (final Object o : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			entityManager.merge(o);
		}
	}

	@Override
	public void items(final Request request, final Response response) throws Exception {

		String x = "";
		switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
			case "AssetDay":
				x = "AssetDay";
				break;
			case "AssetMinute":
				x = "AssetMinute";
				break;
			case "TECHNICAL_INDICATOR":
				getAlgSets(request, response);
				return;
			default:
				System.out.println("Invalid request");
				return;
		}

		if (!Arrays.asList(Symbol.SYMBOLS).contains((String) request.getParam(GlobalConstant.SYMBOL))) {
			System.out.println("request param symbol does not contain valid symbol at algorithm cruncher dao items");
			return;
		}

		final String queryStr = "SELECT DISTINCT x FROM " + x
				+ " AS x WHERE x.symbol =:symbol AND x.epochSeconds>:startingEpochSeconds AND x.epochSeconds <:endingEpochSeconds";

		final Query query = entityManager.createQuery(queryStr);
		query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
		query.setParameter("startingEpochSeconds", request.getParam("STARTING_EPOCH_SECONDS"));
		query.setParameter("endingEpochSeconds", request.getParam("ENDING_EPOCH_SECONDS"));
		final List<?> items = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, items);
	}

	@Override
	public void itemCount(final Request request, final Response response) throws Exception {

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
			case "CONFIGURATION":
				x = "Configuration";
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

		final Query query = entityManager.createQuery(queryStr);

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
	public void item(final Request request, final Response response) throws NoResultException {

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
			case "CONFIGURATION":
				getConfiguration(request, response);
				return;
			default:
				break;
		}

		if (!Arrays.asList(Symbol.SYMBOLS).contains((String) request.getParam(GlobalConstant.SYMBOL))) {
			System.out.println("request param symbol does not contain valid symbol at algorithm cruncher dao item");
			return;
		}

		final String queryStr = "SELECT DISTINCT x FROM " + x + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.symbol =:symbol";
		final Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
		query.setParameter("type", request.getParam(GlobalConstant.TYPE));
		query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
		final Object result = query.getSingleResult();

		response.addParam(GlobalConstant.ITEM, result);
	}

	@Override
	public void initializedAssetDay(final Request request, final Response response) throws NoResultException {
		final String queryStr = "SELECT DISTINCT x FROM AssetDay" + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.symbol =:symbol";
		final Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
		query.setParameter("type", request.getParam(GlobalConstant.TYPE));
		query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
		final AssetDay result = (AssetDay) query.getSingleResult();
		Hibernate.initialize(result.getAssetMinutes());

		response.addParam(GlobalConstant.ITEM, result);
	}

	@Override
	public void getRecentAssetMinutes(final Request request, final Response response) {
		final String x = (String) request.getParam(GlobalConstant.SYMBOL);

		if (!Arrays.asList(Symbol.SYMBOLS).contains(x)) {
			System.out.println("Symbol does not match symbols");
		}

		final String queryStr = "SELECT * FROM tradeanalyzer_main.ta_asset_minute WHERE symbol = \""
				+ x
				+ "\" ORDER BY epoch_seconds DESC LIMIT 200;";

		final Query query = entityManager.createNativeQuery(queryStr, AssetMinute.class);
		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
		for (final Object o : (ArrayList.class.cast(query.getResultList()))) {
			assetMinutes.add(AssetMinute.class.cast(o));
		}
		Collections.reverse(assetMinutes);

		response.addParam(GlobalConstant.ITEMS, assetMinutes);
	}

	public void getAlgSets(final Request request, final Response response) {
		final String queryStr = "Select DISTINCT x FROM TechnicalIndicator x";
		final Query query = entityManager.createQuery(queryStr);

		final Set<SMA> smaSet = new HashSet<SMA>();
		final Set<LBB> lbbSet = new HashSet<LBB>();
		final Set<UBB> ubbSet = new HashSet<UBB>();

		for (final Object o : ArrayList.class.cast(query.getResultList())) {

			final TechnicalIndicator x = (TechnicalIndicator) o;

			switch (x.getTechnicalIndicatorType()) {

				case TechnicalIndicator.GOLDENCROSS:
					final SMA shortSMA = new SMA();
					shortSMA.setSymbol(x.getSymbol());
					shortSMA.setType(x.getShortSMAType());

					final SMA longSMA = new SMA();
					longSMA.setSymbol(x.getSymbol());
					longSMA.setType(x.getLongSMAType());

					smaSet.add(shortSMA);
					smaSet.add(longSMA);
					break;

				case TechnicalIndicator.LOWERBOLLINGERBAND:
					final LBB lbb = new LBB();
					lbb.setSymbol(x.getSymbol());
					lbb.setType(x.getLBBType());
					lbb.setStandardDeviations(x.getStandardDeviations());
					lbbSet.add(lbb);
					break;

				case TechnicalIndicator.UPPERBOLLINGERBAND:
					final UBB ubb = new UBB();
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

	public void getConfiguration(final Request request, final Response response) {
		final String queryStr = "SELECT DISTINCT x FROM Configuration AS x";
		final Query query = entityManager.createQuery(queryStr);
		response.addParam(GlobalConstant.ITEM, query.getSingleResult());
	}
}
