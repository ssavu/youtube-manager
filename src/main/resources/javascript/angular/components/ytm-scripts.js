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
(function(){
   function capitalize() {
       return function(value){
           return value != null ? value.slice(0,1).toUpperCase() + value.slice(1).toLowerCase() : value;
       }
   }

   angular
       .module('youtubeManager')
       .filter('capitalizeFirst', [capitalize]);
})();(function () {
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
})();(function () {
    'use strict';

    angular
        .module('youtubeManager')
        .directive('messageKey', messageKeyDirective);

    messageKeyDirective.$inject = ['i18nService'];
    function messageKeyDirective(i18nService) {
        return {
            restrict: 'A',
            link: function ($scope, $element, $attrs) {
                var i18n;
                if (!$attrs.messageParams) {
                    i18n = i18nService.message($attrs.messageKey);
                } else {
                    i18n = i18nService.format($attrs.messageKey, $attrs.messageParams);
                }

                if ($attrs.messageAttr) {
                    // store the i18n in the specified element attr
                    $element.attr($attrs.messageAttr, i18n);
                } else {
                    // set the i18n as element text
                    $element.text(i18n);
                }
            }
        };
    }
})();(function () {
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
})();(function() {
   function managerDirective(contextualData) {
        return {
            restrict: 'E',
            scope: {},
            templateUrl: contextualData.jsBase + '/manager/manager.html',
            controller: ManagerController,
            controllerAs: 'mc',
            bindToController: true,
            link:linkFunc
        };

        function linkFunc(scope, el, attr, ctrl) {

        }
    }

    angular
        .module('youtubeManager')
        .directive('ytmManager', ['contextualData', managerDirective]);

    var ManagerController = function(contextualData, $http, VUS) {
        var mc = this;
        mc.videoDetails = {};
        mc.videoDetails.tags = [];
        mc.currentUploadingVideos = {};

        mc.requestAccessToken = requestAccessToken;
        mc.uploadVideo = uploadVideo;
        mc.clearVideoFromList = clearVideoFromList;

        //TEMP
        mc.resetToken = resetToken;

        if (contextualData.tempAuthCode != null) {
            $http({
                url: contextualData.apiBase + '/playlist/authorize/process/' + contextualData.siteId,
                method: 'POST',
                data: {
                    code: contextualData.tempAuthCode
                }
            }).then(function(response){
                initVideoUpload();
            });
        } else {
            $http({
                url: contextualData.apiBase + '/playlist/authorize/verify/' + contextualData.locale +  '/' + contextualData.siteId,
                method: 'GET'
            }).then(function(response) {
                console.log(response);
                if (response.data.authorized) {
                    $http({
                        url: contextualData.apiBase + '/playlist/authorize/reinitialize/' + contextualData.locale +  '/' + contextualData.siteId,
                        method: 'POST'
                    }).then(function(response){
                        if (response.data.result) {
                            initVideoUpload();
                        } else {
                            initRequireToken();
                        }
                    });
                } else {
                    //Request the auth code so that we can obtain an access token.
                    initRequireToken();
                }
            });
        }
        function initVideoUpload() {
            //Retrieve video options
            $http({
                url: contextualData.apiBase + '/playlist/video/formats',
                method: 'GET'
            }).then(function(response){
                mc.videoFormats = response.data.formats;
            });

            $http({
                url: contextualData.apiBase + '/playlist/video/statustypes',
                method: 'GET'
            }).then(function(response){
                mc.videoStatusTypes = response.data.statustypes;
            });
        }
        function uploadVideo(file) {
            if (file == null) {
                return;
            }
            mc.videoDetails.uuid = generateUUID();
            mc.videoDetails.size = file.size;
            var data = new FormData();
            data.append('file', file);
            data.append('videoDetails', JSON.stringify(mc.videoDetails));
            VUS.registerVideoUploadProgressRequest(mc.videoDetails.uuid, notifyUploadProgress);
            mc.currentUploadingVideos[mc.videoDetails.uuid] = {
                total: 100,
                current: 0,
                title: mc.videoDetails.title
            };
            mc.videoDetails = {};
            mc.videoDetails.tags = [];
            $http({
                url: contextualData.apiBase + '/playlist/video/upload/' + contextualData.locale + '/' + contextualData.siteId,
                method: 'POST',
                headers: {
                    'Content-Type': undefined
                },
                data: data,
                transformRequest: angular.identity
            }).then(function(response) {});
        }

        function requestAccessToken(file) {
            if (file) {
                $http({
                    url: contextualData.apiBase + '/playlist/authorize/initialize/' + contextualData.locale +  '/' + contextualData.siteId + '/' + mc.selectedScopes,
                    method: 'POST',
                    data: file
                }).then(function(response){
                    if (response.data.result) {
                        window.parent.location.href = response.data.result;
                    }
                });
            }
        }

        function initRequireToken () {
            mc.askRequestToken = true;
            $http({
                url: contextualData.apiBase + '/playlist/scopes',
                method: 'GET'
            }).then(function(response){
                mc.scopeTypes = response.data.scopes;
            });
        }

        function resetToken() {
            $http({
                url: contextualData.apiBase + '/playlist/reset/' + contextualData.siteId,
                method: 'GET'
            }).then(function(){
               window.location.href = window.location.href;
            });
        }

        //Just for purposes of prototype
        function generateUUID() {
            return (new Date().getMilliseconds() +(Math.random() * 100 / Math.random()) + '').replace('.', '');
        }

        function notifyUploadProgress(videoInfo) {
            mc.currentUploadingVideos[videoInfo.uuid].current = videoInfo.uploadComplete ? 100 : videoInfo.progress * 100;
            mc.currentUploadingVideos[videoInfo.uuid].uploadComplete = videoInfo.uploadComplete;
        }

        function clearVideoFromList(uuid) {
            console.log(mc.currentUploadingVideos[uuid]);
            delete mc.currentUploadingVideos[uuid];
        }
    };

    ManagerController.$inject = ['contextualData', '$http', 'videoUploadService'];
})();(function() {
    var videoUpload = function($http, contextualData, $interval) {

        this.registerVideoUploadProgressRequest = registerVideoUploadProgressRequest;
        this.deRegisterVideoUploadProgressRequest = deRegisterVideoUploadProgressRequest;

        var registeredVideoListeners = {};
        var requestStatus = 0;
        var intervalPromise = null;

        function registerVideoUploadProgressRequest(videoUUID, callback) {
            registeredVideoListeners[videoUUID] = callback;
            if (requestStatus == 0) {
                requestStatus = 1;
                intervalPromise = $interval(requestVideoInfo, 2000);
            }
        }

        function deRegisterVideoUploadProgressRequest(videoUUID) {
            delete registeredVideoListeners[videoUUID];
            $http({
                url: contextualData.apiBase + '/playlist/video/progress/clear/' + videoUUID,
                method: 'POST'
            });
            if (_.isEmpty(registeredVideoListeners)) {
                $interval.cancel(intervalPromise);
                requestStatus = 0;
            }
        }
        function requestVideoInfo() {
            var videoIds = _.keys(registeredVideoListeners);
            $http({
                url: contextualData.apiBase + '/playlist/video/progress',
                method: 'POST',
                data: videoIds
            }).then(function(response) {
                var videos = response.data.result;
                if (videos) {
                    for (var i in registeredVideoListeners) {
                        if (videos[i] != null) {
                            videos[i].uuid = i;
                            registeredVideoListeners[i](videos[i]);
                            if (videos[i].uploadComplete) {
                                deRegisterVideoUploadProgressRequest(i);
                            }
                        }
                    }
                }
            });
        }
    };
    videoUpload.$inject = ['$http', 'contextualData', '$interval'];

    angular
        .module('youtubeManager')
        .service('videoUploadService', videoUpload);
})();(function () {
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
