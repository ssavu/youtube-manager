(function () {
    'use strict';

    angular
        .module('youtubeManager')
        .filter('translate', translateFilter);

    translateFilter.$inject = ['i18nService'];
    function translateFilter(i18n) {
        return function (input) {
            return i18n.message(input);
        };
    }
})();