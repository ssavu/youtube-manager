(function() {
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
})();