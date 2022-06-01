package org.toasthub.analysis.algorithm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
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
import org.toasthub.analysis.model.UBB;
import org.toasthub.model.Symbol;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.CryptoBar;
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
				break;
			case "SAVE":
				save(request, response);
				break;
			case "DELETE":
				delete(request, response);
				break;
			case "BACKLOAD":
				System.out.println("Starting!");
				backloadCryptoData(request, response);
				System.out.println("CryptoData Loaded");
				backloadStockData(request, response);
				System.out.println("StockData Loaded");
				backloadAlgDays(request, response);
				System.out.println("Algorithm Days Loaded");
				backloadAlgMinutes(request, response);
				System.out.println("Algorithm Minutes Loaded");
				break;
			case "LOAD":
				loadStockData(request, response);
				loadCryptoData(request, response);
				tempLoadAlgs(request, response);
				break;
			default:
				break;
		}

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
			for (String stockName : Symbol.STOCKSYMBOLS) {
				List<StockBar> stockBars;
				ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime first = ZonedDateTime
						.ofInstant(Instant.ofEpochSecond(START_OF_2021), ZoneId.of("America/New_York"))
						.truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime second = first.plusDays(1).minusMinutes(1);
				AssetDay stockDay;
				AssetMinute stockMinute;
				Set<AssetMinute> stockMinutes;
				List<AssetDay> stockDays = new ArrayList<AssetDay>();
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

						stockDay = new AssetDay();
						stockDay.setSymbol(stockName);
						stockDay.setHigh(new BigDecimal(stockBar.get(0).getHigh()));
						stockDay.setEpochSeconds(first.toEpochSecond());
						stockDay.setClose(new BigDecimal(stockBar.get(0).getClose()));
						stockDay.setLow(new BigDecimal(stockBar.get(0).getLow()));
						stockDay.setOpen(new BigDecimal(stockBar.get(0).getOpen()));
						stockDay.setVolume(stockBar.get(0).getVolume());
						stockDay.setVwap(new BigDecimal(stockBar.get(0).getVwap()));
						stockMinutes = new LinkedHashSet<AssetMinute>();

						for (int i = 0; i < stockBars.size(); i++) {
							stockMinute = new AssetMinute();
							stockMinute.setAssetDay(stockDay);
							stockMinute.setSymbol(stockName);
							stockMinute.setEpochSeconds(stockBars.get(i).getTimestamp().toEpochSecond());
							stockMinute.setValue(new BigDecimal(stockBars.get(i).getClose()));
							stockMinute.setVolume(stockBars.get(i).getVolume());
							stockMinute.setVwap(new BigDecimal(stockBars.get(i).getVwap()));
							stockMinutes.add(stockMinute);
						}

						stockDay.setAssetMinutes(stockMinutes);
						stockDays.add(stockDay);
					}

					first = first.plusDays(1);
					second = second.plusDays(1);
				}

				algorithmCruncherDao.saveAll(stockDays);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void backloadCryptoData(Request request, Response response) {
		try {
			Collection<Exchange> exchanges = new ArrayList<Exchange>();
			exchanges.add(Exchange.COINBASE);

			for (String cryptoName : Symbol.CRYPTOSYMBOLS) {
				List<CryptoBar> cryptoBars;
				ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
				ZonedDateTime first = ZonedDateTime
						.ofInstant(Instant.ofEpochSecond(START_OF_2021), ZoneId.of("America/New_York"))
						.truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime second = first.plusDays(1);
				AssetDay cryptoDay;
				AssetMinute cryptoMinute;
				Set<AssetMinute> cryptoMinutes;
				List<AssetDay> cryptoDays = new ArrayList<AssetDay>();
				List<CryptoBar> cryptoBar;
				while (second.isBefore(now)) {
					try {
						cryptoBars = alpacaAPI.cryptoMarketData().getBars(cryptoName,
								exchanges,
								first,
								1500,
								null,
								1,
								BarTimePeriod.MINUTE).getBars();
					} catch (Exception e) {
						System.out.println("Caught!");
						Thread.sleep(1200 * 60);
						System.out.println("Resuming!");
						cryptoBars = alpacaAPI.cryptoMarketData().getBars(cryptoName,
								exchanges,
								first,
								1500,
								null,
								1,
								BarTimePeriod.MINUTE).getBars();
					}

					for (int i = 0; i < cryptoBars.size(); i++) {
						if (!cryptoBars.get(i).getTimestamp().isBefore(second)) {
							cryptoBars = cryptoBars.subList(0, i);
							break;
						}
					}
					try {
						cryptoBar = alpacaAPI.cryptoMarketData().getBars(cryptoName,
								null,
								first,
								1,
								null,
								1,
								BarTimePeriod.DAY).getBars();
					} catch (Exception e) {
						System.out.println("Caught!");
						Thread.sleep(1200 * 60);
						System.out.println("Resuming!");
						cryptoBar = alpacaAPI.cryptoMarketData().getBars(cryptoName,
								null,
								first,
								1,
								null,
								1,
								BarTimePeriod.DAY).getBars();
					}
					if (cryptoBar != null && cryptoBars != null) {

						cryptoDay = new AssetDay();
						cryptoDay.setSymbol(cryptoName);
						cryptoDay.setHigh(new BigDecimal(cryptoBar.get(0).getHigh()));
						cryptoDay.setEpochSeconds(first.toEpochSecond());
						cryptoDay.setClose(new BigDecimal(cryptoBar.get(0).getClose()));
						cryptoDay.setLow(new BigDecimal(cryptoBar.get(0).getLow()));
						cryptoDay.setOpen(new BigDecimal(cryptoBar.get(0).getOpen()));
						cryptoDay.setVolume(cryptoBar.get(0).getVolume().longValue());
						cryptoDay.setVwap(new BigDecimal(cryptoBar.get(0).getVwap()));
						cryptoMinutes = new LinkedHashSet<AssetMinute>();

						for (int i = 0; i < cryptoBars.size(); i++) {
							cryptoMinute = new AssetMinute();
							cryptoMinute.setAssetDay(cryptoDay);
							cryptoMinute.setSymbol(cryptoName);
							cryptoMinute.setEpochSeconds(cryptoBars.get(i).getTimestamp().toEpochSecond());
							cryptoMinute.setValue(new BigDecimal(cryptoBars.get(i).getClose()));
							cryptoMinute.setVolume(cryptoBars.get(i).getVolume().longValue());
							cryptoMinute.setVwap(new BigDecimal(cryptoBars.get(i).getVwap()));
							cryptoMinutes.add(cryptoMinute);
						}

						cryptoDay.setAssetMinutes(cryptoMinutes);
						cryptoDays.add(cryptoDay);
					}

					first = first.plusDays(1);
					second = second.plusDays(1);
				}

				algorithmCruncherDao.saveAll(cryptoDays);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void backloadAlgDays(Request request, Response response) {
		// try {
		// for (String symbol : Symbol.SYMBOLS) {

		// EMA ema12;
		// EMA ema26;
		// SMA sma;
		// MACD macd;
		// LBB lbb;
		// UBB ubb;
		// SL sl;

		// request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
		// request.addParam(GlobalConstant.SYMBOL, symbol);
		// algorithmCruncherDao.items(request, response);

		// List<AssetDay> assetDays = (List<AssetDay>)
		// response.getParam(GlobalConstant.ITEMS);
		// List<BigDecimal> assetDaysValues = new ArrayList<BigDecimal>();
		// for (AssetDay assetDay : assetDays)
		// assetDaysValues.add(assetDay.getClose());

		// for (int i = 0; i < assetDays.size(); i++) {

		// ema12 = new EMA(symbol);
		// ema26 = new EMA(symbol);
		// sma = new SMA(symbol);
		// macd = new MACD(symbol);
		// lbb = new LBB(symbol);
		// ubb = new UBB(symbol);
		// sl = new SL(symbol);

		// if (i >= 49) {
		// sma.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// sma.setSymbol(symbol);
		// sma.setType("50-day");
		// sma.setValue(SMA.calculateSMA(assetDaysValues.subList(i - 49, i + 1)));
		// request.addParam(GlobalConstant.ITEM, sma);
		// algorithmCruncherDao.save(request, response);
		// }

		// if (i >= 19) {
		// sma.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// sma.setSymbol(symbol);
		// sma.setType("20-day");
		// sma.setValue(SMA.calculateSMA(assetDaysValues.subList(i - 19, i + 1)));
		// request.addParam(GlobalConstant.ITEM, sma);
		// algorithmCruncherDao.save(request, response);
		// }

		// if (i >= 14) {
		// sma.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// sma.setSymbol(symbol);
		// sma.setType("15-day");
		// sma.setValue(SMA.calculateSMA(assetDaysValues.subList(i - 14, i + 1)));
		// request.addParam(GlobalConstant.ITEM, sma);
		// algorithmCruncherDao.save(request, response);
		// }

		// if (i >= 25) {
		// ema26.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// ema26.setSymbol(symbol);
		// ema26.setType("26-day");

		// request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		// request.addParam(GlobalConstant.TYPE, "26-day");
		// request.addParam(GlobalConstant.SYMBOL, symbol);
		// request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(i -
		// 1).getEpochSeconds());

		// try {
		// algorithmCruncherDao.item(request, response);
		// ema26.setValue(EMA.calculateEMA(assetDaysValues.subList(i - 25, i + 1),
		// ((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		// } catch (Exception e) {
		// if (e.getMessage().equals("No entity found for query"))
		// ema26.setValue(EMA.calculateEMA(assetDaysValues.subList(i - 25, i + 1)));
		// else
		// System.out.println(e.getMessage());
		// }

		// request.addParam(GlobalConstant.ITEM, ema26);
		// algorithmCruncherDao.save(request, response);
		// }

		// if (i >= 11) {
		// ema12.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// ema12.setSymbol(symbol);
		// ema12.setType("12-day");

		// request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		// request.addParam(GlobalConstant.TYPE, "12-day");
		// request.addParam(GlobalConstant.SYMBOL, symbol);
		// request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(i -
		// 1).getEpochSeconds());

		// try {
		// algorithmCruncherDao.item(request, response);
		// ema12.setValue(EMA.calculateEMA(assetDaysValues.subList(i - 11, i + 1),
		// ((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		// } catch (Exception e) {
		// if (e.getMessage().equals("No entity found for query"))
		// ema12.setValue(EMA.calculateEMA(assetDaysValues.subList(i - 11, i + 1)));
		// else
		// System.out.println(e.getMessage());
		// }

		// request.addParam(GlobalConstant.ITEM, ema12);
		// algorithmCruncherDao.save(request, response);
		// }

		// if (i >= 25) {
		// macd.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// macd.setSymbol(symbol);
		// macd.setType("Day");

		// request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		// request.addParam(GlobalConstant.SYMBOL, symbol);
		// request.addParam(GlobalConstant.EPOCHSECONDS,
		// assetDays.get(i).getEpochSeconds());
		// try {
		// request.addParam(GlobalConstant.TYPE, "26-day");
		// algorithmCruncherDao.item(request, response);
		// EMA longEMA = (EMA) response.getParam(GlobalConstant.ITEM);
		// request.addParam(GlobalConstant.TYPE, "12-day");
		// algorithmCruncherDao.item(request, response);
		// EMA shortEMA = (EMA) response.getParam(GlobalConstant.ITEM);
		// macd.setValue(shortEMA.getValue().subtract(longEMA.getValue()));
		// } catch (Exception e) {
		// if (e.getMessage().equals("No entity found for query"))
		// macd.setValue(MACD.calculateMACD(assetDaysValues.subList(i - 25, i + 1)));
		// else
		// System.out.println(e.getMessage());
		// }

		// request.addParam(GlobalConstant.ITEM, macd);
		// algorithmCruncherDao.save(request, response);
		// }

		// if (i >= 19) {
		// lbb.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// lbb.setSymbol(symbol);
		// lbb.setType("20-day");

		// request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		// request.addParam(GlobalConstant.SYMBOL, symbol);
		// request.addParam(GlobalConstant.TYPE, "20-day");
		// request.addParam(GlobalConstant.EPOCHSECONDS,
		// assetDays.get(i).getEpochSeconds());

		// try {
		// algorithmCruncherDao.item(request, response);
		// lbb.setValue(LBB.calculateLBB(assetDaysValues.subList(i - 19, i + 1),
		// ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		// } catch (Exception e) {
		// if (e.getMessage().equals("No entity found for query"))
		// lbb.setValue(LBB.calculateLBB(assetDaysValues.subList(i - 19, i + 1)));
		// else
		// System.out.println(e.getMessage());
		// }

		// request.addParam(GlobalConstant.ITEM, lbb);
		// algorithmCruncherDao.save(request, response);
		// }

		// if (i >= 19) {
		// ubb.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// ubb.setSymbol(symbol);
		// ubb.setType("20-day");

		// request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		// request.addParam(GlobalConstant.SYMBOL, symbol);
		// request.addParam(GlobalConstant.TYPE, "20-day");
		// request.addParam(GlobalConstant.EPOCHSECONDS,
		// assetDays.get(i).getEpochSeconds());

		// try {
		// algorithmCruncherDao.item(request, response);
		// ubb.setValue(UBB.calculateUBB(assetDaysValues.subList(i - 19, i + 1),
		// ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		// } catch (Exception e) {
		// if (e.getMessage().equals("No entity found for query"))
		// ubb.setValue(UBB.calculateUBB(assetDaysValues.subList(i - 19, i + 1)));
		// else
		// System.out.println(e.getMessage());
		// }

		// request.addParam(GlobalConstant.ITEM, ubb);
		// algorithmCruncherDao.save(request, response);
		// }

		// if (i >= 32) {
		// sl.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		// sl.setSymbol(symbol);
		// sl.setType("Day");

		// request.addParam(GlobalConstant.IDENTIFIER, "MACD");
		// request.addParam(GlobalConstant.SYMBOL, symbol);
		// request.addParam(GlobalConstant.TYPE, "DAY");

		// try {
		// BigDecimal[] macdArr = new BigDecimal[9];

		// for (int f = 0; f < 9; f++) {
		// request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(i -
		// f).getEpochSeconds());
		// algorithmCruncherDao.item(request, response);
		// macdArr[f] = (((MACD) response.getParam(GlobalConstant.ITEM)).getValue());
		// }

		// sl.setValue(SL.calculateSL(macdArr));

		// } catch (Exception e) {
		// if (e.getMessage().equals("No entity found for query"))
		// sl.setValue(SL.calculateSL(assetDaysValues.subList(i - 32, i + 1)));
		// else
		// System.out.println(e.getMessage());
		// }

		// request.addParam(GlobalConstant.ITEM, sl);
		// algorithmCruncherDao.save(request, response);
		// }
		// }
		// }

		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	@Override
	@SuppressWarnings("unchecked")
	public void backloadAlgMinutes(Request request, Response response) {
		/*
		 * try {
		 * for (String symbol : Symbol.SYMBOLS) {
		 * 
		 * EMA ema12;
		 * EMA ema26;
		 * SMA sma;
		 * MACD macd;
		 * LBB lbb;
		 * UBB ubb;
		 * SL sl;
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "AssetMinute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * algorithmCruncherDao.items(request, response);
		 * 
		 * List<AssetMinute> assetMinutes = (List<AssetMinute>)
		 * response.getParam(GlobalConstant.ITEMS);
		 * List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();
		 * for (AssetMinute assetMinute : assetMinutes)
		 * assetMinuteValues.add(assetMinute.getValue());
		 * 
		 * for (int i = 0; i < assetMinutes.size(); i++) {
		 * 
		 * ema12 = new EMA(symbol);
		 * ema26 = new EMA(symbol);
		 * sma = new SMA(symbol);
		 * macd = new MACD(symbol);
		 * lbb = new LBB(symbol);
		 * ubb = new UBB(symbol);
		 * sl = new SL(symbol);
		 * 
		 * if (i >= 49) {
		 * sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * sma.setSymbol(symbol);
		 * sma.setType("50-minute");
		 * sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - 49, i + 1)));
		 * request.addParam(GlobalConstant.ITEM, sma);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * if (i >= 19) {
		 * sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * sma.setSymbol(symbol);
		 * sma.setType("20-minute");
		 * sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - 19, i + 1)));
		 * request.addParam(GlobalConstant.ITEM, sma);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * if (i >= 14) {
		 * sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * sma.setSymbol(symbol);
		 * sma.setType("15-minute");
		 * sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - 14, i + 1)));
		 * request.addParam(GlobalConstant.ITEM, sma);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * if (i >= 25) {
		 * ema26.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * ema26.setSymbol(symbol);
		 * ema26.setType("26-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "26-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetMinutes.get(i -
		 * 1).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ema26.setValue(EMA.calculateEMA(assetMinuteValues.subList(i - 25, i + 1),
		 * ((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ema26.setValue(EMA.calculateEMA(assetMinuteValues.subList(i - 25, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ema26);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * if (i >= 11) {
		 * ema12.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * ema12.setSymbol(symbol);
		 * ema12.setType("12-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "12-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetMinutes.get(i -
		 * 1).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ema12.setValue(EMA.calculateEMA(assetMinuteValues.subList(i - 11, i + 1),
		 * ((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ema12.setValue(EMA.calculateEMA(assetMinuteValues.subList(i - 11, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ema12);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * if (i >= 25) {
		 * macd.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * macd.setSymbol(symbol);
		 * macd.setType("Minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(i).getEpochSeconds());
		 * try {
		 * request.addParam(GlobalConstant.TYPE, "26-minute");
		 * algorithmCruncherDao.item(request, response);
		 * EMA longEMA = (EMA) response.getParam(GlobalConstant.ITEM);
		 * request.addParam(GlobalConstant.TYPE, "12-minute");
		 * algorithmCruncherDao.item(request, response);
		 * EMA shortEMA = (EMA) response.getParam(GlobalConstant.ITEM);
		 * macd.setValue(shortEMA.getValue().subtract(longEMA.getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * macd.setValue(MACD.calculateMACD(assetMinuteValues.subList(i - 25, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, macd);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * if (i >= 19) {
		 * lbb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * lbb.setSymbol(symbol);
		 * lbb.setType("20-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "20-minute");
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(i).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * lbb.setValue(LBB.calculateLBB(assetMinuteValues.subList(i - 19, i + 1),
		 * ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * lbb.setValue(LBB.calculateLBB(assetMinuteValues.subList(i - 19, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, lbb);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * if (i >= 19) {
		 * ubb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * ubb.setSymbol(symbol);
		 * ubb.setType("20-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "20-minute");
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(i).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ubb.setValue(UBB.calculateUBB(assetMinuteValues.subList(i - 19, i + 1),
		 * ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ubb.setValue(UBB.calculateUBB(assetMinuteValues.subList(i - 19, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ubb);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * if (i >= 32) {
		 * sl.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * sl.setSymbol(symbol);
		 * sl.setType("Minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "MACD");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "Minute");
		 * 
		 * try {
		 * BigDecimal[] macdArr = new BigDecimal[9];
		 * 
		 * for (int f = 0; f < 9; f++) {
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(i - f).getEpochSeconds());
		 * algorithmCruncherDao.item(request, response);
		 * macdArr[f] = (((MACD) response.getParam(GlobalConstant.ITEM)).getValue());
		 * }
		 * 
		 * sl.setValue(SL.calculateSL(macdArr));
		 * 
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * sl.setValue(SL.calculateSL(assetMinuteValues.subList(i - 32, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, sl);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * }
		 * }
		 * 
		 * } catch (Exception e) {
		 * e.printStackTrace();
		 * }
		 */
	}

	@Override
	public void loadStockData(Request request, Response response) {
		try {
			for (String stockName : Symbol.STOCKSYMBOLS) {
				List<StockBar> stockBars;
				ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York")).minusSeconds(60 * 20);
				AssetDay stockDay;
				AssetMinute stockMinute;
				Set<AssetMinute> stockMinutes;
				List<StockBar> stockBar;
				Set<AssetMinute> preExistingStockMinutes = new LinkedHashSet<AssetMinute>();
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

					stockDay = new AssetDay();
					stockMinutes = new LinkedHashSet<AssetMinute>();

					request.addParam(GlobalConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
					request.addParam(GlobalConstant.SYMBOL, stockName);
					request.addParam(GlobalConstant.TYPE, "AssetDay");
					request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");

					try {
						algorithmCruncherDao.initializedAssetDay(request, response);
						stockDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
						preExistingStockMinutes = stockDay.getAssetMinutes();
						preExisting = true;
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query")) {
							stockDay.setSymbol(stockName);
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
						stockMinute = new AssetMinute();
						stockMinute.setAssetDay(stockDay);
						stockMinute.setSymbol(stockName);
						stockMinute.setEpochSeconds(stockBars.get(i).getTimestamp().toEpochSecond());
						stockMinute.setValue(new BigDecimal(stockBars.get(i).getClose()));
						stockMinute.setVolume(stockBars.get(i).getVolume());
						stockMinute.setVwap(new BigDecimal(stockBars.get(i).getVwap()));
						stockMinutes.add(stockMinute);
					}

					if (preExisting) {
						for (AssetMinute tempStockMinute : stockMinutes) {
							request.addParam(GlobalConstant.ITEM, tempStockMinute);
							algorithmCruncherDao.save(request, response);
						}

					}
					if (!preExisting) {
						stockDay.setAssetMinutes(stockMinutes);
						request.addParam(GlobalConstant.ITEM, stockDay);
						algorithmCruncherDao.save(request, response);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadCryptoData(Request request, Response response) {
		try {
			Collection<Exchange> exchanges = new ArrayList<Exchange>();
			exchanges.add(Exchange.COINBASE);

			for (String cryptoName : Symbol.CRYPTOSYMBOLS) {
				List<CryptoBar> cryptoBars;
				ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York"));
				AssetDay cryptoDay;
				AssetMinute cryptoMinute;
				Set<AssetMinute> cryptoMinutes;
				List<CryptoBar> cryptoBar;
				Set<AssetMinute> preExistingCryptoMinutes = new LinkedHashSet<AssetMinute>();
				boolean preExisting = false;

				cryptoBars = alpacaAPI.cryptoMarketData().getBars(cryptoName,
						exchanges,
						today.truncatedTo(ChronoUnit.DAYS),
						1500,
						null,
						1,
						BarTimePeriod.MINUTE).getBars();

				cryptoBar = alpacaAPI.cryptoMarketData().getBars(cryptoName,
						exchanges,
						today.truncatedTo(ChronoUnit.DAYS),
						1,
						null,
						1,
						BarTimePeriod.DAY).getBars();

				if (cryptoBar != null) {

					cryptoDay = new AssetDay();
					cryptoMinutes = new LinkedHashSet<AssetMinute>();

					request.addParam(GlobalConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
					request.addParam(GlobalConstant.SYMBOL, cryptoName);
					request.addParam(GlobalConstant.TYPE, "AssetDay");
					request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");

					try {
						algorithmCruncherDao.initializedAssetDay(request, response);
						cryptoDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
						preExistingCryptoMinutes = cryptoDay.getAssetMinutes();
						preExisting = true;
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query")) {
							cryptoDay.setSymbol(cryptoName);
							cryptoDay.setHigh(BigDecimal.valueOf(cryptoBar.get(0).getHigh()));
							cryptoDay.setEpochSeconds(today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
							cryptoDay.setClose(BigDecimal.valueOf(cryptoBar.get(0).getClose()));
							cryptoDay.setLow(BigDecimal.valueOf(cryptoBar.get(0).getLow()));
							cryptoDay.setOpen(BigDecimal.valueOf(cryptoBar.get(0).getOpen()));
							cryptoDay.setVolume(cryptoBar.get(0).getVolume().longValue());
							cryptoDay.setVwap(BigDecimal.valueOf(cryptoBar.get(0).getVwap()));
							preExisting = false;
						} else {
							System.out.println(e.getMessage());
							e.printStackTrace();
						}
					}
					for (int i = preExistingCryptoMinutes.size(); i < cryptoBars.size(); i++) {
						cryptoMinute = new AssetMinute();
						cryptoMinute.setAssetDay(cryptoDay);
						cryptoMinute.setSymbol(cryptoName);
						cryptoMinute.setEpochSeconds(cryptoBars.get(i).getTimestamp().toEpochSecond());
						cryptoMinute.setValue(BigDecimal.valueOf(cryptoBars.get(i).getClose()));
						cryptoMinute.setVolume(cryptoBars.get(i).getVolume().longValue());
						cryptoMinute.setVwap(BigDecimal.valueOf(cryptoBars.get(i).getVwap()));
						cryptoMinutes.add(cryptoMinute);
					}

					if (preExisting) {
						for (AssetMinute tempCryptoMinute : cryptoMinutes) {
							request.addParam(GlobalConstant.ITEM, tempCryptoMinute);
							algorithmCruncherDao.save(request, response);
						}

					}
					if (!preExisting) {
						cryptoDay.setAssetMinutes(cryptoMinutes);
						request.addParam(GlobalConstant.ITEM, cryptoDay);
						algorithmCruncherDao.save(request, response);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void loadAlgDays(Request request, Response response) {
		/*
		 * try {
		 * for (String symbol : Symbol.SYMBOLS) {
		 * 
		 * EMA ema13;
		 * EMA ema26;
		 * SMA sma;
		 * MACD macd;
		 * LBB lbb;
		 * UBB ubb;
		 * SL sl;
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * algorithmCruncherDao.items(request, response);
		 * 
		 * List<AssetDay> assetDays = (List<AssetDay>)
		 * response.getParam(GlobalConstant.ITEMS);
		 * List<BigDecimal> assetDaysValues = new ArrayList<BigDecimal>();
		 * 
		 * int i = assetDays.size() - 1;
		 * for (AssetDay assetDay : assetDays)
		 * assetDaysValues.add(assetDay.getClose());
		 * 
		 * ema13 = new EMA(symbol);
		 * ema26 = new EMA(symbol);
		 * sma = new SMA(symbol);
		 * macd = new MACD(symbol);
		 * lbb = new LBB(symbol);
		 * ubb = new UBB(symbol);
		 * sl = new SL(symbol);
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.TYPE, "50-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * sma.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * sma.setSymbol(symbol);
		 * sma.setType("50-day");
		 * sma.setValue(SMA.calculateSMA(assetDaysValues.subList(i - 49, i + 1)));
		 * request.addParam(GlobalConstant.ITEM, sma);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.TYPE, "15-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * sma.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * sma.setSymbol(symbol);
		 * sma.setType("15-day");
		 * sma.setValue(SMA.calculateSMA(assetDaysValues.subList(i - 14, i + 1)));
		 * request.addParam(GlobalConstant.ITEM, sma);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "26-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * ema26.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * ema26.setSymbol(symbol);
		 * ema26.setType("26-day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "26-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(i -
		 * 1).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ema26.setValue(EMA.calculateEMA(assetDaysValues.subList(i - 25, i + 1),
		 * ((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ema26.setValue(EMA.calculateEMA(assetDaysValues.subList(i - 25, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ema26);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "13-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * ema13.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * ema13.setSymbol(symbol);
		 * ema13.setType("13-day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "13-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(i -
		 * 1).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ema13.setValue(EMA.calculateEMA(assetDaysValues.subList(i - 12, i + 1),
		 * ((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ema13.setValue(EMA.calculateEMA(assetDaysValues.subList(i - 12, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ema13);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "MACD");
		 * request.addParam(GlobalConstant.TYPE, "Day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * macd.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * macd.setSymbol(symbol);
		 * macd.setType("Day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetDays.get(i).getEpochSeconds());
		 * try {
		 * request.addParam(GlobalConstant.TYPE, "26-day");
		 * algorithmCruncherDao.item(request, response);
		 * EMA longEMA = (EMA) response.getParam(GlobalConstant.ITEM);
		 * request.addParam(GlobalConstant.TYPE, "13-day");
		 * algorithmCruncherDao.item(request, response);
		 * EMA shortEMA = (EMA) response.getParam(GlobalConstant.ITEM);
		 * macd.setValue(shortEMA.getValue().subtract(longEMA.getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * macd.setValue(MACD.calculateMACD(assetDaysValues.subList(i - 25, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, macd);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "LBB");
		 * request.addParam(GlobalConstant.TYPE, "50-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * lbb.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * lbb.setSymbol(symbol);
		 * lbb.setType("50-day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "50-day");
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetDays.get(i).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * lbb.setValue(
		 * LBB.calculateLBB(
		 * assetDaysValues.subList(i - 49, i + 1),
		 * ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * lbb.setValue(LBB.calculateLBB(assetDaysValues.subList(i - 49, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, lbb);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "LBB");
		 * request.addParam(GlobalConstant.TYPE, "20-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * lbb.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * lbb.setSymbol(symbol);
		 * lbb.setType("20-day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "20-day");
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetDays.get(i).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * lbb.setValue(LBB.calculateLBB(assetDaysValues.subList(i - 19, i + 1),
		 * ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * lbb.setValue(LBB.calculateLBB(assetDaysValues.subList(i - 19, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, lbb);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "UBB");
		 * request.addParam(GlobalConstant.TYPE, "20-day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * ubb.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * ubb.setSymbol(symbol);
		 * ubb.setType("20-day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "20-day");
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetDays.get(i).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ubb.setValue(UBB.calculateUBB(assetDaysValues.subList(i - 19, i + 1),
		 * ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ubb.setValue(UBB.calculateUBB(assetDaysValues.subList(i - 19, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ubb);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SL");
		 * request.addParam(GlobalConstant.TYPE, "Day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size()
		 * - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * sl.setEpochSeconds(assetDays.get(i).getEpochSeconds());
		 * sl.setSymbol(symbol);
		 * sl.setType("Day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "MACD");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "DAY");
		 * 
		 * try {
		 * BigDecimal[] macdArr = new BigDecimal[9];
		 * 
		 * for (int f = 0; f < 9; f++) {
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(i -
		 * f).getEpochSeconds());
		 * algorithmCruncherDao.item(request, response);
		 * macdArr[f] = (((MACD) response.getParam(GlobalConstant.ITEM)).getValue());
		 * }
		 * 
		 * sl.setValue(SL.calculateSL(macdArr));
		 * 
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * sl.setValue(SL.calculateSL(assetDaysValues.subList(i - 32, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, sl);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * }
		 * 
		 * } catch (Exception e) {
		 * e.printStackTrace();
		 * }
		 */
	}

	@Override
	@SuppressWarnings("unchecked")
	public void loadAlgMinutes(Request request, Response response) {
		/*
		 * try {
		 * for (String symbol : Symbol.SYMBOLS) {
		 * 
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * algorithmCruncherDao.getRecentAssetMinutes(request, response);
		 * List<AssetMinute> assetMinutes = (List<AssetMinute>)
		 * response.getParam(GlobalConstant.ITEMS);
		 * List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();
		 * 
		 * int i = assetMinutes.size() - 1;
		 * for (AssetMinute assetMinute : assetMinutes)
		 * assetMinuteValues.add(assetMinute.getValue());
		 * 
		 * EMA ema13;
		 * EMA ema26;
		 * SMA sma;
		 * MACD macd;
		 * LBB lbb;
		 * UBB ubb;
		 * SL sl;
		 * 
		 * ema13 = new EMA(symbol);
		 * ema26 = new EMA(symbol);
		 * sma = new SMA(symbol);
		 * macd = new MACD(symbol);
		 * lbb = new LBB(symbol);
		 * ubb = new UBB(symbol);
		 * sl = new SL(symbol);
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.TYPE, "50-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * sma.setSymbol(symbol);
		 * sma.setType("50-minute");
		 * sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - 49, i + 1)));
		 * request.addParam(GlobalConstant.ITEM, sma);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.TYPE, "15-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * sma.setSymbol(symbol);
		 * sma.setType("15-minute");
		 * sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - 14, i + 1)));
		 * request.addParam(GlobalConstant.ITEM, sma);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "26-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * ema26.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * ema26.setSymbol(symbol);
		 * ema26.setType("26-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "26-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetMinutes.get(i -
		 * 1).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ema26.setValue(EMA.calculateEMA(assetMinuteValues.subList(i - 25, i + 1),
		 * ((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ema26.setValue(EMA.calculateEMA(assetMinuteValues.subList(i - 25, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ema26);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "13-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * ema13.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * ema13.setSymbol(symbol);
		 * ema13.setType("13-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.TYPE, "13-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetMinutes.get(i -
		 * 1).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ema13.setValue(EMA.calculateEMA(assetMinuteValues.subList(i - 12, i + 1),
		 * ((EMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ema13.setValue(EMA.calculateEMA(assetMinuteValues.subList(i - 12, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ema13);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "MACD");
		 * request.addParam(GlobalConstant.TYPE, "Day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * macd.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * macd.setSymbol(symbol);
		 * macd.setType("Day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "EMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(i).getEpochSeconds());
		 * try {
		 * request.addParam(GlobalConstant.TYPE, "26-minute");
		 * algorithmCruncherDao.item(request, response);
		 * EMA longEMA = (EMA) response.getParam(GlobalConstant.ITEM);
		 * request.addParam(GlobalConstant.TYPE, "13-minute");
		 * algorithmCruncherDao.item(request, response);
		 * EMA shortEMA = (EMA) response.getParam(GlobalConstant.ITEM);
		 * macd.setValue(shortEMA.getValue().subtract(longEMA.getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * macd.setValue(MACD.calculateMACD(assetMinuteValues.subList(i - 25, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, macd);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "LBB");
		 * request.addParam(GlobalConstant.TYPE, "50-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * lbb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * lbb.setSymbol(symbol);
		 * lbb.setType("50-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "50-minute");
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(i).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * lbb.setValue(LBB.calculateLBB(assetMinuteValues.subList(i - 49, i + 1),
		 * ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * lbb.setValue(LBB.calculateLBB(assetMinuteValues.subList(i - 49, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, lbb);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "LBB");
		 * request.addParam(GlobalConstant.TYPE, "20-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * lbb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * lbb.setSymbol(symbol);
		 * lbb.setType("20-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "20-minute");
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(i).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * lbb.setValue(LBB.calculateLBB(assetMinuteValues.subList(i - 19, i + 1),
		 * ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * lbb.setValue(LBB.calculateLBB(assetMinuteValues.subList(i - 19, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, lbb);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "UBB");
		 * request.addParam(GlobalConstant.TYPE, "20-minute");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * ubb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * ubb.setSymbol(symbol);
		 * ubb.setType("20-minute");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "20-minute");
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(i).getEpochSeconds());
		 * 
		 * try {
		 * algorithmCruncherDao.item(request, response);
		 * ubb.setValue(UBB.calculateUBB(assetMinuteValues.subList(i - 19, i + 1),
		 * ((SMA) response.getParam(GlobalConstant.ITEM)).getValue()));
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * ubb.setValue(UBB.calculateUBB(assetMinuteValues.subList(i - 19, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, ubb);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "SL");
		 * request.addParam(GlobalConstant.TYPE, "Day");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.EPOCHSECONDS,
		 * assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		 * algorithmCruncherDao.itemCount(request, response);
		 * 
		 * if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
		 * sl.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		 * sl.setSymbol(symbol);
		 * sl.setType("Day");
		 * 
		 * request.addParam(GlobalConstant.IDENTIFIER, "MACD");
		 * request.addParam(GlobalConstant.SYMBOL, symbol);
		 * request.addParam(GlobalConstant.TYPE, "DAY");
		 * 
		 * try {
		 * BigDecimal[] macdArr = new BigDecimal[9];
		 * 
		 * for (int f = 0; f < 9; f++) {
		 * request.addParam(GlobalConstant.EPOCHSECONDS, assetMinutes.get(i -
		 * f).getEpochSeconds());
		 * algorithmCruncherDao.item(request, response);
		 * macdArr[f] = (((MACD) response.getParam(GlobalConstant.ITEM)).getValue());
		 * }
		 * 
		 * sl.setValue(SL.calculateSL(macdArr));
		 * 
		 * } catch (Exception e) {
		 * if (e.getMessage().equals("No entity found for query"))
		 * sl.setValue(SL.calculateSL(assetMinuteValues.subList(i - 32, i + 1)));
		 * else
		 * System.out.println(e.getMessage());
		 * }
		 * 
		 * request.addParam(GlobalConstant.ITEM, sl);
		 * algorithmCruncherDao.save(request, response);
		 * }
		 * }
		 * 
		 * } catch (Exception e) {
		 * e.printStackTrace();
		 * }
		 */
	}

	public void tempLoadAlgs(Request request, Response response) {
		loadGoldenCrossAlgs(request, response);
		loadLowerBollingerBandAlgs(request, response);
		loadUpperBollingerBandAlgs(request, response);
	}

	public void loadGoldenCrossAlgs(Request request, Response response) {
		try {
			request.addParam("EVAL_PERIOD", "DAY");
			request.addParam(GlobalConstant.IDENTIFIER, "TRADE_SIGNAL");
			request.addParam("TRADE_SIGNAL", "GoldenCross");
			algorithmCruncherDao.items(request, response);

			ArrayList<Properties> properties = new ArrayList<Properties>();

			if(request.getParam(GlobalConstant.ITEMS) != null){
				for(Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))){
					properties.add(Properties.class.cast(obj));
				}
			}

			for (Properties p : properties) {

				SMA sma;
				String symbol = p.getProperty("SYMBOL");
				String shortSMAType = p.getProperty("SHORT_SMA_TYPE");
				String longSMAType = p.getProperty("LONG_SMA_TYPE");
				int shortSMAPeriod = Integer.valueOf(shortSMAType.substring(0, shortSMAType.indexOf("-")));
				int longSMAPeriod = Integer.valueOf(longSMAType.substring(0, longSMAType.indexOf("-")));

				request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
				request.addParam(GlobalConstant.SYMBOL, symbol);
				algorithmCruncherDao.items(request, response);

				List<AssetDay> assetDays = new ArrayList<AssetDay>();
				List<BigDecimal> assetDaysValues = new ArrayList<BigDecimal>();

				for(Object obj : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))){
					AssetDay assetDay = AssetDay.class.cast(obj);
					assetDays.add(assetDay);
					assetDaysValues.add(assetDay.getClose());
				}

				int i = assetDays.size() - 1;

				if(i < 0) return;

				sma = new SMA(symbol);

				request.addParam(GlobalConstant.IDENTIFIER, "SMA");
				request.addParam(GlobalConstant.TYPE, longSMAType);
				request.addParam(GlobalConstant.SYMBOL, symbol);
				request.addParam(GlobalConstant.EPOCHSECONDS,
						assetDays.get(assetDays.size() - 1).getEpochSeconds());
				algorithmCruncherDao.itemCount(request, response);

				if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0
						&& i >= longSMAPeriod) {
					sma.setEpochSeconds(assetDays.get(i).getEpochSeconds());
					sma.setSymbol(symbol);
					sma.setType(longSMAType);
					sma.setValue(SMA.calculateSMA(assetDaysValues.subList(i - (longSMAPeriod - 1), i + 1)));
					request.addParam(GlobalConstant.ITEM, sma);
					algorithmCruncherDao.save(request, response);
				}

				request.addParam(GlobalConstant.IDENTIFIER, "SMA");
				request.addParam(GlobalConstant.TYPE, shortSMAType);
				request.addParam(GlobalConstant.SYMBOL, symbol);
				request.addParam(GlobalConstant.EPOCHSECONDS,
						assetDays.get(assetDays.size() - 1).getEpochSeconds());
				algorithmCruncherDao.itemCount(request, response);

				if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0
						&& i >= shortSMAPeriod) {
					sma.setEpochSeconds(assetDays.get(i).getEpochSeconds());
					sma.setSymbol(symbol);
					sma.setType(shortSMAType);
					sma.setValue(SMA.calculateSMA(assetDaysValues.subList(i - (shortSMAPeriod - 1), i + 1)));
					request.addParam(GlobalConstant.ITEM, sma);
					algorithmCruncherDao.save(request, response);
				}
			}

			request.addParam("EVAL_PERIOD", "MINUTE");
			request.addParam(GlobalConstant.IDENTIFIER, "TRADE_SIGNAL");
			request.addParam("TRADE_SIGNAL", "GoldenCross");
			algorithmCruncherDao.items(request, response);

			properties = new ArrayList<Properties>();

			if(request.getParam(GlobalConstant.ITEMS) != null){
				for(Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))){
					properties.add(Properties.class.cast(obj));
				}
			}

			for (Properties p : properties) {
				String symbol = p.getProperty("SYMBOL");
				String shortSMAType = p.getProperty("SHORT_SMA_TYPE");
				String longSMAType = p.getProperty("LONG_SMA_TYPE");
				int shortSMAPeriod = Integer.valueOf(shortSMAType.substring(0, shortSMAType.indexOf("-")));
				int longSMAPeriod = Integer.valueOf(longSMAType.substring(0, longSMAType.indexOf("-")));

				request.addParam(GlobalConstant.SYMBOL, symbol);
				algorithmCruncherDao.getRecentAssetMinutes(request, response);

				List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
				List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

				for(Object obj : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))){
					AssetMinute assetMinute = AssetMinute.class.cast(obj);
					assetMinutes.add(assetMinute);
					assetMinuteValues.add(assetMinute.getValue());
				}

				int i = assetMinutes.size() - 1;

				if(i < 0) return;

				SMA sma;

				sma = new SMA(symbol);

				request.addParam(GlobalConstant.IDENTIFIER, "SMA");
				request.addParam(GlobalConstant.TYPE, longSMAType);
				request.addParam(GlobalConstant.SYMBOL, symbol);
				request.addParam(GlobalConstant.EPOCHSECONDS,
						assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
				algorithmCruncherDao.itemCount(request, response);

				if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0
						&& i >= longSMAPeriod) {
					sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
					sma.setSymbol(symbol);
					sma.setType(longSMAType);
					sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - (longSMAPeriod - 1), i + 1)));
					request.addParam(GlobalConstant.ITEM, sma);
					algorithmCruncherDao.save(request, response);
				}

				request.addParam(GlobalConstant.IDENTIFIER, "SMA");
				request.addParam(GlobalConstant.TYPE, shortSMAType);
				request.addParam(GlobalConstant.SYMBOL, symbol);
				request.addParam(GlobalConstant.EPOCHSECONDS,
						assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
				algorithmCruncherDao.itemCount(request, response);

				if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0
						&& i >= shortSMAPeriod) {
					sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
					sma.setSymbol(symbol);
					sma.setType(shortSMAType);
					sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - (shortSMAPeriod - 1), i + 1)));
					request.addParam(GlobalConstant.ITEM, sma);
					algorithmCruncherDao.save(request, response);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void loadLowerBollingerBandAlgs(Request request, Response response) {
		try {
			request.addParam("EVAL_PERIOD", "DAY");
			request.addParam(GlobalConstant.IDENTIFIER, "TRADE_SIGNAL");
			request.addParam("TRADE_SIGNAL", "LowerBollingerBand");
			algorithmCruncherDao.items(request, response);

			for (Properties p : (List<Properties>) response.getParam(GlobalConstant.ITEMS)) {

				LBB lbb;
				String symbol = p.getProperty("SYMBOL");
				BigDecimal standardDeviations = (BigDecimal) p.get("STANDARD_DEVIATIONS");
				String lbbType = p.getProperty("LBB_TYPE");
				int lbbPeriod = Integer.valueOf(lbbType.substring(0, lbbType.indexOf("-")));

				request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
				request.addParam(GlobalConstant.SYMBOL, symbol);
				algorithmCruncherDao.items(request, response);

				List<AssetDay> assetDays = (List<AssetDay>) response.getParam(GlobalConstant.ITEMS);
				List<BigDecimal> assetDaysValues = new ArrayList<BigDecimal>();

				int i = assetDays.size() - 1;

				for (AssetDay assetDay : assetDays)
					assetDaysValues.add(assetDay.getClose());

				lbb = new LBB(symbol);

				request.addParam(GlobalConstant.IDENTIFIER, "LBB");
				request.addParam(GlobalConstant.TYPE, lbbType);
				request.addParam("STANDARD_DEVIATIONS", standardDeviations);
				request.addParam(GlobalConstant.SYMBOL, symbol);
				request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size() - 1).getEpochSeconds());
				algorithmCruncherDao.itemCount(request, response);

				if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0
						&& i >= lbbPeriod) {
					lbb.setEpochSeconds(assetDays.get(i).getEpochSeconds());
					lbb.setSymbol(symbol);
					lbb.setType(lbbType);
					lbb.setStandardDeviations(standardDeviations);

					request.addParam(GlobalConstant.IDENTIFIER, "SMA");
					request.addParam(GlobalConstant.SYMBOL, symbol);
					request.addParam(GlobalConstant.TYPE, lbbType);
					request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(i).getEpochSeconds());

					try {
						algorithmCruncherDao.item(request, response);
						lbb.setValue(
								LBB.calculateLBB(
										assetDaysValues.subList(i - (lbbPeriod - 1), i + 1),
										((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
										standardDeviations));
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							lbb.setValue(
									LBB.calculateLBB(
											assetDaysValues.subList(i - (lbbPeriod - 1), i + 1),
											standardDeviations));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, lbb);
					algorithmCruncherDao.save(request, response);
				}
			}

			request.addParam("EVAL_PERIOD", "MINUTE");
			request.addParam(GlobalConstant.IDENTIFIER, "TRADE_SIGNAL");
			request.addParam("TRADE_SIGNAL", "LowerBollingerBand");
			algorithmCruncherDao.items(request, response);

			for (Properties p : (List<Properties>) response.getParam(GlobalConstant.ITEMS)) {

				LBB lbb;
				String symbol = p.getProperty("SYMBOL");
				BigDecimal standardDeviations = (BigDecimal) p.get("STANDARD_DEVIATIONS");
				String lbbType = p.getProperty("LBB_TYPE");
				int lbbPeriod = Integer.valueOf(lbbType.substring(0, lbbType.indexOf("-")));

				request.addParam(GlobalConstant.SYMBOL, symbol);
				algorithmCruncherDao.getRecentAssetMinutes(request, response);
				List<AssetMinute> assetMinutes = (List<AssetMinute>) response.getParam(GlobalConstant.ITEMS);
				List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

				int i = assetMinutes.size() - 1;
				for (AssetMinute assetMinute : assetMinutes)
					assetMinuteValues.add(assetMinute.getValue());

				lbb = new LBB(symbol);

				request.addParam(GlobalConstant.IDENTIFIER, "LBB");
				request.addParam(GlobalConstant.TYPE, lbbType);
				request.addParam("STANDARD_DEVIATIONS", standardDeviations);
				request.addParam(GlobalConstant.SYMBOL, symbol);
				request.addParam(GlobalConstant.EPOCHSECONDS,
						assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
				algorithmCruncherDao.itemCount(request, response);

				if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0
						&& i >= lbbPeriod) {
					lbb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
					lbb.setSymbol(symbol);
					lbb.setType(lbbType);
					lbb.setStandardDeviations(standardDeviations);

					request.addParam(GlobalConstant.IDENTIFIER, "SMA");
					request.addParam(GlobalConstant.SYMBOL, symbol);
					request.addParam(GlobalConstant.TYPE, lbbType);
					request.addParam(GlobalConstant.EPOCHSECONDS, assetMinutes.get(i).getEpochSeconds());

					try {
						algorithmCruncherDao.item(request, response);
						lbb.setValue(
								LBB.calculateLBB(
										assetMinuteValues.subList(i - (lbbPeriod - 1), i + 1),
										((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
										standardDeviations));
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							lbb.setValue(
									LBB.calculateLBB(
											assetMinuteValues.subList(i - (lbbPeriod - 1), i + 1),
											standardDeviations));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, lbb);
					algorithmCruncherDao.save(request, response);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void loadUpperBollingerBandAlgs(Request request, Response response) {
		try {
			request.addParam("EVAL_PERIOD", "DAY");
			request.addParam(GlobalConstant.IDENTIFIER, "TRADE_SIGNAL");
			request.addParam("TRADE_SIGNAL", "UpperBollingerBand");
			algorithmCruncherDao.items(request, response);

			for (Properties p : (List<Properties>) response.getParam(GlobalConstant.ITEMS)) {

				UBB ubb;
				String symbol = p.getProperty("SYMBOL");
				BigDecimal standardDeviations = (BigDecimal) p.get("STANDARD_DEVIATIONS");
				String ubbType = p.getProperty("UBB_TYPE");
				int ubbPeriod = Integer.valueOf(ubbType.substring(0, ubbType.indexOf("-")));

				request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
				request.addParam(GlobalConstant.SYMBOL, symbol);
				algorithmCruncherDao.items(request, response);

				List<AssetDay> assetDays = (List<AssetDay>) response.getParam(GlobalConstant.ITEMS);
				List<BigDecimal> assetDaysValues = new ArrayList<BigDecimal>();

				int i = assetDays.size() - 1;

				for (AssetDay assetDay : assetDays)
					assetDaysValues.add(assetDay.getClose());

				ubb = new UBB(symbol);

				request.addParam(GlobalConstant.IDENTIFIER, "UBB");
				request.addParam(GlobalConstant.TYPE, ubbType);
				request.addParam("STANDARD_DEVIATIONS", standardDeviations);
				request.addParam(GlobalConstant.SYMBOL, symbol);
				request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(assetDays.size() - 1).getEpochSeconds());
				algorithmCruncherDao.itemCount(request, response);

				if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0
						&& i >= ubbPeriod) {
					ubb.setEpochSeconds(assetDays.get(i).getEpochSeconds());
					ubb.setSymbol(symbol);
					ubb.setType(ubbType);
					ubb.setStandardDeviations(standardDeviations);

					request.addParam(GlobalConstant.IDENTIFIER, "SMA");
					request.addParam(GlobalConstant.SYMBOL, symbol);
					request.addParam(GlobalConstant.TYPE, ubbType);
					request.addParam(GlobalConstant.EPOCHSECONDS, assetDays.get(i).getEpochSeconds());

					try {
						algorithmCruncherDao.item(request, response);
						ubb.setValue(
								UBB.calculateUBB(
										assetDaysValues.subList(i - (ubbPeriod - 1), i + 1),
										((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
										standardDeviations));
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							ubb.setValue(
									UBB.calculateUBB(
											assetDaysValues.subList(i - (ubbPeriod - 1), i + 1),
											standardDeviations));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, ubb);
					algorithmCruncherDao.save(request, response);
				}
			}

			request.addParam("EVAL_PERIOD", "MINUTE");
			request.addParam(GlobalConstant.IDENTIFIER, "TRADE_SIGNAL");
			request.addParam("TRADE_SIGNAL", "UpperBollingerBand");
			algorithmCruncherDao.items(request, response);

			for (Properties p : (List<Properties>) response.getParam(GlobalConstant.ITEMS)) {

				UBB ubb;
				String symbol = p.getProperty("SYMBOL");
				BigDecimal standardDeviations = (BigDecimal) p.get("STANDARD_DEVIATIONS");
				String ubbType = p.getProperty("UBB_TYPE");
				int ubbPeriod = Integer.valueOf(ubbType.substring(0, ubbType.indexOf("-")));

				request.addParam(GlobalConstant.SYMBOL, symbol);
				algorithmCruncherDao.getRecentAssetMinutes(request, response);
				List<AssetMinute> assetMinutes = (List<AssetMinute>) response.getParam(GlobalConstant.ITEMS);
				List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

				int i = assetMinutes.size() - 1;
				for (AssetMinute assetMinute : assetMinutes)
					assetMinuteValues.add(assetMinute.getValue());

				ubb = new UBB(symbol);

				request.addParam(GlobalConstant.IDENTIFIER, "UBB");
				request.addParam(GlobalConstant.TYPE, ubbType);
				request.addParam("STANDARD_DEVIATIONS", standardDeviations);
				request.addParam(GlobalConstant.SYMBOL, symbol);
				request.addParam(GlobalConstant.EPOCHSECONDS,
						assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
				algorithmCruncherDao.itemCount(request, response);

				if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 0
						&& i >= ubbPeriod) {
					ubb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
					ubb.setSymbol(symbol);
					ubb.setType(ubbType);
					ubb.setStandardDeviations(standardDeviations);

					request.addParam(GlobalConstant.IDENTIFIER, "SMA");
					request.addParam(GlobalConstant.SYMBOL, symbol);
					request.addParam(GlobalConstant.TYPE, ubbType);
					request.addParam(GlobalConstant.EPOCHSECONDS, assetMinutes.get(i).getEpochSeconds());

					try {
						algorithmCruncherDao.item(request, response);
						ubb.setValue(
								UBB.calculateUBB(
										assetMinuteValues.subList(i - (ubbPeriod - 1), i + 1),
										((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
										standardDeviations));
					} catch (Exception e) {
						if (e.getMessage().equals("No entity found for query"))
							ubb.setValue(
									UBB.calculateUBB(
											assetMinuteValues.subList(i - (ubbPeriod - 1), i + 1),
											standardDeviations));
						else
							System.out.println(e.getMessage());
					}

					request.addParam(GlobalConstant.ITEM, ubb);
					algorithmCruncherDao.save(request, response);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
