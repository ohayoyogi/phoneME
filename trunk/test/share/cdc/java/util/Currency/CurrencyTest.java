/*
 * Copyright 1990-2006 Sun Microsystems, Inc. All Rights Reserved. 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 only,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included at /legal/license.txt).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 or visit www.sun.com if you need additional information or have
 * any questions.
 */

/*
 * @test @(#)CurrencyTest.java	1.4 05/03/13
 * @bug 4290801
 * @summary Basic tests for Currency class.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


public class CurrencyTest {

    // save a copy of available locales
    static Locale[] supportedLocales;

    public static void main(String[] args) throws Exception {
        testCurrencyCodeValidation();
        testLocaleMapping();
        testSymbols();
        testFractionDigits();
        testSerialization();
    }
    
    static void testCurrencyCodeValidation() {
        // test creation of some valid currencies
        testValidCurrency("USD");
        testValidCurrency("EUR");
        testValidCurrency("GBP");
        testValidCurrency("JPY");
        testValidCurrency("CNY");
        testValidCurrency("CHF");
        
        // test creation of some fictitious currencies
        testInvalidCurrency("AQD");
        testInvalidCurrency("US$");
        testInvalidCurrency("\u20AC");
    }
    
    static void testValidCurrency(String currencyCode) {
        Currency currency1 = Currency.getInstance(currencyCode);
        Currency currency2 = Currency.getInstance(currencyCode);
        if (currency1 != currency2) {
            throw new RuntimeException("Didn't get same instance for same currency code");
        }
        if (!currency1.getCurrencyCode().equals(currencyCode)) {
            throw new RuntimeException("Currency code changed");
        }
    }
    
    static void testInvalidCurrency(String currencyCode) {
        boolean gotException = false;
        try {
            Currency currency = Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        if (!gotException) {
            throw new RuntimeException("didn't get specified exception");
        }
    }
    
    static void testLocaleMapping() {
        // very basic test: most countries have their own currency, and then
        // their currency code is an extension of their country code.
        Locale[] locales = Locale.getAvailableLocales();
        int goodCountries = 0;
        int ownCurrencies = 0;
        for (int i = 0; i < locales.length; i++) {
            Locale locale = locales[i];
            if (locale.getCountry().length() == 0) {
                boolean gotException = false;
                try {
                    Currency.getInstance(locale);
                } catch (IllegalArgumentException e) {
                    gotException = true;
                }
                if (!gotException) {
                    throw new RuntimeException("didn't get specified exception");
                }
            } else {
                goodCountries++;
                Currency currency = Currency.getInstance(locale);
                if (currency.getCurrencyCode().indexOf(locale.getCountry()) == 0) {
                    ownCurrencies++;
                }
            }
        }
        System.out.println("Countries tested: " + goodCountries +
                ", own currencies: " + ownCurrencies);
        if (ownCurrencies < (goodCountries / 2 + 1)) {
            throw new RuntimeException("suspicious: not enough countries have their own currency.");
        }
        
        // check a few countries that don't change their currencies too often
        String[] country1 = {"US", "CA", "JP", "CN", "SG", "CH"};
        String[] currency1 = {"USD", "CAD", "JPY", "CNY", "SGD", "CHF"};
        for (int i = 0; i < country1.length; i++) {
            checkCountryCurrency(country1[i], currency1[i]);
        }

        // check a few countries that are known to change their currencies soon
        String[] country2 = {"DE", "FR", "ES", "IT", "NL", "BE"};
        String[] currency2 = {"DEM", "FRF", "ESP", "ITL", "NLG", "BEF"};
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
        Calendar date = new GregorianCalendar(2002, Calendar.JANUARY, 1);
        long switchOver = date.getTime().getTime();
        boolean switchedOver = System.currentTimeMillis() >= switchOver;
        for (int i = 0; i < country2.length; i++) {
            checkCountryCurrency(country2[i],
                    switchedOver ? "EUR" : currency2[i]);
        }
        
        // check a country code which doesn't have a currency
        checkCountryCurrency("AQ", null);
        
        // check an invalid country code
        boolean gotException = false;
        try {
            Currency.getInstance(new Locale("", "EU"));
        } catch (IllegalArgumentException e) {
            gotException = true;
        }
        if (!gotException) {
            throw new RuntimeException("didn't get specified exception.");
        }

        // save the locale list for other tests to use
        supportedLocales = new Locale[locales.length];
        System.arraycopy((Object)locales, 0,
                         (Object)supportedLocales, 0, locales.length);

    }
    
    static void checkCountryCurrency(String countryCode, String expected) {
        Locale locale = new Locale("", countryCode);
        Currency currency = Currency.getInstance(locale);
        String code = (currency != null) ? currency.getCurrencyCode() : null;
        if (!(expected == null ? code == null : expected.equals(code))) {
            throw new RuntimeException("Wrong currency for " +
                    locale.getDisplayCountry() +
                    ": expected " + expected + ", got " + code);
        }
    }
    
    static void testSymbols() {
        testSymbol("USD", Locale.US, "$");
        testSymbol("EUR", Locale.GERMANY, "\u20AC");
        testSymbol("USD", Locale.PRC, "USD");
    }
    
    static void testSymbol(String currencyCode, Locale locale, String expectedSymbol) {
        if (supportedLocales == null)
           supportedLocales = Locale.getAvailableLocales();

        for (int i = 0; i < supportedLocales.length; i++) {
           if (supportedLocales[i].getCountry().equals(locale.getCountry())) {
              String symbol = Currency.getInstance(currencyCode).getSymbol(locale);
              if (!symbol.equals(expectedSymbol)) {
                  throw new RuntimeException("Wrong symbol for currency " +
                     currencyCode +": expected " + expectedSymbol +
                     ", got " + symbol);
              }
              return;
           }
        }
        System.out.println(locale + " not supported, ignoring a symbol test.");
    }
    
    static void testFractionDigits() {
        testFractionDigits("USD", 2);
        testFractionDigits("EUR", 2);
        testFractionDigits("JPY", 0);
        testFractionDigits("XDR", -1);
    }
    
    static void testFractionDigits(String currencyCode, int expectedFractionDigits) {
        int digits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        if (digits != expectedFractionDigits) {
            throw new RuntimeException("Wrong number of fraction digits for currency " +
                    currencyCode +": expected " + expectedFractionDigits +
                    ", got " + digits);
        }
    }
    
    static void testSerialization() throws Exception {
        Currency currency1 = Currency.getInstance("DEM");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oStream = new ObjectOutputStream(baos);
        oStream.writeObject(currency1);
        oStream.flush();
        byte[] bytes = baos.toByteArray();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream iStream = new ObjectInputStream(bais);
        Currency currency2 = (Currency) iStream.readObject();
        
        if (currency1 != currency2) {
            throw new RuntimeException("serialization breaks class invariant");
        }
    }       
}
