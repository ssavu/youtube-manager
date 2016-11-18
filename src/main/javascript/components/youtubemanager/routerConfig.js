(function () {
    'use strict';

    angular.module('youtubeManager').config(function ($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise(function ($injector, $location) {
            var $state = $injector.get('$state');
            if ($location.$$state == null) {
                $state.go('youtubeManager', {}, {
                    location: false
                });
            }
        });

        $stateProvider
            .state('youtubeManager', {
                url: '/youtube-manager',
                template: '<ytm-manager>'
            })
    }, ['$stateProvider', '$urlRouterProvider']);
})();
