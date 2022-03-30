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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
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
		Query query = entityManager.createNativeQuery(
				"SELECT * FROM tradeanalyzer_main.ta_asset_day ORDER BY id DESC LIMIT 0, 1;", AssetDay.class);
		Object result = query.getSingleResult();
		response.addParam(GlobalConstant.ITEM, result);
	}

	@Override
	public void getRecentAssetMinute(Request request, Response response) {
		Query query = entityManager.createNativeQuery(
				"SELECT * FROM tradeanalyzer_main.ta_asset_minute ORDER BY id DESC LIMIT 0, 1;", AssetMinute.class);
		Object result = query.getSingleResult();
		response.addParam(GlobalConstant.ITEM, result);
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

		String queryStr = "SELECT COUNT(DISTINCT x) FROM " + x + " as x ";

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

		Long count = (Long) query.getSingleResult();
		if (count == null) {
			count = 0l;
		}
		response.addParam(GlobalConstant.ITEMCOUNT, count);
	}

	@Override
	public void item(Request request, Response response) throws Exception {

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
	public void initializedAssetDay(Request request, Response response) throws Exception {
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
}
