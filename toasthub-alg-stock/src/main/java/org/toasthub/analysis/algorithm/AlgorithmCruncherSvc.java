package org.toasthub.analysis.algorithm;

import org.toasthub.common.BaseSvc;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface AlgorithmCruncherSvc extends BaseSvc {
	public void backloadAlgDays(Request request, Response response);
	public void backloadAlgMinutes(Request request, Response response);
	public void backloadStockData(Request request, Response response);
	public void backloadCryptoData(Request request, Response response);
	public void loadStockData(Request request, Response response);
	public void loadCryptoData(Request request, Response response);
	public void loadAlgDays(Request request, Response response);
	public void loadAlgMinutes(Request request, Response response);
	public void recentStats(Request request, Response response);
}
