package org.toasthub.analysis.algorithm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

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

	public static final int START_OF_2022 = 1640998860;

	// Constructors
	public AlgorithmCruncherSvcImpl() {
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
				break;
			case "LOAD":
				loadStockData(request, response);
				loadCryptoData(request, response);
				loadAlgs(request, response);
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
			System.out.println("Database is currently running skipping this time");
			return;
		}
		new Thread(() -> {
			tradeAnalysisJobRunning.set(true);
			process(request, response);
			tradeAnalysisJobRunning.set(false);
		}).start();

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
						.ofInstant(Instant.ofEpochSecond(START_OF_2022), ZoneId.of("America/New_York"))
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

				request.addParam(GlobalConstant.ITEMS, stockDays);
				algorithmCruncherDao.saveAll(request, response);
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
						.ofInstant(Instant.ofEpochSecond(START_OF_2022), ZoneId.of("America/New_York"))
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

				request.addParam(GlobalConstant.ITEMS, cryptoDays);
				algorithmCruncherDao.saveAll(request, response);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadStockData(Request request, Response response) {
		try {
			for (String stockName : Symbol.STOCKSYMBOLS) {
				ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York")).minusSeconds(60 * 20);

				Set<AssetMinute> preExistingStockMinutes = new LinkedHashSet<AssetMinute>();

				List<StockBar> stockBars = alpacaAPI.stockMarketData().getBars(stockName,
						today.truncatedTo(ChronoUnit.DAYS),
						today,
						null,
						null,
						1,
						BarTimePeriod.MINUTE,
						BarAdjustment.SPLIT,
						BarFeed.SIP).getBars();

				List<StockBar> stockBar = alpacaAPI.stockMarketData().getBars(stockName,
						today.truncatedTo(ChronoUnit.DAYS),
						today,
						null,
						null,
						1,
						BarTimePeriod.DAY,
						BarAdjustment.SPLIT,
						BarFeed.SIP).getBars();

				if (stockBar == null) {
					return;
				}

				AssetDay stockDay = new AssetDay();
				Set<AssetMinute> stockMinutes = new LinkedHashSet<AssetMinute>();

				request.addParam(GlobalConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(GlobalConstant.SYMBOL, stockName);
				request.addParam(GlobalConstant.TYPE, "AssetDay");
				request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");

				try {
					algorithmCruncherDao.initializedAssetDay(request, response);
					stockDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
					preExistingStockMinutes = stockDay.getAssetMinutes();
				} catch (NoResultException e) {
					stockDay.setSymbol(stockName);
					stockDay.setEpochSeconds(today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				}

				stockDay.setHigh(new BigDecimal(stockBar.get(0).getHigh()));
				stockDay.setClose(new BigDecimal(stockBar.get(0).getClose()));
				stockDay.setLow(new BigDecimal(stockBar.get(0).getLow()));
				stockDay.setOpen(new BigDecimal(stockBar.get(0).getOpen()));
				stockDay.setVolume(stockBar.get(0).getVolume());
				stockDay.setVwap(new BigDecimal(stockBar.get(0).getVwap()));

				for (int i = preExistingStockMinutes.size(); i < stockBars.size(); i++) {
					AssetMinute stockMinute = new AssetMinute();
					stockMinute.setAssetDay(stockDay);
					stockMinute.setSymbol(stockName);
					stockMinute.setEpochSeconds(stockBars.get(i).getTimestamp().toEpochSecond());
					stockMinute.setValue(new BigDecimal(stockBars.get(i).getClose()));
					stockMinute.setVolume(stockBars.get(i).getVolume());
					stockMinute.setVwap(new BigDecimal(stockBars.get(i).getVwap()));
					stockMinutes.add(stockMinute);
				}

				stockMinutes.addAll(preExistingStockMinutes);

				stockDay.setAssetMinutes(stockMinutes);
				request.addParam(GlobalConstant.ITEM, stockDay);
				algorithmCruncherDao.save(request, response);
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
				ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York"));
				Set<AssetMinute> preExistingCryptoMinutes = new LinkedHashSet<AssetMinute>();

				List<CryptoBar> cryptoBars = alpacaAPI.cryptoMarketData().getBars(cryptoName,
						exchanges,
						today.truncatedTo(ChronoUnit.DAYS),
						1500,
						null,
						1,
						BarTimePeriod.MINUTE).getBars();

				List<CryptoBar> cryptoBar = alpacaAPI.cryptoMarketData().getBars(cryptoName,
						exchanges,
						today.truncatedTo(ChronoUnit.DAYS),
						1,
						null,
						1,
						BarTimePeriod.DAY).getBars();

				if (cryptoBar == null) {
					return;
				}

				AssetDay cryptoDay = new AssetDay();
				Set<AssetMinute> cryptoMinutes = new LinkedHashSet<AssetMinute>();

				request.addParam(GlobalConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(GlobalConstant.SYMBOL, cryptoName);
				request.addParam(GlobalConstant.TYPE, "AssetDay");
				request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");

				try {
					algorithmCruncherDao.initializedAssetDay(request, response);
					cryptoDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
					preExistingCryptoMinutes = cryptoDay.getAssetMinutes();
				} catch (NoResultException e) {
					cryptoDay.setSymbol(cryptoName);
					cryptoDay.setEpochSeconds(today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				}

				cryptoDay.setHigh(BigDecimal.valueOf(cryptoBar.get(0).getHigh()));
				cryptoDay.setClose(BigDecimal.valueOf(cryptoBar.get(0).getClose()));
				cryptoDay.setLow(BigDecimal.valueOf(cryptoBar.get(0).getLow()));
				cryptoDay.setOpen(BigDecimal.valueOf(cryptoBar.get(0).getOpen()));
				cryptoDay.setVolume(cryptoBar.get(0).getVolume().longValue());
				cryptoDay.setVwap(BigDecimal.valueOf(cryptoBar.get(0).getVwap()));
				cryptoDay.setLastUpdated(today.truncatedTo(ChronoUnit.MINUTES).toEpochSecond());

				for (int i = preExistingCryptoMinutes.size(); i < cryptoBars.size(); i++) {
					AssetMinute cryptoMinute = new AssetMinute();
					cryptoMinute.setAssetDay(cryptoDay);
					cryptoMinute.setSymbol(cryptoName);
					cryptoMinute.setEpochSeconds(cryptoBars.get(i).getTimestamp().toEpochSecond());
					cryptoMinute.setValue(BigDecimal.valueOf(cryptoBars.get(i).getClose()));
					cryptoMinute.setVolume(cryptoBars.get(i).getVolume().longValue());
					cryptoMinute.setVwap(BigDecimal.valueOf(cryptoBars.get(i).getVwap()));
					cryptoMinutes.add(cryptoMinute);
				}

				cryptoMinutes.addAll(preExistingCryptoMinutes);

				cryptoDay.setAssetMinutes(cryptoMinutes);
				request.addParam(GlobalConstant.ITEM, cryptoDay);
				algorithmCruncherDao.save(request, response);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadAlgs(Request request, Response response) {
		request.addParam("EVALUATION_PERIOD", "DAY");
		request.addParam(GlobalConstant.IDENTIFIER, "TECHNICAL_INDICATOR");

		try {
			algorithmCruncherDao.items(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Set<SMA> smaSetDay = new HashSet<SMA>();
		Set<LBB> lbbSetDay = new HashSet<LBB>();
		Set<UBB> ubbSetDay = new HashSet<UBB>();

		if (response.getParam("SMA_SET") != null) {
			for (Object obj : Set.class.cast(response.getParam("SMA_SET"))) {
				smaSetDay.add((SMA) obj);
			}
		}

		if (response.getParam("LBB_SET") != null) {
			for (Object obj : Set.class.cast(response.getParam("LBB_SET"))) {
				lbbSetDay.add((LBB) obj);
			}
		}

		if (response.getParam("UBB_SET") != null) {
			for (Object obj : Set.class.cast(response.getParam("UBB_SET"))) {
				ubbSetDay.add((UBB) obj);
			}
		}

		Stream.of(Symbol.SYMBOLS).forEach(symbol -> {

			request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
			request.addParam(GlobalConstant.SYMBOL, symbol);
			try {
				algorithmCruncherDao.items(request, response);
			} catch (Exception e) {
				e.printStackTrace();
			}

			List<AssetDay> assetDays = new ArrayList<AssetDay>();
			List<BigDecimal> assetDaysValues = new ArrayList<BigDecimal>();

			for (Object obj : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
				AssetDay assetDay = AssetDay.class.cast(obj);
				assetDays.add(assetDay);
				assetDaysValues.add(assetDay.getClose());
			}

			int i = assetDays.size() - 1;

			if (i < 0)
				return;

			List<SMA> smaListDay = new ArrayList<SMA>();
			smaSetDay.stream()
					.filter(sma -> sma.getSymbol().equals(symbol))
					.forEach(sma -> {

						int smaPeriod = Integer.valueOf(sma.getType().substring(0, sma.getType().indexOf("-")));

						if (i < smaPeriod) {
							return;
						}

						request.addParam(GlobalConstant.IDENTIFIER, "SMA");
						request.addParam(GlobalConstant.TYPE, sma.getType());
						request.addParam(GlobalConstant.SYMBOL, symbol);
						request.addParam(GlobalConstant.EPOCHSECONDS,
								assetDays.get(i).getLastUpdated());

						try {
							algorithmCruncherDao.itemCount(request, response);
						} catch (Exception e) {
							e.printStackTrace();
						}

						if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) != 0) {
							return;
						}

						sma.setEpochSeconds(assetDays.get(i).getLastUpdated());
						sma.setSymbol(symbol);
						sma.setType(sma.getType());
						sma.setCorrespondingDay(assetDays.get(i).getEpochSeconds());

						sma.setValue(SMA.calculateSMA(assetDaysValues.subList(i - (smaPeriod - 1), i + 1)));
						smaListDay.add(sma);
					});

			request.addParam(GlobalConstant.ITEMS, smaListDay);
			algorithmCruncherDao.saveAll(request, response);

			List<LBB> lbbListDay = new ArrayList<LBB>();
			lbbSetDay.stream()
					.filter(lbb -> lbb.getSymbol().equals(symbol))
					.forEach(lbb -> {

						int lbbPeriod = Integer.valueOf(lbb.getType().substring(0, lbb.getType().indexOf("-")));

						if (i < lbbPeriod) {
							return;
						}

						String lbbType = lbb.getType();
						BigDecimal standardDeviations = lbb.getStandardDeviations();

						request.addParam(GlobalConstant.IDENTIFIER, "LBB");
						request.addParam(GlobalConstant.TYPE, lbbType);
						request.addParam("STANDARD_DEVIATIONS", standardDeviations);
						request.addParam(GlobalConstant.SYMBOL, symbol);
						request.addParam(GlobalConstant.EPOCHSECONDS,
								assetDays.get(i).getLastUpdated());

						try {
							algorithmCruncherDao.itemCount(request, response);
						} catch (Exception e) {
							e.printStackTrace();
						}

						if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
							return;
						}

						lbb.setEpochSeconds(assetDays.get(i).getLastUpdated());
						lbb.setSymbol(symbol);
						lbb.setType(lbbType);
						lbb.setStandardDeviations(standardDeviations);
						lbb.setCorrespondingDay(assetDays.get(i).getEpochSeconds());

						request.addParam(GlobalConstant.IDENTIFIER, "SMA");
						request.addParam(GlobalConstant.SYMBOL, symbol);
						request.addParam(GlobalConstant.TYPE, lbbType);
						request.addParam(GlobalConstant.EPOCHSECONDS, lbb.getEpochSeconds());

						try {
							algorithmCruncherDao.item(request, response);
							lbb.setValue(
									LBB.calculateLBB(
											assetDaysValues.subList(i - (lbbPeriod - 1), i + 1),
											((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
											standardDeviations));
						} catch (NoResultException e) {
							lbb.setValue(
									LBB.calculateLBB(
											assetDaysValues.subList(i - (lbbPeriod - 1), i + 1),
											standardDeviations));
						}
						lbbListDay.add(lbb);
					});

			request.addParam(GlobalConstant.ITEMS, lbbListDay);
			algorithmCruncherDao.saveAll(request, response);

			List<UBB> ubbListDay = new ArrayList<UBB>();
			ubbSetDay.stream()
					.filter(ubb -> ubb.getSymbol().equals(symbol))
					.forEach(ubb -> {

						String ubbType = ubb.getType();
						int ubbPeriod = Integer.valueOf(ubbType.substring(0, ubbType.indexOf("-")));

						if (i < ubbPeriod) {
							return;
						}

						BigDecimal standardDeviations = ubb.getStandardDeviations();

						request.addParam(GlobalConstant.IDENTIFIER, "UBB");
						request.addParam(GlobalConstant.TYPE, ubbType);
						request.addParam("STANDARD_DEVIATIONS", standardDeviations);
						request.addParam(GlobalConstant.SYMBOL, symbol);
						request.addParam(GlobalConstant.EPOCHSECONDS,
								assetDays.get(i).getLastUpdated());

						try {
							algorithmCruncherDao.itemCount(request, response);
						} catch (Exception e) {
							e.printStackTrace();
						}

						if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
							return;
						}

						ubb.setEpochSeconds(assetDays.get(i).getLastUpdated());
						ubb.setSymbol(symbol);
						ubb.setType(ubbType);
						ubb.setStandardDeviations(standardDeviations);
						ubb.setCorrespondingDay(assetDays.get(i).getEpochSeconds());

						request.addParam(GlobalConstant.IDENTIFIER, "SMA");
						request.addParam(GlobalConstant.SYMBOL, symbol);
						request.addParam(GlobalConstant.TYPE, ubbType);
						request.addParam(GlobalConstant.EPOCHSECONDS, ubb.getEpochSeconds());

						try {
							algorithmCruncherDao.item(request, response);
							ubb.setValue(
									UBB.calculateUBB(
											assetDaysValues.subList(i - (ubbPeriod - 1), i + 1),
											((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
											standardDeviations));
						} catch (NoResultException e) {
							ubb.setValue(
									UBB.calculateUBB(
											assetDaysValues.subList(i - (ubbPeriod - 1), i + 1),
											standardDeviations));
						}
						ubbListDay.add(ubb);
					});

			request.addParam(GlobalConstant.ITEMS, ubbListDay);
			algorithmCruncherDao.saveAll(request, response);
		});

		request.addParam("EVALUATION_PERIOD", "MINUTE");
		request.addParam(GlobalConstant.IDENTIFIER, "TECHNICAL_INDICATOR");

		try {
			algorithmCruncherDao.items(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Set<SMA> smaSetMinute = new HashSet<SMA>();
		Set<LBB> lbbSetMinute = new HashSet<LBB>();
		Set<UBB> ubbSetMinute = new HashSet<UBB>();

		if (response.getParam("SMA_SET") != null) {
			for (Object obj : Set.class.cast(response.getParam("SMA_SET"))) {
				smaSetMinute.add((SMA) obj);
			}
		}

		if (response.getParam("LBB_SET") != null) {
			for (Object obj : Set.class.cast(response.getParam("LBB_SET"))) {
				lbbSetMinute.add((LBB) obj);
			}
		}

		if (response.getParam("UBB_SET") != null) {
			for (Object obj : Set.class.cast(response.getParam("UBB_SET"))) {
				ubbSetMinute.add((UBB) obj);
			}
		}

		Stream.of(Symbol.SYMBOLS).forEach(symbol -> {

			request.addParam(GlobalConstant.SYMBOL, symbol);
			algorithmCruncherDao.getRecentAssetMinutes(request, response);

			List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
			List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

			for (Object obj : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
				AssetMinute assetMinute = AssetMinute.class.cast(obj);
				assetMinutes.add(assetMinute);
				assetMinuteValues.add(assetMinute.getValue());
			}

			int i = assetMinutes.size() - 1;

			if (i < 0)
				return;

			List<SMA> smaListMinute = new ArrayList<SMA>();
			smaSetMinute.stream()
					.filter(sma -> sma.getSymbol().equals(symbol))
					.forEach(sma -> {

						int smaPeriod = Integer.valueOf(sma.getType().substring(0, sma.getType().indexOf("-")));

						if (i < smaPeriod) {
							return;
						}

						request.addParam(GlobalConstant.IDENTIFIER, "SMA");
						request.addParam(GlobalConstant.TYPE, sma.getType());
						request.addParam(GlobalConstant.SYMBOL, symbol);
						request.addParam(GlobalConstant.EPOCHSECONDS,
								assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
						try {
							algorithmCruncherDao.itemCount(request, response);
						} catch (Exception e) {
							e.printStackTrace();
						}

						if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
							return;
						}

						sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
						sma.setSymbol(symbol);
						sma.setType(sma.getType());

						sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - (smaPeriod - 1), i + 1)));
						smaListMinute.add(sma);
					});

			request.addParam(GlobalConstant.ITEMS, smaListMinute);
			algorithmCruncherDao.saveAll(request, response);

			List<LBB> lbbListMinute = new ArrayList<LBB>();
			lbbSetMinute.stream()
					.filter(lbb -> lbb.getSymbol().equals(symbol))
					.forEach(lbb -> {

						String lbbType = lbb.getType();
						BigDecimal standardDeviations = lbb.getStandardDeviations();
						int lbbPeriod = Integer.valueOf(lbb.getType().substring(0, lbb.getType().indexOf("-")));

						if (i < lbbPeriod) {
							return;
						}

						request.addParam(GlobalConstant.IDENTIFIER, "LBB");
						request.addParam(GlobalConstant.TYPE, lbbType);
						request.addParam("STANDARD_DEVIATIONS", standardDeviations);
						request.addParam(GlobalConstant.SYMBOL, symbol);
						request.addParam(GlobalConstant.EPOCHSECONDS,
								assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
						try {
							algorithmCruncherDao.itemCount(request, response);
						} catch (Exception e) {
							e.printStackTrace();
						}

						if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
							return;
						}

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
						} catch (NoResultException e) {
							lbb.setValue(
									LBB.calculateLBB(
											assetMinuteValues.subList(i - (lbbPeriod - 1), i + 1),
											standardDeviations));
						}
						lbbListMinute.add(lbb);
					});

			request.addParam(GlobalConstant.ITEMS, lbbListMinute);
			algorithmCruncherDao.saveAll(request, response);

			List<UBB> ubbListMinute = new ArrayList<UBB>();
			ubbSetMinute.stream()
					.filter(ubb -> ubb.getSymbol().equals(symbol))
					.forEach(ubb -> {

						String ubbType = ubb.getType();
						BigDecimal standardDeviations = ubb.getStandardDeviations();
						int ubbPeriod = Integer.valueOf(ubb.getType().substring(0, ubb.getType().indexOf("-")));

						if (i < ubbPeriod) {
							return;
						}

						request.addParam(GlobalConstant.IDENTIFIER, "UBB");
						request.addParam(GlobalConstant.TYPE, ubbType);
						request.addParam("STANDARD_DEVIATIONS", standardDeviations);
						request.addParam(GlobalConstant.SYMBOL, symbol);
						request.addParam(GlobalConstant.EPOCHSECONDS,
								assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
						try {
							algorithmCruncherDao.itemCount(request, response);
						} catch (Exception e) {
							e.printStackTrace();
						}

						if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
							return;
						}
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
						} catch (NoResultException e) {
							ubb.setValue(
									UBB.calculateUBB(
											assetMinuteValues.subList(i - (ubbPeriod - 1), i + 1),
											standardDeviations));
						}
						ubbListMinute.add(ubb);
					});

			request.addParam(GlobalConstant.ITEMS, ubbListMinute);
			algorithmCruncherDao.saveAll(request, response);
		});
	}
}
