package io.github.thepun.fix.multitool.cases;

import io.github.thepun.fix.multitool.CaseContext;
import io.github.thepun.fix.multitool.MultiToolCase;
import io.github.thepun.fix.multitool.Output;
import io.github.thepun.fix.multitool.Params;
import io.github.thepun.fix.multitool.client.fix.FixConnection;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MassQuote;

public class AcceptPrimeXmAndSendPrices implements MultiToolCase {

    @Override
    public void execute(Params params, Output out) throws FieldNotFound, InterruptedException {
        FixConnection fixSession = FixConnection.accept(params.getSubgroup("local.primexm.fix"));

        double price = 1.0;
        String subscription;
        for (;;) {
            MarketDataRequest marketDataRequest = fixSession.read(MarketDataRequest.class);
            Group group = marketDataRequest.getGroups(NoRelatedSym.FIELD).get(0);
            String symbol = group.getString(Symbol.FIELD);
            if (symbol.equals("EURUSD")) {
                subscription = marketDataRequest.getString(MDReqID.FIELD);
                break;
            }
        }

        while (CaseContext.get().isRunning()) {
            price = price + 0.1;

            NoQuoteEntries quote = new NoQuoteEntries();
            quote.setField(new QuoteEntryID("0"));
            quote.setField(new BidSize(100000));
            quote.setField(new OfferSize(10000));
            quote.setField(new BidSpotRate(price));
            quote.setField(new OfferSpotRate(price));

            MassQuote.NoQuoteSets quoteSet = new MassQuote.NoQuoteSets();
            quoteSet.set(new QuoteSetID(subscription));
            quoteSet.addGroup(quote);

            MassQuote massQuote = new MassQuote();
            massQuote.addGroup(quoteSet);

            fixSession.write(massQuote);

            Thread.sleep(5000);
        }
    }


    public static class NoQuoteEntries extends Group {

        static final long serialVersionUID = 20050617;
        private static final int[] ORDER = {299, 55, 65, 48, 22, 454, 460, 461, 167, 762, 200, 541, 201, 224, 225, 239, 226, 227, 228, 255, 543, 470, 471, 472, 240, 202, 947, 206, 231, 223, 207, 106, 348, 349, 107, 350, 351, 691, 667, 875, 876, 864, 873, 874, 555, 132, 133, 134, 135, 62, 188, 190, 189, 191, 631, 632, 633, 634, 60, 336, 625, 64, 40, 193, 192, 642, 643, 15, 0};

        public NoQuoteEntries() {
            super(295, 299, ORDER);
        }
    }
}
