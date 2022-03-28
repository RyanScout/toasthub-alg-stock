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
import org.toasthub.analysis.model.StockDay;
import org.toasthub.analysis.model.StockMinute;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;


@Repository("AlgorithmCruncherDao")
@Transactional()
public class AlgorithmCruncherDaoImpl implements AlgorithmCruncherDao {

	@Autowired
	protected EntityManager entityManager;

	@Override
	public void getRecentStockDay(Request request, Response response){
		Query query = entityManager.createNativeQuery("SELECT * FROM stockanalyzer_main.sa_stock_day ORDER BY id DESC LIMIT 0, 1;" , StockDay.class);
		Object result = query.getSingleResult();
		response.addParam(GlobalConstant.ITEM , result);
	}

	@Override
	public void getRecentStockMinute(Request request, Response response){
		Query query = entityManager.createNativeQuery("SELECT * FROM stockanalyzer_main.sa_stock_minute ORDER BY id DESC LIMIT 0, 1;" , StockMinute.class);
		Object result = query.getSingleResult();
		response.addParam(GlobalConstant.ITEM , result);
	}


	@Override
	public void delete(Request request, Response response) throws Exception {
	}
	@Override
	public void save(Request request, Response response) throws Exception {
		entityManager.merge( (Object) request.getParam(GlobalConstant.ITEM) );
	}

	@Override
	public void saveAll(List<StockDay> stockDays) {
		for (StockDay tempStockDay : stockDays) {
			entityManager.merge(tempStockDay);
		}
	}

	@Override
	public void items(Request request, Response response) throws Exception {
		String queryStr = "SELECT DISTINCT x FROM "
		+request.getParam(GlobalConstant.IDENTIFIER)
		+" AS x ";

		Query query = entityManager.createQuery(queryStr);
		List<?> items = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, items);
	}

	@Override
	public void itemCount(Request request, Response response) throws Exception {
		String queryStr = "SELECT COUNT(DISTINCT x) FROM "
		+request.getParam(GlobalConstant.IDENTIFIER)
		+" as x ";

		boolean and = false;
		if (request.containsParam(GlobalConstant.EPOCHSECONDS)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.epochSeconds =:epochSeconds ";
			and = true;
		}
		if (request.containsParam(GlobalConstant.STOCK)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.stock =:stock ";
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
			query.setParameter("epochSeconds", (long)request.getParam(GlobalConstant.EPOCHSECONDS));
		}
		if (request.containsParam(GlobalConstant.TYPE)) {
			query.setParameter("type", (String) request.getParam(GlobalConstant.TYPE));
		}
		if (request.containsParam(GlobalConstant.STOCK)) {
			query.setParameter("stock", (String) request.getParam(GlobalConstant.STOCK));
		}

		Long count = (Long) query.getSingleResult();
		if (count == null) {
			count = 0l;
		}
		response.addParam(GlobalConstant.ITEMCOUNT, count);
	}

	@Override
	public void item(Request request, Response response) throws Exception {
		String queryStr = "SELECT DISTINCT x FROM " + request.getParam(GlobalConstant.IDENTIFIER) + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.stock =:stock";
		Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
		query.setParameter("type", request.getParam(GlobalConstant.TYPE));
		query.setParameter("stock", request.getParam(GlobalConstant.STOCK));
		Object result = query.getSingleResult();

		response.addParam(GlobalConstant.ITEM , result);
	}

	@Override
	public void initializedStockDay(Request request, Response response) throws Exception {
		String queryStr = "SELECT DISTINCT x FROM " + request.getParam(GlobalConstant.IDENTIFIER) + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.stock =:stock";
		Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
		query.setParameter("type", request.getParam(GlobalConstant.TYPE));
		query.setParameter("stock", request.getParam(GlobalConstant.STOCK));
		StockDay result = (StockDay)query.getSingleResult();
		Hibernate.initialize(result.getStockMinutes());

		response.addParam(GlobalConstant.ITEM , result);
	}
}
