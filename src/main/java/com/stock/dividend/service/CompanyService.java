package com.stock.dividend.service;

import com.stock.dividend.exception.impl.NoCompanyException;
import com.stock.dividend.model.Company;
import com.stock.dividend.model.ScrapedResult;
import com.stock.dividend.persist.CompanyRepository;
import com.stock.dividend.persist.DividendRepository;
import com.stock.dividend.persist.entity.CompanyEntity;
import com.stock.dividend.persist.entity.DividendEntity;
import com.stock.dividend.scraper.Scraper;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyService {

    private final Trie trie;
    private final Scraper yahooFinanceScraper;

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    public Company save(String ticker) {
        boolean exists = this.companyRepository.existsByTicker(ticker);
        if (exists) {
            throw new RuntimeException("already exists ticker -> " + ticker);
        }

        return this.storeCompanyAndDividend(ticker);
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return this.companyRepository.findAll(pageable);
    }

    private Company storeCompanyAndDividend(String ticker) {

        // ticker 를 기준으로 회사를 스크래핑
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);
        if (ObjectUtils.isEmpty(company)) {
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }

        // 해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

        // 스크래핑 결과
        CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntityList = scrapedResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());

        this.dividendRepository.saveAll(dividendEntityList);

        return company;
    }

    /**
     * LIKE연산을 이용한 방법으로 구현이 TRIE를 이용하는 것보다 간단
     * 하지만 데이터베이스에서 데이터를 찾으므로 DB에 부하를 줄 수 있음
     * 데이터의 양, 크기와 해당 연산이 발생하는 비용 등을 계산해서 DB에 부하를 주지 않을 정도라면 사용해도 되나
     * DB에 부하가 많이 가는 경우에는 지양해야 함
     */
    public List<String> getCompanyNamesByKeword(String keyword) {
        Pageable limit = PageRequest.of(0, 10);
        Page<CompanyEntity> companyEntities = this.companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);
        return companyEntities.stream()
                .map(e -> e.getName())
                .collect(Collectors.toList());
    }

    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    /**
     * trie를 쓰기 위한 메모리가 필요하고, 데이터를 찾는 연산 또한 서버에서 이루어짐
     */
    public Object autocomplete(String keyword) {
        return this.trie.prefixMap(keyword).keySet()
                .stream()
                //.limit(10)
                .collect(Collectors.toList());
    }

    public void deleteAutocompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        var company = this.companyRepository.findByTicker(ticker)
                                            .orElseThrow(() -> new NoCompanyException());

        // 해당 회사의 저장된 배당금도 지워준다.
        this.dividendRepository.deleteAllByCompanyId(company.getId());
        this.companyRepository.delete(company);

        // trie에 있는 데이터도 지움
        this.deleteAutocompleteKeyword(company.getName());
        return company.getName();
    }
}