package org.toasthub.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarFeed;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

public class Functions {

    // all moving averages return a moving average of days yielded in stockbars

    // returns List of StockBars from past minutesPast minutes to int minutes ago (+
    // 20 min delay)
    public static List<StockBar> swingTradingBars(AlpacaAPI alpacaAPI, String stockName, String startDate, String endDate){
        int startYear = Integer.parseInt(startDate.substring(0, 4));
        int startMonth = Integer.parseInt(startDate.substring(5, 7));
        int startDay = Integer.parseInt(startDate.substring(8, 10));
        int endYear = Integer.parseInt(endDate.substring(0, 4));
        int endMonth = Integer.parseInt(endDate.substring(5, 7));
        int endDay = Integer.parseInt(endDate.substring(8, 10));
        try {
            return alpacaAPI.stockMarketData().getBars(
                    stockName,
                    ZonedDateTime.of(startYear, startMonth, startDay, 9, 30, 0, 0, ZoneId.of("America/New_York")),
                    ZonedDateTime.of(endYear, endMonth, endDay, 12 + 4, 0, 0, 0, ZoneId.of("America/New_York")),
                    null,
                    null,
                    1,
                    BarTimePeriod.DAY,
                    BarAdjustment.SPLIT,
                    BarFeed.SIP).getBars();
        } catch (AlpacaClientException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static List<StockBar> swingTradingOverlapBars(AlpacaAPI alpacaAPI, String stockName, String date) {
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        Calendar pastCalendar = Calendar.getInstance();
        pastCalendar.set(Calendar.YEAR, year);
        pastCalendar.set(Calendar.MONTH, month - 1);
        pastCalendar.set(Calendar.DAY_OF_MONTH, day);
        pastCalendar.add(Calendar.DAY_OF_MONTH, -1);
        Calendar pastCalendar2 = Calendar.getInstance();
        pastCalendar2.set(Calendar.YEAR, year);
        pastCalendar2.set(Calendar.MONTH, month - 1);
        pastCalendar2.set(Calendar.DAY_OF_MONTH, day);
        pastCalendar2.add(Calendar.DAY_OF_MONTH, -200);
        try {
            return alpacaAPI.stockMarketData().getBars(
                    stockName,
                    ZonedDateTime.of(pastCalendar2.get(Calendar.YEAR), (pastCalendar2.get(Calendar.MONTH)) + 1,
                            (pastCalendar2.get(Calendar.DAY_OF_MONTH)),9, 30, 0, 0, ZoneId.of("America/New_York")),
                    ZonedDateTime.of(pastCalendar.get(Calendar.YEAR), (pastCalendar.get(Calendar.MONTH)) + 1,
                            (pastCalendar.get(Calendar.DAY_OF_MONTH)), 12 + 4, 0, 0, 0, ZoneId.of("America/New_York")),
                    null,
                    null,
                    1,
                    BarTimePeriod.DAY,
                    BarAdjustment.SPLIT,
                    BarFeed.SIP).getBars();
        } catch (AlpacaClientException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static List<StockBar> currentStockBars(AlpacaAPI alpacaAPI, String stockName, int minutesPast) {
        try {
            return alpacaAPI.stockMarketData().getBars(
                    stockName,
                    ZonedDateTime.of(FunctionalCalendar.getPastYear(minutesPast),
                            FunctionalCalendar.getPastMonth(minutesPast),
                            FunctionalCalendar.getPastDay(minutesPast),
                            FunctionalCalendar.getPastHour(minutesPast),
                            FunctionalCalendar.getPastMinute(minutesPast), 0, 0, ZoneId.of("America/New_York")),

                    ZonedDateTime.of(FunctionalCalendar.getPastYear(20),
                            FunctionalCalendar.getPastMonth(20),
                            FunctionalCalendar.getPastDay(20),
                            FunctionalCalendar.getPastHour(20),
                            FunctionalCalendar.getPastMinute(20), 0, 0, ZoneId.of("America/New_York")),
                    null,
                    null,
                    1,
                    BarTimePeriod.MINUTE,
                    BarAdjustment.SPLIT,
                    BarFeed.SIP).getBars();
        } catch (AlpacaClientException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static List<StockBar> dayTradingBars(AlpacaAPI alpacaAPI, String stockName, String date) {
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        try {
            return alpacaAPI.stockMarketData().getBars(
                    stockName,
                    ZonedDateTime.of(year, month, day, 9, 30, 0, 0, ZoneId.of("America/New_York")),
                    ZonedDateTime.of(year, month, day, 12 + 4, 0, 0, 0, ZoneId.of("America/New_York")),
                    null,
                    null,
                    1,
                    BarTimePeriod.MINUTE,
                    BarAdjustment.SPLIT,
                    BarFeed.SIP).getBars();
        } catch (AlpacaClientException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static List<StockBar> dayTradingOverlapBars(AlpacaAPI alpacaAPI, String stockName, String date) {
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        Calendar pastCalendar = Calendar.getInstance();
        pastCalendar.set(Calendar.YEAR, year);
        pastCalendar.set(Calendar.MONTH, month - 1);
        pastCalendar.set(Calendar.DAY_OF_MONTH, day);
        pastCalendar.add(Calendar.DAY_OF_MONTH, -1);
        try {
            return alpacaAPI.stockMarketData().getBars(
                    stockName,
                    ZonedDateTime.of(pastCalendar.get(Calendar.YEAR), (pastCalendar.get(Calendar.MONTH)) + 1,
                            (pastCalendar.get(Calendar.DAY_OF_MONTH)), 12 + 2, 0, 0, 0, ZoneId.of("America/New_York")),
                    ZonedDateTime.of(pastCalendar.get(Calendar.YEAR), (pastCalendar.get(Calendar.MONTH)) + 1,
                            (pastCalendar.get(Calendar.DAY_OF_MONTH)), 12 + 4, 0, 0, 0, ZoneId.of("America/New_York")),
                    null,
                    null,
                    1,
                    BarTimePeriod.MINUTE,
                    BarAdjustment.SPLIT,
                    BarFeed.SIP).getBars();
        } catch (AlpacaClientException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static List<StockBar> backloadBars(AlpacaAPI alpacaAPI, String stockName) {
        try {
            return alpacaAPI.stockMarketData().getBars(
                    stockName,
                    ZonedDateTime.of(2021, 1, 1, 9, 30, 0, 0, ZoneId.of("America/New_York")),
                    ZonedDateTime.of(FunctionalCalendar.getPastYear(20),
                    FunctionalCalendar.getPastMonth(20),
                    FunctionalCalendar.getPastDay(20),
                    FunctionalCalendar.getPastHour(20),
                    FunctionalCalendar.getPastMinute(20),
                    0, 0, ZoneId.of("America/New_York")),
                    null,
                    null,
                    1,
                    BarTimePeriod.MINUTE,
                    BarAdjustment.SPLIT,
                    BarFeed.SIP).getBars();
        } catch (AlpacaClientException exception) {
            exception.printStackTrace();
            return null;
        }
    }
    

    public static BigDecimal calculateSD(List<StockBar> stockBars) {
        double sum = 0.0, standardDeviation = 0.0;
        int length = stockBars.size();

        for (int i = 0; i < length; i++) {
            sum += stockBars.get(i).getClose();
        }
        double mean = sum / length;

        for (int i = 0; i < length; i++) {
            standardDeviation += Math.pow(stockBars.get(i).getClose() - mean, 2);
        }
        return BigDecimal.valueOf(Math.sqrt(standardDeviation / length));
    }

    // calculates using typical price
    public static BigDecimal simpleMovingAverage(List<StockBar> stockBars) {
        BigDecimal sma = BigDecimal.ZERO;
        for (int i = 0; i < stockBars.size(); i++)
            sma = sma.add(BigDecimal.valueOf(stockBars.get(i).getClose()));

        return sma.divide(BigDecimal.valueOf(stockBars.size()), MathContext.DECIMAL32);
    }

    public static BigDecimal upperBollingerBand(List<StockBar> stockBars, int start, int end) {
        BigDecimal sma = simpleMovingAverage(stockBars);
        sma = sma.add(calculateSD(stockBars));
        return sma.add((calculateSD(stockBars)));
    }

    public static BigDecimal lowerBollingerBand(List<StockBar> stockBars) {
        BigDecimal sma = simpleMovingAverage(stockBars);
        sma = sma.subtract(calculateSD(stockBars));
        return sma.subtract(calculateSD(stockBars));
    }

    public static BigDecimal calculateEma(List<StockBar> stockBars) {
        BigDecimal initEma;
        List<StockBar> trimmedStockBars = stockBars.subList(0, stockBars.size() - 1);
        initEma = simpleMovingAverage(trimmedStockBars);

        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (stockBars.size() + 1));

        return (BigDecimal.valueOf(stockBars.get(stockBars.size() - 1).getClose()).multiply(multiplier))
                .add(initEma.multiply((BigDecimal.ONE.subtract(multiplier))));

    }

    // must be given stockbar list containg at least 26 periods
    public static BigDecimal calculateMACD(List<StockBar> stockBars) {
        List<StockBar> trimmedStockBars = stockBars.subList(stockBars.size() - 26, stockBars.size());
        BigDecimal longEMA = calculateEma(trimmedStockBars);

        trimmedStockBars = stockBars.subList(stockBars.size() - 13, stockBars.size());
        BigDecimal shortEMA = calculateEma(trimmedStockBars);
        return shortEMA.subtract(longEMA);
    }

    // must be given stockbar list containing at least 35 periods
    public static BigDecimal signalLine(List<StockBar> stockBars) {
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (9 + 1));
        BigDecimal macdAverage = BigDecimal.ZERO;
        List<StockBar> stockBar;
        for (int i = 8; i > 0; i--) {
            stockBar = stockBars.subList(0, stockBars.size() - i);
            macdAverage = macdAverage.add(calculateMACD(stockBar));
        }
        macdAverage = macdAverage.divide(BigDecimal.valueOf(8), MathContext.DECIMAL32);
        macdAverage = macdAverage.multiply((BigDecimal.ONE.subtract(multiplier)));
        return calculateMACD(stockBars).multiply(multiplier).add(macdAverage);
    }

}
