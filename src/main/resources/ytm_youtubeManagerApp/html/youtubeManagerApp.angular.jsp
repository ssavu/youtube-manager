<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="angular/components/ytm-scripts.js"/>
<template:addResources type="css" resources="youtubemanager/app.css"/>

<div ui-view id="youtube_manager_${currentNode.identifier}"></div>

<script>
    (function() {
        var contextData = {};
        contextData.context = '${url.context}';
        contextData.serverBase = '${url.baseEdit}';
        contextData.sitePath = '${renderContext.site.path}';
        contextData.locale = '${renderContext.UILocale.language}';
        contextData.language = '${renderContext.mainResourceLocale.language}';
        contextData.moduleBase =  '${url.currentModule}';
        contextData.filesBase =  '${url.files}';
        contextData.siteId = '${renderContext.site.identifier}';
        contextData.siteName = '${renderContext.site.name}';
        contextData.jcrRestAPIBase =  '/modules/api/jcr/v1';
        contextData.moduleVersion = '${script.view.moduleVersion}';
        contextData.apiBase = contextData.context + '/modules/youtube-manager';
        contextData.jsBase = contextData.context + contextData.moduleBase + '/javascript/angular/components/youtubemanager';

        var redirectedPath = $(this)[0].parent.parent.window.location.hash;
        contextData.authCodeResponse = !_.isEmpty(redirectedPath) ? redirectedPath.replace('#', '') : null;
        var tempAuthCode = $(this)[0].parent.parent.window.location.href;
        var tempAuthCodeIndex = tempAuthCode.indexOf('code=');
        contextData.tempAuthCode = tempAuthCodeIndex != -1 ? tempAuthCode.substring(tempAuthCodeIndex + 5, tempAuthCode.length - 1) : null;

        angular.module('youtubeManager').constant('contextualData', contextData);

        angular.element(document).ready(function () {
            moment.locale('${renderContext.UILocale.language}');
            angular.bootstrap(document.getElementById('youtube_manager_${currentNode.identifier}'), ['youtubeManager']);
        });
    })();
</script>

