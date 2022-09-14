package com.stock.dividend;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOError;
import java.io.IOException;

@SpringBootApplication
public class DividendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DividendApplication.class, args);

        try {

            Connection connection = Jsoup.connect("https://finance.yahoo.com/quote/COKE/history?period1=99100800&period2=1663113600&interval=1mo&filter=history&frequency=1mo&includeAdjustedClose=true");
            Document document = connection.get();

            Elements elements = document.getElementsByAttributeValue("data-test", "historical-prices");
            Element element = elements.get(0);  // 몇 번째에 있는 element를 가져올 건지 정하는 부분

            Element tbody = element.children().get(1);
            for (Element e : tbody.children()) {
                String s = e.text();
                if (!s.endsWith("Dividend")) {
                    continue;
                }

                String[] splits = s.split(" ");
                String month = splits[0];
                int day = Integer.parseInt(splits[1].replace(",", ""));
                int year = Integer.parseInt(splits[2]);
                String dividend = splits[3];

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
