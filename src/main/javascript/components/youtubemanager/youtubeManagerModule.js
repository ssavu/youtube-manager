(function () {
    'use strict';

    angular.module('youtubeManager', [
        'ngMaterial',
        'ngFileUpload',
        'dndLists',
        'ui.router',
        'toaster',
        'angularSpectrumColorpicker'
    ])
        .config(function ($mdThemingProvider) {
            $mdThemingProvider.theme('default')
                .primaryPalette('blue-grey', {
                    'default': '500',
                    'hue-1': '200',
                    'hue-2': '700',
                    'hue-3': '900'
                })
                .accentPalette('deep-orange')
                .warnPalette('red');
        });
})();
