(function() {
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
})();