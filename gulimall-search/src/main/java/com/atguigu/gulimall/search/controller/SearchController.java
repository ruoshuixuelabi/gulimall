package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author admin
 */
@Controller
public class SearchController {
    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("list.html")
    public String listPage(SearchParam searchParam, Model model, HttpServletRequest servletRequest) {
        String queryString = servletRequest.getQueryString();
        searchParam.set_queryString(queryString);
        SearchResult searchResult = mallSearchService.search(searchParam);
        model.addAttribute("result", searchResult);
        return "list";
    }
}
