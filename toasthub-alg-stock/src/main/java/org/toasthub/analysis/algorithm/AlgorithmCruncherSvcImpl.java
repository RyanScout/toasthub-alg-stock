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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.model.Configuration;
import org.toasthub.model.Symbol;
import org.toasthub.model.TechnicalIndicator;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.bytebuddy.agent.builder.AgentBuilder.CircularityLock.Global;
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

	final AtomicBoolean databaseIsBackloaded = new AtomicBoolean(false);

	public static final int START_OF_2022 = 1640998860;

	// Constructors
	public AlgorithmCruncherSvcImpl() {
	}

	@Override
	public void process(final Request request, final Response response) {
		final String action = (String) request.getParams().get("action");

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
			case "BACKLOAD_ALG":
				backloadAlg(request, response);
				break;
			default:
				break;
		}

	}

	@Override
	public void delete(final Request request, final Response response) {
		try {
			algorithmCruncherDao.delete(request, response);
			algorithmCruncherDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				algorithmCruncherDao.items(request, response);
			}
			response.setStatus(Response.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void item(final Request request, final Response response) {
		try {
			algorithmCruncherDao.item(request, response);
			response.setStatus(Response.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void items(final Request request, final Response response) {
		try {
			algorithmCruncherDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				algorithmCruncherDao.items(request, response);
			}
			response.setStatus(Response.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void dataBaseBackLoader() {
		final Request request = new Request();
		final Response response = new Response();

		request.addParam(GlobalConstant.IDENTIFIER, "CONFIGURATION");
		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		Configuration config = new Configuration();

		if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {

			try {
				algorithmCruncherDao.item(request, response);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			config = Configuration.class.cast(response.getParam(GlobalConstant.ITEM));
		}

		if (!config.isBackloaded()) {
			request.addParam("action", "BACKLOAD");
			process(request, response);
		}

		config.setBackloaded(true);
		request.addParam(GlobalConstant.ITEM, config);
		try {
			algorithmCruncherDao.save(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		databaseIsBackloaded.set(true);
	}

	@Scheduled(cron = "0 * * * * ?")
	public void dataBaseUpdater() {

		if (!databaseIsBackloaded.get()) {
			System.out.println("Database is not backloaded yet");
			return;
		}

		if (tradeAnalysisJobRunning.get()) {
			System.out.println("Database is currently running skipping this time");
			return;
		}

		new Thread(() -> {
			tradeAnalysisJobRunning.set(true);

			final Request request = new Request();
			final Response response = new Response();

			request.addParam("action", "LOAD");
			process(request, response);

			tradeAnalysisJobRunning.set(false);
		}).start();
	}

	@Override
	public void save(final Request request, final Response response) {
		// TODO Auto-generated method stub

	}

	public void backloadStockData(final Request request, final Response response) {
		try {
			for (final String stockName : Symbol.STOCKSYMBOLS) {
				List<StockBar> stockBars;
				final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime first = ZonedDateTime
						.ofInstant(Instant.ofEpochSecond(START_OF_2022), ZoneId.of("America/New_York"))
						.truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime second = first.plusDays(1).minusMinutes(1);
				AssetDay stockDay;
				AssetMinute stockMinute;
				Set<AssetMinute> stockMinutes;
				final List<AssetDay> stockDays = new ArrayList<AssetDay>();
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
					} catch (final Exception e) {
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
					} catch (final Exception e) {
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

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void backloadCryptoData(final Request request, final Response response) {
		try {
			final Collection<Exchange> exchanges = new ArrayList<Exchange>();
			exchanges.add(Exchange.COINBASE);

			for (final String cryptoName : Symbol.CRYPTOSYMBOLS) {
				List<CryptoBar> cryptoBars;
				final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
				ZonedDateTime first = ZonedDateTime
						.ofInstant(Instant.ofEpochSecond(START_OF_2022), ZoneId.of("America/New_York"))
						.truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime second = first.plusDays(1);
				AssetDay cryptoDay;
				AssetMinute cryptoMinute;
				Set<AssetMinute> cryptoMinutes;
				final List<AssetDay> cryptoDays = new ArrayList<AssetDay>();
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
					} catch (final Exception e) {
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
					} catch (final Exception e) {
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

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void loadStockData(final Request request, final Response response) {
		try {
			for (final String stockName : Symbol.STOCKSYMBOLS) {
				final ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York")).minusSeconds(60 * 20);

				Set<AssetMinute> preExistingStockMinutes = new LinkedHashSet<AssetMinute>();

				final List<StockBar> stockBars = alpacaAPI.stockMarketData().getBars(stockName,
						today.truncatedTo(ChronoUnit.DAYS),
						today,
						null,
						null,
						1,
						BarTimePeriod.MINUTE,
						BarAdjustment.SPLIT,
						BarFeed.SIP).getBars();

				final List<StockBar> stockBar = alpacaAPI.stockMarketData().getBars(stockName,
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
				final Set<AssetMinute> stockMinutes = new LinkedHashSet<AssetMinute>();

				request.addParam(GlobalConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(GlobalConstant.SYMBOL, stockName);
				request.addParam(GlobalConstant.TYPE, "AssetDay");
				request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");

				try {
					algorithmCruncherDao.initializedAssetDay(request, response);
					stockDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
					preExistingStockMinutes = stockDay.getAssetMinutes();
				} catch (final NoResultException e) {
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
					final AssetMinute stockMinute = new AssetMinute();
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
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void loadCryptoData(final Request request, final Response response) {
		try {
			final Collection<Exchange> exchanges = new ArrayList<Exchange>();
			exchanges.add(Exchange.COINBASE);

			for (final String cryptoName : Symbol.CRYPTOSYMBOLS) {
				final ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York"));
				Set<AssetMinute> preExistingCryptoMinutes = new LinkedHashSet<AssetMinute>();

				final List<CryptoBar> cryptoBars = alpacaAPI.cryptoMarketData().getBars(cryptoName,
						exchanges,
						today.truncatedTo(ChronoUnit.DAYS),
						1500,
						null,
						1,
						BarTimePeriod.MINUTE).getBars();

				final List<CryptoBar> cryptoBar = alpacaAPI.cryptoMarketData().getBars(cryptoName,
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
				final Set<AssetMinute> cryptoMinutes = new LinkedHashSet<AssetMinute>();

				request.addParam(GlobalConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(GlobalConstant.SYMBOL, cryptoName);
				request.addParam(GlobalConstant.TYPE, "AssetDay");
				request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");

				try {
					algorithmCruncherDao.initializedAssetDay(request, response);
					cryptoDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
					preExistingCryptoMinutes = cryptoDay.getAssetMinutes();
				} catch (final NoResultException e) {
					cryptoDay.setSymbol(cryptoName);
					cryptoDay.setEpochSeconds(today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				}

				cryptoDay.setHigh(BigDecimal.valueOf(cryptoBar.get(0).getHigh()));
				cryptoDay.setClose(BigDecimal.valueOf(cryptoBar.get(0).getClose()));
				cryptoDay.setLow(BigDecimal.valueOf(cryptoBar.get(0).getLow()));
				cryptoDay.setOpen(BigDecimal.valueOf(cryptoBar.get(0).getOpen()));
				cryptoDay.setVolume(cryptoBar.get(0).getVolume().longValue());
				cryptoDay.setVwap(BigDecimal.valueOf(cryptoBar.get(0).getVwap()));

				for (int i = preExistingCryptoMinutes.size(); i < cryptoBars.size(); i++) {
					final AssetMinute cryptoMinute = new AssetMinute();
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
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void loadAlgs(final Request request, final Response response) {
		request.addParam(GlobalConstant.IDENTIFIER, "TECHNICAL_INDICATOR");

		try {
			algorithmCruncherDao.items(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final Set<SMA> smaSet = new HashSet<SMA>();
		final Set<LBB> lbbSet = new HashSet<LBB>();
		final Set<UBB> ubbSet = new HashSet<UBB>();

		if (response.getParam("SMA_SET") != null) {
			for (final Object obj : Set.class.cast(response.getParam("SMA_SET"))) {
				smaSet.add((SMA) obj);
			}
		}

		if (response.getParam("LBB_SET") != null) {
			for (final Object obj : Set.class.cast(response.getParam("LBB_SET"))) {
				lbbSet.add((LBB) obj);
			}
		}

		if (response.getParam("UBB_SET") != null) {
			for (final Object obj : Set.class.cast(response.getParam("UBB_SET"))) {
				ubbSet.add((UBB) obj);
			}
		}

		Stream.of(Symbol.SYMBOLS).forEach(symbol -> {

			final List<AssetDay> assetDays = new ArrayList<AssetDay>();
			final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();

			final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));

			request.addParam("STARTING_EPOCH_SECONDS", now.minusDays(1000).toEpochSecond());
			request.addParam("ENDING_EPOCH_SECONDS", now.plusDays(1000).toEpochSecond());
			request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
			request.addParam(GlobalConstant.SYMBOL, symbol);

			try {
				algorithmCruncherDao.items(request, response);
			} catch (final Exception e) {
				e.printStackTrace();
			}

			for (final Object obj : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
				final AssetDay assetDay = AssetDay.class.cast(obj);
				assetDays.add(assetDay);
			}

			assetDays.sort((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()));

			if (assetDays.size() - 1 < 0)
				return;

			request.addParam("STARTING_EPOCH_SECONDS", now.minusMinutes(1000).toEpochSecond());
			request.addParam("ENDING_EPOCH_SECONDS", now.plusMinutes(1000).toEpochSecond());
			request.addParam(GlobalConstant.IDENTIFIER, "AssetMinute");
			request.addParam(GlobalConstant.SYMBOL, symbol);

			try {
				algorithmCruncherDao.items(request, response);
			} catch (final Exception e) {
				e.printStackTrace();
			}

			for (final Object obj : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
				final AssetMinute assetMinute = AssetMinute.class.cast(obj);
				assetMinutes.add(assetMinute);
			}

			assetMinutes.sort((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()));

			if (assetMinutes.size() - 1 < 0)
				return;

			final List<SMA> smaList = new ArrayList<SMA>();
			smaSet.stream()
					.filter(sma -> sma.getSymbol().equals(symbol))
					.forEach(sma -> {

						final String evaluationPeriod = sma.getType().substring(sma.getType().indexOf("-") + 1);

						request.addParam(GlobalConstant.ITEM, sma);
						request.addParam("SUCCESSFUL", false);

						switch (evaluationPeriod) {
							case "day":
								request.addParam("RECENT_ASSET_MINUTE", assetMinutes.get(assetMinutes.size() - 1));
								request.addParam(GlobalConstant.ITEMS, assetDays);
								configureSMADay(request, response);
								break;
							case "minute":
								request.addParam(GlobalConstant.ITEMS, assetMinutes);
								configureSMAMinute(request, response);
								break;
							default:
								System.out.println("Invalid evaluationPeriod");
								return;
						}

						if ((boolean) request.getParam("SUCCESSFUL")) {
							smaList.add(sma);
						}

					});

			request.addParam(GlobalConstant.ITEMS, smaList);
			algorithmCruncherDao.saveAll(request, response);

			final List<LBB> lbbList = new ArrayList<LBB>();
			lbbSet.stream()
					.filter(lbb -> lbb.getSymbol().equals(symbol))
					.forEach(lbb -> {

						final String evaluationPeriod = lbb.getType().substring(lbb.getType().indexOf("-") + 1);

						request.addParam(GlobalConstant.ITEM, lbb);
						request.addParam("SUCCESSFUL", false);

						switch (evaluationPeriod) {
							case "day":
								request.addParam("RECENT_ASSET_MINUTE", assetMinutes.get(assetMinutes.size() - 1));
								request.addParam(GlobalConstant.ITEMS, assetDays);
								configureLBBDay(request, response);
								break;
							case "minute":
								request.addParam(GlobalConstant.ITEMS, assetMinutes);
								configureLBBMinute(request, response);
								break;
							default:
								System.out.println("Invalid evaluationPeriod");
								return;
						}

						if ((boolean) request.getParam("SUCCESSFUL")) {
							lbbList.add(lbb);
						}
					});

			request.addParam(GlobalConstant.ITEMS, lbbList);
			algorithmCruncherDao.saveAll(request, response);

			final List<UBB> ubbList = new ArrayList<UBB>();
			ubbSet.stream()
					.filter(ubb -> ubb.getSymbol().equals(symbol))
					.forEach(ubb -> {

						final String evaluationPeriod = ubb.getType().substring(ubb.getType().indexOf("-") + 1);

						request.addParam(GlobalConstant.ITEM, ubb);
						request.addParam("SUCCESSFUL", false);

						switch (evaluationPeriod) {
							case "day":
								request.addParam("RECENT_ASSET_MINUTE", assetMinutes.get(assetMinutes.size() - 1));
								request.addParam(GlobalConstant.ITEMS, assetDays);
								configureUBBDay(request, response);
								break;
							case "minute":
								request.addParam(GlobalConstant.ITEMS, assetMinutes);
								configureUBBMinute(request, response);
								break;
							default:
								System.out.println("Invalid evaluationPeriod");
								return;
						}

						if ((boolean) request.getParam("SUCCESSFUL")) {
							ubbList.add(ubb);
						}
					});

			request.addParam(GlobalConstant.ITEMS, ubbList);
			algorithmCruncherDao.saveAll(request, response);
		});
	}

	public void backloadAlg(final Request request, final Response response) {
		switch ((String) request.getParam("TECHNICAL_INDICATOR_TYPE")) {
			case TechnicalIndicator.GOLDENCROSS:
				backloadSMA(request, response);
				break;
			case TechnicalIndicator.LOWERBOLLINGERBAND:
				break;
			case TechnicalIndicator.UPPERBOLLINGERBAND:
				break;
			default:
				System.out.println("INVALID TECHINCAL INDICATOR TYPE AT ALGORITHMCRUCNHERSVC BACKLOADALG");
				break;
		}
	}

	public void backloadSMA(Request request, Response response) {
		String technicalIndicatorType = (String) request.getParam("TECHNICAL_INDICATOR_KEY");
		String shortSMAType = technicalIndicatorType.substring(0, technicalIndicatorType.indexOf(":"));
		String longSMAType = technicalIndicatorType.substring(technicalIndicatorType.indexOf(":") + 1);
		String symbol = (String) request.getParam("SYMBOL");

		request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		request.addParam(GlobalConstant.TYPE, shortSMAType);
		request.addParam(GlobalConstant.SYMBOL, symbol);

		algorithmCruncherDao.getEarliestAlgTime(request, response);
		long end = (long) response.getParam(GlobalConstant.ITEM);

		
	}

	public void configureSMAMinute(final Request request, final Response response) {
		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
		final List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

		final SMA sma = (SMA) request.getParam(GlobalConstant.ITEM);
		final String symbol = sma.getSymbol();
		final String smaType = sma.getType();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetMinute assetMinute = AssetMinute.class.cast(obj);
			assetMinutes.add(assetMinute);
			assetMinuteValues.add(assetMinute.getValue());
		}

		final int i = assetMinutes.size() - 1;
		final int smaPeriod = Integer.valueOf(smaType.substring(0, smaType.indexOf("-")));

		if (i < smaPeriod) {
			return;
		}

		request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		request.addParam(GlobalConstant.TYPE, smaType);
		request.addParam(GlobalConstant.SYMBOL, symbol);
		request.addParam(GlobalConstant.EPOCHSECONDS,
				assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());
		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}

		sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		sma.setSymbol(symbol);
		sma.setType(smaType);

		sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - (smaPeriod - 1), i + 1)));

		request.addParam("SUCCESSFUL", true);
	}

	public void configureLBBMinute(final Request request, final Response response) {
		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
		final List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

		final LBB lbb = (LBB) request.getParam(GlobalConstant.ITEM);
		final String symbol = lbb.getSymbol();
		final String lbbType = lbb.getType();
		final BigDecimal standardDeviations = lbb.getStandardDeviations();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetMinute assetMinute = AssetMinute.class.cast(obj);
			assetMinutes.add(assetMinute);
			assetMinuteValues.add(assetMinute.getValue());
		}

		final int i = assetMinutes.size() - 1;
		final int lbbPeriod = Integer.valueOf(lbbType.substring(0, lbbType.indexOf("-")));

		if (i < lbbPeriod) {
			return;
		}

		request.addParam(GlobalConstant.IDENTIFIER, "LBB");
		request.addParam(GlobalConstant.TYPE, lbbType);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);
		request.addParam(GlobalConstant.SYMBOL, symbol);
		request.addParam(GlobalConstant.EPOCHSECONDS,
				assetMinutes.get(i).getEpochSeconds());
		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
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
		} catch (final NoResultException e) {
			lbb.setValue(
					LBB.calculateLBB(
							assetMinuteValues.subList(i - (lbbPeriod - 1), i + 1),
							standardDeviations));
		}
		request.addParam("SUCCESSFUL", true);
	}

	public void configureUBBMinute(final Request request, final Response response) {

		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
		final List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

		final UBB ubb = (UBB) request.getParam(GlobalConstant.ITEM);
		final String symbol = ubb.getSymbol();
		final String ubbType = ubb.getType();
		final BigDecimal standardDeviations = ubb.getStandardDeviations();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetMinute assetMinute = AssetMinute.class.cast(obj);
			assetMinutes.add(assetMinute);
			assetMinuteValues.add(assetMinute.getValue());
		}

		final int i = assetMinutes.size() - 1;
		final int ubbPeriod = Integer.valueOf(ubbType.substring(0, ubbType.indexOf("-")));

		if (i < ubbPeriod) {
			return;
		}

		request.addParam(GlobalConstant.IDENTIFIER, "UBB");
		request.addParam(GlobalConstant.TYPE, ubbType);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);
		request.addParam(GlobalConstant.SYMBOL, symbol);
		request.addParam(GlobalConstant.EPOCHSECONDS,
				assetMinutes.get(i).getEpochSeconds());
		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
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
		} catch (final NoResultException e) {
			ubb.setValue(
					UBB.calculateUBB(
							assetMinuteValues.subList(i - (ubbPeriod - 1), i + 1),
							standardDeviations));
		}
		request.addParam("SUCCESSFUL", true);
	}

	public void configureSMADay(final Request request, final Response response) {
		final List<AssetDay> assetDays = new ArrayList<AssetDay>();
		final List<BigDecimal> assetDayValues = new ArrayList<BigDecimal>();
		final AssetMinute assetMinute = (AssetMinute) request.getParam("RECENT_ASSET_MINUTE");

		final SMA sma = (SMA) request.getParam(GlobalConstant.ITEM);
		final String symbol = sma.getSymbol();
		final String smaType = sma.getType();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetDay assetDay = AssetDay.class.cast(obj);
			assetDays.add(assetDay);
			assetDayValues.add(assetDay.getClose());
		}

		assetDayValues.set(assetDayValues.size() - 1, assetMinute.getValue());

		final int i = assetDays.size() - 1;
		final int smaPeriod = Integer.valueOf(smaType.substring(0, smaType.indexOf("-")));

		if (i < smaPeriod) {
			return;
		}

		request.addParam(GlobalConstant.IDENTIFIER, "SMA");
		request.addParam(GlobalConstant.TYPE, smaType);
		request.addParam(GlobalConstant.SYMBOL, symbol);
		request.addParam(GlobalConstant.EPOCHSECONDS,
				assetMinute.getEpochSeconds());

		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) != 0) {
			return;
		}

		sma.setEpochSeconds(assetMinute.getEpochSeconds());
		sma.setSymbol(symbol);
		sma.setType(smaType);
		sma.setCorrespondingDay(assetDays.get(i).getEpochSeconds());

		sma.setValue(SMA.calculateSMA(assetDayValues.subList(i - (smaPeriod - 1), i + 1)));

		request.addParam("SUCCESSFUL", true);
	}

	public void configureLBBDay(final Request request, final Response response) {
		final List<AssetDay> assetDays = new ArrayList<AssetDay>();
		final List<BigDecimal> assetDayValues = new ArrayList<BigDecimal>();
		final AssetMinute assetMinute = (AssetMinute) request.getParam("RECENT_ASSET_MINUTE");

		final LBB lbb = (LBB) request.getParam(GlobalConstant.ITEM);
		final String symbol = lbb.getSymbol();
		final String lbbType = lbb.getType();
		final BigDecimal standardDeviations = lbb.getStandardDeviations();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetDay assetDay = AssetDay.class.cast(obj);
			assetDays.add(assetDay);
			assetDayValues.add(assetDay.getClose());
		}

		assetDayValues.set(assetDayValues.size() - 1, assetMinute.getValue());

		final int i = assetDays.size() - 1;
		final int lbbPeriod = Integer.valueOf(lbbType.substring(0, lbbType.indexOf("-")));

		if (i < lbbPeriod) {
			return;
		}

		request.addParam(GlobalConstant.IDENTIFIER, "LBB");
		request.addParam(GlobalConstant.TYPE, lbbType);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);
		request.addParam(GlobalConstant.SYMBOL, symbol);
		request.addParam(GlobalConstant.EPOCHSECONDS,
				assetMinute.getEpochSeconds());

		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}

		lbb.setEpochSeconds(assetMinute.getEpochSeconds());
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
							assetDayValues.subList(i - (lbbPeriod - 1), i + 1),
							((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
							standardDeviations));
		} catch (final NoResultException e) {
			lbb.setValue(
					LBB.calculateLBB(
							assetDayValues.subList(i - (lbbPeriod - 1), i + 1),
							standardDeviations));
		}

		request.addParam("SUCCESSFUL", true);
	}

	public void configureUBBDay(final Request request, final Response response) {
		final List<AssetDay> assetDays = new ArrayList<AssetDay>();
		final List<BigDecimal> assetDayValues = new ArrayList<BigDecimal>();
		final AssetMinute assetMinute = (AssetMinute) request.getParam("RECENT_ASSET_MINUTE");

		final UBB ubb = (UBB) request.getParam(GlobalConstant.ITEM);
		final String symbol = ubb.getSymbol();
		final String ubbType = ubb.getType();
		final BigDecimal standardDeviations = ubb.getStandardDeviations();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetDay assetDay = AssetDay.class.cast(obj);
			assetDays.add(assetDay);
			assetDayValues.add(assetDay.getClose());
		}

		assetDayValues.set(assetDayValues.size() - 1, assetMinute.getValue());

		final int i = assetDays.size() - 1;
		final int ubbPeriod = Integer.valueOf(ubbType.substring(0, ubbType.indexOf("-")));

		if (i < ubbPeriod) {
			return;
		}

		request.addParam(GlobalConstant.IDENTIFIER, "UBB");
		request.addParam(GlobalConstant.TYPE, ubbType);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);
		request.addParam(GlobalConstant.SYMBOL, symbol);
		request.addParam(GlobalConstant.EPOCHSECONDS,
				assetMinute.getEpochSeconds());

		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}

		ubb.setEpochSeconds(assetMinute.getEpochSeconds());
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
							assetDayValues.subList(i - (ubbPeriod - 1), i + 1),
							((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
							standardDeviations));
		} catch (final NoResultException e) {
			ubb.setValue(
					UBB.calculateUBB(
							assetDayValues.subList(i - (ubbPeriod - 1), i + 1),
							standardDeviations));
		}

		request.addParam("SUCCESSFUL", true);
	}

}
