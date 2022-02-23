package org.toasthub.analysis.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.EMA;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.MACD;
import org.toasthub.analysis.model.SL;
import org.toasthub.analysis.model.SMA;
import org.toasthub.common.Functions;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;


@Service("AlgorithmCruncherSvc")
public class AlgorithmCruncherSvcImpl implements AlgorithmCruncherSvc {

	@Autowired
	protected AlpacaAPI alpacaAPI;
	
	@Autowired
	protected AlgorithmCruncherDao tradeBlasterDao;
	
	@Autowired

	final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);
	
	// Constructors
	public AlgorithmCruncherSvcImpl() {
	}
	
		
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	@Override
	public void process(Request request, Response response) {
		String action = (String) request.getParams().get("action");
		
		switch (action) {
		case "ITEM":
			item(request, response);
			break;
		case "LIST":
			items(request, response);
			break;
		case "SAVE":
			save(request, response);
			break;
		case "DELETE":
			delete(request, response);
			break;
		default:
			break;
		}
		
	}




	@Override
	public void delete(Request request, Response response) {
		try {
			tradeBlasterDao.delete(request, response);
			tradeBlasterDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				tradeBlasterDao.items(request, response);
			}
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}



	@Override
	public void item(Request request, Response response) {
		try {
			tradeBlasterDao.item(request, response);
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}


	@Override
	public void items(Request request, Response response) {
		try {
			tradeBlasterDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				tradeBlasterDao.items(request, response);
			}
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}
	
	@Scheduled(cron="0 * * * * ?")
	public void tradeAnalysisTask() {
		
		// if (tradeAnalysisJobRunning.get()) {
		// 	System.out.println("Trade analysis is currently running skipping this time");
		// 	return;

		// } else {
		// 	new Thread(()->{
		// 		tradeAnalysisJobRunning.set(true);
		// 		algorithumCruncherSvc.load();
		// 		tradeAnalysisJobRunning.set(false);
		// 	}).start();
		// }
	}


	@Override
	public void save(Request request, Response response) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void backload(Request request, Response response) {
		String stockName = "SPY";
		List<StockBar> stockBars = Functions.backloadBars(alpacaAPI, stockName);
		EMA ema13;
		EMA ema26;
		SMA sma;
		MACD macd;
		LBB lbb;
		SL sl;
		Map<String, List<?>> map = new HashMap<String, List<?>>();
		List<SMA> smaList = new ArrayList<SMA>();
		List<MACD> macdList = new ArrayList<MACD>();
		List<LBB> lbbList = new ArrayList<LBB>();
		List<SL> slList = new ArrayList<SL>();
		List<EMA> emaList = new ArrayList<EMA>();
		if (stockBars != null)
			for (int i = 50; i < stockBars.size(); i++) {

				ema13 = new EMA(stockName);
				ema26 = new EMA(stockName);
				sma = new SMA(stockName);
				macd = new MACD(stockName);
				lbb = new LBB(stockName);
				sl = new SL(stockName);

				sma.initializer(stockBars.subList(i-50, i + 1), 50);
				if (!tradeBlasterDao.queryChecker(
						"SMA", sma.getEpochSeconds(), sma.getType(), sma.getStock())) 
						{
					sma.setValue(SMA.calculateSMA(sma.getStockBars()));
					smaList.add(sma);
				}

				ema26.initializer(stockBars.subList(i-26, i + 1), 26);
				if (!tradeBlasterDao.queryChecker(
						"EMA", ema26.getEpochSeconds(), ema26.getType(), ema26.getStock())) 
						{
					ema26.setValue(tradeBlasterDao.queryEMAValue(ema26));
					emaList.add(ema26);
				}

				ema13.initializer(stockBars.subList(i-13, i + 1), 13);
				if (!tradeBlasterDao.queryChecker(
						"EMA", ema13.getEpochSeconds(), ema13.getType(), ema13.getStock())) 
						{
					ema13.setValue(tradeBlasterDao.queryEMAValue(ema13));
					emaList.add(ema13);
				}

				macd.initializer(stockBars.subList(i-50, i + 1));
				if (!tradeBlasterDao.queryChecker(
						"MACD", macd.getEpochSeconds(), macd.getType(), macd.getStock())) 
						{
					macd.setValue(tradeBlasterDao.queryMACDValue(macd));
					macdList.add(macd);
				}

				lbb.initializer(stockBars.subList(i-50, i + 1), 50);
				if (!tradeBlasterDao.queryChecker(
						"LBB", lbb.getEpochSeconds(), lbb.getType(), lbb.getStock())) 
						{
					lbb.setValue(tradeBlasterDao.queryLBBValue(lbb));
					lbbList.add(lbb);
				}

				sl.initializer(stockBars.subList(i-50, i + 1));
				if (!tradeBlasterDao.queryChecker(
						"SL", sl.getEpochSeconds(), sl.getType(), sl.getStock())) 
						{
					sl.setValue(tradeBlasterDao.querySLValue(sl));
					slList.add(sl);
				}
			}
		map.put("EMA", emaList);
		map.put("SMA", smaList);
		map.put("MACD", macdList);
		map.put("SL", slList);
		map.put("LBB", lbbList);
		tradeBlasterDao.saveAll(map);
	}

	@Override
	public void load() {
		String stockName = "SPY";
		List<StockBar> stockBars = Functions.currentStockBars(alpacaAPI, stockName, 200);
		EMA ema13;
		EMA ema26;
		SMA sma;
		MACD macd;
		LBB lbb;
		SL sl;
		Map<String, List<?>> map = new HashMap<String, List<?>>();
		List<SMA> smaList = new ArrayList<SMA>();
		List<MACD> macdList = new ArrayList<MACD>();
		List<LBB> lbbList = new ArrayList<LBB>();
		List<SL> slList = new ArrayList<SL>();
		List<EMA> emaList = new ArrayList<EMA>();
		if (stockBars != null)
			for (int i = 51; i < stockBars.size(); i++) {

				ema13 = new EMA(stockName);
				ema26 = new EMA(stockName);
				sma = new SMA(stockName);
				macd = new MACD(stockName);
				lbb = new LBB(stockName);
				sl = new SL(stockName);

				sma.initializer(stockBars.subList(0, i + 1), 50);
				if (!tradeBlasterDao.queryChecker(
						"SMA", sma.getEpochSeconds(), sma.getType(), sma.getStock())) {
					sma.setValue(SMA.calculateSMA(sma.getStockBars()));
					smaList.add(sma);
				}

				ema26.initializer(stockBars.subList(0, i + 1), 26);
				if (!tradeBlasterDao.queryChecker(
						"EMA", ema26.getEpochSeconds(), ema26.getType(), ema26.getStock())) {
					ema26.setValue(tradeBlasterDao.queryEMAValue(ema26));
					emaList.add(ema26);
				}

				ema13.initializer(stockBars.subList(0, i + 1), 13);
				if (!tradeBlasterDao.queryChecker(
						"EMA", ema13.getEpochSeconds(), ema13.getType(), ema13.getStock())) {
					ema13.setValue(tradeBlasterDao.queryEMAValue(ema13));
					emaList.add(ema13);
				}

				macd.initializer(stockBars.subList(0, i + 1));
				if (!tradeBlasterDao.queryChecker(
						"MACD", macd.getEpochSeconds(), macd.getType(), macd.getStock())) {
					macd.setValue(tradeBlasterDao.queryMACDValue(macd));
					macdList.add(macd);
				}

				lbb.initializer(stockBars.subList(0, i + 1), 50);
				if (!tradeBlasterDao.queryChecker(
						"LBB", lbb.getEpochSeconds(), lbb.getType(), lbb.getStock())) {
					lbb.setValue(tradeBlasterDao.queryLBBValue(lbb));
					lbbList.add(lbb);
				}

				sl.initializer(stockBars.subList(0, i + 1));
				if (!tradeBlasterDao.queryChecker(
						"SL", sl.getEpochSeconds(), sl.getType(), sl.getStock())) {
					sl.setValue(tradeBlasterDao.querySLValue(sl));
					slList.add(sl);
				}
			}
		map.put("EMA", emaList);
		map.put("SMA", smaList);
		map.put("MACD", macdList);
		map.put("SL", slList);
		map.put("LBB", lbbList);
		tradeBlasterDao.saveAll(map);
	}

	
}
