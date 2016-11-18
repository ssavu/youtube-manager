(function () {
    'use strict';

    angular
        .module('youtubeManager')
        .service('i18nService', i18nService);

    function i18nService() {
        this.message = function (key, fallback) {
            if (ytmi18n && ytmi18n[key]) {
                return ytmi18n[key];
            } else if (angular.isDefined(fallback)) {
                return fallback;
            } else {
                return '???' + key + '???';
            }
        };

        this.format = function (key, params) {
            var replacer = function (params) {
                return function (s, index) {
                    return params[index] ? (params[index] === '__void__' ? '' : params[index]) : '';
                };
            };

            if (params) {
                if (ytmi18n && ytmi18n[key]) {
                    return ytmi18n[key].replace(/\{(\w+)\}/g, replacer(params.split('|')));
                } else {
                    return '???' + key + '???';
                }
            } else {
                return this.message(key);
            }
        };
    }
})();