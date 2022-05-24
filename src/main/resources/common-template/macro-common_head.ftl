<#--    Solo - A small and beautiful blogging system written in Java.    Copyright (c) 2010-present, b3log.org    Solo is licensed under Mulan PSL v2.    You can use this software according to the terms and conditions of the Mulan PSL v2.    You may obtain a copy of Mulan PSL v2 at:            http://license.coscl.org.cn/MulanPSL2    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.    See the Mulan PSL v2 for more details.--><#macro head title description=''><meta charset="utf-8"/><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0"/><meta name="theme-color" content="#3b3e43"><meta name="apple-mobile-web-app-capable" content="yes"><meta name="mobile-web-app-capable" content="yes"/><meta name="apple-mobile-web-app-status-bar-style" content="black"><meta name="format-detection" content="telephone=no"/><title>${title}</title>    <#if description != ''><meta name="description" content="${description}"/><meta property="og:description" content="${description}"/>    <#elseif metaDescription??><meta name="description" content="${metaDescription}"/><meta property="og:description" content="${metaDescription}"/>    <#else>    <#if !blogSubtitle??>        <#assign blogSubtitle = ''>    </#if><meta name="description" content="${blogTitle?html}。${blogSubtitle?html}"/><meta property="og:description" content="${blogTitle?html}。${blogSubtitle?html}"/>    </#if>    <#if metaKeywords??>    <meta name="keywords" content="${metaKeywords}"/>    </#if><link rel="dns-prefetch" href="${staticServePath}"/><link rel="dns-prefetch" href="https://unpkg.com"/><link rel="preconnect" href="${staticServePath}"><link rel="icon" type="image/png" href="${faviconURL}"/><link rel="apple-touch-icon" href="${faviconURL}"><link rel="shortcut icon" type="image/x-icon" href="${faviconURL}"><meta name="copyright" content="B3log"/><meta http-equiv="Window-target" content="_top"/><meta property="og:locale" content="${langLabel}"/><meta property="og:title" content="${title}"/><meta property="og:site_name" content="${blogTitle?html}"/><meta property="og:url"      content="${servePath}${request.requestURI}<#if request.queryString??>?${request.queryString}</#if>"/><meta property="og:image" content="${faviconURL}"/><link rel="search" type="application/opensearchdescription+xml" title="${title}" href="/opensearch.xml"><link href="${servePath}/rss.xml" title="RSS" type="application/rss+xml" rel="alternate"/><link rel="manifest" href="${servePath}/manifest.json">    <#if paginationCurrentPageNum??>        <#if paginationCurrentPageNum == 1>            <link rel="canonical" href="${servePath}${request.requestURI}">        <#else>            <link rel="canonical" href="${servePath}${request.requestURI}<#if request.queryString??>?${request.queryString}</#if>">        </#if>    <#else>        <link rel="canonical" href="${servePath}${request.requestURI}">    </#if>    <#nested><#if htmlHead??>    ${htmlHead}</#if><script src="https://unpkg.com/vditor@3.8.4/dist/js/icons/ant.js" async="" id="vditorIconScript"></script></#macro>