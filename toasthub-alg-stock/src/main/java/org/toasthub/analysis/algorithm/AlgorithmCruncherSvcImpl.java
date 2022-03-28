package org.toasthub.analysis.algorithm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.EMA;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.MACD;
import org.toasthub.analysis.model.SL;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.StockDay;
import org.toasthub.analysis.model.StockMinute;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarFeed;

@Service("AlgorithmCruncherSvc")
public class AlgorithmCruncherSvcImpl implements AlgorithmCruncherSvc {

	@Autowired
	protected AlpacaAPI alpacaAPI;

	@Autowired
	protected AlgorithmCruncherDao algorithmCruncherDao;

	final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);

	public static final int START_OF_2021 = 1609477200;

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
				recentStats(request, response);
				break;
			case "SAVE":
				save(request, response);
				break;
			case "DELETE":
				delete(request, response);
				break;
			case "BACKLOAD":
				System.out.println("Starting!");
				backloadStockData(request, response);
				System.out.println("StockData Loaded");
				backloadAlgs(request, response);
				System.out.println("Algorithm Data Loaded");
				break;
			case "LOAD":
				loadStockData(request, response);
				loadAlgs(request, response);
				break;
			default:
				break;
		}

	}

	@Override
	public void recentStats(Request request, Response response) {
		algorithmCruncherDao.getRecentStockDay(request, response);
		response.addParam("STOCKDAY", response.getParam(GlobalConstant.ITEM));
		algorithmCruncherDao.getRecentStockMinute(request, response);
		response.addParam("STOCKMINUTE", response.getParam(GlobalConstant.ITEM));
	}

	@Override
	public void delete(Request request, Response response) {
		try {
			algorithmCruncherDao.delete(request, response);
			algorithmCruncherDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				algorithmCruncherDao.items(request, response);
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
			algorithmCruncherDao.item(request, response);
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void items(Request request, Response response) {
		try {
			algorithmCruncherDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				algorithmCruncherDao.items(request, response);
			}
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Scheduled(cron = "0 * * * * ?")
	public void dataBaseUpdater() {

		Request request = new Request();
		request.addParam("action", "LOAD");
		Response response = new Response();

		if (tradeAnalysisJobRunning.get()) {
			System.out.println("Trade analysis is currently running skipping this time");
			return;

		} else {
			new Thread(() -> {
				tradeAnalysisJobRunning.set(true);
				this.process(request, response);
				tradeAnalysisJobRunning.set(false);
			}).start();
		}
	}

	@Override
	public void save(Request request, Response response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void backloadStockData(Request request, Response response) {
		try {
			String stockName = "SPY";
			List<StockBar> stockBars;
			ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS);
			ZonedDateTime first = ZonedDateTime
					.ofInstant(Instant.ofEpochSecond(START_OF_2021), ZoneId.of("America/New_York"))
					.truncatedTo(ChronoUnit.DAYS);
			ZonedDateTime second = first.plusDays(1).minusMinutes(1);
			StockDay stockDay;
			StockMinute stockMinute;
			Set<StockMinute> stockMinutes;
			List<StockDay> stockDays = new ArrayList<StockDay>();
			List<StockBar> stockBar;

			while (second.isBefore(now)) {
				try {
					stockBars = alpacaAPI.stockMarketData().getBars(stockName,
							first,
							second,
							null,
							null,
							1,
							BarTimePeriod.MINUTE,
							BarAdjustment.SPLIT,
							BarFeed.SIP).getBars();
				} catch (Exception e) {
					System.out.println("Caught!");
					Thread.sleep(1200 * 60);
					System.out.println("Resuming!");
					stockBars = alpacaAPI.stockMarketData().getBars(stockName,
							first,
							second,
							null,
							null,
							1,
							BarTimePeriod.MINUTE,
							BarAdjustment.SPLIT,
							BarFeed.SIP).getBars();
				}

				try {
					stockBar = alpacaAPI.stockMarketData().getBars(stockName,
							first,
							second,
							null,
							null,
							1,
							BarTimePeriod.DAY,
							BarAdjustment.SPLIT,
							BarFeed.SIP).getBars();
				} catch (Exception e) {
					System.out.println("Caught!");
					Thread.sleep(1200 * 60);
					System.out.println("Resuming!");
					stockBar = alpacaAPI.stockMarketData().getBars(stockName,
							first,
							second,
							null,
							null,
							1,
							BarTimePeriod.DAY,
							BarAdjustment.SPLIT,
							BarFeed.SIP).getBars();
				}
				if (stockBar != null && stockBars != null) {

					stockDay = new StockDay();
					stockDay.setStock(stockName);
					stockDay.setHigh(new BigDecimal(stockBar.get(0).getHigh()));
					stockDay.setEpochSeconds(first.toEpochSecond());
					stockDay.setClose(new BigDecimal(stockBar.get(0).getClose()));
					stockDay.setLow(new BigDecimal(stockBar.get(0).getLow()));
					stockDay.setOpen(new BigDecimal(stockBar.get(0).getOpen()));
					stockDay.setVolume(stockBar.get(0).getVolume());
					stockDay.setVwap(new BigDecimal(stockBar.get(0).getVwap()));
					stockMinutes = new LinkedHashSet<StockMinute>();

					for (int i = 0; i < stockBars.size(); i++) {
						stockMinute = new StockMinute();
						stockMinute.setStockDay(stockDay);
						stockMinute.setStock(stockName);
						stockMinute.setEpochSeconds(stockBars.get(i).getTimestamp().toEpochSecond());
						stockMinute.setValue(new BigDecimal(stockBars.get(i).getClose()));
						stockMinute.setVolume(stockBars.get(i).getVolume());
						stockMinute.setVwap(new BigDecimal(stockBars.get(i).getVwap()));
						stockMinutes.add(stockMinute);
					}

					stockDay.setStockMinutes(stockMinutes);
					stockDays.add(stockDay);
				}

				first = first.plusDays(1);
				second = second.plusDays(1);
			}

			algorithmCruncherDao.saveAll(stockDays);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void backloadAlgs(Request request, Response response) {
		try {
			String stockName = "SPY";

			EMA ema12;
			EMA ema26;
			SMA sma;
			MACD macd;
			LBB lbb;
			SL sl;

			request.addParam(GlobalConstant.IDENTIFIER, "StockDay");
			algorithmCruncherDao.items(request, response);

			List<StockDay> stockDays = (List<StockDay>) response.getParam(GlobalConstant.ITEMS);
			List<BigDecimal> stockDaysValues = new ArrayList<BigDecimal>();
			for (StockDay stockDay : stockDays)
				stockDaysValues.add(stockDay.getClose());

			for (int i = 0; i < stockDays.size(); i++) {

				ema12 = new EMA(stockName);
				ema26 = new EMA(stockName);
				sma = new SMA(stockName);
				macd = new MACD(stockName);
				lbb = new LBB(stockName);
				sl = new SL(stockName);

				if (i >= 49) {
					sma.setEpochSeconds(stockDays.get(i).getEpochSeconds());
					sma.setStock(stockName);
					sma.setType("50-day");
					sma.setValue(SMA.calculateSMA(stockDaysValues.subList(i - 49, i + 1)));
					request.addParam(GlobalConstant.ITEM, sma);
					algorithmCruncherDao.save(request, response);
				}

				if (i >= 19) {
					sma.setEpochSeconds(stockDays.get(i).getEpochSeconds());
					sma.setStock(stockName);
					sma.setType("20-day");
					sma.setValue(SMA.calculateSMA(stockDaysValues.subList(i - 19, i + 1)));
					request.addParam(GlobalConstant.ITEM, sma);
					algorithmCruncherDao.save(request, response);
				}

				if (i >= 14) {
					sma.setEpochSeconds(stockDays.get(i).getEpochSeconds());
					sma.setStock(stockName);
					sma.setType("15-day");
					sma.setValue(SMA.calculateSMA(stockDaysValues.subList(i - 14, i + 1)));
					request.addParam(GlobalConstant.ITEM, sma);
					algorithmCruncherDao.save(request, response);
				}

				if (i >= 25) {
					ema26.setEpochSeconds(stockDays.get(i).getEpochSeconds());
					ema26.setStock(stockName);
					ema26.setType("26-day");

					request.addParam(GlobalConstant.IDENTIFIER, "EMA");
					request.addParam(GlobalConstant.TYPE, "26-day");
					request.addParam(GlobalConstant.STOCK, stockName);
					request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i - 1).getEpochSeconds());

					try {
						algorithmCruncherDao.item(request, response);
						ema26.setValue(EMA.calculateEMA(stockDaysValues.subList(i - 25, i + 1),
								((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							ema26.setValue(EMA.calculateEMA(stockDaysValues.subList(i - 25, i + 1)));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, ema26);
					algorithmCruncherDao.save(request, response);
				}

				if (i >= 11) {
					ema12.setEpochSeconds(stockDays.get(i).getEpochSeconds());
					ema12.setStock(stockName);
					ema12.setType("12-day");

					request.addParam(GlobalConstant.IDENTIFIER, "EMA");
					request.addParam(GlobalConstant.TYPE, "12-day");
					request.addParam(GlobalConstant.STOCK, stockName);
					request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i - 1).getEpochSeconds());

					try {
						algorithmCruncherDao.item(request, response);
						ema12.setValue(EMA.calculateEMA(stockDaysValues.subList(i - 11, i + 1),
								((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							ema12.setValue(EMA.calculateEMA(stockDaysValues.subList(i - 11, i + 1)));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, ema12);
					algorithmCruncherDao.save(request, response);
				}

				if (i >= 25) {
					macd.setEpochSeconds(stockDays.get(i).getEpochSeconds());
					macd.setStock(stockName);
					macd.setType("Day");

					request.addParam(GlobalConstant.IDENTIFIER, "EMA");
					request.addParam(GlobalConstant.STOCK, stockName);
					request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i).getEpochSeconds());
					try {
						request.addParam(GlobalConstant.TYPE, "26-day");
						algorithmCruncherDao.item(request, response);
						EMA longEMA = (EMA) response.getParam(GlobalConstant.ITEM);
						request.addParam(GlobalConstant.TYPE, "12-day");
						algorithmCruncherDao.item(request, response);
						EMA shortEMA = (EMA) response.getParam(GlobalConstant.ITEM);
						macd.setValue(shortEMA.getValue().subtract(longEMA.getValue()));
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							macd.setValue(MACD.calculateMACD(stockDaysValues.subList(i - 25, i + 1)));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, macd);
					algorithmCruncherDao.save(request, response);
				}

				if (i >= 19) {
					lbb.setEpochSeconds(stockDays.get(i).getEpochSeconds());
					lbb.setStock(stockName);
					lbb.setType("20-day");

					request.addParam(GlobalConstant.IDENTIFIER, "SMA");
					request.addParam(GlobalConstant.STOCK, stockName);
					request.addParam(GlobalConstant.TYPE, "20-day");
					request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i).getEpochSeconds());

					try {
						algorithmCruncherDao.item(request, response);
						lbb.setValue(LBB.calculateLBB(stockDaysValues.subList(i - 19, i + 1),
								((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							lbb.setValue(LBB.calculateLBB(stockDaysValues.subList(i - 19, i + 1)));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, lbb);
					algorithmCruncherDao.save(request, response);
				}

				if (i >= 32) {
					sl.setEpochSeconds(stockDays.get(i).getEpochSeconds());
					sl.setStock(stockName);
					sl.setType("Day");

					request.addParam(GlobalConstant.IDENTIFIER, "MACD");
					request.addParam(GlobalConstant.STOCK, stockName);
					request.addParam(GlobalConstant.TYPE, "DAY");

					try {
						BigDecimal[] macdArr = new BigDecimal[9];

						for (int f = 0; f < 9; f++) {
							request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i - f).getEpochSeconds());
							algorithmCruncherDao.item(request, response);
							macdArr[f] = (((MACD) response.getParam(GlobalConstant.ITEM)).getValue());
						}

						sl.setValue(SL.calculateSL(macdArr));

					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							sl.setValue(SL.calculateSL(stockDaysValues.subList(i - 32, i + 1)));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, sl);
					algorithmCruncherDao.save(request, response);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadStockData(Request request, Response response) {
		try {
			String stockName = "SPY";
			List<StockBar> stockBars;
			ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York")).minusSeconds(60 * 20);
			StockDay stockDay;
			StockMinute stockMinute;
			Set<StockMinute> stockMinutes;
			List<StockBar> stockBar;
			Set<StockMinute> preExistingStockMinutes = new LinkedHashSet<StockMinute>();
			boolean preExisting = false;

			stockBars = alpacaAPI.stockMarketData().getBars(stockName,
					today.truncatedTo(ChronoUnit.DAYS),
					today,
					null,
					null,
					1,
					BarTimePeriod.MINUTE,
					BarAdjustment.SPLIT,
					BarFeed.SIP).getBars();

			stockBar = alpacaAPI.stockMarketData().getBars(stockName,
					today.truncatedTo(ChronoUnit.DAYS),
					today,
					null,
					null,
					1,
					BarTimePeriod.DAY,
					BarAdjustment.SPLIT,
					BarFeed.SIP).getBars();

			if (stockBar != null) {

				stockDay = new StockDay();
				stockMinutes = new LinkedHashSet<StockMinute>();

				request.addParam(GlobalConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(GlobalConstant.STOCK, stockName);
				request.addParam(GlobalConstant.TYPE, "StockDay");
				request.addParam(GlobalConstant.IDENTIFIER, "StockDay");

				try {
					algorithmCruncherDao.initializedStockDay(request, response);
					stockDay = (StockDay) response.getParam(GlobalConstant.ITEM);
					preExistingStockMinutes = stockDay.getStockMinutes();
					preExisting = true;
				} catch (Exception e) {
					if (e.getMessage().equals("No entity found for query")) {
						stockDay.setStock(stockName);
						stockDay.setHigh(new BigDecimal(stockBar.get(0).getHigh()));
						stockDay.setEpochSeconds(today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
						stockDay.setClose(new BigDecimal(stockBar.get(0).getClose()));
						stockDay.setLow(new BigDecimal(stockBar.get(0).getLow()));
						stockDay.setOpen(new BigDecimal(stockBar.get(0).getOpen()));
						stockDay.setVolume(stockBar.get(0).getVolume());
						stockDay.setVwap(new BigDecimal(stockBar.get(0).getVwap()));
						preExisting = false;
					} else
						System.out.println(e.getMessage());
				}
				for (int i = preExistingStockMinutes.size(); i < stockBars.size(); i++) {
					stockMinute = new StockMinute();
					stockMinute.setStockDay(stockDay);
					stockMinute.setStock(stockName);
					stockMinute.setEpochSeconds(stockBars.get(i).getTimestamp().toEpochSecond());
					stockMinute.setValue(new BigDecimal(stockBars.get(i).getClose()));
					stockMinute.setVolume(stockBars.get(i).getVolume());
					stockMinute.setVwap(new BigDecimal(stockBars.get(i).getVwap()));
					stockMinutes.add(stockMinute);
				}

				if (preExisting) {
					for (StockMinute tempStockMinute : stockMinutes) {
						request.addParam(GlobalConstant.ITEM, tempStockMinute);
						algorithmCruncherDao.save(request, response);
					}

				}
				if (!preExisting) {
					stockDay.setStockMinutes(stockMinutes);
					request.addParam(GlobalConstant.ITEM, stockDay);
					algorithmCruncherDao.save(request, response);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void loadAlgs(Request request, Response response) {
		try {
			String stockName = "SPY";

			EMA ema13;
			EMA ema26;
			SMA sma;
			MACD macd;
			LBB lbb;
			SL sl;

			request.addParam(GlobalConstant.IDENTIFIER, "StockDay");
			algorithmCruncherDao.items(request, response);

			List<StockDay> stockDays = (List<StockDay>) response.getParam(GlobalConstant.ITEMS);
			List<BigDecimal> stockDaysValues = new ArrayList<BigDecimal>();

			int i = stockDays.size() - 1;
			for (StockDay stockDay : stockDays)
				stockDaysValues.add(stockDay.getClose());

			ema13 = new EMA(stockName);
			ema26 = new EMA(stockName);
			sma = new SMA(stockName);
			macd = new MACD(stockName);
			lbb = new LBB(stockName);
			sl = new SL(stockName);

			request.addParam(GlobalConstant.IDENTIFIER, "SMA");
			request.addParam(GlobalConstant.TYPE, "50-day");
			request.addParam(GlobalConstant.STOCK, stockName);
			request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(stockDays.size() - 1).getEpochSeconds());
			algorithmCruncherDao.itemCount(request, response);

			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
				sma.setEpochSeconds(stockDays.get(i).getEpochSeconds());
				sma.setStock(stockName);
				sma.setType("50-day");
				sma.setValue(SMA.calculateSMA(stockDaysValues.subList(i - 49, i + 1)));
				request.addParam(GlobalConstant.ITEM, sma);
				algorithmCruncherDao.save(request, response);
			}

			request.addParam(GlobalConstant.IDENTIFIER, "SMA");
			request.addParam(GlobalConstant.TYPE, "15-day");
			request.addParam(GlobalConstant.STOCK, stockName);
			request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(stockDays.size() - 1).getEpochSeconds());
			algorithmCruncherDao.itemCount(request, response);

			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
				sma.setEpochSeconds(stockDays.get(i).getEpochSeconds());
				sma.setStock(stockName);
				sma.setType("15-day");
				sma.setValue(SMA.calculateSMA(stockDaysValues.subList(i - 14, i + 1)));
				request.addParam(GlobalConstant.ITEM, sma);
				algorithmCruncherDao.save(request, response);
			}

			request.addParam(GlobalConstant.IDENTIFIER, "EMA");
			request.addParam(GlobalConstant.TYPE, "26-day");
			request.addParam(GlobalConstant.STOCK, stockName);
			request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(stockDays.size() - 1).getEpochSeconds());
			algorithmCruncherDao.itemCount(request, response);

			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
				ema26.setEpochSeconds(stockDays.get(i).getEpochSeconds());
				ema26.setStock(stockName);
				ema26.setType("26-day");

				request.addParam(GlobalConstant.IDENTIFIER, "EMA");
				request.addParam(GlobalConstant.TYPE, "26-day");
				request.addParam(GlobalConstant.STOCK, stockName);
				request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i - 1).getEpochSeconds());

				try {
					algorithmCruncherDao.item(request, response);
					ema26.setValue(EMA.calculateEMA(stockDaysValues.subList(i - 25, i + 1),
							((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
				} catch (Exception e) {
					if (e.getMessage().equals("No entity found for query"))
						ema26.setValue(EMA.calculateEMA(stockDaysValues.subList(i - 25, i + 1)));
					else
						System.out.println(e.getMessage());
				}

				request.addParam(GlobalConstant.ITEM, ema26);
				algorithmCruncherDao.save(request, response);
			}

			request.addParam(GlobalConstant.IDENTIFIER, "EMA");
			request.addParam(GlobalConstant.TYPE, "13-day");
			request.addParam(GlobalConstant.STOCK, stockName);
			request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(stockDays.size() - 1).getEpochSeconds());
			algorithmCruncherDao.itemCount(request, response);

			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
				ema13.setEpochSeconds(stockDays.get(i).getEpochSeconds());
				ema13.setStock(stockName);
				ema13.setType("13-day");

				request.addParam(GlobalConstant.IDENTIFIER, "EMA");
				request.addParam(GlobalConstant.TYPE, "13-day");
				request.addParam(GlobalConstant.STOCK, stockName);
				request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i - 1).getEpochSeconds());

				try {
					algorithmCruncherDao.item(request, response);
					ema13.setValue(EMA.calculateEMA(stockDaysValues.subList(i - 12, i + 1),
							((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
				} catch (Exception e) {
					if (e.getMessage().equals("No entity found for query"))
						ema13.setValue(EMA.calculateEMA(stockDaysValues.subList(i - 12, i + 1)));
					else
						System.out.println(e.getMessage());
				}

				request.addParam(GlobalConstant.ITEM, ema13);
				algorithmCruncherDao.save(request, response);
			}

			request.addParam(GlobalConstant.IDENTIFIER, "MACD");
			request.addParam(GlobalConstant.TYPE, "Day");
			request.addParam(GlobalConstant.STOCK, stockName);
			request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(stockDays.size() - 1).getEpochSeconds());
			algorithmCruncherDao.itemCount(request, response);

			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
				macd.setEpochSeconds(stockDays.get(i).getEpochSeconds());
				macd.setStock(stockName);
				macd.setType("Day");

				request.addParam(GlobalConstant.IDENTIFIER, "EMA");
				request.addParam(GlobalConstant.STOCK, stockName);
				request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i).getEpochSeconds());
				try {
					request.addParam(GlobalConstant.TYPE, "26-day");
					algorithmCruncherDao.item(request, response);
					EMA longEMA = (EMA) response.getParam(GlobalConstant.ITEM);
					request.addParam(GlobalConstant.TYPE, "13-day");
					algorithmCruncherDao.item(request, response);
					EMA shortEMA = (EMA) response.getParam(GlobalConstant.ITEM);
					macd.setValue(shortEMA.getValue().subtract(longEMA.getValue()));
				} catch (Exception e) {
					if (e.getMessage().equals("No entity found for query"))
						macd.setValue(MACD.calculateMACD(stockDaysValues.subList(i - 25, i + 1)));
					else
						System.out.println(e.getMessage());
				}

				request.addParam(GlobalConstant.ITEM, macd);
				algorithmCruncherDao.save(request, response);
			}

			request.addParam(GlobalConstant.IDENTIFIER, "LBB");
			request.addParam(GlobalConstant.TYPE, "50-day");
			request.addParam(GlobalConstant.STOCK, stockName);
			request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(stockDays.size() - 1).getEpochSeconds());
			algorithmCruncherDao.itemCount(request, response);

			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
				lbb.setEpochSeconds(stockDays.get(i).getEpochSeconds());
				lbb.setStock(stockName);
				lbb.setType("50-day");

				request.addParam(GlobalConstant.IDENTIFIER, "SMA");
				request.addParam(GlobalConstant.STOCK, stockName);
				request.addParam(GlobalConstant.TYPE, "50-day");
				request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i).getEpochSeconds());

				try {
					algorithmCruncherDao.item(request, response);
					lbb.setValue(LBB.calculateLBB(stockDaysValues.subList(i - 49, i + 1),
							((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
				} catch (Exception e) {
					if (e.getMessage().equals("No entity found for query"))
						lbb.setValue(LBB.calculateLBB(stockDaysValues.subList(i - 49, i + 1)));
					else
						System.out.println(e.getMessage());
				}

				request.addParam(GlobalConstant.ITEM, lbb);
				algorithmCruncherDao.save(request, response);
			}

			request.addParam(GlobalConstant.IDENTIFIER, "SL");
			request.addParam(GlobalConstant.TYPE, "Day");
			request.addParam(GlobalConstant.STOCK, stockName);
			request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(stockDays.size() - 1).getEpochSeconds());
			algorithmCruncherDao.itemCount(request, response);

			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
				sl.setEpochSeconds(stockDays.get(i).getEpochSeconds());
				sl.setStock(stockName);
				sl.setType("Day");

				request.addParam(GlobalConstant.IDENTIFIER, "MACD");
				request.addParam(GlobalConstant.STOCK, stockName);
				request.addParam(GlobalConstant.TYPE, "DAY");

				try {
					BigDecimal[] macdArr = new BigDecimal[9];

					for (int f = 0; f < 9; f++) {
						request.addParam(GlobalConstant.EPOCHSECONDS, stockDays.get(i - f).getEpochSeconds());
						algorithmCruncherDao.item(request, response);
						macdArr[f] = (((MACD) response.getParam(GlobalConstant.ITEM)).getValue());
					}

					sl.setValue(SL.calculateSL(macdArr));

				} catch (Exception e) {
					if (e.getMessage().equals("No entity found for query"))
						sl.setValue(SL.calculateSL(stockDaysValues.subList(i - 32, i + 1)));
					else
						System.out.println(e.getMessage());
				}

				request.addParam(GlobalConstant.ITEM, sl);
				algorithmCruncherDao.save(request, response);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
