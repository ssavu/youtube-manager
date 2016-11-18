(function(){
   function capitalize() {
       return function(value){
           return value != null ? value.slice(0,1).toUpperCase() + value.slice(1).toLowerCase() : value;
       }
   }

   angular
       .module('youtubeManager')
       .filter('capitalizeFirst', [capitalize]);
})();