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

package org.toasthub.common;

import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface BaseDao {
	public void delete(Request request, Response response) throws Exception;
	public void save(Request request, Response response) throws Exception;
	public void items(Request request, Response response) throws Exception;
	public void itemCount(Request request, Response response) throws Exception;
	public void item(Request request, Response response) throws Exception;
}
