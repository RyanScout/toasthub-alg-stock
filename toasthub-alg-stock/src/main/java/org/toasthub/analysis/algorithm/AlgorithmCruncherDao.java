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

import org.toasthub.analysis.model.StockDay;
import org.toasthub.common.BaseDao;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface AlgorithmCruncherDao extends BaseDao {
	public void saveAll(List<StockDay> stockDays);
	public void initializedStockDay(Request request, Response response) throws Exception;
	public void getRecentStockDay(Request request, Response response);
	public void getRecentStockMinute(Request request, Response response);
	
}