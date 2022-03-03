package org.toasthub.analysis.algorithm;

import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface AlgorithmCruncherSvc {
	public void process(Request request, Response response);
	public void save(Request request, Response response);
	public void delete(Request request, Response response);
	public void item(Request request, Response response);
	public void items(Request request, Response response);
	public void backloadAlgs(Request request, Response response);
	public void backloadStockData(Request request, Response response);
	public void loadStockData(Request request, Response response);
	public void loadAlgs(Request request, Response response);
}
