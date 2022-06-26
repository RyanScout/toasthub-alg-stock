package org.toasthub.analysis.algorithm;

import org.toasthub.common.BaseSvc;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface AlgorithmCruncherSvc extends BaseSvc {
	public void backloadStockData(Request request, Response response);
	public void backloadCryptoData(Request request, Response response);
	public void loadStockData(Request request, Response response);
	public void loadCryptoData(Request request, Response response);
}
