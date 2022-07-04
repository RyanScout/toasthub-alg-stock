package org.toasthub.analysis.algorithm;

import org.toasthub.common.BaseSvc;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface AlgorithmCruncherSvc extends BaseSvc {
    public void loadStockData(final Request request, final Response response);
    public void loadCryptoData(final Request request, final Response response);
    public void loadAlgorithmData(final Request request, final Response response);
    public void initializeDatabase();
}
